package com.erp.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.erp.system.domain.MdmProject;

import java.util.List;

/**
 * MDM 项目主数据服务接口。
 */
public interface IMdmProjectService extends IService<MdmProject> {

    /**
     * 查询项目列表。
     *
     * @param projectCode 项目编码
     * @param projectName 项目名称
     * @param status      状态
     * @return 项目列表
     */
    List<MdmProject> selectProjectList(String projectCode, String projectName, String status);

    /**
     * 新增项目。
     *
     * @param project 项目对象
     * @return true 表示成功
     */
    boolean createProject(MdmProject project);

    /**
     * 修改项目。
     *
     * @param project 项目对象
     * @return true 表示成功
     */
    boolean updateProject(MdmProject project);

    /**
     * 停用项目。
     *
     * @param projectId 项目ID
     * @return true 表示成功
     */
    boolean disableProject(Long projectId);

    /**
     * 删除项目（逻辑删除）。
     *
     * @param projectId 项目ID
     * @return true 表示成功
     */
    boolean removeProject(Long projectId);
}
