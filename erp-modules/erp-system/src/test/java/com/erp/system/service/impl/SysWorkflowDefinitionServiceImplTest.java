package com.erp.system.service.impl;

import com.erp.system.domain.SysWorkflowDefinition;
import com.erp.system.domain.SysWorkflowInstance;
import com.erp.system.mapper.SysWorkflowDefinitionMapper;
import com.erp.system.mapper.SysWorkflowInstanceMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 流程定义服务单元测试。
 */
@ExtendWith(MockitoExtension.class)
class SysWorkflowDefinitionServiceImplTest {

    @Mock
    private SysWorkflowDefinitionMapper workflowDefinitionMapper;

    @Mock
    private SysWorkflowInstanceMapper workflowInstanceMapper;

    private SysWorkflowDefinitionServiceImpl workflowDefinitionService;

    /**
     * 初始化被测服务并注入 Mapper mock。
     */
    @BeforeEach
    void setUp() {
        workflowDefinitionService = new SysWorkflowDefinitionServiceImpl(workflowInstanceMapper);
        ReflectionTestUtils.setField(workflowDefinitionService, "baseMapper", workflowDefinitionMapper);
        initTableInfoIfAbsent(SysWorkflowDefinition.class);
        initTableInfoIfAbsent(SysWorkflowInstance.class);
    }

    /**
     * 初始化实体元数据缓存，保证 LambdaQueryWrapper 在纯单测场景可用。
     *
     * @param entityClass 实体类型
     */
    private void initTableInfoIfAbsent(Class<?> entityClass) {
        if (TableInfoHelper.getTableInfo(entityClass) != null) {
            return;
        }
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant builderAssistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(builderAssistant, entityClass);
    }

    /**
     * 验证流程定义列表查询可正常返回。
     */
    @Test
    void shouldSelectDefinitionList() {
        when(workflowDefinitionMapper.selectList(any())).thenReturn(Collections.singletonList(new SysWorkflowDefinition()));

        List<SysWorkflowDefinition> definitionList = workflowDefinitionService.selectList("采购", "purchase", "purchase", "1");

        Assertions.assertEquals(1, definitionList.size());
        verify(workflowDefinitionMapper).selectList(any());
    }

    /**
     * 验证新增流程定义时会自动生成版本号并落库。
     */
    @Test
    void shouldCreateDefinitionWithGeneratedVersion() {
        SysWorkflowDefinition definition = new SysWorkflowDefinition();
        definition.setProcessKey("purchase_apply");
        definition.setProcessName("采购审批流程");
        definition.setCategory("purchase");
        definition.setModelContent("{\"startNodeKey\":\"START_1\",\"nodes\":[{\"nodeKey\":\"START_1\",\"nodeType\":\"start\"},{\"nodeKey\":\"NODE_1\",\"nodeType\":\"approval\"}],\"edges\":[{\"from\":\"START_1\",\"to\":\"NODE_1\"}]}");
        when(workflowDefinitionMapper.selectOne(any(), anyBoolean())).thenReturn(null);
        when(workflowDefinitionMapper.selectCount(any())).thenReturn(0L);
        when(workflowDefinitionMapper.insert(any(SysWorkflowDefinition.class))).thenReturn(1);

        boolean success = workflowDefinitionService.createDefinition(definition, "admin");

        Assertions.assertTrue(success);
        Assertions.assertEquals(1, definition.getVersion());
    }

    /**
     * 验证流程定义模型缺少审批节点时不允许新增。
     */
    @Test
    void shouldRejectCreateDefinitionWhenModelInvalid() {
        SysWorkflowDefinition definition = new SysWorkflowDefinition();
        definition.setProcessKey("invalid_apply");
        definition.setProcessName("无审批定义");
        definition.setModelContent("{\"startNodeKey\":\"START_1\",\"nodes\":[{\"nodeKey\":\"START_1\",\"nodeType\":\"start\"},{\"nodeKey\":\"END_1\",\"nodeType\":\"end\"}],\"edges\":[{\"from\":\"START_1\",\"to\":\"END_1\"}]}");

        boolean success = workflowDefinitionService.createDefinition(definition, "admin");

        Assertions.assertFalse(success);
    }

