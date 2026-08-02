package com.erp.ai.service.impl;

import com.erp.ai.config.ErpAiProperties;
import com.erp.ai.model.AiChatMessage;
import com.erp.ai.model.AiModelCompletion;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiOpenAiCompatibleClientUsageTest {
    @Test
    void shouldParseOpenAiUsageAndSendStreamingUsageRequest() {
        ErpAiProperties properties = new ErpAiProperties();
        properties.setMaxOutputTokens(2048);
        AiOpenAiCompatibleClient client = new AiOpenAiCompatibleClient(properties,
                new ObjectMapper(), mock(AiQuotaGuard.class));
        String body = "{\"choices\":[{\"message\":{\"content\":\"ok\"}}],"
                + "\"usage\":{\"prompt_tokens\":17,\"completion_tokens\":5}}";

        AiModelCompletion completion = ReflectionTestUtils.invokeMethod(client, "extractCompletion", body);
        String request = ReflectionTestUtils.invokeMethod(client, "buildChatRequestBody", null,
                List.of(new AiChatMessage("user", "hello")), true, List.of());

        assertThat(completion.getInputTokens()).isEqualTo(17L);
        assertThat(completion.getOutputTokens()).isEqualTo(5L);
        assertThat(completion.isUsageReported()).isTrue();
        assertThat(request).contains("\"max_tokens\":2048")
                .contains("\"stream_options\":{\"include_usage\":true}");
    }

    @Test
    void shouldReserveThenSettleReportedUsageForSuccessfulCompletion() throws Exception {
        HttpServer server = startServer(200, "application/json",
                "{\"choices\":[{\"message\":{\"content\":\"ok\"}}],"
                        + "\"usage\":{\"prompt_tokens\":17,\"completion_tokens\":5}}");
        try {
            ErpAiProperties properties = properties(server);
            AiQuotaGuard quotaGuard = mock(AiQuotaGuard.class);
            AiQuotaReservation reservation = reservation();
            when(quotaGuard.reserve(anyLong())).thenReturn(reservation);
            AiOpenAiCompatibleClient client = new AiOpenAiCompatibleClient(properties,
                    new ObjectMapper(), quotaGuard);

            AiModelCompletion completion = client.completeChat(
                    List.of(new AiChatMessage("user", "hello")), List.of());

            assertThat(completion.getContent()).isEqualTo("ok");
            verify(quotaGuard).reserve(anyLong());
            verify(quotaGuard).settle(reservation, 17L, 5L, true);
            verify(quotaGuard, never()).release(reservation);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void shouldReleaseReservationWhenModelRequestFails() throws Exception {
        HttpServer server = startServer(500, "application/json",
                "{\"error\":{\"message\":\"upstream unavailable\"}}");
        try {
            ErpAiProperties properties = properties(server);
            AiQuotaGuard quotaGuard = mock(AiQuotaGuard.class);
            AiQuotaReservation reservation = reservation();
            when(quotaGuard.reserve(anyLong())).thenReturn(reservation);
            AiOpenAiCompatibleClient client = new AiOpenAiCompatibleClient(properties,
                    new ObjectMapper(), quotaGuard);

            assertThatThrownBy(() -> client.completeChat(
                    List.of(new AiChatMessage("user", "hello")), List.of()))
                    .isInstanceOf(IOException.class)
                    .hasMessage("upstream unavailable");

            verify(quotaGuard).release(reservation);
            verify(quotaGuard, never()).settle(reservation, null, null, false);
        } finally {
            server.stop(0);
        }
    }

    private static ErpAiProperties properties(HttpServer server) {
        ErpAiProperties properties = new ErpAiProperties();
        properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/v1");
        properties.setChatPath("/chat/completions");
        properties.setReadTimeoutMs(5000L);
        return properties;
    }

    private static AiQuotaReservation reservation() {
        return new AiQuotaReservation("tenant-a", 1785542400000L,
                "input-ref", "output-ref", 128L, 4096L);
    }

    private static HttpServer startServer(int status, String contentType, String responseBody) throws IOException {
        byte[] responseBytes = responseBody.getBytes(StandardCharsets.UTF_8);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            try (exchange) {
                exchange.getRequestBody().readAllBytes();
                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.sendResponseHeaders(status, responseBytes.length);
                exchange.getResponseBody().write(responseBytes);
            }
        });
        server.start();
        return server;
    }
}
