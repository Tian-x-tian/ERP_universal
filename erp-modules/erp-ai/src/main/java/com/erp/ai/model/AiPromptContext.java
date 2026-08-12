package com.erp.ai.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * AI 提示词上下文。
 */
public class AiPromptContext {
    /**
     * 当前用户上下文。
     */
    private CurrentUserContext currentUser;

    /**
     * 当前页面上下文。
     */
    private AiPageContext pageContext;

    /**
     * 待办上下文是否可用。
     */
    private boolean todoContextAvailable;

    /**
     * 待办上下文状态提示。
     */
    private String todoContextMessage;

    /**
     * 当前用户待办数量。
     */
    private int todoCount;

    /**
     * 注入提示词的待办摘要列表。
     */
    private List<TodoSummary> todoList = new ArrayList<>();

    /**
     * 消息上下文是否可用。
     */
    private boolean noticeContextAvailable;

    /**
     * 消息上下文状态提示。
     */
    private String noticeContextMessage;

    /**
     * 当前用户未读消息数量。
     */
    private int unreadNoticeCount;

    /**
     * 注入提示词的消息摘要列表。
     */
    private List<NoticeSummary> noticeList = new ArrayList<>();

    public CurrentUserContext getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(CurrentUserContext currentUser) {
        this.currentUser = currentUser;
    }

    public AiPageContext getPageContext() {
        return pageContext;
    }

    public void setPageContext(AiPageContext pageContext) {
        this.pageContext = pageContext;
    }

    public boolean isTodoContextAvailable() {
        return todoContextAvailable;
    }

    public void setTodoContextAvailable(boolean todoContextAvailable) {
        this.todoContextAvailable = todoContextAvailable;
    }

    public String getTodoContextMessage() {
        return todoContextMessage;
    }

    public void setTodoContextMessage(String todoContextMessage) {
        this.todoContextMessage = todoContextMessage;
    }

    public int getTodoCount() {
        return todoCount;
    }

    public void setTodoCount(int todoCount) {
        this.todoCount = todoCount;
    }

    public List<TodoSummary> getTodoList() {
        return todoList;
    }

    public void setTodoList(List<TodoSummary> todoList) {
        this.todoList = todoList;
    }

    public boolean isNoticeContextAvailable() {
        return noticeContextAvailable;
    }

    public void setNoticeContextAvailable(boolean noticeContextAvailable) {
        this.noticeContextAvailable = noticeContextAvailable;
    }

    public String getNoticeContextMessage() {
        return noticeContextMessage;
    }

    public void setNoticeContextMessage(String noticeContextMessage) {
        this.noticeContextMessage = noticeContextMessage;
    }

    public int getUnreadNoticeCount() {
        return unreadNoticeCount;
    }

    public void setUnreadNoticeCount(int unreadNoticeCount) {
        this.unreadNoticeCount = unreadNoticeCount;
    }

    public List<NoticeSummary> getNoticeList() {
        return noticeList;
    }

    public void setNoticeList(List<NoticeSummary> noticeList) {
        this.noticeList = noticeList;
    }

    /**
     * 当前用户上下文。
     */
    public static class CurrentUserContext {
        /**
         * 用户主键。
         */
        private Long userId;

        /**
         * 用户账号。
         */
        private String userName;

        /**
         * 用户昵称。
         */
        private String nickName;

        /**
         * 租户编号。
         */
        private String tenantId;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

        public String getNickName() {
            return nickName;
        }

        public void setNickName(String nickName) {
            this.nickName = nickName;
        }

        public String getTenantId() {
            return tenantId;
        }

        public void setTenantId(String tenantId) {
            this.tenantId = tenantId;
        }
    }

    /**
     * 待办摘要对象。
     */
    public static class TodoSummary {
        /**
         * 待办ID，仅供动作解析使用。
         */
        private Long todoId;

        /**
         * 流程任务ID，仅供动作解析使用。
         */
        private Long taskId;

        /**
         * 流程名称。
         */
        private String processName;

        /**
         * 节点名称。
         */
        private String nodeName;

        /**
         * 业务单号。
         */
        private String businessNo;

