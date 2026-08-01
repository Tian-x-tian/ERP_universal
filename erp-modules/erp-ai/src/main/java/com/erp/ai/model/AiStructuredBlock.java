package com.erp.ai.model;

import com.erp.platform.contract.model.PlatformAiDataSet;

import java.io.Serializable;

/**
 * AI 结构化输出区块。
 *
 * <p>一次只读工具调用会同时产出两份东西：一份压缩成文本回灌给模型继续推理，
 * 一份就是这个区块，通过 SSE 的 {@code block} 事件推给前端渲染成指标卡 / 表格 / 图表。
 * 这样面板上看到的可视化与模型看到的事实来自同一次查询，不会出现两边对不上的情况。</p>
 */
public class AiStructuredBlock implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 区块类型，当前固定为 dataset */
    private String blockType = "dataset";
    /** 产出该区块的工具名 */
    private String toolName;
    /** 数据集内容 */
    private PlatformAiDataSet dataSet;

    public AiStructuredBlock() {
    }

    public AiStructuredBlock(String toolName, PlatformAiDataSet dataSet) {
        this.toolName = toolName;
        this.dataSet = dataSet;
    }

    public String getBlockType() {
        return blockType;
    }

    public void setBlockType(String blockType) {
        this.blockType = blockType;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public PlatformAiDataSet getDataSet() {
        return dataSet;
    }

    public void setDataSet(PlatformAiDataSet dataSet) {
        this.dataSet = dataSet;
    }
}
