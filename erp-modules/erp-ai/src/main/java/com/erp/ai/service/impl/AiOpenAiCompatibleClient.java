package com.erp.ai.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.erp.ai.config.ErpAiProperties;
import com.erp.ai.model.AiChatMessage;
import com.erp.ai.model.AiModelCompletion;
import com.erp.ai.model.AiToolCall;
import com.erp.ai.model.AiToolDefinition;
import com.erp.ai.service.AiModelClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * OpenAI 兼容协议模型客户端。
 */
@Service
public class AiOpenAiCompatibleClient implements AiModelClient {
    private static final int MAX_RETRY_ATTEMPTS = 2;
    private final ErpAiProperties erpAiProperties;
    private final ObjectMapper objectMapper;
    private final AiQuotaGuard quotaGuard;

    public AiOpenAiCompatibleClient(ErpAiProperties erpAiProperties, ObjectMapper objectMapper,
            AiQuotaGuard quotaGuard) {
        this.erpAiProperties = erpAiProperties;
        this.objectMapper = objectMapper;
        this.quotaGuard = quotaGuard;
    }

    /**
     * 探测当前模型服务是否可用。
     *
     * @return true 表示可用
     */
    @Override
    public boolean isAvailable() {
        if (!erpAiProperties.isEnabled()) {
            return false;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(resolveUri("/models"))
                    .timeout(Duration.ofMillis(Math.max(1000L, erpAiProperties.getReadTimeoutMs())))
                    .GET()
                    .build();
            HttpResponse<InputStream> response = sendWithRetry(request);
            try (InputStream inputStream = response.body()) {
                return response.statusCode() >= 200 && response.statusCode() < 300;
            }
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * 以非流式方式请求模型补全，并支持工具调用结果解析。
     *
     * @param messages 对话消息列表
     * @param tools    可用工具列表
     * @return 模型补全结果
     * @throws IOException          IO 异常
     * @throws InterruptedException 中断异常
     */
    @Override
    public AiModelCompletion completeChat(List<AiChatMessage> messages, List<AiToolDefinition> tools)
            throws IOException, InterruptedException {
        return completeChat(null, messages, tools);
    }

    /**
     * 以非流式方式请求指定模型补全，并支持工具调用结果解析。
     *
     * @param model    模型编号
     * @param messages 对话消息列表
     * @param tools    可用工具列表
     * @return 模型补全结果
     * @throws IOException          IO 异常
     * @throws InterruptedException 中断异常
     */
    @Override
    public AiModelCompletion completeChat(String model, List<AiChatMessage> messages, List<AiToolDefinition> tools)
            throws IOException, InterruptedException {
        String requestBody = buildChatRequestBody(model, messages, false, tools);
        AiQuotaReservation reservation = quotaGuard.reserve(inputTokenUpperBound(requestBody));
        AiModelCompletion completion;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(resolveUri(erpAiProperties.getChatPath()))
                    .timeout(Duration.ofMillis(Math.max(5000L, erpAiProperties.getReadTimeoutMs())))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json, text/event-stream")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<InputStream> response = sendWithRetry(request);
            try (InputStream inputStream = response.body()) {
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new IOException(resolveErrorMessage(readAsString(inputStream), response.statusCode()));
                }
                String contentType = response.headers().firstValue("Content-Type").orElse("");
                if (isEventStreamResponse(contentType)) {
                    completion = readEventStream(inputStream, null, new AiModelCompletion());
                } else {
                    completion = extractCompletion(readAsString(inputStream));
                }
            }
        } catch (IOException | InterruptedException | RuntimeException ex) {
            releaseAfterFailure(reservation, ex);
            throw ex;
        }
        quotaGuard.settle(reservation, completion.getInputTokens(), completion.getOutputTokens(),
                completion.isUsageReported());
        return completion;
    }

    /**
     * 以流式方式请求模型回复。
     *
     * @param messages      对话消息列表
     * @param deltaConsumer 流式增量回调
     * @return 最终完整回复
     * @throws IOException          IO 异常
     * @throws InterruptedException 中断异常
     */
    @Override
    public String streamChat(List<AiChatMessage> messages, Consumer<String> deltaConsumer) throws IOException, InterruptedException {
        return streamChat(null, messages, deltaConsumer);
    }

