package com.erp.system.controller;

import com.erp.common.core.domain.R;
import com.erp.system.domain.SysPost;
import com.erp.system.service.ISysPostService;
import com.erp.system.support.StatusFieldSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 岗位管理控制层
 */
@Tag(name = "岗位管理")
@RestController
@RequestMapping("/system/post")
@RequiredArgsConstructor
public class SysPostController {

    private final ISysPostService postService;

    @Operation(summary = "查询岗位列表")
    @PreAuthorize("@ss.hasPermi('system:post:list')")
    @GetMapping("/list")
    public R<List<SysPost>> list() {
        return R.success(normalizePostList(postService.list()));
    }

    @Operation(summary = "查询岗位详情")
    @PreAuthorize("@ss.hasPermi('system:post:query')")
    @GetMapping("/{postId}")
    public R<SysPost> getInfo(@PathVariable("postId") Long postId) {
        return R.success(normalizePost(postService.getById(postId)));
    }

    @Operation(summary = "新增岗位")
    @PreAuthorize("@ss.hasPermi('system:post:add')")
    @PostMapping
    public R<Boolean> add(@RequestBody SysPost post) {
        if (post == null) {
            return R.failed("岗位参数不能为空");
        }
        post.setStatus(StatusFieldSupport.normalizeBinaryStatus(post.getStatus()));
        return R.success(postService.save(post));
    }

    @Operation(summary = "修改岗位")
    @PreAuthorize("@ss.hasPermi('system:post:edit')")
    @PutMapping
    public R<Boolean> edit(@RequestBody SysPost post) {
        if (post == null || post.getPostId() == null) {
            return R.failed("岗位ID不能为空");
        }
        return R.success(postService.updateById(post));
    }

    @Operation(summary = "删除岗位")
    @PreAuthorize("@ss.hasPermi('system:post:remove')")
    @DeleteMapping("/{postId}")
    public R<Boolean> remove(@PathVariable("postId") Long postId) {
        return R.success(postService.removeById(postId));
    }

    /**
     * 规范岗位列表中的状态字段，避免前端出现空白状态。
     *
     * @param postList 岗位列表
     * @return 状态字段已规范化的岗位列表
     */
    private List<SysPost> normalizePostList(List<SysPost> postList) {
        if (postList == null || postList.isEmpty()) {
            return postList;
        }
        for (SysPost post : postList) {
            normalizePost(post);
        }
        return postList;
    }

    /**
     * 规范岗位状态字段。
     *
     * @param post 岗位对象
     * @return 规范化后的岗位对象
     */
    private SysPost normalizePost(SysPost post) {
        if (post != null) {
            post.setStatus(StatusFieldSupport.normalizeBinaryStatus(post.getStatus()));
        }
        return post;
    }
}
