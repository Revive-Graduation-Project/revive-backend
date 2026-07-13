#!/usr/bin/env node
/**
 * ══════════════════════════════════════════════════════════════════════
 *  Revive Restaurant — Production Seed Script
 * ══════════════════════════════════════════════════════════════════════
 *
 *  Strategy: Hit every real API endpoint through the Gateway, so all
 *  business logic, validation, and event publishing runs exactly as it
 *  would in production.  Nothing is inserted directly into the DB.
 *
 *  Dependency order:
 *    1. Login as admin  →  get JWT
 *    2. Create ingredients (POST /api/ingredients/create)  ← see note *
 *    3. Create 10 meals  (POST /api/menu)
 *    4. Register 3 client users (POST /auth/signup)
 *    5. Login as each client  →  get their JWTs
 *    6. Place orders           (POST /api/orders)
 *
 *  * The IngredientController only exposes stock-update endpoints.
 *    Ingredients are normally created via the CSV import flow.
 *    For seeding we call the internal resolveOrSaveIngredient path by
 *    going through POST /api/inventory/import-json with a minimal
 *    JSON payload, which triggers the RabbitMQ → menu-service event
 *    that creates the ingredients in menu_db.
 *
 *    If you prefer, set SKIP_INGREDIENT_IMPORT=true and supply
 *    INGREDIENT_IDS as a JSON map (see CONFIG section).
 *
 * ══════════════════════════════════════════════════════════════════════
 *  USAGE
 * ══════════════════════════════════════════════════════════════════════
 *
 *   # Local Docker Compose:
 *   node seed.js
 *
 *   # Custom gateway URL:
 *   GATEWAY_URL=https://your-railway-gateway.up.railway.app node seed.js
 *
 *   # Skip ingredient import (if ingredients already exist):
 *   SKIP_INGREDIENT_IMPORT=true INGREDIENT_IDS='{"Chicken Breast":1,"Rice":2,...}' node seed.js
 *
 *   # Use a different admin account:
 *   ADMIN_EMAIL=himayasin00@gmail.com ADMIN_PASSWORD=admin node seed.js
 *
 * ══════════════════════════════════════════════════════════════════════
 *  REQUIREMENTS: Node.js 18+  (uses built-in fetch)
 * ══════════════════════════════════════════════════════════════════════
 */

// ─── CONFIG ─────────────────────────────────────────────────────────────────
const GATEWAY_URL   = (process.env.GATEWAY_URL || 'http://localhost:8080').replace(/\/$/, '');
const ADMIN_EMAIL   = process.env.ADMIN_EMAIL    || 'admin@revive.com';
const ADMIN_PASSWORD = process.env.ADMIN_PASSWORD || 'admin123';

// Set to true if you already have ingredients in the DB and know their IDs.
const SKIP_INGREDIENT_IMPORT = process.env.SKIP_INGREDIENT_IMPORT === 'true';

// If SKIP_INGREDIENT_IMPORT=true, provide this map: name → id
// e.g. '{"Chicken Breast":1,"Basmati Rice":2}'
let INGREDIENT_IDS = {};
try {
  if (process.env.INGREDIENT_IDS) {
    INGREDIENT_IDS = JSON.parse(process.env.INGREDIENT_IDS);
  }
} catch {
  console.error('❌ INGREDIENT_IDS env var is not valid JSON');
  process.exit(1);
}

// Import poll interval (ms) and max wait (ms)
const IMPORT_POLL_MS  = 3000;
const IMPORT_TIMEOUT_MS = 120_000;

// ─── HELPERS ─────────────────────────────────────────────────────────────────
const log  = (...a) => console.log('[SEED]', ...a);
const warn = (...a) => console.warn('[WARN]', ...a);
const err  = (...a) => { console.error('[ERROR]', ...a); };

