package com.restaurant.inventory.helper;

import com.opencsv.CSVReader;
import com.restaurant.inventory.dto.IngredientEntry;
import com.restaurant.inventory.exception.InvalidCsvException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Low-level CSV parsing utility.
 * Reads a CSV file and maps each row to a column-name → value map,
 * using the first row as the header definition.
 */
@Slf4j
@Component
public class CsvParserHelper {

    private static final String CSV_EXTENSION = ".csv";

    /**
     * Parses the given CSV file into a list of row maps.
     * Keys are column headers (trimmed), values are cell values (trimmed).
     *
     * @param file the uploaded CSV file (already validated)
     * @return list of row maps, ready to be serialised as JSON
     */
    public List<Map<String, String>> parse(MultipartFile file) {
        try (Reader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
                CSVReader csvReader = new CSVReader(reader)) {

            List<String[]> allRows = csvReader.readAll();

            if (allRows.isEmpty()) {
                throw new InvalidCsvException("CSV file is empty");
            }

            String[] headers = allRows.get(0);
            List<Map<String, String>> result = new ArrayList<>();

            for (int i = 1; i < allRows.size(); i++) {
                String[] row = allRows.get(i);
                Map<String, String> rowMap = new LinkedHashMap<>();
                for (int j = 0; j < headers.length; j++) {
                    String value = (j < row.length) ? row[j].trim() : "";
                    rowMap.put(headers[j].trim(), value);
                }
                result.add(rowMap);
            }

            log.debug("CsvParserHelper: parsed {} data rows from '{}'", result.size(), file.getOriginalFilename());
            return result;

        } catch (InvalidCsvException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidCsvException("Failed to read CSV: " + e.getMessage(), e);
        }
    }

    /**
     * Builds on top of parse() to return a meal → parsed meal data map.
     * Keys are meal names, values are MealCsvEntry objects containing
     * the ingredient list, category, and price.
     *
     * @param file the uploaded CSV file
     * @return LinkedHashMap of meal name to MealCsvEntry
     */
    public LinkedHashMap<String, MealCsvEntry> parseMenu(MultipartFile file) {
        List<Map<String, String>> rows = parse(file);
        LinkedHashMap<String, MealCsvEntry> menuMap = new LinkedHashMap<>();

        if (rows.isEmpty()) {
            return menuMap;
        }

        // Detect format: Check if the first row has "ingredients" column
        boolean isListFormat = rows.get(0).containsKey("ingredients");

        for (Map<String, String> row : rows) {
            String mealName = row.get("meal_name");
            if (mealName == null || mealName.isBlank()) {
                log.warn("Skipping row with missing meal_name");
                continue;
            }
            String category = row.getOrDefault("category", "Uncategorized");
            String priceRaw = row.getOrDefault("price", "0");
            String description = row.getOrDefault("description", "");

            double price = 0;
            try {
                price = Double.parseDouble(priceRaw.trim());
            } catch (Exception e) {
                log.warn("Invalid price '{}' for meal '{}', defaulting to 0", priceRaw, mealName);
            }

            if (isListFormat) {
                String ingredientsRaw = row.get("ingredients");
                List<IngredientEntry> ingredients = parseIngredientsList(ingredientsRaw);
                menuMap.put(mealName.trim(), new MealCsvEntry(mealName.trim(), ingredients, category.trim(), price, description.trim()));
            } else {
                String ingredient = row.get("ingredient");
                String quantityRaw = row.get("quantity");
                String unit = row.get("unit");

                if (ingredient == null || ingredient.isBlank()) {
                    log.warn("Skipping row with missing ingredient for meal '{}'", mealName);
                    continue;
                }

                double quantity = 0;
                try {
                    quantity = Double.parseDouble(quantityRaw.trim());
                } catch (Exception e) {
                    log.warn("Invalid quantity '{}' for ingredient '{}', defaulting to 0", quantityRaw, ingredient);
                }

                IngredientEntry entry = new IngredientEntry(
                        ingredient.trim(),
                        quantity,
                        unit == null ? "" : unit.trim());

                double finalPrice = price;
                MealCsvEntry mealEntry = menuMap.computeIfAbsent(mealName, k ->
                        new MealCsvEntry(mealName.trim(), new ArrayList<>(), category.trim(), finalPrice, description.trim()));
                mealEntry.ingredients().add(entry);
            }
        }

        log.debug("CsvParserHelper: parsed {} meals from CSV", menuMap.size());
        return menuMap;
    }

    private List<IngredientEntry> parseIngredientsList(String ingredientsRaw) {
        List<IngredientEntry> list = new ArrayList<>();
        if (ingredientsRaw == null || ingredientsRaw.isBlank()) {
            return list;
        }

        // Split by semicolon
        String[] parts = ingredientsRaw.split(";");
        for (String part : parts) {
            part = part.trim();
            if (part.isEmpty()) continue;

            String name = part;
            double quantity = 0;
            String unit = "";

            if (part.contains(":")) {
                int colonIdx = part.lastIndexOf(':');
                name = part.substring(0, colonIdx).trim();
                String qtyUnitStr = part.substring(colonIdx + 1).trim();

                // Parse numeric prefix and unit (e.g. "150 g" or "150g" or "1.5 kg")
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("^([0-9.]+)\\s*(.*)$");
                java.util.regex.Matcher matcher = pattern.matcher(qtyUnitStr);
                if (matcher.matches()) {
                    try {
                        quantity = Double.parseDouble(matcher.group(1));
                    } catch (NumberFormatException e) {
                        log.warn("Failed to parse quantity from '{}'", qtyUnitStr);
                    }
                    unit = matcher.group(2).trim();
                }
            } else {
                log.warn("Ingredient entry '{}' does not contain quantity colon (:), defaulting quantity to 0", part);
            }

            list.add(new IngredientEntry(name, quantity, unit));
        }
        return list;
    }

    /**
     * Holds the parsed CSV data for a single meal.
     */
    public record MealCsvEntry(String mealName, List<IngredientEntry> ingredients, String category, double price, String description) {
    }

    public void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidCsvException("Uploaded file is empty");
        }
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase().endsWith(CSV_EXTENSION)) {
            throw new InvalidCsvException("File must have a .csv extension");
        }
    }
}