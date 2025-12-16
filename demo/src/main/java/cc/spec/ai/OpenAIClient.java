
package cc.spec.ai;

import java.util.List;
import java.util.ArrayList;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class OpenAIClient {
    private final String apiKey;
    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";

    public OpenAIClient() {
        this.apiKey = loadApiKey();
    }

    private String loadApiKey() {
        // Look for .env in project root
        Path envPath = Path.of(System.getProperty("user.dir"), ".env");
        if (!Files.exists(envPath)) {
            throw new RuntimeException(".env file not found in project root");
        }
        try {
            for (String line : Files.readAllLines(envPath)) {
                if (line.trim().startsWith("OpenApi_key=")) {
                    return line.trim().substring("OpenApi_key=".length());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read .env file", e);
        }
        throw new RuntimeException("OpenApi_key not found in .env file");
    }

    /**
     * Calls the OpenAI API with the given prompt and returns the response.
     * @param prompt The prompt to send to OpenAI
     * @return The response from OpenAI
     */
    public String chatCompletion(String prompt) throws IOException, InterruptedException {
        // Use Jackson to build the JSON body
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        Map<String, Object> body = new HashMap<>();
        body.put("model", "gpt-3.5-turbo");
        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", prompt);
        messages.add(userMsg);
        body.put("messages", messages);
        String requestBody = mapper.writeValueAsString(body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OPENAI_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("OpenAI API error: " + response.statusCode() + "\n" + response.body());
        }
        return response.body();
    }
}