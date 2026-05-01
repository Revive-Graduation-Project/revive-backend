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
public class Deepseek {

    @Value("${app.keys.deepseek}")
    private String apiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String aiMenuNormalizer(String prompt) {
        try {
            String systemInstruction = "You are a USDA FoodData Central ingredient name normalizer.\n\n" +
                    "You will receive a comma separated list of ingredient names exactly as a restaurant manager wrote them.\n\n" +
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
                    "The output array must have the exact same number of elements as the input, in the same order.";

            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", "deepseek-v4-pro");
            
            ObjectNode thinking = requestBody.putObject("thinking");
            thinking.put("type", "enabled");
            
            requestBody.put("reasoning_effort", "high");
            requestBody.put("stream", false);

            ArrayNode messages = requestBody.putArray("messages");
            
            ObjectNode systemMessage = messages.addObject();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemInstruction);

            ObjectNode userMessage = messages.addObject();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.deepseek.com/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Deepseek API request failed with status " + response.statusCode() + ": " + response.body());
            }

            JsonNode rootNode = objectMapper.readTree(response.body());
            JsonNode messageContentNode = rootNode.path("choices").path(0).path("message").path("content");
            
            if (messageContentNode.isMissingNode() || messageContentNode.isNull()) {
                throw new RuntimeException("Unexpected response format from Deepseek API");
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
            throw new RuntimeException("Deepseek AI request failed: " + e.getMessage(), e);
        }
    }
}