async function api(method, path, body, token, extraHeaders = {}) {
  const url = `${GATEWAY_URL}${path}`;
  const headers = {
    'Content-Type': 'application/json',
    ...extraHeaders,
  };
  if (token) headers['Authorization'] = `Bearer ${token}`;

  const opts = { method, headers };
  if (body !== undefined) opts.body = JSON.stringify(body);

  const res = await fetch(url, opts);
  const text = await res.text();

  let data;
  try { data = JSON.parse(text); } catch { data = text; }

  if (!res.ok) {
    throw new Error(`${method} ${path} → HTTP ${res.status}: ${JSON.stringify(data)}`);
  }
  return data;
}

async function sleep(ms) {
  return new Promise(r => setTimeout(r, ms));
}

// ─── STEP 1: ADMIN LOGIN ─────────────────────────────────────────────────────
async function adminLogin() {
  log('🔐 Logging in as admin...');
  const res = await api('POST', '/auth/login', {
    email: ADMIN_EMAIL,
    password: ADMIN_PASSWORD,
  });
  log(`   ✅ Admin token obtained (role: ${res.role}, userId: ${res.userId})`);
  return res.token; // JWT
}

// ─── STEP 2: INGREDIENT IMPORT ───────────────────────────────────────────────
//
// The inventory-service /api/inventory/import-json endpoint accepts an array
// of MealCsvEntry objects, kicks off an async import job, processes them with
// the USDA API + AI normalization, then publishes a RabbitMQ event that the
// menu-management-service consumes to create Ingredient entities.
//
// For seeding purposes we send a minimal set of common restaurant ingredients
// packaged as a list of meals (one ingredient per "meal" entry is fine —
// the import pipeline just needs to see the ingredient names).
//
// Because the import is ASYNC (RabbitMQ) we poll the job status until it
// reaches COMPLETED or FAILED.

/**
 * Minimal CSV-entry shaped payloads.
 * Each entry lists ingredient names under "ingredients".
 * We group them into batches so the import job is manageable.
 */
const IMPORT_BATCHES = [
  // Batch 1 – proteins & dairy
  [
    { mealName: 'Ingredient Batch 1', category: 'Main', price: 1, description: 'Seed batch', ingredients: [
      { name: 'Chicken Breast', quantity: 150, unit: 'g' },
      { name: 'Minced Beef',    quantity: 150, unit: 'g' },
      { name: 'Salmon Fillet',  quantity: 150, unit: 'g' },
      { name: 'Eggs',           quantity: 60,  unit: 'g' },
      { name: 'Mozzarella Cheese', quantity: 50, unit: 'g' },
      { name: 'Feta Cheese',    quantity: 50,  unit: 'g' },
    ]},
  ],
  // Batch 2 – carbs & vegetables
  [
    { mealName: 'Ingredient Batch 2', category: 'Main', price: 1, description: 'Seed batch', ingredients: [
      { name: 'Basmati Rice',   quantity: 100, unit: 'g' },
      { name: 'Penne Pasta',    quantity: 100, unit: 'g' },
      { name: 'Burger Bun',     quantity: 80,  unit: 'g' },
      { name: 'Pita Bread',     quantity: 60,  unit: 'g' },
      { name: 'Tomato',         quantity: 80,  unit: 'g' },
      { name: 'Lettuce',        quantity: 40,  unit: 'g' },
      { name: 'Onion',          quantity: 40,  unit: 'g' },
      { name: 'Cucumber',       quantity: 50,  unit: 'g' },
    ]},
  ],
  // Batch 3 – sauces & extras
  [
    { mealName: 'Ingredient Batch 3', category: 'Side', price: 1, description: 'Seed batch', ingredients: [
      { name: 'Olive Oil',      quantity: 15,  unit: 'ml' },
      { name: 'Tomato Sauce',   quantity: 80,  unit: 'g' },
      { name: 'Garlic',         quantity: 10,  unit: 'g' },
      { name: 'Mixed Herbs',    quantity: 5,   unit: 'g' },
      { name: 'Caesar Dressing',quantity: 30,  unit: 'ml' },
      { name: 'French Fries',   quantity: 120, unit: 'g' },
      { name: 'Sesame Seeds',   quantity: 5,   unit: 'g' },
    ]},
  ],
];

