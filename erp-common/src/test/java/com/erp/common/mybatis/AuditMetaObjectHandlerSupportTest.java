package com.erp.common.mybatis;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.reflection.MetaObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Date;

/**
 * 审计字段自动填充单元测试。
 * 覆盖原先分散在各 ServiceImpl 里、由手写赋值承担的留痕职责。
 */
class AuditMetaObjectHandlerSupportTest {

    private static final MybatisConfiguration CONFIGURATION = new MybatisConfiguration();

    @BeforeAll
    static void initTableInfo() {
        if (TableInfoHelper.getTableInfo(SampleEntity.class) == null) {
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(CONFIGURATION, ""), SampleEntity.class);
        }
    }

    /**
     * 验证插入时四个审计字段都会被补齐。
     */
    @Test
    void shouldFillAllAuditFieldsOnInsert() {
        SampleEntity entity = new SampleEntity();

        newHandler("admin").insertFill(metaObject(entity));

        Assertions.assertEquals("admin", entity.getCreateBy());
        Assertions.assertEquals("admin", entity.getUpdateBy());
        Assertions.assertNotNull(entity.getCreateTime());
        Assertions.assertNotNull(entity.getUpdateTime());
    }

    /**
     * 验证更新时只补更新人与更新时间，不会覆盖创建留痕。
     */
    @Test
    void shouldOnlyFillUpdateFieldsOnUpdate() {
        SampleEntity entity = new SampleEntity();

        newHandler("admin").updateFill(metaObject(entity));

        Assertions.assertNull(entity.getCreateBy());
        Assertions.assertNull(entity.getCreateTime());
        Assertions.assertEquals("admin", entity.getUpdateBy());
        Assertions.assertNotNull(entity.getUpdateTime());
    }

    /**
     * 验证已显式赋值的字段不会被自动填充覆盖（数据导入保留原始操作人的场景）。
     */
    @Test
    void shouldNotOverrideExplicitValues() {
        Date originalTime = new Date(0L);
        SampleEntity entity = new SampleEntity();
        entity.setCreateBy("importer");
        entity.setCreateTime(originalTime);

        newHandler("admin").insertFill(metaObject(entity));

        Assertions.assertEquals("importer", entity.getCreateBy());
        Assertions.assertEquals(originalTime, entity.getCreateTime());
        Assertions.assertEquals("admin", entity.getUpdateBy());
    }

    /**
     * 验证无登录态时回退为 system，且解析异常不会向外抛出。
     */
    @Test
    void shouldFallbackToSystemOperator() {
        SampleEntity blankOperatorEntity = new SampleEntity();
        newHandler("   ").insertFill(metaObject(blankOperatorEntity));
        Assertions.assertEquals("system", blankOperatorEntity.getCreateBy());

        SampleEntity failingEntity = new SampleEntity();
        AuditMetaObjectHandlerSupport failingHandler = new AuditMetaObjectHandlerSupport() {
            @Override
            protected String resolveOperator() {
                throw new IllegalStateException("no security context");
            }
        };
        Assertions.assertDoesNotThrow(() -> failingHandler.insertFill(metaObject(failingEntity)));
        Assertions.assertEquals("system", failingEntity.getCreateBy());
    }

    /**
     * 构造指定操作人的填充器。
     *
     * @param operator 操作人账号
     * @return 填充器
     */
    private AuditMetaObjectHandlerSupport newHandler(String operator) {
        return new AuditMetaObjectHandlerSupport() {
            @Override
            protected String resolveOperator() {
                return operator;
            }
        };
    }

    /**
     * 包装实体元对象。
     *
     * @param entity 实体
     * @return 元对象
     */
    private MetaObject metaObject(Object entity) {
        return CONFIGURATION.newMetaObject(entity);
    }

    /**
     * 测试用实体。
     */
    @TableName("sample_entity")
    static class SampleEntity extends BaseAuditEntity {
        private static final long serialVersionUID = 1L;

        private Long id;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }
    }
}
