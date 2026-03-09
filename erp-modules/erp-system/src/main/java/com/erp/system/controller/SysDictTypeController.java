package com.erp.system.controller;

import com.erp.common.core.domain.R;
import com.erp.system.domain.SysDictType;
import com.erp.system.service.ISysDictTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 字典类型控制层
 */
@Tag(name = "字典类型管理")
@RestController
@RequestMapping("/system/dict/type")
@RequiredArgsConstructor
public class SysDictTypeController {

    private final ISysDictTypeService dictTypeService;

    @Operation(summary = "查询字典类型列表")
    @PreAuthorize("@ss.hasPermi('system:dict:list')")
    @GetMapping("/list")
    public R<List<SysDictType>> list() {
        return R.success(dictTypeService.list());
    }

    @Operation(summary = "查询字典类型详细")
    @PreAuthorize("@ss.hasPermi('system:dict:query')")
    @GetMapping("/{dictId}")
    public R<SysDictType> getInfo(@PathVariable("dictId") Long dictId) {
        return R.success(dictTypeService.getById(dictId));
    }

    @Operation(summary = "新增字典类型")
    @PreAuthorize("@ss.hasPermi('system:dict:add')")
    @PostMapping
    public R<Void> add(@RequestBody SysDictType dictType) {
        dictTypeService.save(dictType);
        return R.success();
    }

    @Operation(summary = "修改字典类型")
    @PreAuthorize("@ss.hasPermi('system:dict:edit')")
    @PutMapping
    public R<Void> edit(@RequestBody SysDictType dictType) {
        dictTypeService.updateById(dictType);
        return R.success();
    }

    @Operation(summary = "删除字典类型")
    @PreAuthorize("@ss.hasPermi('system:dict:remove')")
    @DeleteMapping("/{dictIds}")
    public R<Void> remove(@PathVariable("dictIds") List<Long> dictIds) {
        dictTypeService.removeByIds(dictIds);
        return R.success();
    }
}
