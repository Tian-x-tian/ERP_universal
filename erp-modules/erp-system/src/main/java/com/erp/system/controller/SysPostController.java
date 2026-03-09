package com.erp.system.controller;

import com.erp.common.core.domain.R;
import com.erp.system.domain.SysPost;
import com.erp.system.service.ISysPostService;
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
        return R.success(postService.list());
    }

    @Operation(summary = "查询岗位详情")
    @PreAuthorize("@ss.hasPermi('system:post:query')")
    @GetMapping("/{postId}")
    public R<SysPost> getInfo(@PathVariable("postId") Long postId) {
        return R.success(postService.getById(postId));
    }

    @Operation(summary = "新增岗位")
    @PreAuthorize("@ss.hasPermi('system:post:add')")
    @PostMapping
    public R<Boolean> add(@RequestBody SysPost post) {
        return R.success(postService.save(post));
    }

    @Operation(summary = "修改岗位")
    @PreAuthorize("@ss.hasPermi('system:post:edit')")
    @PutMapping
    public R<Boolean> edit(@RequestBody SysPost post) {
        return R.success(postService.updateById(post));
    }

    @Operation(summary = "删除岗位")
    @PreAuthorize("@ss.hasPermi('system:post:remove')")
    @DeleteMapping("/{postId}")
    public R<Boolean> remove(@PathVariable("postId") Long postId) {
        return R.success(postService.removeById(postId));
    }
}
