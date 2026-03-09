package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.system.domain.SysCodeRule;
import com.erp.system.mapper.SysCodeRuleMapper;
import com.erp.system.service.ISysCodeRuleService;
import com.erp.system.support.StatusFieldSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

/**
 * 编码规则服务实现
 */
@Service
public class SysCodeRuleServiceImpl extends ServiceImpl<SysCodeRuleMapper, SysCodeRule> implements ISysCodeRuleService {

    /**
     * 新增编码规则时规范状态字段和基础字段。
     *
     * @param entity 编码规则实体
     * @return 新增结果
     */
    @Override
    public boolean save(SysCodeRule entity) {
        normalizeRule(entity, true, null);
        return super.save(entity);
    }

    /**
     * 修改编码规则时规范状态字段和更新时间。
     *
     * @param entity 编码规则实体
     * @return 修改结果
     */
    @Override
    public boolean updateById(SysCodeRule entity) {
        String currentStatus = null;
        if (entity != null && entity.getRuleId() != null) {
            SysCodeRule existedRule = getById(entity.getRuleId());
            currentStatus = existedRule == null ? null : existedRule.getStatus();
        }
        normalizeRule(entity, false, currentStatus);
        return super.updateById(entity);
    }

    /**
     * 预览编码样例。
     *
     * @param ruleCode 规则编码
     * @return 预览编码
     */
    @Override
    public String previewCode(String ruleCode) {
        SysCodeRule rule = getRuleByCode(ruleCode);
        if (rule == null || !StatusFieldSupport.isEnabled(rule.getStatus())) {
            return null;
        }
        long nextSeq = (rule.getCurrentSeq() == null ? 0L : rule.getCurrentSeq()) + 1L;
        return buildCode(rule, nextSeq);
    }

    /**
     * 按规则生成下一条编码，并推进流水值。
     *
     * @param ruleCode 规则编码
     * @return 生成后的编码
     */
    @Override
    @Transactional
    public String nextCode(String ruleCode) {
        SysCodeRule rule = getRuleByCode(ruleCode);
        if (rule == null || !StatusFieldSupport.isEnabled(rule.getStatus())) {
            return null;
        }
        long nextSeq = (rule.getCurrentSeq() == null ? 0L : rule.getCurrentSeq()) + 1L;
        rule.setCurrentSeq(nextSeq);
        updateById(rule);
        return buildCode(rule, nextSeq);
    }

    /**
     * 按规则编码查询规则。
     *
     * @param ruleCode 规则编码
     * @return 规则对象
     */
    private SysCodeRule getRuleByCode(String ruleCode) {
        if (!StringUtils.hasText(ruleCode)) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<SysCodeRule>().eq(SysCodeRule::getRuleCode, ruleCode.trim()));
    }

    /**
     * 根据规则组装最终编码。
     *
     * @param rule 规则对象
     * @param seq  流水值
     * @return 编码字符串
     */
    private String buildCode(SysCodeRule rule, long seq) {
        String prefix = StringUtils.hasText(rule.getPrefix()) ? rule.getPrefix().trim() : "";
        String pattern = StringUtils.hasText(rule.getDatePattern()) ? rule.getDatePattern().trim() : "yyyyMMdd";
        String datePart = formatDate(pattern);
        int seqLength = rule.getSeqLength() == null || rule.getSeqLength() < 1 ? 4 : rule.getSeqLength();
        String seqPart = String.format(Locale.ROOT, "%0" + seqLength + "d", seq);
        return prefix + datePart + seqPart;
    }

    /**
     * 日期格式化（格式错误时回退 yyyyMMdd）。
     *
     * @param pattern 日期格式
     * @return 日期字符串
     */
    private String formatDate(String pattern) {
        try {
            return LocalDate.now().format(DateTimeFormatter.ofPattern(pattern));
        } catch (Exception ex) {
            return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        }
    }

    /**
     * 规范编码规则核心字段，避免状态为空导致前端无法判断启停。
     *
     * @param rule          规则对象
     * @param isCreate      是否为新增操作
     * @param currentStatus 当前已落库状态值（更新场景使用）
     */
    private void normalizeRule(SysCodeRule rule, boolean isCreate, String currentStatus) {
        if (rule == null) {
            return;
        }
        if (isCreate) {
            rule.setStatus(StatusFieldSupport.normalizeBinaryStatus(rule.getStatus()));
        } else {
            rule.setStatus(StatusFieldSupport.normalizeBinaryStatusForUpdate(rule.getStatus(), currentStatus));
        }
        if (StringUtils.hasText(rule.getRuleCode())) {
            rule.setRuleCode(rule.getRuleCode().trim());
        }
        if (StringUtils.hasText(rule.getRuleName())) {
            rule.setRuleName(rule.getRuleName().trim());
        }
        if (isCreate) {
            rule.setCreateTime(new Date());
        } else {
            rule.setUpdateTime(new Date());
        }
    }
}
