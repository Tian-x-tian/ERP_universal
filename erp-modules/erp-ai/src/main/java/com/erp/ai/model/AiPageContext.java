package com.erp.ai.model;

/**
 * 当前页面上下文。
 */
public class AiPageContext {
    /**
     * 当前页面路径。
     */
    private String path;

    /**
     * 当前页面标题。
     */
    private String title;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