async function pollImportJob(jobId, adminToken) {
  const deadline = Date.now() + IMPORT_TIMEOUT_MS;
  while (Date.now() < deadline) {
    await sleep(IMPORT_POLL_MS);
    const job = await api('GET', `/api/inventory/import-status/${jobId}`, undefined, adminToken);
    log(`   ⏳ Import job ${jobId}: ${job.status} (${job.processedRecords}/${job.totalRecords})`);
    if (job.status === 'COMPLETED') return job;
    if (job.status === 'FAILED')    throw new Error(`Import job failed: ${job.message}`);
  }
  throw new Error(`Import job ${jobId} timed out after ${IMPORT_TIMEOUT_MS / 1000}s`);
}

async function importIngredients(adminToken) {
  if (SKIP_INGREDIENT_IMPORT) {
    log('⏭️  Skipping ingredient import (SKIP_INGREDIENT_IMPORT=true)');
    return;
  }

  log('\n📦 Step 2: Importing ingredients via inventory-service...');
  log('   Note: This triggers async USDA + AI lookup → RabbitMQ → menu-service');
  log('   Estimated time: 60–120 s per batch\n');

  const adminHeaders = { 'X-User-Role': 'ADMIN' };

  for (let i = 0; i < IMPORT_BATCHES.length; i++) {
    const batch = IMPORT_BATCHES[i];
    log(`   📤 Starting import batch ${i + 1}/${IMPORT_BATCHES.length}...`);

    const res = await api('POST', '/api/inventory/import-json', batch, adminToken, adminHeaders);
    const jobId = res.jobId;
    log(`   🔖 Job ID: ${jobId}`);

    await pollImportJob(jobId, adminToken);
    log(`   ✅ Batch ${i + 1} completed\n`);

    // Small pause between batches to avoid overwhelming RabbitMQ
    if (i < IMPORT_BATCHES.length - 1) await sleep(5000);
  }

  log('✅ All ingredient batches imported. Waiting 10s for menu-service to process events...');
  await sleep(10_000);
}

// ─── STEP 3: FETCH INGREDIENT IDs ────────────────────────────────────────────
async function fetchIngredientIds(adminToken) {
  if (SKIP_INGREDIENT_IMPORT && Object.keys(INGREDIENT_IDS).length > 0) {
    log('\n📋 Using provided INGREDIENT_IDS map');
    return INGREDIENT_IDS;
  }

  log('\n🔍 Step 3: Fetching ingredient IDs from menu-service...');
  const ingredients = await api('GET', '/api/ingredients', undefined, adminToken);
  const idMap = {};
  for (const ing of ingredients) {
    idMap[ing.name] = ing.id;
  }
  log(`   Found ${ingredients.length} ingredients`);
  if (ingredients.length === 0) {
    throw new Error(
      'No ingredients found! Either the import failed or it hasn\'t propagated yet.\n' +
      'Try running again with SKIP_INGREDIENT_IMPORT=false, or wait a few minutes and re-run.'
    );
  }
  return idMap;
}

// ─── STEP 4: CREATE MEALS ─────────────────────────────────────────────────────
//
// Prices are in Egyptian Pounds (EGP), calibrated to realistic 2024 market rates.
// A typical Egyptian fast-food meal costs 80–200 EGP.

