package com.erp.common.mybatis;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.Date;

/**
 * 审计字段自动填充基类。
 * 各服务继承本类并提供当前操作人解析方式，注册为 Spring Bean 后由 MyBatis Plus 自动装配。
 *
 * @see BaseAuditEntity
 */
public abstract class AuditMetaObjectHandlerSupport implements MetaObjectHandler {
    /**
     * 无登录态场景（定时任务、内部调用、启动初始化）使用的兜底操作人。
     */
    protected static final String SYSTEM_OPERATOR = "system";

    private static final String FIELD_CREATE_BY = "createBy";
    private static final String FIELD_CREATE_TIME = "createTime";
    private static final String FIELD_UPDATE_BY = "updateBy";
    private static final String FIELD_UPDATE_TIME = "updateTime";

    private static final Logger log = LoggerFactory.getLogger(AuditMetaObjectHandlerSupport.class);

    /**
     * 插入时补齐创建人、创建时间、更新人、更新时间。
     *
     * @param metaObject 实体元对象
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        Date now = new Date();
        String operator = currentOperator();
        strictInsertFill(metaObject, FIELD_CREATE_BY, String.class, operator);
        strictInsertFill(metaObject, FIELD_CREATE_TIME, Date.class, now);
        strictInsertFill(metaObject, FIELD_UPDATE_BY, String.class, operator);
        strictInsertFill(metaObject, FIELD_UPDATE_TIME, Date.class, now);
    }

    /**
     * 更新时补齐更新人与更新时间。
     *
     * @param metaObject 实体元对象
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        strictUpdateFill(metaObject, FIELD_UPDATE_BY, String.class, currentOperator());
        strictUpdateFill(metaObject, FIELD_UPDATE_TIME, Date.class, new Date());
    }

    /**
     * 解析当前操作人账号，由各服务基于自身的登录用户解析器实现。
     *
     * @return 当前操作人账号；无登录态时返回 null
     */
    protected abstract String resolveOperator();

    /**
     * 解析操作人并做兜底，任何异常都不允许影响正常的数据写入。
     *
     * @return 操作人账号
     */
    private String currentOperator() {
        try {
            String operator = resolveOperator();
            return StringUtils.hasText(operator) ? operator.trim() : SYSTEM_OPERATOR;
        } catch (RuntimeException ex) {
            log.warn("解析审计操作人失败，回退为 {}", SYSTEM_OPERATOR, ex);
            return SYSTEM_OPERATOR;
        }
    }
}
