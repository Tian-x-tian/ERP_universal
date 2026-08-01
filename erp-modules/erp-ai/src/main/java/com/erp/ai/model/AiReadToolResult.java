package com.erp.ai.model;

import java.io.Serializable;

/**
 * 只读工具执行结果。
 *
 * <p>{@code modelText} 回灌给模型继续推理，{@code block} 推给前端渲染，两者同源。</p>
 */
public class AiReadToolResult implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 工具名称 */
    private String toolName;
    /** 是否执行成功 */
    private boolean success;
    /** 回灌给模型的紧凑文本 */
    private String modelText;
    /** 推送给前端的结构化区块，失败时为空 */
    private AiStructuredBlock block;

    /**
     * 构造成功结果。
     *
     * @param toolName  工具名称
     * @param modelText 回灌文本
     * @param block     结构化区块
     * @return 执行结果
     */
    public static AiReadToolResult success(String toolName, String modelText, AiStructuredBlock block) {
        AiReadToolResult result = new AiReadToolResult();
        result.toolName = toolName;
        result.success = true;
        result.modelText = modelText;
        result.block = block;
        return result;
    }

    /**
     * 构造失败结果。
     *
     * @param toolName 工具名称
     * @param message  失败原因
     * @return 执行结果
     */
    public static AiReadToolResult failure(String toolName, String message) {
        AiReadToolResult result = new AiReadToolResult();
        result.toolName = toolName;
        result.success = false;
        result.modelText = "查询失败：" + message;
        return result;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getModelText() {
        return modelText;
    }

    public void setModelText(String modelText) {
        this.modelText = modelText;
    }

    public AiStructuredBlock getBlock() {
        return block;
    }

    public void setBlock(AiStructuredBlock block) {
        this.block = block;
    }
}
