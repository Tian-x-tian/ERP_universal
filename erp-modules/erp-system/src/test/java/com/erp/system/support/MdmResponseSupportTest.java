package com.erp.system.support;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.core.domain.PageData;
import com.erp.common.core.domain.R;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * MDM 统一分页响应辅助工具单元测试。
 */
class MdmResponseSupportTest {

    /**
     * 验证分页响应会统一输出 PageData 结构。
     */
    @Test
    void shouldWrapPageResultIntoPageData() {
        Page<String> page = new Page<>(2L, 5L, 12L);
        page.setRecords(List.of("A", "B"));

        R<PageData<String>> response = MdmResponseSupport.page(page);

        Assertions.assertTrue(response.isSuccess());
        Assertions.assertNotNull(response.getData());
        Assertions.assertEquals(2L, response.getData().getPageNum());
        Assertions.assertEquals(5L, response.getData().getPageSize());
        Assertions.assertEquals(12L, response.getData().getTotal());
        Assertions.assertEquals(List.of("A", "B"), response.getData().getItems());
    }
}