function buildMeals(idMap) {
  // Helper: look up an ingredient ID, warn & skip if not found
  function ing(name, quantity) {
    const id = idMap[name];
    if (!id) {
      warn(`   Ingredient "${name}" not found in DB — skipping from this meal`);
      return null;
    }
    return { ingredientId: id, quantity };
  }

  function compact(arr) {
    return arr.filter(Boolean);
  }

  return [
    // ── BURGERS ────────────────────────────────────────────────────────
    {
      name: 'Classic Smash Burger',
      description: 'Juicy smashed beef patty with lettuce, tomato, and our signature sauce on a toasted brioche bun',
      price: 129.0,
      category: 'Burgers',
      ingredients: compact([
        ing('Minced Beef',   180),
        ing('Burger Bun',    90),
        ing('Lettuce',       40),
        ing('Tomato',        60),
        ing('Onion',         30),
        ing('Sesame Seeds',  5),
      ]),
    },
    {
      name: 'Crispy Chicken Burger',
      description: 'Golden-fried chicken breast fillet with pickles, coleslaw, and spicy mayo',
      price: 119.0,
      category: 'Burgers',
      ingredients: compact([
        ing('Chicken Breast', 180),
        ing('Burger Bun',     90),
        ing('Lettuce',        40),
        ing('Tomato',         60),
        ing('Sesame Seeds',   5),
      ]),
    },

    // ── GRILLS ─────────────────────────────────────────────────────────
    {
      name: 'Grilled Chicken Platter',
      description: 'Marinated grilled chicken breast served with basmati rice, salad, and garlic sauce',
      price: 145.0,
      category: 'Grills',
      ingredients: compact([
        ing('Chicken Breast', 250),
        ing('Basmati Rice',   150),
        ing('Tomato',         60),
        ing('Cucumber',       60),
        ing('Lettuce',        30),
        ing('Olive Oil',      15),
        ing('Garlic',         8),
        ing('Mixed Herbs',    5),
      ]),
    },
    {
      name: 'Kofta & Rice',
      description: 'Seasoned minced beef kofta skewers served over fluffy basmati rice with tahini sauce',
      price: 139.0,
      category: 'Grills',
      ingredients: compact([
        ing('Minced Beef',  200),
        ing('Basmati Rice', 150),
        ing('Onion',        40),
        ing('Garlic',       8),
        ing('Mixed Herbs',  6),
        ing('Tomato',       60),
      ]),
    },

    // ── PASTA ──────────────────────────────────────────────────────────
    {
      name: 'Chicken Penne Arrabbiata',
      description: 'Al-dente penne pasta tossed in spicy tomato arrabbiata sauce with grilled chicken strips',
      price: 115.0,
      category: 'Pasta',
      ingredients: compact([
        ing('Penne Pasta',    150),
        ing('Chicken Breast', 150),
        ing('Tomato Sauce',   120),
        ing('Garlic',         8),
        ing('Olive Oil',      15),
        ing('Mixed Herbs',    5),
      ]),
    },
    {
      name: 'Creamy Salmon Pasta',
      description: 'Pan-seared salmon fillet over penne in a rich cream sauce with capers and dill',
      price: 175.0,
      category: 'Pasta',
      ingredients: compact([
        ing('Salmon Fillet', 180),
        ing('Penne Pasta',   150),
        ing('Olive Oil',     15),
        ing('Garlic',        8),
        ing('Mixed Herbs',   5),
      ]),
    },

    // ── WRAPS / SANDWICHES ─────────────────────────────────────────────
    {
      name: 'Chicken Caesar Wrap',
      description: 'Grilled chicken strips, romaine lettuce, parmesan, and Caesar dressing in a warm tortilla wrap',
      price: 89.0,
      category: 'Wraps',
      ingredients: compact([
        ing('Chicken Breast',  150),
        ing('Pita Bread',      80),
        ing('Lettuce',         50),
        ing('Tomato',          40),
        ing('Caesar Dressing', 30),
        ing('Garlic',          5),
      ]),
    },
    {
      name: 'Beef & Cheese Shawarma',
      description: 'Slow-cooked seasoned beef with tomatoes, pickles, tahini, and garlic sauce in fresh pita bread',
      price: 95.0,
      category: 'Wraps',
      ingredients: compact([
        ing('Minced Beef',    160),
        ing('Pita Bread',     80),
        ing('Tomato',         60),
        ing('Onion',          30),
        ing('Garlic',         8),
        ing('Mozzarella Cheese', 30),
      ]),
    },

    // ── SALADS ─────────────────────────────────────────────────────────
    {
      name: 'Greek Feta Salad',
      description: 'Fresh tomatoes, cucumber, olives, red onion, and generous crumbled feta — drizzled with olive oil',
      price: 75.0,
      category: 'Salads',
      ingredients: compact([
        ing('Tomato',     120),
        ing('Cucumber',   100),
        ing('Onion',       40),
        ing('Feta Cheese', 80),
        ing('Olive Oil',   20),
        ing('Mixed Herbs',  3),
      ]),
    },

    // ── SIDES ──────────────────────────────────────────────────────────
    {
      name: 'Loaded Cheese Fries',
      description: 'Crispy golden fries smothered in melted mozzarella, topped with jalapeños and our house sauce',
      price: 69.0,
      category: 'Sides',
      ingredients: compact([
        ing('French Fries',      180),
        ing('Mozzarella Cheese',  60),
        ing('Tomato Sauce',       30),
        ing('Garlic',              5),
      ]),
    },
  ];
}

