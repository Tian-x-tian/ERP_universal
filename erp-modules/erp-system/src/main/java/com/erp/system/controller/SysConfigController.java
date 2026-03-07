package com.erp.system.controller;

import com.erp.common.core.domain.R;
import com.erp.system.domain.SysConfig;
import com.erp.system.service.ISysConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 参数配置控制层
 */
@Tag(name = "参数配置管理")
@RestController
@RequestMapping("/system/config")
@RequiredArgsConstructor
public class SysConfigController {

    private final ISysConfigService configService;

    @Operation(summary = "获取参数配置列表")
    @GetMapping("/list")
    public R<List<SysConfig>> list() {
        return R.success(configService.list());
    }

    @Operation(summary = "根据参数键名查询参数值")
    @GetMapping("/configKey/{configKey}")
    public R<String> getConfigKey(@PathVariable("configKey") String configKey) {
        return R.success(configService.selectConfigByKey(configKey));
    }

    @Operation(summary = "查询参数配置详细")
    @GetMapping("/{configId}")
    public R<SysConfig> getInfo(@PathVariable("configId") Integer configId) {
        return R.success(configService.getById(configId));
    }

    @Operation(summary = "新增参数配置")
    @PostMapping
    public R<Void> add(@RequestBody SysConfig config) {
        configService.save(config);
        return R.success();
    }

    @Operation(summary = "修改参数配置")
    @PutMapping
    public R<Void> edit(@RequestBody SysConfig config) {
        configService.updateById(config);
        return R.success();
    }

    @Operation(summary = "删除参数配置")
    @DeleteMapping("/{configIds}")
    public R<Void> remove(@PathVariable("configIds") List<Integer> configIds) {
        configService.removeByIds(configIds);
        return R.success();
    }
}
