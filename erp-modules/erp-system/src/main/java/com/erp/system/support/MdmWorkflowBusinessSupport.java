package com.erp.system.support;

import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * MDM 工作流业务标识辅助工具。
 */
public final class MdmWorkflowBusinessSupport {

    private MdmWorkflowBusinessSupport() {
    }

    /**
     * 根据 MDM 域类型解析工作流业务类型。
     *
     * @param domainType 域类型
     * @return 工作流业务类型
     */
    public static String resolveBusinessType(String domainType) {
        String normalized = normalizeDomainType(domainType);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        return "MDM_" + normalized;
    }

    /**
     * 根据 MDM 域类型与业务主键构建业务单号。
     *
     * @param domainType 域类型
     * @param bizId      业务主键
     * @return 业务单号
     */
    public static String buildBusinessNo(String domainType, Long bizId) {
        String normalized = normalizeDomainType(domainType);
        if (!StringUtils.hasText(normalized) || bizId == null) {
            return null;
        }
        return "MDM:" + normalized + ":" + bizId;
    }

    /**
     * 规范域类型，兼容下划线与不同大小写。
     *
     * @param domainType 域类型
     * @return 规范化结果
     */
    public static String normalizeDomainType(String domainType) {
        if (!StringUtils.hasText(domainType)) {
            return null;
        }
        return domainType.trim().replace('-', '_').toUpperCase(Locale.ROOT);
    }
}
