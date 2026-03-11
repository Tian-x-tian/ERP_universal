package com.erp.common.core.domain;

import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 分页数据统一结构。
 *
 * @param <T> 列表元素类型
 */
@Data
public class PageData<T> implements Serializable {
    private List<T> items;
    private long pageNum;
    private long pageSize;
    private long total;

    /**
     * 构建分页数据对象。
     *
     * @param items    列表数据
     * @param pageNum  当前页
     * @param pageSize 每页条数
     * @param total    总数
     * @param <T>      列表元素类型
     * @return 分页数据对象
     */
    public static <T> PageData<T> of(List<T> items, long pageNum, long pageSize, long total) {
        PageData<T> pageData = new PageData<>();
        pageData.setItems(items == null ? Collections.<T>emptyList() : items);
        pageData.setPageNum(pageNum);
        pageData.setPageSize(pageSize);
        pageData.setTotal(total);
        return pageData;
    }
}
