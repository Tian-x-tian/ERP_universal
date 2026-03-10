package com.erp.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.erp.system.domain.SysWorkflowDefinition;

import java.util.List;

/**
 * 流程定义服务接口
 */
public interface ISysWorkflowDefinitionService extends IService<SysWorkflowDefinition> {

    /**
     * 查询流程定义列表。
     *
     * @param processName 流程名称关键字
     * @param processKey  流程标识关键字
     * @param category    流程分类
     * @param status      状态
     * @return 流程定义列表
     */
    List<SysWorkflowDefinition> selectList(String processName, String processKey, String category, String status);

    /**
     * 新增流程定义。
     *
     * @param definition 流程定义
     * @param operator   操作人账号
     * @return 新增结果
     */
    boolean createDefinition(SysWorkflowDefinition definition, String operator);

    /**
     * 修改流程定义。
     *
     * @param definition 流程定义
     * @param operator   操作人账号
     * @return 修改结果
     */
    boolean updateDefinition(SysWorkflowDefinition definition, String operator);

    /**
     * 发布流程定义。
     *
     * @param definitionId 流程定义ID
     * @param operator     操作人账号
     * @return 发布结果
     */
    boolean publishDefinition(Long definitionId, String operator);

    /**
     * 停用流程定义。
     *
     * @param definitionId 流程定义ID
     * @param operator     操作人账号
     * @return 停用结果
     */
    boolean disableDefinition(Long definitionId, String operator);

    /**
     * 按流程标识查询版本历史。
     *
     * @param processKey 流程标识
     * @return 版本历史列表（按版本倒序）
     */
    List<SysWorkflowDefinition> selectHistoryByProcessKey(String processKey);

    /**
     * 从已有流程定义创建新版本草稿。
     *
     * @param definitionId 来源流程定义ID
     * @param operator     操作人账号
     * @return 新版本草稿，创建失败返回 null
     */
    SysWorkflowDefinition createNewVersion(Long definitionId, String operator);

    /**
     * 删除流程定义（受保护删除）。
     *
     * @param definitionIds 流程定义ID集合
     * @param operator      操作人账号
     * @return 删除结果
     */
    boolean removeDefinitions(List<Long> definitionIds, String operator);

    /**
     * 按流程标识查询最新发布版本。
     *
     * @param processKey 流程标识
     * @return 已发布流程定义
     */
    SysWorkflowDefinition selectLatestPublishedByProcessKey(String processKey);
}
