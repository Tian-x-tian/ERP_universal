package com.erp.system.ai.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.erp.system.ai.config.ErpAiProperties;
import com.erp.system.ai.model.AiChatMessage;
import com.erp.system.ai.model.AiModelCompletion;
import com.erp.system.ai.model.AiToolDefinition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * OpenAI 兼容客户端测试。
 */
class AiOpenAiCompatibleClientTest {
    private HttpServer httpServer;

    /**
     * 测试结束后关闭本地测试服务。
     */
    @AfterEach
    void tearDown() {
        if (httpServer != null) {
            httpServer.stop(0);
        }
    }

    /**
     * 验证客户端能够解析 OpenAI 兼容的流式响应。
     *
     * @throws Exception 异常
     */
    @Test
    void shouldParseStreamedChatCompletion() throws Exception {
        httpServer = startServer("/v1/chat/completions", exchange -> {
            byte[] body = ("""
                    data: {"choices":[{"delta":{"content":"你"}}]}

                    data: {"choices":[{"delta":{"content":"好"}}]}

                    data: [DONE]

                    """).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/event-stream; charset=UTF-8");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(body);
            }
        });
        AiOpenAiCompatibleClient client = new AiOpenAiCompatibleClient(buildProperties(), new ObjectMapper());
        List<String> deltaList = new ArrayList<>();

        String responseText = client.streamChat(Collections.singletonList(new AiChatMessage("user", "你好")), deltaList::add);

        Assertions.assertEquals("你好", responseText);
        Assertions.assertEquals(List.of("你", "好"), deltaList);
    }

    /**
     * 验证当上游不支持流式时，会自动降级解析普通 JSON 响应。
     *
     * @throws Exception 异常
     */
    @Test
    void shouldFallbackToJsonResponseWhenStreamUnavailable() throws Exception {
        httpServer = startServer("/v1/chat/completions", exchange -> {
            byte[] body = """
                    {"choices":[{"message":{"content":"整段回复"}}]}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(body);
            }
        });
        AiOpenAiCompatibleClient client = new AiOpenAiCompatibleClient(buildProperties(), new ObjectMapper());

        String responseText = client.streamChat(Collections.singletonList(new AiChatMessage("user", "你好")), delta -> {
        });

        Assertions.assertEquals("整段回复", responseText);
    }

    /**
     * 验证客户端能够解析 OpenAI 兼容工具调用响应。
     *
     * @throws Exception 异常
     */
    @Test
    void shouldParseToolCallResponse() throws Exception {
        httpServer = startServer("/v1/chat/completions", exchange -> {
            byte[] body = """
                    {"choices":[{"message":{"content":"需要执行动作","tool_calls":[{"id":"call_1","type":"function","function":{"name":"todo_finish","arguments":"{\\"todoId\\":1}"}}]}}]}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(body);
            }
        });
        AiOpenAiCompatibleClient client = new AiOpenAiCompatibleClient(buildProperties(), new ObjectMapper());

        AiToolDefinition toolDefinition = new AiToolDefinition();
        toolDefinition.setName("todo_finish");
        toolDefinition.setDescription("办结待办");
        toolDefinition.setParameters(new LinkedHashMap<>());

        AiModelCompletion completion = client.completeChat(
                Collections.singletonList(new AiChatMessage("user", "帮我办结第一条待办")),
                Collections.singletonList(toolDefinition));

        Assertions.assertEquals("需要执行动作", completion.getContent());
        Assertions.assertEquals(1, completion.getToolCalls().size());
        Assertions.assertEquals("todo_finish", completion.getToolCalls().get(0).getName());
        Assertions.assertEquals("{\"todoId\":1}", completion.getToolCalls().get(0).getArgumentsJson());
    }

    /**
     * 验证模型可用性探测接口。
     *
     * @throws Exception 异常
     */
    @Test
    void shouldProbeModelAvailability() throws Exception {
        httpServer = startServer("/v1/models", exchange -> {
            byte[] body = """
                    {"object":"list","data":[{"id":"gpt-5.1"}]}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(body);
            }
        });
        AiOpenAiCompatibleClient client = new AiOpenAiCompatibleClient(buildProperties(), new ObjectMapper());

        Assertions.assertTrue(client.isAvailable());
    }

    /**
     * 启动本地测试服务。
     *
     * @param contextPath 处理路径
     * @param handler     处理逻辑
     * @return HTTP 服务
     * @throws IOException IO 异常
     */
    private HttpServer startServer(String contextPath, ExchangeHandler handler) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext(contextPath, exchange -> {
            try {
                handler.handle(exchange);
            } finally {
                exchange.close();
            }
        });
        server.start();
        return server;
    }

    /**
     * 构造测试用配置。
     *
     * @return AI 配置
     */
    private ErpAiProperties buildProperties() {
        ErpAiProperties properties = new ErpAiProperties();
        properties.setBaseUrl("http://127.0.0.1:" + httpServer.getAddress().getPort() + "/v1");
        properties.setChatPath("/chat/completions");
        properties.setModel("gpt-5.1");
        return properties;
    }

    /**
     * 测试 HTTP 处理函数。
     */
    @FunctionalInterface
    private interface ExchangeHandler {

        /**
         * 处理当前 HTTP 交换对象。
         *
         * @param exchange HTTP 交换对象
         * @throws IOException IO 异常
         */
        void handle(HttpExchange exchange) throws IOException;
    }
}
