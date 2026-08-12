package com.erp.workflow.service;

import com.erp.workflow.contract.domain.SysWorkflowDefinition;
import com.erp.workflow.contract.domain.vo.WorkflowTemplateActivateBody;
import com.erp.workflow.contract.domain.vo.WorkflowTemplateVO;

import java.util.List;

/**
 * 流程模板服务接口。
 */
public interface ISysWorkflowTemplateService {

    /**
     * 查询模板市场列表。
     *
     * @param industry 行业筛选
     * @return 模板列表
     */
    List<WorkflowTemplateVO> selectTemplateList(String industry);

    /**
     * 启用模板并生成流程定义草稿。
     *
     * @param templateCode 模板编码
     * @param activateBody 启用参数
     * @param operator     操作人账号
     * @return 新增的流程定义
     */
    SysWorkflowDefinition activateTemplate(String templateCode, WorkflowTemplateActivateBody activateBody, String operator);
}


