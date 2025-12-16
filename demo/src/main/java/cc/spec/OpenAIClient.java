package cc.spec;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;

/**
 * Client for interacting with OpenAI API
 */
public class OpenAIClient {
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public OpenAIClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.objectMapper = new ObjectMapper();
        // Try environment variable first
        String key = System.getenv("OPENAI_API_KEY");
        if (key == null || key.isEmpty()) {
            // Try to read from .env file in project root
            try {
                Path envPath = Path.of(System.getProperty("user.dir"), ".env");
                if (Files.exists(envPath)) {
                    for (String line : Files.readAllLines(envPath)) {
                        if (line.trim().startsWith("OPENAI_API_KEY=")) {
                            key = line.trim().substring("OPENAI_API_KEY=".length());
                            break;
                        }
                    }
                }
            } catch (IOException e) {
                // Ignore, will fail below if key is still null
            }
        }
        this.apiKey = key;
    }

    /**
     * Sends a chat completion request to OpenAI API
     * @param prompt The prompt to send
     * @return The response content from OpenAI
     * @throws IOException If the request fails
     */
    public String chatCompletion(String prompt) throws IOException {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IOException("OPENAI_API_KEY environment variable is not set");
        }

        try {
            // Build the request body
            String requestBody = buildRequestBody(prompt);
            
            // Create HTTP request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofSeconds(60))
                    .build();

            // Send request and get response
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new IOException("OpenAI API returned status code: " + response.statusCode() + 
                                    ", body: " + response.body());
            }

            // Parse response
            return parseResponse(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Request interrupted", e);
        } catch (Exception e) {
            throw new IOException("Failed to call OpenAI API: " + e.getMessage(), e);
        }
    }

    private String buildRequestBody(String prompt) throws IOException {
        Map<String, Object> requestMap = Map.of(
            "model", "gpt-3.5-turbo",
            "messages", List.of(
                Map.of("role", "user", "content", prompt)
            ),
            "temperature", 0.7,
            "max_tokens", 4096
        );
        return objectMapper.writeValueAsString(requestMap);
    }

    @SuppressWarnings("unchecked")
    private String parseResponse(String responseBody) throws IOException {
        Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
        List<Object> choices = (List<Object>) responseMap.get("choices");
        if (choices != null && !choices.isEmpty()) {
            Map<String, Object> firstChoice = (Map<String, Object>) choices.get(0);
            Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
            return (String) message.get("content");
        }
        throw new IOException("No choices in OpenAI response");
    }
}

