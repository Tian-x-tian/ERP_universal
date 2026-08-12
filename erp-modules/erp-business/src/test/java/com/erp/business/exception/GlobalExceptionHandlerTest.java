package com.erp.business.exception;

import com.erp.common.core.domain.R;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.CannotGetJdbcConnectionException;

/**
 * 业务模块全局异常处理器单元测试。
 */
class GlobalExceptionHandlerTest {

    /**
     * 验证数据库连接失败时返回明确的库存模块提示。
     */
    @Test
    void shouldReturnReadableMessageWhenBusinessDatasourceIsUnavailable() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        ResponseEntity<R<Void>> response = handler.handleDatabaseConnectivityException(
                new CannotGetJdbcConnectionException("Failed to obtain JDBC Connection"));

        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals(500, response.getStatusCode().value());
        Assertions.assertEquals("数据库连接异常",
                response.getBody().getMessage());
    }
}
