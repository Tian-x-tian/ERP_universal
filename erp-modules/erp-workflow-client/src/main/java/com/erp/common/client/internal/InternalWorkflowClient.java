package com.erp.common.client.internal;

import com.erp.workflow.contract.domain.vo.WorkflowInstanceDetailVO;
import com.erp.workflow.contract.domain.vo.WorkflowDefinitionLiteVO;
import com.erp.workflow.contract.domain.vo.WorkflowProcessOptionVO;
import com.erp.workflow.contract.domain.vo.WorkflowStartBody;
import com.erp.workflow.contract.domain.vo.WorkflowTaskActionBody;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Collections;
import java.util.List;

/**
 * 工作流内部接口客户端。
 */
@Component
public class InternalWorkflowClient {
    private final RestTemplate restTemplate;
    private final InternalRequestHeaderFactory headerFactory;
    private final InternalSystemClientProperties properties;

    public InternalWorkflowClient(RestTemplate internalSystemRestTemplate,
            InternalRequestHeaderFactory headerFactory,
            InternalSystemClientProperties properties) {
        this.restTemplate = internalSystemRestTemplate;
        this.headerFactory = headerFactory;
        this.properties = properties;
    }

    /**
     * 查询业务动作可选流程列表。
     *
     * @param domainType 业务域类型
     * @param actionCode 动作编码
     * @return 流程选项列表
     */
    public List<WorkflowProcessOptionVO> listProcessOptions(String domainType, String actionCode) {
        ResponseEntity<List<WorkflowProcessOptionVO>> response = restTemplate.exchange(
                UriComponentsBuilder.fromUri(buildUri("/workflow/internal/bindings/options"))
                        .queryParam("domainType", domainType)
                        .queryParam("actionCode", actionCode)
                        .build(true)
                        .toUri(),
                HttpMethod.GET,
                new HttpEntity<>(headerFactory.buildHeaders()),
                new ParameterizedTypeReference<List<WorkflowProcessOptionVO>>() {
                });
        List<WorkflowProcessOptionVO> body = response.getBody();
        return body == null ? Collections.emptyList() : body;
    }

    /**
     * 查询指定业务的最新流程实例详情。
     *
     * @param businessType 业务类型
     * @param businessNo   业务单号
     * @return 流程实例详情
     */
    public WorkflowInstanceDetailVO getLatestInstanceDetail(String businessType, String businessNo) {
        URI uri = UriComponentsBuilder.fromUri(buildUri("/workflow/internal/instances/latest/detail"))
                .queryParam("businessType", businessType)
                .queryParam("businessNo", businessNo)
                .build(true)
                .toUri();
        return exchange(uri, HttpMethod.GET, null, WorkflowInstanceDetailVO.class);
    }

    /**
     * 判断是否存在运行中的流程实例。
     *
     * @param businessType 业务类型
     * @param businessNo   业务单号
     * @return true 表示存在运行中实例
     */
    public boolean hasRunningInstance(String businessType, String businessNo) {
        URI uri = UriComponentsBuilder.fromUri(buildUri("/workflow/internal/instances/running"))
                .queryParam("businessType", businessType)
                .queryParam("businessNo", businessNo)
                .build(true)
                .toUri();
        Boolean response = exchange(uri, HttpMethod.GET, null, Boolean.class);
        return Boolean.TRUE.equals(response);
    }

    /**
     * 发起流程。
     *
     * @param startBody 发起参数
     * @return true 表示成功
     */
    public boolean startProcess(WorkflowStartBody startBody) {
        Boolean response = exchange(buildUri("/workflow/internal/start"),
                HttpMethod.POST,
                startBody,
                Boolean.class);
        return Boolean.TRUE.equals(response);
    }

    /**
     * 审批通过任务。
     *
     * @param taskId     任务ID
     * @param actionBody 审批参数
     * @return true 表示成功
     */
    public boolean approveTask(Long taskId, WorkflowTaskActionBody actionBody) {
        Boolean response = exchange(buildUri("/workflow/internal/tasks/approve/" + taskId),
                HttpMethod.POST,
                actionBody,
                Boolean.class);
        return Boolean.TRUE.equals(response);
    }

    /**
     * 驳回任务。
     *
     * @param taskId     任务ID
     * @param actionBody 审批参数
     * @return true 表示成功
     */
    public boolean rejectTask(Long taskId, WorkflowTaskActionBody actionBody) {
        Boolean response = exchange(buildUri("/workflow/internal/tasks/reject/" + taskId),
                HttpMethod.POST,
                actionBody,
                Boolean.class);
        return Boolean.TRUE.equals(response);
    }

    /**
     * 中止指定业务的运行中流程实例。
     *
     * @param businessType 业务类型
     * @param businessNo   业务单号
     * @param actionBody   中止参数
     * @return true 表示成功
     */
    public boolean abortProcess(String businessType, String businessNo, WorkflowTaskActionBody actionBody) {
        URI uri = UriComponentsBuilder.fromUri(buildUri("/workflow/internal/instances/abort"))
                .queryParam("businessType", businessType)
                .queryParam("businessNo", businessNo)
                .build(true)
                .toUri();
        Boolean response = exchange(uri, HttpMethod.POST, actionBody, Boolean.class);
        return Boolean.TRUE.equals(response);
    }

    /**
     * 发布流程定义。
     *
     * @param definitionId 流程定义ID
     * @return true 表示成功
     */
    public boolean publishDefinition(Long definitionId) {
        Boolean response = exchange(buildUri("/workflow/internal/definitions/publish/" + definitionId),
                HttpMethod.POST,
                null,
                Boolean.class);
        return Boolean.TRUE.equals(response);
    }