async function createMeals(adminToken, ingredientIdMap) {
  log('\n🍽️  Step 4: Creating 10 meals...');
  const meals = buildMeals(ingredientIdMap);
  const adminRoleHeader = { 'X-User-Role': 'ADMIN' };
  const createdMeals = [];

  for (const meal of meals) {
    if (meal.ingredients.length === 0) {
      warn(`   ⚠️  Skipping "${meal.name}" — no valid ingredients resolved`);
      continue;
    }
    try {
      const created = await api('POST', '/api/menu', meal, adminToken, adminRoleHeader);
      log(`   ✅ Created meal: ${created.name} (id=${created.id}, EGP ${created.price})`);
      createdMeals.push(created);
    } catch (e) {
      warn(`   ⚠️  Failed to create meal "${meal.name}": ${e.message}`);
    }
  }

  log(`\n   ${createdMeals.length}/${meals.length} meals created successfully`);
  return createdMeals;
}

// ─── STEP 5: CREATE CLIENT USERS ─────────────────────────────────────────────
const SEED_CLIENTS = [
  {
    email: 'omar.seed@revive.com',
    password: 'seed123456',
    firstName: 'Omar',
    lastName: 'Hassan',
    phoneNumber: '+201001234567',
    age: 27,
    gender: 'MALE',
    exercisesRegularly: true,
    height: 178.0, heightUnit: 'cm',
    weight: 78.0,  weightUnit: 'kg',
    goal: 'MAINTAIN_WEIGHT',
  },
  {
    email: 'nour.seed@revive.com',
    password: 'seed123456',
    firstName: 'Nour',
    lastName: 'Ahmed',
    phoneNumber: '+201112345678',
    age: 24,
    gender: 'FEMALE',
    exercisesRegularly: false,
    height: 163.0, heightUnit: 'cm',
    weight: 60.0,  weightUnit: 'kg',
    goal: 'LOSE_WEIGHT',
  },
  {
    email: 'youssef.seed@revive.com',
    password: 'seed123456',
    firstName: 'Youssef',
    lastName: 'Khalil',
    phoneNumber: '+201234567890',
    age: 32,
    gender: 'MALE',
    exercisesRegularly: true,
    height: 182.0, heightUnit: 'cm',
    weight: 88.0,  weightUnit: 'kg',
    goal: 'GAIN_MUSCLE',
  },
];

async function createClients() {
  log('\n👤 Step 5: Registering seed client users...');
  const clients = [];

  for (const client of SEED_CLIENTS) {
    try {
      await api('POST', '/auth/signup', client);
      log(`   ✅ Registered client: ${client.email}`);
      clients.push(client);
    } catch (e) {
      // 409 Conflict = already exists — not a problem for idempotency
      if (e.message.includes('409') || e.message.toLowerCase().includes('already')) {
        warn(`   ⚠️  Client ${client.email} already exists — continuing`);
        clients.push(client);
      } else {
        warn(`   ⚠️  Failed to register ${client.email}: ${e.message}`);
      }
    }
  }

  return clients;
}

