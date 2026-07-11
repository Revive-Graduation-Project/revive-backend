package com.restaurant.inventory.hooks;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class OpenRouter {

    @Value("${app.keys.openrouter}")
    private String apiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String aiMenuNormalizer(String prompt) {
        try {
            String systemInstruction = "You are a USDA FoodData Central ingredient name normalizer.\n\n" +
                    "You will receive a large batch (a comma-separated list) of unique ingredient names from a restaurant's inventory.\n\n"
                    +
                    "Your only job is to convert each name into the most accurate USDA database search term\n" +
                    "so that the top result is the correct ingredient.\n\n" +
                    "Respond with ONLY a valid JSON array — no markdown, no explanation, no extra text.\n" +
                    "No opening or closing text. Just the raw JSON array.\n\n" +
                    "Each element must follow this exact structure:\n" +
                    "{\n" +
                    "  \"originalName\": \"string\",\n" +
                    "  \"query\": \"string\"\n" +
                    "}\n\n" +
                    "Rules:\n" +
                    "- \"originalName\" must be copied exactly from the input, do not change it\n" +
                    "- \"query\" must identify the ingredient as it appears in USDA:\n" +
                    "    - use the natural, unprocessed, unbranded form\n" +
                    "    - include type and state where relevant\n" +
                    "      (e.g. \"ground beef raw\", \"wheat bread slice\", \"yellow onion raw\")\n" +
                    "    - 2 to 5 words maximum\n" +
                    "    - if the name is already a perfect USDA term, keep it as is\n\n" +
                    "CRITICAL: The output array MUST have the exact same number of elements as the input batch, in the exact same order.";

            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", "google/gemma-4-26b-a4b-it:free");

            ArrayNode messages = requestBody.putArray("messages");

            ObjectNode systemMessage = messages.addObject();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemInstruction);

            ObjectNode userMessage = messages.addObject();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://openrouter.ai/api/v1/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    // Optional but recommended headers by OpenRouter
                    .header("HTTP-Referer", "http://localhost:8089")
                    .header("X-Title", "Restaurant Microservices")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException(
                        "OpenRouter API request failed with status " + response.statusCode() + ": " + response.body());
            }

            JsonNode rootNode = objectMapper.readTree(response.body());
            JsonNode messageContentNode = rootNode.path("choices").path(0).path("message").path("content");

            if (messageContentNode.isMissingNode() || messageContentNode.isNull()) {
                throw new RuntimeException("Unexpected response format from OpenRouter API: " + response.body());
            }

            String responseText = messageContentNode.asText();

            // Clean up possible markdown code block wrappers
            if (responseText.startsWith("```json")) {
                responseText = responseText.substring(7);
            } else if (responseText.startsWith("```")) {
                responseText = responseText.substring(3);
            }
            if (responseText.endsWith("```")) {
                responseText = responseText.substring(0, responseText.length() - 3);
            }

            return responseText.trim();

        } catch (Exception e) {
            throw new RuntimeException("OpenRouter AI request failed: " + e.getMessage(), e);
        }
    }
}
