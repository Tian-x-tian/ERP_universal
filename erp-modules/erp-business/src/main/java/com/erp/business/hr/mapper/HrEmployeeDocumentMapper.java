package com.erp.business.hr.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.business.hr.domain.HrEmployeeDocument;
import org.apache.ibatis.annotations.Mapper;

/**
 * 员工电子档案底座 Mapper。
 */
@Mapper
public interface HrEmployeeDocumentMapper extends BaseMapper<HrEmployeeDocument> {
}