        /**
         * 优先级编码。
         */
        private String priority;

        /**
         * 状态编码。
         */
        private String status;

        /**
         * 截止时间。
         */
        private Date dueTime;

        /**
         * 创建时间。
         */
        private Date createTime;

        public Long getTodoId() {
            return todoId;
        }

        public void setTodoId(Long todoId) {
            this.todoId = todoId;
        }

        public Long getTaskId() {
            return taskId;
        }

        public void setTaskId(Long taskId) {
            this.taskId = taskId;
        }

        public String getProcessName() {
            return processName;
        }

        public void setProcessName(String processName) {
            this.processName = processName;
        }

        public String getNodeName() {
            return nodeName;
        }

        public void setNodeName(String nodeName) {
            this.nodeName = nodeName;
        }

        public String getBusinessNo() {
            return businessNo;
        }

        public void setBusinessNo(String businessNo) {
            this.businessNo = businessNo;
        }

        public String getPriority() {
            return priority;
        }

        public void setPriority(String priority) {
            this.priority = priority;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Date getDueTime() {
            return dueTime;
        }

        public void setDueTime(Date dueTime) {
            this.dueTime = dueTime;
        }

        public Date getCreateTime() {
            return createTime;
        }

        public void setCreateTime(Date createTime) {
            this.createTime = createTime;
        }
    }

    /**
     * 消息摘要对象。
     */
    public static class NoticeSummary {
        /**
         * 消息ID，仅供动作解析使用。
         */
        private Long noticeId;

        /**
         * 消息标题。
         */
        private String title;

        /**
         * 消息类型。
         */
        private String noticeType;

        /**
         * 消息来源。
         */
        private String source;

        /**
         * 业务单号。
         */
        private String businessNo;

        /**
         * 阅读状态。
         */
        private String status;

        /**
         * 送达状态。
         */
        private String deliveryStatus;

        /**
         * 创建时间。
         */
        private Date createTime;

        public Long getNoticeId() {
            return noticeId;
        }

        public void setNoticeId(Long noticeId) {
            this.noticeId = noticeId;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getNoticeType() {
            return noticeType;
        }

        public void setNoticeType(String noticeType) {
            this.noticeType = noticeType;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public String getBusinessNo() {
            return businessNo;
        }

        public void setBusinessNo(String businessNo) {
            this.businessNo = businessNo;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getDeliveryStatus() {
            return deliveryStatus;
        }

        public void setDeliveryStatus(String deliveryStatus) {
            this.deliveryStatus = deliveryStatus;
        }

        public Date getCreateTime() {
            return createTime;
        }

        public void setCreateTime(Date createTime) {
            this.createTime = createTime;
        }
    }

    /**
     * 流程定义候选对象，仅供动作解析使用。
     */
    public static class WorkflowDefinitionCandidate {
        /**
         * 流程定义ID。
         */
        private Long definitionId;

        /**
         * 流程标识。
         */
        private String processKey;

        /**
         * 流程名称。
         */
        private String processName;

        /**
         * 流程分类。
         */
        private String category;

        /**
         * 版本号。
         */
        private Integer version;

        /**
         * 状态。
         */
        private String status;

        public Long getDefinitionId() {
            return definitionId;
        }

        public void setDefinitionId(Long definitionId) {
            this.definitionId = definitionId;
        }

        public String getProcessKey() {
            return processKey;
        }

        public void setProcessKey(String processKey) {
            this.processKey = processKey;
        }

        public String getProcessName() {
            return processName;
        }

        public void setProcessName(String processName) {
            this.processName = processName;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public Integer getVersion() {
            return version;
        }

        public void setVersion(Integer version) {
            this.version = version;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    /**
     * 未发布流程定义候选列表。
     */
    private List<WorkflowDefinitionCandidate> definitionCandidates = new ArrayList<>();

    public List<WorkflowDefinitionCandidate> getDefinitionCandidates() {
        return definitionCandidates;
    }

    public void setDefinitionCandidates(List<WorkflowDefinitionCandidate> definitionCandidates) {
        this.definitionCandidates = definitionCandidates;
    }
}
