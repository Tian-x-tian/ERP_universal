package com.erp.system.controller;

import com.erp.common.core.domain.R;
import com.erp.system.domain.SysDictData;
import com.erp.system.service.ISysDictDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 字典数据控制层
 */
@Tag(name = "字典数据管理")
@RestController
@RequestMapping("/system/dict/data")
@RequiredArgsConstructor
public class SysDictDataController {

    private final ISysDictDataService dictDataService;

    @Operation(summary = "根据字典类型查询字典数据信息")
    @GetMapping("/type/{dictType}")
    public R<List<SysDictData>> dictType(@PathVariable("dictType") String dictType) {
        return R.success(dictDataService.selectDictDataByType(dictType));
    }

    @Operation(summary = "查询字典数据列表")
    @GetMapping("/list")
    public R<List<SysDictData>> list() {
        return R.success(dictDataService.list());
    }

    @Operation(summary = "查询字典数据详细")
    @GetMapping("/{dictCode}")
    public R<SysDictData> getInfo(@PathVariable("dictCode") Long dictCode) {
        return R.success(dictDataService.getById(dictCode));
    }

    @Operation(summary = "新增字典数据")
    @PostMapping
    public R<Void> add(@RequestBody SysDictData dictData) {
        dictDataService.save(dictData);
        return R.success();
    }

    @Operation(summary = "修改字典数据")
    @PutMapping
    public R<Void> edit(@RequestBody SysDictData dictData) {
        dictDataService.updateById(dictData);
        return R.success();
    }

    @Operation(summary = "删除字典数据")
    @DeleteMapping("/{dictCodes}")
    public R<Void> remove(@PathVariable("dictCodes") List<Long> dictCodes) {
        dictDataService.removeByIds(dictCodes);
        return R.success();
    }
}
