package com.erp.system.domain.vo;

import java.io.Serializable;

/**
 * MDM 版本化动作请求体。
 */
public class MdmVersionActionBody implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 当前版本号 */
    private Integer versionNo;

    public Integer getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(Integer versionNo) {
        this.versionNo = versionNo;
    }
}