async function loginClients(clients) {
  log('\n🔑 Logging in seed clients...');
  const authenticated = [];

  for (const client of clients) {
    try {
      const res = await api('POST', '/auth/login', {
        email: client.email,
        password: client.password,
      });
      authenticated.push({ ...client, token: res.token, userId: res.userId });
      log(`   ✅ Logged in: ${client.email} (userId=${res.userId})`);
    } catch (e) {
      warn(`   ⚠️  Could not log in ${client.email}: ${e.message}`);
    }
  }

  return authenticated;
}

// ─── STEP 6: PLACE ORDERS ────────────────────────────────────────────────────
//
// Each order goes through the full saga:
//   placeOrder → inventory reservation → payment intent creation (if CREDIT_CARD)
//
// We use CASH payment to avoid needing Stripe test keys in the seed.

function buildOrders(createdMeals, ingredientIdMap) {
  if (createdMeals.length === 0) {
    warn('No meals available — skipping order seeding');
    return [];
  }

  // Helper to pick meals by name (falls back to index if not found)
  const find = (name) => createdMeals.find(m => m.name === name) || createdMeals[0];

  const burger  = find('Classic Smash Burger');
  const chicken = find('Grilled Chicken Platter');
  const pasta   = find('Chicken Penne Arrabbiata');
  const wrap    = find('Chicken Caesar Wrap');
  const shawarma= find('Beef & Cheese Shawarma');
  const salad   = find('Greek Feta Salad');
  const fries   = find('Loaded Cheese Fries');
  const kofta   = find('Kofta & Rice');

  return [
    // Client 0 (Omar) — hungry guy, big order
    {
      clientIndex: 0,
      label: 'Omar – Burger meal + fries',
      payload: {
        items: [
          { mealId: burger.id,  quantity: 2, customizations: null },
          { mealId: fries.id,   quantity: 2, customizations: null },
        ],
        points: 0,
        paymentMethod: 'CASH',
      },
    },
    {
      clientIndex: 0,
      label: 'Omar – Grilled chicken platter',
      payload: {
        items: [
          { mealId: chicken.id, quantity: 1, customizations: null },
          { mealId: salad.id,   quantity: 1, customizations: null },
        ],
        points: 0,
        paymentMethod: 'CASH',
      },
    },

    // Client 1 (Nour) — lighter orders
    {
      clientIndex: 1,
      label: 'Nour – Caesar wrap + salad',
      payload: {
        items: [
          { mealId: wrap.id,    quantity: 1, customizations: null },
          { mealId: salad.id,   quantity: 1, customizations: null },
        ],
        points: 0,
        paymentMethod: 'CASH',
      },
    },
    {
      clientIndex: 1,
      label: 'Nour – Pasta order',
      payload: {
        items: [
          { mealId: pasta.id,   quantity: 1, customizations: null },
        ],
        points: 0,
        paymentMethod: 'CASH',
      },
    },

    // Client 2 (Youssef) — protein-focused
    {
      clientIndex: 2,
      label: 'Youssef – Kofta & shawarma',
      payload: {
        items: [
          { mealId: kofta.id,    quantity: 1, customizations: null },
          { mealId: shawarma.id, quantity: 1, customizations: null },
        ],
        points: 0,
        paymentMethod: 'CASH',
      },
    },
    {
      clientIndex: 2,
      label: 'Youssef – Chicken platter x2',
      payload: {
        items: [
          { mealId: chicken.id,  quantity: 2, customizations: null },
          { mealId: fries.id,    quantity: 1, customizations: null },
        ],
        points: 0,
        paymentMethod: 'CASH',
      },
    },
    // Client 2 (Youssef) — Custom meal
    {
      clientIndex: 2,
      label: 'Youssef – Custom Protein Bowl',
      payload: {
        items: [
          {
            mealId: null,
            quantity: 1,
            customizations: {
              primary: { id: ingredientIdMap['Chicken Breast'] },
              additions: [
                { id: ingredientIdMap['Basmati Rice'], grams: 150 },
                { id: ingredientIdMap['Tomato'], grams: 50 },
                { id: ingredientIdMap['Olive Oil'], grams: 15 }
              ]
            }
          }
        ],
        points: 0,
        paymentMethod: 'CASH',
      },
    },
  ];
}

