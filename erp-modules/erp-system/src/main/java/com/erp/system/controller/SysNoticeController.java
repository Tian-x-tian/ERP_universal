package com.erp.system.controller;

import com.erp.common.core.domain.R;
import com.erp.system.domain.SysNotice;
import com.erp.system.security.service.SecurityUserResolver;
import com.erp.system.service.ISysNoticeService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 系统消息通知控制层
 */
@RestController
@RequestMapping("/system/notice")
public class SysNoticeController {

    private final ISysNoticeService noticeService;
    private final SecurityUserResolver securityUserResolver;

    public SysNoticeController(ISysNoticeService noticeService, SecurityUserResolver securityUserResolver) {
        this.noticeService = noticeService;
        this.securityUserResolver = securityUserResolver;
    }

    /**
     * 查询当前登录用户消息列表。
     *
     * @param noticeType 消息类型
     * @return 消息列表
     */
    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('system:todo:list')")
    public R<List<SysNotice>> list(@RequestParam(value = "noticeType", required = false) String noticeType) {
        return R.success(noticeService.selectByCurrentUser(resolveCurrentUserId(), noticeType));
    }

    /**
     * 标记单条消息已读。
     *
     * @param noticeId 消息ID
     * @return 更新结果
     */
    @PostMapping("/read/{noticeId}")
    @PreAuthorize("@ss.hasPermi('system:todo:handle')")
    public R<Boolean> read(@PathVariable("noticeId") Long noticeId) {
        boolean success = noticeService.markRead(noticeId, resolveCurrentUserId());
        return success ? R.success(true) : R.failed("消息标记已读失败");
    }

    /**
     * 标记当前用户全部消息已读。
     *
     * @return 更新结果
     */
    @PostMapping("/readAll")
    @PreAuthorize("@ss.hasPermi('system:todo:handle')")
    public R<Integer> readAll() {
        return R.success(noticeService.markAllRead(resolveCurrentUserId()));
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
