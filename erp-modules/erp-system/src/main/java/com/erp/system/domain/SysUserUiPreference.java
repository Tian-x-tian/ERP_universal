package com.erp.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.mybatis.BaseAuditEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * UI 个性化偏好对象 sys_user_ui_preference。
 *
 * <p>同一张表承载两类作用域：
 * <ul>
 *   <li>scope_type = 0：租户默认策略（user_id 固定为 0，可携带锁定项）</li>
 *   <li>scope_type = 1：用户个人偏好</li>
 * </ul>
 */
@TableName("sys_user_ui_preference")
@Data
@EqualsAndHashCode(callSuper = false)
public class SysUserUiPreference extends BaseAuditEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 偏好ID */
    @TableId(type = IdType.AUTO)
    private Long preferenceId;

    /** 租户编号 */
    private String tenantId;

    /** 作用域（0租户默认 1用户个人） */
    private String scopeType;

    /** 用户ID（租户默认固定为0） */
    private Long userId;

    /** UI 偏好 JSON */
    private String preferenceJson;

    /** 锁定项（逗号分隔，仅租户默认生效） */
    private String lockedKeys;

    /** 创建者 */

    /** 创建时间 */

    /** 更新者 */

    /** 更新时间 */
}