    /**
     * 验证已发布流程定义不允许直接编辑，避免生效版本被篡改。
     */
    @Test
    void shouldRejectUpdateWhenDefinitionNotDraft() {
        SysWorkflowDefinition existed = new SysWorkflowDefinition();
        existed.setDefinitionId(10L);
        existed.setProcessKey("purchase_apply");
        existed.setStatus("1");
        when(workflowDefinitionMapper.selectById(10L)).thenReturn(existed);

        SysWorkflowDefinition updateBody = new SysWorkflowDefinition();
        updateBody.setDefinitionId(10L);
        updateBody.setProcessName("采购审批流程-变更");

        boolean success = workflowDefinitionService.updateDefinition(updateBody, "admin");

        Assertions.assertFalse(success);
    }

    /**
     * 验证发布流程定义时会更新目标版本为已发布状态。
     */
    @Test
    void shouldPublishDefinition() {
        SysWorkflowDefinition existed = new SysWorkflowDefinition();
        existed.setDefinitionId(11L);
        existed.setProcessKey("purchase_apply");
        existed.setStatus("0");
        when(workflowDefinitionMapper.selectById(11L)).thenReturn(existed);
        when(workflowDefinitionMapper.update(any(), any())).thenReturn(1);
        when(workflowDefinitionMapper.updateById(any(SysWorkflowDefinition.class))).thenReturn(1);

        boolean success = workflowDefinitionService.publishDefinition(11L, "admin");

        Assertions.assertTrue(success);
        ArgumentCaptor<SysWorkflowDefinition> updateCaptor = ArgumentCaptor.forClass(SysWorkflowDefinition.class);
        verify(workflowDefinitionMapper).updateById(updateCaptor.capture());
        Assertions.assertEquals("1", updateCaptor.getValue().getStatus());
    }

    /**
     * 验证按流程标识查询版本历史可按预期返回。
     */
    @Test
    void shouldSelectHistoryByProcessKey() {
        when(workflowDefinitionMapper.selectList(any())).thenReturn(Arrays.asList(new SysWorkflowDefinition(), new SysWorkflowDefinition()));

        List<SysWorkflowDefinition> history = workflowDefinitionService.selectHistoryByProcessKey("purchase_apply");

        Assertions.assertEquals(2, history.size());
    }

    /**
     * 验证可从当前定义创建新版本草稿。
     */
    @Test
    void shouldCreateNewVersionFromSourceDefinition() {
        SysWorkflowDefinition source = new SysWorkflowDefinition();
        source.setDefinitionId(20L);
        source.setProcessKey("expense_apply");
        source.setProcessName("报销审批流程");
        source.setCategory("expense");
        source.setVersion(1);
        source.setStatus("1");
        source.setFormSchema("{\"fields\":[]}");
        source.setModelContent("{\"nodes\":[]}");
        source.setRemark("source");
        source.setTenantId("000000");

        SysWorkflowDefinition latest = new SysWorkflowDefinition();
        latest.setVersion(1);

        when(workflowDefinitionMapper.selectById(20L)).thenReturn(source);
        when(workflowDefinitionMapper.selectOne(any(), anyBoolean())).thenReturn(latest);
        when(workflowDefinitionMapper.insert(any(SysWorkflowDefinition.class))).thenReturn(1);

        SysWorkflowDefinition newVersion = workflowDefinitionService.createNewVersion(20L, "admin");

        Assertions.assertNotNull(newVersion);
        Assertions.assertEquals("0", newVersion.getStatus());
        Assertions.assertEquals(2, newVersion.getVersion());
        Assertions.assertEquals("expense_apply", newVersion.getProcessKey());
    }