    /**
     * 查询流程定义轻量列表。
     *
     * @param processName 流程名称关键字
     * @param processKey  流程标识关键字
     * @param status      状态
     * @return 轻量流程定义列表
     */
    public List<WorkflowDefinitionLiteVO> listDefinitionLite(String processName, String processKey, String status) {
        ResponseEntity<List<WorkflowDefinitionLiteVO>> response = restTemplate.exchange(
                UriComponentsBuilder.fromUri(buildUri("/workflow/internal/definitions/lite"))
                        .queryParamIfPresent("processName", nullableValue(processName))
                        .queryParamIfPresent("processKey", nullableValue(processKey))
                        .queryParamIfPresent("status", nullableValue(status))
                        .build(true)
                        .toUri(),
                HttpMethod.GET,
                new HttpEntity<>(headerFactory.buildHeaders()),
                new ParameterizedTypeReference<List<WorkflowDefinitionLiteVO>>() {
                });
        List<WorkflowDefinitionLiteVO> body = response.getBody();
        return body == null ? Collections.emptyList() : body;
    }

    /**
     * 签收待办。
     *
     * @param todoId 待办ID
     * @return true 表示成功
     */
    public boolean claimTodo(Long todoId) {
        Boolean response = exchange(buildUri("/workflow/internal/todos/claim/" + todoId),
                HttpMethod.POST,
                null,
                Boolean.class);
        return Boolean.TRUE.equals(response);
    }

    /**
     * 办结待办。
     *
     * @param todoId 待办ID
     * @return true 表示成功
     */
    public boolean finishTodo(Long todoId) {
        Boolean response = exchange(buildUri("/workflow/internal/todos/finish/" + todoId),
                HttpMethod.POST,
                null,
                Boolean.class);
        return Boolean.TRUE.equals(response);
    }

    public com.erp.workflow.contract.domain.SysTodoTask getTodoTask(Long todoId) {
        return exchange(buildUri("/workflow/internal/tasks/todo/" + todoId), HttpMethod.GET, null,
                com.erp.workflow.contract.domain.SysTodoTask.class);
    }

    public com.erp.workflow.contract.domain.SysTodoTask getTodoTaskByTaskId(Long taskId) {
        return exchange(buildUri("/workflow/internal/tasks/todo/by-task/" + taskId), HttpMethod.GET, null,
                com.erp.workflow.contract.domain.SysTodoTask.class);
    }

    public com.erp.workflow.contract.domain.SysWorkflowTask getWorkflowTask(Long taskId) {
        return exchange(buildUri("/workflow/internal/tasks/workflow/" + taskId), HttpMethod.GET, null,
                com.erp.workflow.contract.domain.SysWorkflowTask.class);
    }

    public com.erp.workflow.contract.domain.SysWorkflowTask getWorkflowTaskByTodoId(Long todoId) {
        return exchange(buildUri("/workflow/internal/tasks/workflow/by-todo/" + todoId), HttpMethod.GET, null,
                com.erp.workflow.contract.domain.SysWorkflowTask.class);
    }

    public Long countPendingTodos(Long userId) {
        return exchange(UriComponentsBuilder.fromUri(buildUri("/workflow/internal/tasks/todo/pending-count"))
                .queryParam("userId", userId).build(true).toUri(), HttpMethod.GET, null, Long.class);
    }

    public List<com.erp.workflow.contract.domain.SysTodoTask> getPendingTodos(Long userId, int limit) {
        ResponseEntity<List<com.erp.workflow.contract.domain.SysTodoTask>> response = restTemplate.exchange(
                UriComponentsBuilder.fromUri(buildUri("/workflow/internal/tasks/todo/pending"))
                        .queryParam("userId", userId)
                        .queryParam("limit", limit)
                        .build(true)
                        .toUri(),
                HttpMethod.GET,
                new HttpEntity<>(headerFactory.buildHeaders()),
                new ParameterizedTypeReference<List<com.erp.workflow.contract.domain.SysTodoTask>>() {
                });
        List<com.erp.workflow.contract.domain.SysTodoTask> body = response.getBody();
        return body == null ? Collections.emptyList() : body;
    }

    /**
     * 发起内部 HTTP 调用。
     *
     * @param uri          目标地址
     * @param method       请求方法
     * @param body         请求体
     * @param responseType 响应类型
     * @param <T>          响应泛型
     * @return 响应对象
     */
    private <T> T exchange(URI uri, HttpMethod method, Object body, Class<T> responseType) {
        HttpHeaders headers = headerFactory.buildHeaders();
        ResponseEntity<T> response = restTemplate.exchange(uri, method, new HttpEntity<>(body, headers), responseType);
        return response.getBody();
    }

    /**
     * 构建完整内部调用地址。
     *
     * @param path 接口路径
     * @return URI
     */
    private URI buildUri(String path) {
        return UriComponentsBuilder.fromHttpUrl(properties.resolveWorkflowBaseUrl())
                .path(path)
                .build(true)
                .toUri();
    }

    /**
     * 将可选字符串包装为 Optional，便于 URI 组装时自动忽略空值。
     *
     * @param value 原始值
     * @return Optional 包装结果
     */
    private java.util.Optional<String> nullableValue(String value) {
        return StringUtils.hasText(value) ? java.util.Optional.of(value.trim()) : java.util.Optional.empty();
    }
}
