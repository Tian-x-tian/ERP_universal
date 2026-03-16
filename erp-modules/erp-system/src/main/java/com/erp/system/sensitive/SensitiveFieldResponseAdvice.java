package com.erp.system.sensitive;

import com.erp.common.core.domain.PageData;
import com.erp.common.core.domain.R;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * 统一处理响应中的敏感字段脱敏。
 */
@RestControllerAdvice
public class SensitiveFieldResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {
        if (!(body instanceof R)) {
            return body;
        }
        if (request instanceof ServletServerHttpRequest) {
            String path = ((ServletServerHttpRequest) request).getServletRequest().getRequestURI();
            if (path == null || !path.contains("/system/mdm/")) {
                return body;
            }
        }
        R<?> result = (R<?>) body;
        maskRecursively(result.getData(), new IdentityHashMap<>());
        return body;
    }

    /**
     * 递归脱敏对象中的标记字段。
     *
     * @param source  原始对象
     * @param visited 已访问对象
     */
    private void maskRecursively(Object source, Map<Object, Boolean> visited) {
        if (source == null || visited.containsKey(source)) {
            return;
        }
        if (source instanceof CharSequence || source instanceof Number || source instanceof Enum<?>) {
            return;
        }
        if (source instanceof PageData) {
            maskRecursively(((PageData<?>) source).getItems(), visited);
            return;
        }
        if (source instanceof Collection) {
            Collection<?> collection = (Collection<?>) source;
            if (CollectionUtils.isEmpty(collection)) {
                return;
            }
            visited.put(source, Boolean.TRUE);
            for (Object item : collection) {
                maskRecursively(item, visited);
            }
            return;
        }
        if (source instanceof Map) {
            visited.put(source, Boolean.TRUE);
            for (Object value : ((Map<?, ?>) source).values()) {
                maskRecursively(value, visited);
            }
            return;
        }
        Package sourcePackage = source.getClass().getPackage();
        if (sourcePackage != null) {
            String packageName = sourcePackage.getName();
            if (packageName.startsWith("java.") || packageName.startsWith("javax.")) {
                return;
            }
        }
        visited.put(source, Boolean.TRUE);
        Class<?> currentClass = source.getClass();
        while (currentClass != null && currentClass != Object.class) {
            Field[] fields = currentClass.getDeclaredFields();
            for (Field field : fields) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                field.setAccessible(true);
                try {
                    SensitiveField annotation = field.getAnnotation(SensitiveField.class);
                    Object fieldValue = field.get(source);
                    if (annotation != null && fieldValue instanceof String) {
                        field.set(source, SensitiveMaskingSupport.mask((String) fieldValue, annotation.value()));
                        continue;
                    }
                    maskRecursively(fieldValue, visited);
                } catch (IllegalAccessException ignored) {
                    // 忽略不可访问字段，避免影响主流程。
                }
            }
            currentClass = currentClass.getSuperclass();
        }
    }
}
