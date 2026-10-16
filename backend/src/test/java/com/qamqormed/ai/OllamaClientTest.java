package com.qamqormed.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qamqormed.config.AiProperties;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OllamaClientTest {

    private static final List<OllamaClient.OllamaMessage> MESSAGES = List.of(
            new OllamaClient.OllamaMessage("system", "System instructions"),
            new OllamaClient.OllamaMessage("user", "Previous question"),
            new OllamaClient.OllamaMessage("assistant", "Previous answer"),
            new OllamaClient.OllamaMessage("user", "Новый вопрос")
    );
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final BlockingQueue<RecordedRequest> requests = new LinkedBlockingQueue<>();
    private final AtomicInteger responseStatus = new AtomicInteger(200);
    private HttpServer server;
    private AiProperties properties;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/chat", exchange -> {
            try (exchange) {
                JsonNode payload = objectMapper.readTree(exchange.getRequestBody());
                requests.add(new RecordedRequest(
                        exchange.getRequestMethod(),
                        exchange.getRequestHeaders().getFirst("Authorization"),
                        exchange.getRequestHeaders().getFirst("ngrok-skip-browser-warning"),
                        payload
                ));
                boolean streaming = payload.path("stream").asBoolean();
                String response = responseStatus.get() != 200 ? "Upstream authentication error details" : streaming ? """
                        {"message":{"thinking":"Hidden thinking"},"done":false}

                        {"message":{"content":"Ответ "},"done":false}
                        {"message":{"content":"модели"},"done":false}
                        {"done":true}
                        {"message":{"content":"Ignored after done"},"done":false}
                        """ : """
                        {"message":{"content":"Ответ модели"},"done":true}
                        """;
                byte[] body = response.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", streaming ? "application/x-ndjson" : "application/json");
                exchange.sendResponseHeaders(responseStatus.get(), body.length);
                exchange.getResponseBody().write(body);
            }
        });
        server.start();
        properties = new AiProperties();
        properties.setBaseUrl(URI.create("http://127.0.0.1:" + server.getAddress().getPort()));
    }

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void sendsUtf8BasicAuthWhenBothCredentialsAreConfigured(boolean streaming) throws Exception {
        properties.setBasicAuthUsername("test-пользователь");
        properties.setBasicAuthPassword(" test-пароль:with:colons ");

        assertThat(invokeClient(streaming)).isEqualTo("Ответ модели");

        RecordedRequest request = takeRequest();
        assertThat(request.authorization()).startsWith("Basic ");
        String credentials = new String(Base64.getDecoder().decode(request.authorization().substring(6)), StandardCharsets.UTF_8);
        assertThat(credentials).isEqualTo("test-пользователь: test-пароль:with:colons ");
        assertRequestPayload(request, streaming);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void supportsLocalOllamaWithoutCredentials(boolean streaming) throws Exception {
        assertThat(invokeClient(streaming)).isEqualTo("Ответ модели");

        RecordedRequest request = takeRequest();
        assertThat(request.authorization()).isNull();
        assertRequestPayload(request, streaming);
    }

    @ParameterizedTest
    @MethodSource("incompleteCredentials")
    void omitsAuthorizationWhenEitherCredentialIsMissing(String username, String password, boolean streaming) throws Exception {
        properties.setBasicAuthUsername(username);
        properties.setBasicAuthPassword(password);

        assertThat(invokeClient(streaming)).isEqualTo("Ответ модели");
        assertThat(takeRequest().authorization()).isNull();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void rejectsUpstreamUnauthorizedResponseWithoutExposingDetails(boolean streaming) throws Exception {
        properties.setBasicAuthUsername("test-user");
        properties.setBasicAuthPassword("test-password");
        responseStatus.set(401);

        assertThatThrownBy(() -> invokeClient(streaming))
                .isInstanceOf(AiUnavailableException.class)
                .hasMessage(new AiUnavailableException().getMessage())
                .hasNoCause();
        assertThat(takeRequest().authorization()).startsWith("Basic ");
    }

    private static Stream<Arguments> incompleteCredentials() {
        return Stream.of(false, true).flatMap(streaming -> Stream.of(
                Arguments.of(null, "test-password", streaming),
                Arguments.of("", "test-password", streaming),
                Arguments.of("  ", "test-password", streaming),
                Arguments.of("test-user", null, streaming),
                Arguments.of("test-user", "", streaming),
                Arguments.of("test-user", "  ", streaming)
        ));
    }

    private String invokeClient(boolean streaming) {
        OllamaClient client = new OllamaClient(properties, objectMapper);
        if (!streaming) return client.complete(MESSAGES);
        StringBuilder result = new StringBuilder();
        client.stream(MESSAGES, result::append);
        return result.toString();
    }

    private RecordedRequest takeRequest() throws InterruptedException {
        RecordedRequest request = requests.poll(5, TimeUnit.SECONDS);
        assertThat(request).isNotNull();
        return request;
    }

    private void assertRequestPayload(RecordedRequest request, boolean streaming) {
        assertThat(request.method()).isEqualTo("POST");
        assertThat(request.ngrokSkipBrowserWarning()).isEqualTo("true");
        JsonNode payload = request.payload();
        assertThat(payload.path("model").asText()).isEqualTo("qwen3:8b");
        assertThat(payload.path("stream").asBoolean()).isEqualTo(streaming);
        assertThat(payload.path("think").asBoolean(true)).isFalse();
        assertThat(payload.path("options").path("num_ctx").asInt()).isEqualTo(4096);
        JsonNode expectedMessages = objectMapper.valueToTree(MESSAGES);
        assertThat(payload.path("messages")).isEqualTo(expectedMessages);
        assertThat(payload.has("basicAuthUsername")).isFalse();
        assertThat(payload.has("basicAuthPassword")).isFalse();
    }

    private record RecordedRequest(String method, String authorization, String ngrokSkipBrowserWarning, JsonNode payload) {}
}
