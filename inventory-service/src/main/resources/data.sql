-- Seed ingredients table from a CSV file on startup.
-- Ensure the file 'ingredients.csv' is accessible to the PostgreSQL server process.
-- The CSV must have a header row matching the columns below (id, name, calories_per_gram, protein_per_gram, carbs_per_gram, fats_per_gram).
--
-- Example CSV format:
-- id,name,calories_per_gram,protein_per_gram,carbs_per_gram,fats_per_gram
-- a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11,Chicken Breast,1.65,0.31,0.0,0.036
-- ...

INSERT INTO ingredients (id, name, calories_per_gram, protein_per_gram, carbs_per_gram, fats_per_gram) 
VALUES ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'Chicken Breast', 1.65, 0.31, 0.0, 0.036) 
ON CONFLICT (name) DO NOTHING;
