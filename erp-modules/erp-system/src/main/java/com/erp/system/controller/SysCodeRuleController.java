package com.erp.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.common.core.domain.R;
import com.erp.system.domain.SysCodeRule;
import com.erp.system.service.ISysCodeRuleService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 编码规则控制层
 */
@RestController
@RequestMapping("/system/code/rule")
public class SysCodeRuleController {

    private final ISysCodeRuleService codeRuleService;

    public SysCodeRuleController(ISysCodeRuleService codeRuleService) {
        this.codeRuleService = codeRuleService;
    }

    /**
     * 查询编码规则列表。
     *
     * @param keyword 关键字（规则编码/规则名称）
     * @return 编码规则列表
     */
    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('system:codeRule:list')")
    public R<List<SysCodeRule>> list(@RequestParam(value = "keyword", required = false) String keyword) {
        LambdaQueryWrapper<SysCodeRule> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String key = keyword.trim();
            queryWrapper.and(wrapper -> wrapper.like(SysCodeRule::getRuleCode, key)
                    .or()
                    .like(SysCodeRule::getRuleName, key));
        }
        queryWrapper.orderByAsc(SysCodeRule::getRuleCode);
        return R.success(codeRuleService.list(queryWrapper));
    }

    /**
     * 查询编码规则详情。
     *
     * @param ruleId 规则ID
     * @return 规则详情
     */
    @GetMapping("/{ruleId}")
    @PreAuthorize("@ss.hasPermi('system:codeRule:query')")
    public R<SysCodeRule> getInfo(@PathVariable("ruleId") Long ruleId) {
        return R.success(codeRuleService.getById(ruleId));
    }

    /**
     * 新增编码规则。
     *
     * @param rule 规则对象
     * @return 新增结果
     */
    @PostMapping
    @PreAuthorize("@ss.hasPermi('system:codeRule:add')")
    public R<Boolean> add(@RequestBody SysCodeRule rule) {
        return R.success(codeRuleService.save(rule));
    }

    /**
     * 修改编码规则。
     *
     * @param rule 规则对象
     * @return 修改结果
     */
    @PutMapping
    @PreAuthorize("@ss.hasPermi('system:codeRule:edit')")
    public R<Boolean> edit(@RequestBody SysCodeRule rule) {
        return R.success(codeRuleService.updateById(rule));
    }

    /**
     * 删除编码规则。
     *
     * @param ruleId 规则ID
     * @return 删除结果
     */
    @DeleteMapping("/{ruleId}")
    @PreAuthorize("@ss.hasPermi('system:codeRule:remove')")
    public R<Boolean> remove(@PathVariable("ruleId") Long ruleId) {
        return R.success(codeRuleService.removeById(ruleId));
    }

    /**
     * 预览编码样例。
     *
     * @param ruleCode 规则编码
     * @return 编码样例
     */
    @GetMapping("/preview/{ruleCode}")
    @PreAuthorize("@ss.hasPermi('system:codeRule:query')")
    public R<Map<String, String>> preview(@PathVariable("ruleCode") String ruleCode) {
        String code = codeRuleService.previewCode(ruleCode);
        if (!StringUtils.hasText(code)) {
            return R.failed("规则不存在或已停用");
        }
        Map<String, String> data = new HashMap<>();
        data.put("code", code);
        return R.success(data);
    }

    /**
     * 生成下一条编码。
     *
     * @param ruleCode 规则编码
     * @return 生成编码
     */
    @PostMapping("/generate/{ruleCode}")
    @PreAuthorize("@ss.hasPermi('system:codeRule:generate')")
    public R<Map<String, String>> generate(@PathVariable("ruleCode") String ruleCode) {
        String code = codeRuleService.nextCode(ruleCode);
        if (!StringUtils.hasText(code)) {
            return R.failed("规则不存在或已停用");
        }
        Map<String, String> data = new HashMap<>();
        data.put("code", code);
        return R.success(data);
    }
}
