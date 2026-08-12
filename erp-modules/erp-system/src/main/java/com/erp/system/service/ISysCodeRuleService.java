package com.erp.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.erp.system.domain.SysCodeRule;

/**
 * 编码规则服务接口
 */
public interface ISysCodeRuleService extends IService<SysCodeRule> {

    /**
     * 预览编码样例。
     *
     * @param ruleCode 规则编码
     * @return 预览编码
     */
    String previewCode(String ruleCode);

    /**
     * 按规则生成下一条编码，并推进流水值。
     *
     * @param ruleCode 规则编码
     * @return 生成后的编码
     */
    String nextCode(String ruleCode);
}
