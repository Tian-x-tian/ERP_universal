package com.erp.common.web.error;

import com.erp.common.core.context.RequestTraceContextHolder;
import com.erp.common.core.domain.R;
import com.erp.common.core.domain.ResultCode;
import com.erp.common.web.ApiHttpStatusResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 统一错误响应写出器。
 * 负责将统一错误体写回 Servlet / Reactive 响应对象。
 */
public final class ApiErrorResponseWriter {
    private static final String JSON_CONTENT_TYPE = "application/json; charset=UTF-8";

    private ApiErrorResponseWriter() {
    }

    /**
     * 向 Servlet 响应写出统一错误体。
     *
     * @param request      请求对象
     * @param response     响应对象
     * @param objectMapper JSON 工具
     * @param resultCode   业务码
     * @param message      错误消息
     * @throws IOException IO 异常
     */
    public static void writeServlet(HttpServletRequest request,
            HttpServletResponse response,
            ObjectMapper objectMapper,
            ResultCode resultCode,
            String message) throws IOException {
        String traceId = resolveServletTraceId(request, response);
        String path = resolveServletPath(request);
        R<Void> body = ApiErrorResponseFactory.buildErrorBody(resultCode, message, traceId, path);
        response.setStatus(ApiHttpStatusResolver.resolve(body.getCode()).value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(JSON_CONTENT_TYPE);
        response.setHeader(ApiErrorResponseFactory.TRACE_ID_HEADER, body.getTraceId());
        response.getWriter().write(objectMapper.writeValueAsString(body));
        response.getWriter().flush();
    }

    /**
     * 向 Servlet 响应写出统一错误体，并使用默认业务提示语。
     *
     * @param request      请求对象
     * @param response     响应对象
     * @param objectMapper JSON 工具
     * @param resultCode   业务码
     * @throws IOException IO 异常
     */
    public static void writeServlet(HttpServletRequest request,
            HttpServletResponse response,
            ObjectMapper objectMapper,
            ResultCode resultCode) throws IOException {
        writeServlet(request, response, objectMapper, resultCode, resultCode.getMessage());
    }

    /**
     * 向 Reactive 响应写出统一错误体。
     *
     * @param exchange     WebFlux 请求交换对象
     * @param objectMapper JSON 工具
     * @param resultCode   业务码
     * @param message      错误消息
     * @return 写出结果
     */
    public static Mono<Void> writeReactive(ServerWebExchange exchange,
            ObjectMapper objectMapper,
            ResultCode resultCode,
            String message) {
        String traceId = resolveReactiveTraceId(exchange);
        String path = exchange == null || exchange.getRequest() == null ? null : exchange.getRequest().getURI().getPath();
        R<Void> body = ApiErrorResponseFactory.buildErrorBody(resultCode, message, traceId, path);
        exchange.getResponse().setStatusCode(ApiHttpStatusResolver.resolve(body.getCode()));
        exchange.getResponse().getHeaders().setContentType(new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8));
        exchange.getResponse().getHeaders().set(ApiErrorResponseFactory.TRACE_ID_HEADER, body.getTraceId());
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(body);
            return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
        } catch (Exception ex) {
            byte[] fallback = ("{\"code\":50001,\"message\":\"系统异常\",\"traceId\":\"" + body.getTraceId()
                    + "\",\"path\":\"" + (body.getPath() == null ? "" : body.getPath())
                    + "\",\"timestamp\":\"" + body.getTimestamp() + "\"}").getBytes(StandardCharsets.UTF_8);
            return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(fallback)));
        }
    }

    /**
     * 向 Reactive 响应写出统一错误体，并使用默认业务提示语。
     *
     * @param exchange     WebFlux 请求交换对象
     * @param objectMapper JSON 工具
     * @param resultCode   业务码
     * @return 写出结果
     */
    public static Mono<Void> writeReactive(ServerWebExchange exchange,
            ObjectMapper objectMapper,
            ResultCode resultCode) {
        return writeReactive(exchange, objectMapper, resultCode, resultCode.getMessage());
    }

    /**
     * 解析 Servlet 场景 traceId。
     *
     * @param request  请求对象
     * @param response 响应对象
     * @return traceId
     */
    private static String resolveServletTraceId(HttpServletRequest request, HttpServletResponse response) {
        String traceId = RequestTraceContextHolder.getTraceId();
        if (!StringUtils.hasText(traceId) && request != null) {
            traceId = request.getHeader(ApiErrorResponseFactory.TRACE_ID_HEADER);
        }
        if (!StringUtils.hasText(traceId) && response != null) {
            traceId = response.getHeader(ApiErrorResponseFactory.TRACE_ID_HEADER);
        }
        return ApiErrorResponseFactory.resolveOrGenerateTraceId(traceId);
    }

    /**
     * 解析 Servlet 场景请求路径。
     *
     * @param request 请求对象
     * @return 请求路径
     */
    private static String resolveServletPath(HttpServletRequest request) {
        String path = RequestTraceContextHolder.getPath();
        if (!StringUtils.hasText(path) && request != null) {
            path = request.getRequestURI();
        }
        return StringUtils.hasText(path) ? path : null;
    }

    /**
     * 解析 Reactive 场景 traceId。
     *
     * @param exchange WebFlux 请求交换对象
     * @return traceId
     */
    private static String resolveReactiveTraceId(ServerWebExchange exchange) {
        String traceId = exchange == null || exchange.getRequest() == null
                ? null
                : exchange.getRequest().getHeaders().getFirst(ApiErrorResponseFactory.TRACE_ID_HEADER);
        return ApiErrorResponseFactory.resolveOrGenerateTraceId(traceId);
    }
}
