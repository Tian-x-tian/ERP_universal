package com.erp.business.common.controller;

import com.erp.business.common.service.IAiDatasetService;
import com.erp.platform.contract.model.PlatformAiDataSet;
import com.erp.platform.contract.model.PlatformAiDataSetRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 业务域 AI 只读数据集内部契约控制层。
 */
@RestController
@RequestMapping("/business/internal/ai")
public class BusinessAiInternalController {
    private final IAiDatasetService aiDatasetService;

    public BusinessAiInternalController(IAiDatasetService aiDatasetService) {
        this.aiDatasetService = aiDatasetService;
    }

    /**
     * 执行只读数据集查询。
     *
     * @param request 数据集请求
     * @return 数据集结果
     */
    @PostMapping("/dataset")
    public PlatformAiDataSet dataset(@RequestBody PlatformAiDataSetRequest request) {
        return aiDatasetService.query(request);
    }

    /**
     * 列出本模块支持的数据集编码。
     *
     * @return 数据集编码列表
     */
    @GetMapping("/dataset/keys")
    public List<String> datasetKeys() {
        return aiDatasetService.supportedKeys();
    }
}
