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
     * Builds on top of parse() to return a meal → ingredients map.
     * Keys are meal names, values are lists of IngredientEntry DTOs
     * containing the ingredient name, quantity, and unit.
     *
     * @param file the uploaded CSV file
     * @return LinkedHashMap of meal name to ingredient entries
     */
    public LinkedHashMap<String, List<IngredientEntry>> parseMenu(MultipartFile file) {
        List<Map<String, String>> rows = parse(file);
        LinkedHashMap<String, List<IngredientEntry>> menuMap = new LinkedHashMap<>();

        for (Map<String, String> row : rows) {
            String mealName = row.get("meal_name");
            String ingredient = row.get("ingredient");
            String quantityRaw = row.get("quantity");
            String unit = row.get("unit");

            if (mealName == null || mealName.isBlank()) {
                log.warn("Skipping row with missing meal_name");
                continue;
            }
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
            menuMap.computeIfAbsent(mealName, k -> new ArrayList<>()).add(entry);
        }

        log.debug("CsvParserHelper: parsed {} meals from CSV", menuMap.size());
        return menuMap;
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