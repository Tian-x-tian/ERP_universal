package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.system.domain.SysCodeRule;
import com.erp.system.mapper.SysCodeRuleMapper;
import com.erp.system.service.ISysCodeRuleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * 编码规则服务实现
 */
@Service
public class SysCodeRuleServiceImpl extends ServiceImpl<SysCodeRuleMapper, SysCodeRule> implements ISysCodeRuleService {

    /**
     * 预览编码样例。
     *
     * @param ruleCode 规则编码
     * @return 预览编码
     */
    @Override
    public String previewCode(String ruleCode) {
        SysCodeRule rule = getRuleByCode(ruleCode);
        if (rule == null || !"0".equals(rule.getStatus())) {
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
        if (rule == null || !"0".equals(rule.getStatus())) {
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
}
