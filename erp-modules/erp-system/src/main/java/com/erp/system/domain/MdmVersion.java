package com.erp.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * MDM 版本快照对象 mdm_version。
 */
@Data
@TableName("mdm_version")
public class MdmVersion implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long versionId;
    private String tenantId;
    private String domainType;
    private Long bizId;
    private Integer versionNo;
    private String status;
    private Date effectiveTime;
    private String snapshotJson;
    private String createBy;
    private Date createTime;
}
