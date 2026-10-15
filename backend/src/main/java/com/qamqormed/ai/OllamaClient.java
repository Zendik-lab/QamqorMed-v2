package com.qamqormed.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qamqormed.config.AiProperties;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class OllamaClient {

    public record OllamaMessage(String role, String content) {}

    private final AiProperties properties;
    private final ObjectMapper objectMapper;
    private final URL chatUrl;

    public OllamaClient(AiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        try {
            this.chatUrl = properties.getBaseUrl().resolve("/api/chat").toURL();
        } catch (IOException exception) {
            throw new IllegalArgumentException("Invalid Ollama URL", exception);
        }
    }

    public String complete(List<OllamaMessage> messages) {
        HttpURLConnection connection = null;
        try {
            connection = openConnection(messages, false);
            if (connection.getResponseCode() < 200 || connection.getResponseCode() >= 300) {
                throw new AiUnavailableException();
            }
            JsonNode root = objectMapper.readTree(connection.getInputStream());
            if (root.hasNonNull("error")) throw new AiUnavailableException();
            return visibleContent(root).orElseThrow(AiUnavailableException::new);
        } catch (IOException exception) {
            throw new AiUnavailableException(exception);
        } catch (RuntimeException exception) {
            if (exception instanceof AiUnavailableException unavailable) throw unavailable;
            throw exception;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    public void stream(List<OllamaMessage> messages, Consumer<String> contentConsumer) {
        HttpURLConnection connection = null;
        try {
            connection = openConnection(messages, true);
            if (connection.getResponseCode() < 200 || connection.getResponseCode() >= 300) {
                throw new AiUnavailableException();
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) continue;
                    JsonNode root = objectMapper.readTree(line);
                    if (root.hasNonNull("error")) throw new AiUnavailableException();
                    visibleContent(root).ifPresent(contentConsumer);
                    if (root.path("done").asBoolean(false)) break;
                }
            }
        } catch (IOException exception) {
            throw new AiUnavailableException(exception);
        } catch (RuntimeException exception) {
            if (exception instanceof AiUnavailableException unavailable) throw unavailable;
            throw exception;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private HttpURLConnection openConnection(List<OllamaMessage> messages, boolean stream) throws IOException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", properties.getModel());
        payload.put("messages", messages.stream().map(message -> Map.of(
                "role", message.role(),
                "content", message.content()
        )).toList());
        payload.put("stream", stream);
        payload.put("think", false);
        payload.put("options", Map.of("num_ctx", properties.getNumCtx()));

        HttpURLConnection connection = (HttpURLConnection) chatUrl.openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(Math.toIntExact(Math.min(Integer.MAX_VALUE, properties.getRequestTimeout().toMillis())));
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");
        try (java.io.OutputStream output = connection.getOutputStream()) {
            output.write(objectMapper.writeValueAsBytes(payload));
        }
        return connection;
    }

    private java.util.Optional<String> visibleContent(JsonNode root) {
        JsonNode content = root.path("message").path("content");
        if (content.isTextual() && !content.asText().isEmpty()) return java.util.Optional.of(content.asText());
        return java.util.Optional.empty();
    }
}
