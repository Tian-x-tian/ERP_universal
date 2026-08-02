package com.erp.ai.controller;

import com.erp.ai.model.AiPanelCardVO;
import com.erp.ai.model.AiReadToolResult;
import com.erp.ai.service.AiReadToolService;
import com.erp.common.core.domain.R;
import com.erp.common.core.domain.ResultCode;
import com.erp.platform.contract.model.PlatformAiDataSet;
import com.erp.platform.contract.model.PlatformAiDataSetRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * AI 面板控制层。
 *
 * <p>面板卡片直接调用只读工具取数，不经过模型 —— 一屏十几张卡片如果每张都打一次模型，
 * 既慢又贵。模型只在「简报解读」和「追问」时才介入。</p>
 */
@RestController
@RequestMapping("/system/ai/panel")
public class AiPanelController {
    private final AiReadToolService aiReadToolService;

    public AiPanelController(AiReadToolService aiReadToolService) {
        this.aiReadToolService = aiReadToolService;
    }

    /**
     * 查询当前用户可用的面板卡片。
     *
     * @return 卡片列表
     */
    @GetMapping("/cards")
    public R<List<AiPanelCardVO>> cards() {
        return R.success(aiReadToolService.listAvailableCards());
    }

    /**
     * 执行单张卡片的取数。
     *
     * @param request 数据集请求，datasetKey 传只读工具名
     * @return 数据集
     */
    @PostMapping("/dataset")
    public R<PlatformAiDataSet> dataset(@RequestBody(required = false) PlatformAiDataSetRequest request) {
        String toolName = request == null ? null : request.getDatasetKey();
        if (!aiReadToolService.isReadTool(toolName)) {
            return R.failed(ResultCode.PARAM_ERROR);
        }
        // 权限在 AiReadToolService 内部再判一次，这里不做额外放行。
        AiReadToolResult result = aiReadToolService.execute(toolName, request.getParams());
        if (!result.isSuccess() || result.getBlock() == null) {
            return R.failed(ResultCode.FORBIDDEN);
        }
        return R.success(result.getBlock().getDataSet());
    }
}