async function placeOrders(authenticatedClients, createdMeals, ingredientIdMap) {
  log('\n🛒 Step 6: Placing seed orders...');
  const orders = buildOrders(createdMeals, ingredientIdMap);
  const placed = [];

  for (const order of orders) {
    const client = authenticatedClients[order.clientIndex];
    if (!client) {
      warn(`   ⚠️  Client index ${order.clientIndex} not available — skipping order "${order.label}"`);
      continue;
    }

    try {
      const res = await api('POST', '/api/orders', order.payload, client.token);
      log(`   ✅ Order placed: "${order.label}" → id=${res.id}, total=EGP ${res.totalPrice}, status=${res.status}`);
      placed.push(res);
    } catch (e) {
      warn(`   ⚠️  Failed to place order "${order.label}": ${e.message}`);
    }
  }

  log(`\n   ${placed.length}/${orders.length} orders placed successfully`);
  return placed;
}

// ─── SUMMARY ─────────────────────────────────────────────────────────────────
function printSummary({ meals, clients, orders }) {
  console.log('\n' + '═'.repeat(60));
  console.log('  🌱 SEED COMPLETE');
  console.log('═'.repeat(60));
  console.log(`  Gateway:       ${GATEWAY_URL}`);
  console.log(`  Admin:         ${ADMIN_EMAIL}`);
  console.log(`  Meals created: ${meals.length}`);
  console.log(`  Clients:       ${clients.length}`);
  console.log(`  Orders placed: ${orders.length}`);
  console.log('═'.repeat(60));

  if (meals.length > 0) {
    console.log('\n  Seeded Meals (name | category | EGP):');
    for (const m of meals) {
      console.log(`    - ${m.name.padEnd(35)} ${m.category.padEnd(12)} EGP ${m.price}`);
    }
  }

  if (orders.length > 0) {
    console.log('\n  Seeded Orders (id | clientId | total | status):');
    for (const o of orders) {
      console.log(`    - Order #${String(o.id).padEnd(6)} client=${o.clientId}  EGP ${String(o.totalPrice).padEnd(8)}  ${o.status}`);
    }
  }

  console.log('\n  ⚠️  Ingredient import is ASYNC (RabbitMQ).');
  console.log('     If meals show 0 ingredients, wait 2-3 min and check /api/menu.\n');
}

// ─── MAIN ────────────────────────────────────────────────────────────────────
async function main() {
  console.log('\n' + '═'.repeat(60));
  console.log('  🌱 REVIVE RESTAURANT — SEED SCRIPT');
  console.log('═'.repeat(60));
  console.log(`  Target gateway: ${GATEWAY_URL}`);
  console.log(`  Skip ingredient import: ${SKIP_INGREDIENT_IMPORT}`);
  console.log('═'.repeat(60) + '\n');

  try {
    // 1. Admin auth
    const adminToken = await adminLogin();

    // 2. Ingredient import (async via RabbitMQ)
    await importIngredients(adminToken);

    // 3. Resolve ingredient IDs from menu-service
    const ingredientIdMap = await fetchIngredientIds(adminToken);

    // 4. Create meals
    const createdMeals = await createMeals(adminToken, ingredientIdMap);

    // 5. Register + login clients
    const clients = await createClients();
    const authenticatedClients = await loginClients(clients);

    // 6. Place orders
    const orders = await placeOrders(authenticatedClients, createdMeals, ingredientIdMap);

    // Summary
    printSummary({ meals: createdMeals, clients: authenticatedClients, orders });

  } catch (e) {
    err('Fatal error:', e.message);
    if (process.env.DEBUG) console.error(e);
    process.exit(1);
  }
}

main();
