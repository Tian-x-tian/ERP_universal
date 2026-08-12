package com.erp.business.hr.domain.vo;

import com.erp.business.hr.domain.HrEmployeeArchive;
import com.erp.business.hr.domain.HrEmployeeChange;
import com.erp.business.hr.domain.HrEmployeeCore;
import com.erp.business.hr.domain.HrEmployeePosition;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * HR 员工详情聚合视图。
 */
@Data
public class HrEmployeeDetailVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private HrEmployeeCore core;
    private HrEmployeeArchive archive;
    private List<HrEmployeePosition> positions;
    private List<HrEmployeeChange> changes;
}
