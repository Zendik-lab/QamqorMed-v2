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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class OllamaClient {

    public record OllamaMessage(String role, String content, List<ToolCall> toolCalls) {
        public OllamaMessage(String role, String content) { this(role, content, List.of()); }
    }

    public record ToolDefinition(String name, String description, Map<String, Object> parameters) {}

    public record ToolCall(String name, JsonNode arguments) {}

    public record ToolStreamResult(List<ToolCall> toolCalls) {}

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

    /**
     * Streams visible text and collects native Ollama tool calls. Tool calls
     * are returned only after the response is complete so the service can
     * validate and execute them through its allowlist.
     */
    public ToolStreamResult streamWithTools(List<OllamaMessage> messages, List<ToolDefinition> tools,
                                            Consumer<String> contentConsumer) {
        HttpURLConnection connection = null;
        try {
            connection = openConnection(messages, true, tools);
            int responseCode = connection.getResponseCode();
            if (responseCode == 400 || responseCode == 404 || responseCode == 405) {
                throw new AiToolCallingUnsupportedException(new IOException("Ollama tools are not supported"));
            }
            if (responseCode < 200 || responseCode >= 300) throw new AiUnavailableException();
            List<ToolCall> calls = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) continue;
                    JsonNode root = objectMapper.readTree(line);
                    if (root.hasNonNull("error")) throw new AiUnavailableException();
                    visibleContent(root).ifPresent(contentConsumer);
                    collectToolCalls(root.path("message").path("tool_calls"), calls);
                    if (root.path("done").asBoolean(false)) break;
                }
            }
            return new ToolStreamResult(Collections.unmodifiableList(calls));
        } catch (AiToolCallingUnsupportedException exception) {
            throw exception;
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
        return openConnection(messages, stream, List.of());
    }

    private HttpURLConnection openConnection(List<OllamaMessage> messages, boolean stream,
                                             List<ToolDefinition> tools) throws IOException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", properties.getModel());
        payload.put("messages", messages.stream().map(this::messagePayload).toList());
        payload.put("stream", stream);
        payload.put("think", false);
        payload.put("options", Map.of("num_ctx", properties.getNumCtx()));
        if (!tools.isEmpty()) {
            payload.put("tools", tools.stream().map(tool -> Map.of(
                    "type", "function",
                    "function", Map.of("name", tool.name(), "description", tool.description(), "parameters", tool.parameters())
            )).toList());
        }

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

    private Map<String, Object> messagePayload(OllamaMessage message) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("role", message.role());
        payload.put("content", message.content());
        if (!message.toolCalls().isEmpty()) {
            payload.put("tool_calls", message.toolCalls().stream().map(call -> Map.of(
                    "function", Map.of("name", call.name(), "arguments", call.arguments())
            )).toList());
        }
        return payload;
    }

    private void collectToolCalls(JsonNode nodes, List<ToolCall> calls) {
        if (!nodes.isArray()) return;
        for (JsonNode node : nodes) {
            JsonNode function = node.path("function");
            String name = function.path("name").asText("").trim();
            if (name.isBlank()) continue;
            JsonNode arguments = function.path("arguments");
            JsonNode parsedArguments;
            if (arguments.isMissingNode() || arguments.isNull()) {
                parsedArguments = objectMapper.createObjectNode();
            } else if (arguments.isTextual()) {
                try {
                    parsedArguments = objectMapper.readTree(arguments.asText());
                } catch (IOException exception) {
                    parsedArguments = arguments;
                }
            } else {
                parsedArguments = arguments;
            }
            ToolCall call = new ToolCall(name, parsedArguments);
            JsonNode finalArguments = parsedArguments;
            if (calls.stream().noneMatch(existing -> existing.name().equals(name) && existing.arguments().equals(finalArguments))) {
                calls.add(call);
            }
        }
    }

    private java.util.Optional<String> visibleContent(JsonNode root) {
        JsonNode content = root.path("message").path("content");
        if (content.isTextual() && !content.asText().isEmpty()) return java.util.Optional.of(content.asText());
        return java.util.Optional.empty();
    }
}