    /**
     * 以流式方式请求指定模型回复。
     *
     * @param model         模型编号
     * @param messages      对话消息列表
     * @param deltaConsumer 流式增量回调
     * @return 最终完整回复
     * @throws IOException          IO 异常
     * @throws InterruptedException 中断异常
     */
    @Override
    public String streamChat(String model, List<AiChatMessage> messages, Consumer<String> deltaConsumer)
            throws IOException, InterruptedException {
        String requestBody = buildChatRequestBody(model, messages, true, Collections.emptyList());
        AiQuotaReservation reservation = quotaGuard.reserve(inputTokenUpperBound(requestBody));
        AiModelCompletion completion = new AiModelCompletion();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(resolveUri(erpAiProperties.getChatPath()))
                    .timeout(Duration.ofMillis(Math.max(5000L, erpAiProperties.getReadTimeoutMs())))
                    .header("Content-Type", "application/json")
                    .header("Accept", "text/event-stream, application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<InputStream> response = sendWithRetry(request);
            try (InputStream inputStream = response.body()) {
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new IOException(resolveErrorMessage(readAsString(inputStream), response.statusCode()));
                }
                String contentType = response.headers().firstValue("Content-Type").orElse("");
                if (isEventStreamResponse(contentType)) {
                    completion = readEventStream(inputStream, deltaConsumer, completion);
                } else {
                    completion = extractCompletion(readAsString(inputStream));
                }
                if (!StringUtils.hasText(completion.getContent())) {
                    throw new IOException("AI 未返回有效内容");
                }
            }
        } catch (IOException | InterruptedException | RuntimeException ex) {
            if (completion.isUsageReported()) {
                settleAfterFailure(reservation, completion, ex);
            } else {
                releaseAfterFailure(reservation, ex);
            }
            throw ex;
        }
        quotaGuard.settle(reservation, completion.getInputTokens(), completion.getOutputTokens(),
                completion.isUsageReported());
        return completion.getContent();
    }

    private long inputTokenUpperBound(String requestBody) {
        if (!StringUtils.hasText(requestBody)) {
            return 1L;
        }
        return Math.max(1L, requestBody.getBytes(StandardCharsets.UTF_8).length);
    }

    private void settleAfterFailure(AiQuotaReservation reservation, AiModelCompletion completion, Throwable failure) {
        try {
            quotaGuard.settle(reservation, completion.getInputTokens(), completion.getOutputTokens(), true);
        } catch (RuntimeException settlementFailure) {
            failure.addSuppressed(settlementFailure);
        }
    }

    private void releaseAfterFailure(AiQuotaReservation reservation, Throwable failure) {
        try {
            quotaGuard.release(reservation);
        } catch (RuntimeException releaseFailure) {
            failure.addSuppressed(releaseFailure);
        }
    }

    /**
     * 构造 Chat Completions 请求体。
     *
     * @param messages 对话消息列表
     * @return JSON 字符串
     * @throws IOException 序列化异常
     */
    private String buildChatRequestBody(String model, List<AiChatMessage> messages, boolean stream, List<AiToolDefinition> tools)
            throws IOException {
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", resolveModel(model));
        requestBody.put("stream", stream);
        int maxOutputTokens = erpAiProperties.getMaxOutputTokens();
        if (maxOutputTokens <= 0) {
            throw new IllegalStateException("erp.ai.max-output-tokens must be positive");
        }
        requestBody.put("max_tokens", maxOutputTokens);
        if (stream) {
            requestBody.put("stream_options", Map.of("include_usage", true));
        }
        List<Map<String, String>> messageList = new ArrayList<>();
        for (AiChatMessage message : messages) {
            if (message == null || !StringUtils.hasText(message.getRole()) || !StringUtils.hasText(message.getContent())) {
                continue;
            }
            Map<String, String> messageMap = new LinkedHashMap<>();
            messageMap.put("role", message.getRole().trim());
            messageMap.put("content", message.getContent().trim());
            messageList.add(messageMap);
        }
        requestBody.put("messages", messageList);
        List<Map<String, Object>> toolList = buildToolPayload(tools);
        if (!toolList.isEmpty()) {
            requestBody.put("tools", toolList);
            requestBody.put("tool_choice", "auto");
        }
        return objectMapper.writeValueAsString(requestBody);
    }

    /**
     * 解析最终模型编号。
     *
     * @param model 指定模型
     * @return 模型编号
     */
    private String resolveModel(String model) {
        if (StringUtils.hasText(model)) {
            return model.trim();
        }
        return erpAiProperties.getModel();
    }

    /**
     * 构造 OpenAI 兼容工具载荷。
     *
     * @param tools 工具定义列表
     * @return 工具载荷
     */
    private List<Map<String, Object>> buildToolPayload(List<AiToolDefinition> tools) {
        if (tools == null || tools.isEmpty()) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> toolList = new ArrayList<>();
        for (AiToolDefinition tool : tools) {
            if (tool == null || !StringUtils.hasText(tool.getName())) {
                continue;
            }
            Map<String, Object> function = new LinkedHashMap<>();
            function.put("name", tool.getName().trim());
            function.put("description", StringUtils.hasText(tool.getDescription()) ? tool.getDescription().trim() : "");
            function.put("parameters", tool.getParameters() == null ? Collections.emptyMap() : tool.getParameters());

            Map<String, Object> toolMap = new LinkedHashMap<>();
            toolMap.put("type", "function");
            toolMap.put("function", function);
            toolList.add(toolMap);
        }
        return toolList;
    }

    /**
     * 读取 OpenAI 风格 SSE 流式响应。
     *
     * @param inputStream   响应输入流
     * @param deltaConsumer 增量回调
     * @return 完整回复文本
     * @throws IOException IO 异常
     */
    private AiModelCompletion readEventStream(InputStream inputStream, Consumer<String> deltaConsumer,
            AiModelCompletion completion) throws IOException {
        StringBuilder fullContent = new StringBuilder();
        StringBuilder eventDataBuffer = new StringBuilder();
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.isBlank()) {
                    if (handleEventBlock(eventDataBuffer.toString(), deltaConsumer, fullContent, completion)) {
                        break;
                    }
                    eventDataBuffer.setLength(0);
                    continue;
                }
                if (line.startsWith("data:")) {
                    if (eventDataBuffer.length() > 0) {
                        eventDataBuffer.append('\n');
                    }
                    eventDataBuffer.append(line.substring(5).trim());
                }
            }
            if (eventDataBuffer.length() > 0) {
                handleEventBlock(eventDataBuffer.toString(), deltaConsumer, fullContent, completion);
            }
        }
        completion.setContent(fullContent.toString());
        return completion;
    }

    /**
     * 处理单个 SSE 事件块。
     *
     * @param eventPayload  SSE 事件负载
     * @param deltaConsumer 增量回调
     * @param fullContent   完整文本缓冲区
     * @return true 表示流已结束
     * @throws IOException 解析异常
     */
    private boolean handleEventBlock(String eventPayload, Consumer<String> deltaConsumer,
            StringBuilder fullContent, AiModelCompletion completion) throws IOException {
        if (!StringUtils.hasText(eventPayload)) {
            return false;
        }
        if ("[DONE]".equals(eventPayload.trim())) {
            return true;
        }
        JsonNode rootNode = objectMapper.readTree(eventPayload);
        applyUsage(rootNode, completion);
        JsonNode choicesNode = rootNode.path("choices");
        if (!choicesNode.isArray()) {
            return false;
        }
        for (JsonNode choiceNode : choicesNode) {
            String deltaText = extractTextValue(choiceNode.path("delta").path("content"));
            if (!StringUtils.hasText(deltaText)) {
                deltaText = extractTextValue(choiceNode.path("message").path("content"));
            }
            if (!StringUtils.hasText(deltaText)) {
                continue;
            }
            fullContent.append(deltaText);
            if (deltaConsumer != null) {
                deltaConsumer.accept(deltaText);
            }
        }
        return false;
    }

    /**
     * 从非流式 JSON 响应中解析最终文本。
     *
     * @param body 原始响应体
     * @return 回复文本
     * @throws IOException JSON 解析异常
     */
    private String extractResponseContent(String body) throws IOException {
        return extractCompletion(body).getContent();
    }

    /**
     * 从普通 JSON 响应中解析文本与工具调用。
     *
     * @param body 原始响应体
     * @return 模型补全结果
     * @throws IOException JSON 解析异常
     */
    private AiModelCompletion extractCompletion(String body) throws IOException {
        AiModelCompletion completion = new AiModelCompletion();
        if (!StringUtils.hasText(body)) {
            return completion;
        }
        JsonNode rootNode = objectMapper.readTree(body);
        applyUsage(rootNode, completion);
        String outputText = extractTextValue(rootNode.path("output_text"));
        if (StringUtils.hasText(outputText)) {
            completion.setContent(outputText);
            return completion;
        }
        JsonNode choicesNode = rootNode.path("choices");
        if (!choicesNode.isArray()) {
            return completion;
        }
        for (JsonNode choiceNode : choicesNode) {
            JsonNode messageNode = choiceNode.path("message");
            String messageContent = extractTextValue(messageNode.path("content"));
            if (StringUtils.hasText(messageContent) && !StringUtils.hasText(completion.getContent())) {
                completion.setContent(messageContent);
            }
            String textContent = extractTextValue(choiceNode.path("text"));
            if (StringUtils.hasText(textContent) && !StringUtils.hasText(completion.getContent())) {
                completion.setContent(textContent);
            }
            completion.setToolCalls(extractToolCalls(messageNode.path("tool_calls")));
            if (StringUtils.hasText(completion.getContent()) || !completion.getToolCalls().isEmpty()) {
                return completion;
            }
        }
        return completion;
    }

    private void applyUsage(JsonNode rootNode, AiModelCompletion completion) {
        if (rootNode == null || completion == null) {
            return;
        }
        JsonNode usageNode = rootNode.path("usage");
        if (!usageNode.isObject()) {
            return;
        }
        Long inputTokens = tokenValue(usageNode, "prompt_tokens", "input_tokens");
        Long outputTokens = tokenValue(usageNode, "completion_tokens", "output_tokens");
        if (inputTokens != null || outputTokens != null) {
            completion.setInputTokens(inputTokens);
            completion.setOutputTokens(outputTokens);
            completion.setUsageReported(true);
        }
    }

    private Long tokenValue(JsonNode usageNode, String primaryField, String compatibleField) {
        JsonNode value = usageNode.get(primaryField);
        if (value == null || !value.canConvertToLong()) {
            value = usageNode.get(compatibleField);
        }
        if (value == null || !value.canConvertToLong() || value.asLong() < 0) {
            return null;
        }
        return value.asLong();
    }

    /**
     * 解析工具调用列表。
     *
     * @param toolCallsNode 工具调用节点
     * @return 工具调用列表
     */
    private List<AiToolCall> extractToolCalls(JsonNode toolCallsNode) {
        if (toolCallsNode == null || !toolCallsNode.isArray()) {
            return Collections.emptyList();
        }
        List<AiToolCall> toolCallList = new ArrayList<>();
        for (JsonNode toolCallNode : toolCallsNode) {
            JsonNode functionNode = toolCallNode.path("function");
            String functionName = extractTextValue(functionNode.path("name"));
            if (!StringUtils.hasText(functionName)) {
                continue;
            }
            AiToolCall toolCall = new AiToolCall();
            toolCall.setId(extractTextValue(toolCallNode.path("id")));
            toolCall.setName(functionName);
            toolCall.setArgumentsJson(extractArgumentsJson(functionNode.path("arguments")));
            toolCallList.add(toolCall);
        }
        return toolCallList;
    }

    /**
     * 提取工具参数 JSON 字符串。
     *
     * @param argumentsNode 参数节点
     * @return JSON 字符串
     */
    private String extractArgumentsJson(JsonNode argumentsNode) {
        if (argumentsNode == null || argumentsNode.isMissingNode() || argumentsNode.isNull()) {
            return "{}";
        }
        if (argumentsNode.isTextual()) {
            return argumentsNode.asText("{}");
        }
        try {
            return objectMapper.writeValueAsString(argumentsNode);
        } catch (Exception ex) {
            return "{}";
        }
    }

    /**
     * 提取不同 JSON 结构下的文本值。
     *
     * @param node JSON 节点
     * @return 文本值
     */
    private String extractTextValue(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.isObject()) {
            String textValue = extractTextValue(node.get("text"));
            if (StringUtils.hasText(textValue)) {
                return textValue;
            }
        }
        if (node.isArray()) {
            StringBuilder textBuffer = new StringBuilder();
            for (JsonNode childNode : node) {
                String childText = extractTextValue(childNode);
                if (!StringUtils.hasText(childText)) {
                    continue;
                }
                textBuffer.append(childText);
            }
            return textBuffer.length() == 0 ? null : textBuffer.toString();
        }
        return null;
    }

    /**
     * 判断响应是否为 SSE 流。
     *
     * @param contentType 响应 Content-Type
     * @return true 表示 SSE 响应
     */
    private boolean isEventStreamResponse(String contentType) {
        return StringUtils.hasText(contentType)
                && contentType.toLowerCase().contains("text/event-stream");
    }

    /**
     * 读取输入流为字符串。
     *
     * @param inputStream 输入流
     * @return 文本内容
     * @throws IOException IO 异常
     */
    private String readAsString(InputStream inputStream) throws IOException {
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }

    /**
     * 解析错误提示文案。
     *
     * @param body       原始响应体
     * @param statusCode HTTP 状态码
     * @return 错误提示
     */
    private String resolveErrorMessage(String body, int statusCode) {
        if (!StringUtils.hasText(body)) {
            return "AI 服务请求失败，状态码：" + statusCode;
        }
        try {
            JsonNode rootNode = objectMapper.readTree(body);
            String message = rootNode.path("error").path("message").asText(null);
            if (StringUtils.hasText(message)) {
                return message;
            }
        } catch (Exception ignored) {
            // ignore
        }
        return body;
    }

    /**
     * 拼装完整请求地址。
     *
     * @param requestPath 请求路径
     * @return 完整请求 URI
     */
    private URI resolveUri(String requestPath) {
        String baseUrl = StringUtils.hasText(erpAiProperties.getBaseUrl())
                ? erpAiProperties.getBaseUrl().trim()
                : "http://127.0.0.1:8317/v1";
        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String normalizedRequestPath = StringUtils.hasText(requestPath) ? requestPath.trim() : "";
        if (!normalizedRequestPath.startsWith("/")) {
            normalizedRequestPath = "/" + normalizedRequestPath;
        }
        return URI.create(normalizedBaseUrl + normalizedRequestPath);
    }

    /**
     * 构造 HTTP 客户端。
     *
     * @return HTTP 客户端
     */
    private HttpClient buildHttpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(Math.max(1000L, erpAiProperties.getConnectTimeoutMs())))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    /**
     * 发送模型请求，并在上游偶发断链或 5xx 时自动重试一次。
     *
     * @param request HTTP 请求
     * @return HTTP 响应
     * @throws IOException          IO 异常
     * @throws InterruptedException 中断异常
     */
    private HttpResponse<InputStream> sendWithRetry(HttpRequest request) throws IOException, InterruptedException {
        IOException lastIOException = null;
        HttpResponse<InputStream> lastResponse = null;
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                HttpResponse<InputStream> response = buildHttpClient().send(request, HttpResponse.BodyHandlers.ofInputStream());
                if (response.statusCode() >= 500 && attempt < MAX_RETRY_ATTEMPTS) {
                    closeQuietly(response.body());
                    lastResponse = response;
                    continue;
                }
                return response;
            } catch (IOException ex) {
                lastIOException = ex;
                if (attempt >= MAX_RETRY_ATTEMPTS) {
                    throw ex;
                }
            }
        }
        if (lastResponse != null) {
            return lastResponse;
        }
        throw lastIOException == null ? new IOException("AI 服务请求失败") : lastIOException;
    }

    /**
     * 安静关闭输入流，避免重试时遗留连接。
     *
     * @param inputStream 输入流
     */
    private void closeQuietly(InputStream inputStream) {
        if (inputStream == null) {
            return;
        }
        try (InputStream ignored = inputStream; OutputStream nullOutputStream = OutputStream.nullOutputStream()) {
            ignored.transferTo(nullOutputStream);
        } catch (Exception ignored) {
            // ignore
        }
    }
}
