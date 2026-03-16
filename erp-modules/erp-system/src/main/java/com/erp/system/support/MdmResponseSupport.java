package com.erp.system.support;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.core.domain.PageData;
import com.erp.common.core.domain.R;

import java.util.List;

/**
 * MDM 统一响应辅助工具。
 */
public final class MdmResponseSupport {

    private MdmResponseSupport() {
    }

    /**
     * 将内存分页结果转换为统一分页响应。
     *
     * @param source   原始列表
     * @param pageNum  页码
     * @param pageSize 页长
     * @param <T>      数据类型
     * @return 统一分页响应
     */
    public static <T> R<PageData<T>> page(List<T> source, Long pageNum, Long pageSize) {
        PageData<T> pageData = MdmPageSupport.paginate(source, pageNum, pageSize);
        return R.page(pageData.getItems(), pageData.getPageNum(), pageData.getPageSize(), pageData.getTotal());
    }

    /**
     * 将 MyBatis-Plus 分页结果转换为统一分页响应。
     *
     * @param page 分页对象
     * @param <T>  数据类型
     * @return 统一分页响应
     */
    public static <T> R<PageData<T>> page(Page<T> page) {
        if (page == null) {
            return R.page(List.of(), MdmPageSupport.normalizePageNum(null), MdmPageSupport.normalizePageSize(null), 0L);
        }
        return R.page(page.getRecords(), page.getCurrent(), page.getSize(), page.getTotal());
    }
}
