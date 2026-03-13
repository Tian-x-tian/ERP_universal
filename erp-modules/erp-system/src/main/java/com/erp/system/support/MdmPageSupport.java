package com.erp.system.support;

import com.erp.common.core.domain.PageData;

import java.util.Collections;
import java.util.List;

/**
 * MDM 列表分页工具。
 */
public final class MdmPageSupport {
    private static final long DEFAULT_PAGE_NUM = 1L;
    private static final long DEFAULT_PAGE_SIZE = 20L;
    private static final long MAX_PAGE_SIZE = 200L;

    private MdmPageSupport() {
    }

    /**
     * 对列表结果执行内存分页并返回统一分页结构。
     *
     * @param source   原始列表
     * @param pageNum  当前页
     * @param pageSize 每页条数
     * @param <T>      列表元素类型
     * @return 分页数据
     */
    public static <T> PageData<T> paginate(List<T> source, Long pageNum, Long pageSize) {
        List<T> safeSource = source == null ? Collections.emptyList() : source;
        long normalizedPageNum = normalizePageNum(pageNum);
        long normalizedPageSize = normalizePageSize(pageSize);
        int fromIndex = (int) Math.min((normalizedPageNum - 1) * normalizedPageSize, safeSource.size());
        int toIndex = (int) Math.min(fromIndex + normalizedPageSize, safeSource.size());
        return PageData.of(safeSource.subList(fromIndex, toIndex),
                normalizedPageNum,
                normalizedPageSize,
                safeSource.size());
    }

    /**
     * 规范化页码，避免非法页码导致异常。
     *
     * @param pageNum 原始页码
     * @return 规范化后的页码
     */
    public static long normalizePageNum(Long pageNum) {
        return pageNum == null || pageNum < DEFAULT_PAGE_NUM ? DEFAULT_PAGE_NUM : pageNum;
    }

    /**
     * 规范化分页大小，限制最大值。
     *
     * @param pageSize 原始分页大小
     * @return 规范化后的分页大小
     */
    public static long normalizePageSize(Long pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }
}
