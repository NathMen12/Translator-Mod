package com.nathmen12.translator.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TranslatorApiClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(TranslatorApiClient.class);
    private static final String TRANSLATE_ENDPOINT = "/translate";
    private static final int MAX_RETRIES = 3;
    private static final int BASE_DELAY_MS = 1000;
    private static final int MAX_DELAY_MS = 30000;
    
    private final String baseUrl;
    private final HttpClient httpClient;
    private final Gson gson;
    private final ExecutorService executor;
    
    public TranslatorApiClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
        this.gson = new Gson();
        this.executor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "Translator-API-Worker");
            t.setDaemon(true);
            return t;
        });
        LOGGER.info("Translator API Client initialized with endpoint: {}", this.baseUrl);
    }
    
    public CompletableFuture<TranslationResult> translate(String text, String sourceLang, String targetLang) {
        return translateWithRetry(text, sourceLang, targetLang, 0);
    }
    
    private CompletableFuture<TranslationResult> translateWithRetry(String text, String sourceLang, String targetLang, int attempt) {
        CompletableFuture<TranslationResult> future = new CompletableFuture<>();
        
        executor.submit(() -> {
            try {
                JsonObject requestBody = new JsonObject();
                requestBody.addProperty("text", text);
                requestBody.addProperty("source", sourceLang);
                requestBody.addProperty("target", targetLang);
                
                String jsonBody = gson.toJson(requestBody);
                
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + TRANSLATE_ENDPOINT))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() == 429) {
                    // Rate limited - retry with backoff
                    if (attempt < MAX_RETRIES) {
                        int delay = Math.min(BASE_DELAY_MS * (1 << attempt), MAX_DELAY_MS);
                        LOGGER.warn("Rate limited (429), retrying in {}ms (attempt {}/{})", delay, attempt + 1, MAX_RETRIES);
                        try {
                            Thread.sleep(delay);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            future.completeExceptionally(e);
                            return;
                        }
                        translateWithRetry(text, sourceLang, targetLang, attempt + 1)
                            .thenAccept(future::complete)
                            .exceptionally(ex -> { future.completeExceptionally(ex); return null; });
                        return;
                    } else {
                        future.completeExceptionally(new TranslationException("Rate limit exceeded after " + MAX_RETRIES + " retries"));
                        return;
                    }
                }
                
                if (response.statusCode() >= 400) {
                    String errorMsg = parseErrorResponse(response.body());
                    future.completeExceptionally(new TranslationException("API Error " + response.statusCode() + ": " + errorMsg));
                    return;
                }
                
                TranslationResult result = parseResponse(response.body());
                if (result != null) {
                    future.complete(result);
                } else {
                    future.completeExceptionally(new TranslationException("Invalid response format"));
                }
                
            } catch (Exception e) {
                LOGGER.error("Translation request failed", e);
                future.completeExceptionally(new TranslationException("Request failed: " + e.getMessage(), e));
            }
        });
        
        return future;
    }
    
    private TranslationResult parseResponse(String body) {
        try {
            JsonObject json = gson.fromJson(body, JsonObject.class);
            if (json.has("translatedText")) {
                TranslationResult result = new TranslationResult();
                result.translatedText = json.get("translatedText").getAsString();
                result.source = json.has("source") ? json.get("source").getAsString() : "auto";
                result.target = json.has("target") ? json.get("target").getAsString() : "unknown";
                result.duration = json.has("duration") ? json.get("duration").getAsLong() : 0;
                return result;
            }
        } catch (JsonSyntaxException e) {
            LOGGER.error("Failed to parse translation response: {}", body, e);
        }
        return null;
    }
    
    private String parseErrorResponse(String body) {
        try {
            JsonObject json = gson.fromJson(body, JsonObject.class);
            if (json.has("error")) {
                return json.get("error").getAsString();
            }
            if (json.has("message")) {
                return json.get("message").getAsString();
            }
        } catch (JsonSyntaxException e) {
            // Ignore
        }
        return body.isEmpty() ? "Unknown error" : body.substring(0, Math.min(200, body.length()));
    }
    
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    public static class TranslationResult {
        public String translatedText;
        public String source;
        public String target;
        public long duration;
        public String error;
    }
    
    public static class TranslationException extends Exception {
        public TranslationException(String message) {
            super(message);
        }
        
        public TranslationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}