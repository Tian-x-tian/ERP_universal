package com.erp.business.hr.service;

import com.erp.business.system.domain.SysImexJob;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * 员工导入服务接口。
 */
public interface IHrEmployeeImportService {

    /**
     * 获取导入模板。
     *
     * @return 模板资源
     */
    Resource loadTemplate();

    /**
     * 执行员工导入。
     *
     * @param file 导入文件
     * @return 导入任务
     */
    SysImexJob importEmployees(MultipartFile file);
}