    /**
     * 验证删除流程定义时会阻止删除已发布版本。
     */
    @Test
    void shouldRejectRemoveWhenDefinitionPublished() {
        SysWorkflowDefinition published = new SysWorkflowDefinition();
        published.setDefinitionId(30L);
        published.setStatus("1");
        when(workflowDefinitionMapper.selectBatchIds(anyCollection())).thenReturn(Collections.singletonList(published));

        boolean success = workflowDefinitionService.removeDefinitions(Collections.singletonList(30L), "admin");

        Assertions.assertFalse(success);
    }

    /**
     * 验证删除草稿定义在无实例绑定时可成功。
     */
    @Test
    void shouldRemoveDefinitionWhenNoInstance() {
        SysWorkflowDefinition draft = new SysWorkflowDefinition();
        draft.setDefinitionId(31L);
        draft.setStatus("0");
        when(workflowDefinitionMapper.selectBatchIds(anyCollection())).thenReturn(Collections.singletonList(draft));
        when(workflowInstanceMapper.selectCount(any())).thenReturn(0L);
        when(workflowDefinitionMapper.deleteBatchIds(anyCollection())).thenReturn(1);

        boolean success = workflowDefinitionService.removeDefinitions(Collections.singletonList(31L), "admin");

        Assertions.assertTrue(success);
    }

    /**
     * 验证按流程标识查询最新发布版本可返回目标结果。
     */
    @Test
    void shouldSelectLatestPublishedDefinition() {
        SysWorkflowDefinition published = new SysWorkflowDefinition();
        published.setDefinitionId(40L);
        published.setStatus("1");
        when(workflowDefinitionMapper.selectOne(any(), anyBoolean())).thenReturn(published);

        SysWorkflowDefinition result = workflowDefinitionService.selectLatestPublishedByProcessKey("purchase_apply");

        Assertions.assertNotNull(result);
        Assertions.assertEquals(40L, result.getDefinitionId());
    }

    /**
     * 验证停用流程定义前会校验定义是否存在。
     */
    @Test
    void shouldDisableDefinition() {
        SysWorkflowDefinition existed = new SysWorkflowDefinition();
        existed.setDefinitionId(50L);
        when(workflowDefinitionMapper.selectById(50L)).thenReturn(existed);
        when(workflowDefinitionMapper.updateById(any(SysWorkflowDefinition.class))).thenReturn(1);

        boolean success = workflowDefinitionService.disableDefinition(50L, "admin");

        Assertions.assertTrue(success);
    }

    /**
     * 验证存在实例时禁止编辑，确保已生效实例不可被篡改。
     */
    @Test
    void shouldRejectUpdateWhenDefinitionHasInstance() {
        SysWorkflowDefinition existed = new SysWorkflowDefinition();
        existed.setDefinitionId(60L);
        existed.setProcessKey("contract_apply");
        existed.setStatus("0");
        when(workflowDefinitionMapper.selectById(60L)).thenReturn(existed);
        when(workflowInstanceMapper.selectCount(any())).thenReturn(1L);

        SysWorkflowDefinition updateBody = new SysWorkflowDefinition();
        updateBody.setDefinitionId(60L);
        updateBody.setProcessName("合同审批流程");

        boolean success = workflowDefinitionService.updateDefinition(updateBody, "admin");

        Assertions.assertFalse(success);
    }

    /**
     * 验证存在实例时禁止删除流程定义。
     */
    @Test
    void shouldRejectRemoveWhenDefinitionHasInstance() {
        SysWorkflowDefinition draft = new SysWorkflowDefinition();
        draft.setDefinitionId(70L);
        draft.setStatus("0");
        when(workflowDefinitionMapper.selectBatchIds(anyCollection())).thenReturn(Collections.singletonList(draft));
        when(workflowInstanceMapper.selectCount(any())).thenReturn(1L);

        boolean success = workflowDefinitionService.removeDefinitions(Collections.singletonList(70L), "admin");

        Assertions.assertFalse(success);
    }
}
