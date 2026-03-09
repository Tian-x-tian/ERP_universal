package com.erp.system.controller;

import com.erp.common.core.domain.R;
import com.erp.system.domain.SysTodoTask;
import com.erp.system.security.service.SecurityUserResolver;
import com.erp.system.service.ISysTodoTaskService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 流程待办任务控制层
 */
@RestController
@RequestMapping("/system/todo")
public class SysTodoTaskController {

    private final ISysTodoTaskService todoTaskService;
    private final SecurityUserResolver securityUserResolver;

    public SysTodoTaskController(ISysTodoTaskService todoTaskService, SecurityUserResolver securityUserResolver) {
        this.todoTaskService = todoTaskService;
        this.securityUserResolver = securityUserResolver;
    }

    /**
     * 查询当前登录用户待办任务列表。
     *
     * @param status 状态（0待处理 1处理中 2已完成）
     * @return 待办任务列表
     */
    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('system:todo:list')")
    public R<List<SysTodoTask>> list(@RequestParam(value = "status", required = false) String status) {
        return R.success(todoTaskService.selectByCurrentUser(resolveCurrentUserId(), status));
    }

    /**
     * 签收待办任务。
     *
     * @param todoId 待办ID
     * @return 更新结果
     */
    @PostMapping("/claim/{todoId}")
    @PreAuthorize("@ss.hasPermi('system:todo:handle')")
    public R<Boolean> claim(@PathVariable("todoId") Long todoId) {
        boolean success = todoTaskService.claim(todoId, resolveCurrentUserId());
        return success ? R.success(true) : R.failed("待办签收失败");
    }

    /**
     * 办结待办任务。
     *
     * @param todoId 待办ID
     * @return 更新结果
     */
    @PostMapping("/finish/{todoId}")
    @PreAuthorize("@ss.hasPermi('system:todo:handle')")
    public R<Boolean> finish(@PathVariable("todoId") Long todoId) {
        boolean success = todoTaskService.finish(todoId, resolveCurrentUserId());
        return success ? R.success(true) : R.failed("待办办结失败");
    }

    /**
     * 获取当前登录用户ID，解析失败时回退为默认管理员。
     *
     * @return 当前用户ID
     */
    private Long resolveCurrentUserId() {
        Long currentUserId = securityUserResolver.getCurrentUserId();
        return currentUserId != null ? currentUserId : 1L;
    }
}
