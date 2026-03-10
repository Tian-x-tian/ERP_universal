package com.erp.system.controller;

import com.erp.common.core.context.TenantContextHolder;
import com.erp.common.core.domain.R;
import com.erp.common.core.domain.ResultCode;
import com.erp.system.domain.SysNotice;
import com.erp.system.security.service.SecurityUserResolver;
import com.erp.system.service.ISysNoticeService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.StringUtils;

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
    public R<List<SysNotice>> list(@RequestParam(value = "noticeType", required = false) String noticeType,
                                   @RequestParam(value = "status", required = false) String status,
                                   @RequestParam(value = "deliveryChannel", required = false) String deliveryChannel,
                                   @RequestParam(value = "deliveryStatus", required = false) String deliveryStatus) {
        Long currentUserId = resolveCurrentUserId();
        if (currentUserId == null) {
            return R.failed(ResultCode.UNAUTHORIZED);
        }
        return R.success(noticeService.selectByCurrentUser(currentUserId, noticeType, status, deliveryChannel, deliveryStatus));
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
        Long currentUserId = resolveCurrentUserId();
        if (currentUserId == null) {
            return R.failed(ResultCode.UNAUTHORIZED);
        }
        boolean success = noticeService.markRead(noticeId, currentUserId);
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
        Long currentUserId = resolveCurrentUserId();
        if (currentUserId == null) {
            return R.failed(ResultCode.UNAUTHORIZED);
        }
        return R.success(noticeService.markAllRead(currentUserId));
    }

    /**
     * 查询消息中心管理列表。
     *
     * @param title          标题关键字
     * @param noticeType     消息类型
     * @param receiverUserId 接收人用户ID
     * @param status         状态（0未读 1已读）
     * @return 消息列表
     */
    @GetMapping("/manage/list")
    @PreAuthorize("@ss.hasPermi('system:notice:list')")
    public R<List<SysNotice>> manageList(
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "noticeType", required = false) String noticeType,
            @RequestParam(value = "receiverUserId", required = false) Long receiverUserId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "deliveryChannel", required = false) String deliveryChannel,
            @RequestParam(value = "deliveryStatus", required = false) String deliveryStatus,
            @RequestParam(value = "source", required = false) String source,
            @RequestParam(value = "businessNo", required = false) String businessNo) {
        String tenantId = resolveTenantId();
        if (!StringUtils.hasText(tenantId)) {
            return R.failed(ResultCode.UNAUTHORIZED);
        }
        return R.success(noticeService.selectForManage(tenantId, title, noticeType, receiverUserId, status,
                deliveryChannel, deliveryStatus, source, businessNo));
    }

    /**
     * 查询消息通知详情。
     *
     * @param noticeId 消息ID
     * @return 消息详情
     */
    @GetMapping("/manage/{noticeId}")
    @PreAuthorize("@ss.hasPermi('system:notice:query')")
    public R<SysNotice> manageGet(@PathVariable("noticeId") Long noticeId) {
        return R.success(noticeService.getById(noticeId));
    }

    /**
     * 新增消息通知。
     *
     * @param notice 消息对象
     * @return 新增结果
     */
    @PostMapping("/manage")
    @PreAuthorize("@ss.hasPermi('system:notice:add')")
    public R<Boolean> manageAdd(@RequestBody SysNotice notice) {
        if (notice == null || !StringUtils.hasText(notice.getTitle()) || notice.getReceiverUserId() == null) {
            return R.failed("标题和接收人不能为空");
        }
        String tenantId = resolveTenantId();
        if (!StringUtils.hasText(tenantId)) {
            return R.failed(ResultCode.UNAUTHORIZED);
        }
        notice.setTenantId(tenantId);
        boolean success = noticeService.createNotice(notice);
        return success ? R.success(true) : R.failed("新增消息失败");
    }

    /**
     * 修改消息通知。
     *
     * @param notice 消息对象
     * @return 修改结果
     */
    @PutMapping("/manage")
    @PreAuthorize("@ss.hasPermi('system:notice:edit')")
    public R<Boolean> manageEdit(@RequestBody SysNotice notice) {
        if (notice == null || notice.getNoticeId() == null) {
            return R.failed("消息ID不能为空");
        }
        boolean success = noticeService.updateNotice(notice);
        return success ? R.success(true) : R.failed("修改消息失败");
    }

    /**
     * 删除消息通知。
     *
     * @param noticeIds 消息ID集合
     * @return 删除结果
     */
    @DeleteMapping("/manage/{noticeIds}")
    @PreAuthorize("@ss.hasPermi('system:notice:remove')")
    public R<Boolean> manageRemove(@PathVariable("noticeIds") List<Long> noticeIds) {
        return R.success(noticeService.removeByIds(noticeIds));
    }

    /**
     * 获取当前登录用户ID。
     *
     * @return 当前用户ID，未登录时返回 null
     */
    private Long resolveCurrentUserId() {
        return securityUserResolver.getCurrentUserId();
    }

    /**
     * 获取当前租户编号。
     *
     * @return 租户编号
     */
    private String resolveTenantId() {
        String tenantId = TenantContextHolder.getTenantId();
        return StringUtils.hasText(tenantId) ? tenantId.trim() : null;
    }
}
