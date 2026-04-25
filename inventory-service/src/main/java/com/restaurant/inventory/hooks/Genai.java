package com.restaurant.inventory.hooks;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;

import java.util.Arrays;

@Service
public class Genai {

        @Value("${GOOGLE_API_KEY}")
        private String apiKey;

        public String aiMenuNormalizer(String prompt) {
                try (Client client = new Client.Builder().apiKey(apiKey).build()) {

                        Content systemInstruction = Content.builder()
                                        .parts(Arrays.asList(Part.builder()
                                                        .text("You are a USDA FoodData Central ingredient name normalizer.\n"
                                                                        +
                                                                        "\n" +
                                                                        "You will receive a comma separated list of ingredient names exactly as a restaurant manager wrote them.\n"
                                                                        +
                                                                        "\n" +
                                                                        "Your only job is to convert each name into the most accurate USDA database search term\n"
                                                                        +
                                                                        "so that the top result is the correct ingredient.\n"
                                                                        +
                                                                        "\n" +
                                                                        "Respond with ONLY a valid JSON array — no markdown, no explanation, no extra text.\n"
                                                                        +
                                                                        "No opening or closing text. Just the raw JSON array.\n"
                                                                        +
                                                                        "\n" +
                                                                        "Each element must follow this exact structure:\n"
                                                                        +
                                                                        "{\n" +
                                                                        "  \"originalName\": \"string\",\n" +
                                                                        "  \"query\": \"string\"\n" +
                                                                        "}\n" +
                                                                        "\n" +
                                                                        "Rules:\n" +
                                                                        "- \"originalName\" must be copied exactly from the input, do not change it\n"
                                                                        +
                                                                        "- \"query\" must identify the ingredient as it appears in USDA:\n"
                                                                        +
                                                                        "    - use the natural, unprocessed, unbranded form\n"
                                                                        +
                                                                        "    - include type and state where relevant\n"
                                                                        +
                                                                        "      (e.g. \"ground beef raw\", \"wheat bread slice\", \"yellow onion raw\")\n"
                                                                        +
                                                                        "    - 2 to 5 words maximum\n" +
                                                                        "    - if the name is already a perfect USDA term, keep it as is\n"
                                                                        +
                                                                        "\n" +
                                                                        "The output array must have the exact same number of elements as the input, in the same order.")
                                                        .build()))
                                        .build();

                        GenerateContentConfig config = GenerateContentConfig.builder()
                                        .systemInstruction(systemInstruction)
                                        .build();

                        GenerateContentResponse response = client.models.generateContent(
                                        "gemini-2.5-flash-lite",
                                        prompt,
                                        config);

                        return response.text();

                } catch (Exception e) {
                        throw new RuntimeException("Gemini AI request failed: " + e.getMessage(), e);
                }
        }
}
