package com.erp.system.controller;

import com.erp.common.core.domain.PageData;
import com.erp.common.core.domain.R;
import com.erp.system.domain.MdmCostCenter;
import com.erp.system.domain.MdmOrg;
import com.erp.system.domain.MdmProject;
import com.erp.system.domain.vo.MdmCostCenterWorkflowSubmitBody;
import com.erp.system.domain.vo.MdmOrgWorkflowSubmitBody;
import com.erp.system.domain.vo.MdmProjectWorkflowSubmitBody;
import com.erp.system.service.IMdmCostCenterService;
import com.erp.system.service.IMdmDimensionWorkflowSubmitService;
import com.erp.system.service.IMdmOrgService;
import com.erp.system.service.IMdmProjectService;
import com.erp.system.support.MdmPageSupport;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * MDM 组织、成本中心、项目维度控制层。
 */
@RestController
@RequestMapping("/system/mdm/dimension")
public class MdmDimensionController {
    private final IMdmOrgService orgService;
    private final IMdmCostCenterService costCenterService;
    private final IMdmProjectService projectService;
    private final IMdmDimensionWorkflowSubmitService dimensionWorkflowSubmitService;

    public MdmDimensionController(IMdmOrgService orgService,
            IMdmCostCenterService costCenterService,
            IMdmProjectService projectService,
            IMdmDimensionWorkflowSubmitService dimensionWorkflowSubmitService) {
        this.orgService = orgService;
        this.costCenterService = costCenterService;
        this.projectService = projectService;
        this.dimensionWorkflowSubmitService = dimensionWorkflowSubmitService;
    }

    /**
     * 查询组织列表。
     *
     * @param orgCode 组织编码
     * @param orgName 组织名称
     * @param status  状态
     * @return 组织列表
     */
    @GetMapping("/org/list")
    @PreAuthorize("@ss.hasPermi('system:mdm:org:list')")
    public R<PageData<MdmOrg>> orgList(@RequestParam(value = "orgCode", required = false) String orgCode,
            @RequestParam(value = "orgName", required = false) String orgName,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") Long pageNum,
            @RequestParam(value = "pageSize", required = false, defaultValue = "20") Long pageSize) {
        return R.success(MdmPageSupport.paginate(orgService.selectOrgList(orgCode, orgName, status), pageNum, pageSize));
    }

    /**
     * 查询组织详情。
     *
     * @param orgId 组织ID
     * @return 组织详情
     */
    @GetMapping("/org/{orgId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:org:query')")
    public R<MdmOrg> orgInfo(@PathVariable("orgId") Long orgId) {
        MdmOrg org = orgService.getById(orgId);
        if (org == null || "2".equals(org.getDelFlag())) {
            return R.failed("组织不存在");
        }
        return R.success(org);
    }

    /**
     * 新增组织。
     *
     * @param org 组织对象
     * @return 新增结果
     */
    @PostMapping("/org")
    @PreAuthorize("@ss.hasPermi('system:mdm:org:add')")
    public R<Boolean> orgAdd(@RequestBody MdmOrg org) {
        boolean success = orgService.createOrg(org);
        return success ? R.success(true) : R.failed("新增组织失败，请检查父子层级和编码唯一性");
    }

    /**
     * 修改组织。
     *
     * @param org 组织对象
     * @return 修改结果
     */
    @PutMapping("/org")
    @PreAuthorize("@ss.hasPermi('system:mdm:org:edit')")
    public R<Boolean> orgEdit(@RequestBody MdmOrg org) {
        if (org == null || org.getOrgId() == null) {
            return R.failed("组织ID不能为空");
        }
        boolean success = orgService.updateOrg(org);
        return success ? R.success(true) : R.failed("修改组织失败，请检查层级结构是否成环");
    }

    /**
     * 停用组织。
     *
     * @param orgId 组织ID
     * @return 停用结果
     */
    @PostMapping("/org/disable/{orgId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:org:disable')")
    public R<Boolean> orgDisable(@PathVariable("orgId") Long orgId) {
        boolean success = orgService.disableOrg(orgId);
        return success ? R.success(true) : R.failed("停用组织失败");
    }

    @PostMapping("/org/submit/{orgId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:org:edit')")
    public R<Boolean> orgSubmit(@PathVariable("orgId") Long orgId,
            @RequestBody MdmOrgWorkflowSubmitBody submitBody) {
        boolean success = dimensionWorkflowSubmitService.submitOrgDraftActivation(
                orgId,
                submitBody == null ? null : submitBody.getProcessKey(),
                submitBody == null ? null : submitBody.getRemark());
        return success ? R.success(true) : R.failed("提交组织审批失败");
    }

    @PostMapping("/org/change/{orgId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:org:edit')")
    public R<Boolean> orgSubmitChange(@PathVariable("orgId") Long orgId,
            @RequestBody MdmOrgWorkflowSubmitBody submitBody) {
        boolean success = dimensionWorkflowSubmitService.submitOrgChange(
                orgId,
                submitBody == null ? null : submitBody.getOrg(),
                submitBody == null ? null : submitBody.getProcessKey(),
                submitBody == null ? null : submitBody.getRemark());
        return success ? R.success(true) : R.failed("提交组织变更审批失败");
    }

    @PostMapping("/org/disable/submit/{orgId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:org:disable')")
    public R<Boolean> orgSubmitDisable(@PathVariable("orgId") Long orgId,
            @RequestBody MdmOrgWorkflowSubmitBody submitBody) {
        boolean success = dimensionWorkflowSubmitService.submitOrgDisable(
                orgId,
                submitBody == null ? null : submitBody.getProcessKey(),
                submitBody == null ? null : submitBody.getRemark());
        return success ? R.success(true) : R.failed("提交组织停用审批失败");
    }

    /**
     * 删除组织（逻辑删除）。
     *
     * @param orgId 组织ID
     * @return 删除结果
     */
    @DeleteMapping("/org/{orgId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:org:remove')")
    public R<Boolean> orgRemove(@PathVariable("orgId") Long orgId) {
        boolean success = orgService.removeOrg(orgId);
        return success ? R.success(true) : R.failed("删除组织失败，仅草稿且无子节点组织可删除");
    }

    /**
     * 查询成本中心列表。
     *
     * @param ccCode 成本中心编码
     * @param ccName 成本中心名称
     * @param status 状态
     * @return 成本中心列表
     */
    @GetMapping("/cc/list")
    @PreAuthorize("@ss.hasPermi('system:mdm:cc:list')")
    public R<PageData<MdmCostCenter>> ccList(@RequestParam(value = "ccCode", required = false) String ccCode,
            @RequestParam(value = "ccName", required = false) String ccName,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") Long pageNum,
            @RequestParam(value = "pageSize", required = false, defaultValue = "20") Long pageSize) {
        return R.success(MdmPageSupport.paginate(costCenterService.selectCostCenterList(ccCode, ccName, status), pageNum, pageSize));
    }

    /**
     * 查询成本中心详情。
     *
     * @param ccId 成本中心ID
     * @return 成本中心详情
     */
    @GetMapping("/cc/{ccId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:cc:query')")
    public R<MdmCostCenter> ccInfo(@PathVariable("ccId") Long ccId) {
        MdmCostCenter costCenter = costCenterService.getById(ccId);
        if (costCenter == null || "2".equals(costCenter.getDelFlag())) {
            return R.failed("成本中心不存在");
        }
        return R.success(costCenter);
    }

    /**
     * 新增成本中心。
     *
     * @param costCenter 成本中心对象
     * @return 新增结果
     */
    @PostMapping("/cc")
    @PreAuthorize("@ss.hasPermi('system:mdm:cc:add')")
    public R<Boolean> ccAdd(@RequestBody MdmCostCenter costCenter) {
        boolean success = costCenterService.createCostCenter(costCenter);
        return success ? R.success(true) : R.failed("新增成本中心失败，请检查编码唯一性和组织归属");
    }

    /**
     * 修改成本中心。
     *
     * @param costCenter 成本中心对象
     * @return 修改结果
     */
    @PutMapping("/cc")
    @PreAuthorize("@ss.hasPermi('system:mdm:cc:edit')")
    public R<Boolean> ccEdit(@RequestBody MdmCostCenter costCenter) {
        if (costCenter == null || costCenter.getCcId() == null) {
            return R.failed("成本中心ID不能为空");
        }
        boolean success = costCenterService.updateCostCenter(costCenter);
        return success ? R.success(true) : R.failed("修改成本中心失败，请检查父子层级和组织归属");
    }

    /**
     * 停用成本中心。
     *
     * @param ccId 成本中心ID
     * @return 停用结果
     */
    @PostMapping("/cc/disable/{ccId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:cc:disable')")
    public R<Boolean> ccDisable(@PathVariable("ccId") Long ccId) {
        boolean success = costCenterService.disableCostCenter(ccId);
        return success ? R.success(true) : R.failed("停用成本中心失败");
    }

    @PostMapping("/cc/submit/{ccId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:cc:edit')")
    public R<Boolean> ccSubmit(@PathVariable("ccId") Long ccId,
            @RequestBody MdmCostCenterWorkflowSubmitBody submitBody) {
        boolean success = dimensionWorkflowSubmitService.submitCostCenterDraftActivation(
                ccId,
                submitBody == null ? null : submitBody.getProcessKey(),
                submitBody == null ? null : submitBody.getRemark());
        return success ? R.success(true) : R.failed("提交成本中心审批失败");
    }

    @PostMapping("/cc/change/{ccId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:cc:edit')")
    public R<Boolean> ccSubmitChange(@PathVariable("ccId") Long ccId,
            @RequestBody MdmCostCenterWorkflowSubmitBody submitBody) {
        boolean success = dimensionWorkflowSubmitService.submitCostCenterChange(
                ccId,
                submitBody == null ? null : submitBody.getCostCenter(),
                submitBody == null ? null : submitBody.getProcessKey(),
                submitBody == null ? null : submitBody.getRemark());
        return success ? R.success(true) : R.failed("提交成本中心变更审批失败");
    }

    @PostMapping("/cc/disable/submit/{ccId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:cc:disable')")
    public R<Boolean> ccSubmitDisable(@PathVariable("ccId") Long ccId,
            @RequestBody MdmCostCenterWorkflowSubmitBody submitBody) {
        boolean success = dimensionWorkflowSubmitService.submitCostCenterDisable(
                ccId,
                submitBody == null ? null : submitBody.getProcessKey(),
                submitBody == null ? null : submitBody.getRemark());
        return success ? R.success(true) : R.failed("提交成本中心停用审批失败");
    }

    /**
     * 删除成本中心（逻辑删除）。
     *
     * @param ccId 成本中心ID
     * @return 删除结果
     */
    @DeleteMapping("/cc/{ccId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:cc:remove')")
    public R<Boolean> ccRemove(@PathVariable("ccId") Long ccId) {
        boolean success = costCenterService.removeCostCenter(ccId);
        return success ? R.success(true) : R.failed("删除成本中心失败，仅草稿且无子节点可删除");
    }

    /**
     * 查询项目列表。
     *
     * @param projectCode 项目编码
     * @param projectName 项目名称
     * @param status      状态
     * @return 项目列表
     */
    @GetMapping("/project/list")
    @PreAuthorize("@ss.hasPermi('system:mdm:project:list')")
    public R<PageData<MdmProject>> projectList(
            @RequestParam(value = "projectCode", required = false) String projectCode,
            @RequestParam(value = "projectName", required = false) String projectName,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") Long pageNum,
            @RequestParam(value = "pageSize", required = false, defaultValue = "20") Long pageSize) {
        return R.success(MdmPageSupport.paginate(projectService.selectProjectList(projectCode, projectName, status), pageNum, pageSize));
    }

    /**
     * 查询项目详情。
     *
     * @param projectId 项目ID
     * @return 项目详情
     */
    @GetMapping("/project/{projectId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:project:query')")
    public R<MdmProject> projectInfo(@PathVariable("projectId") Long projectId) {
        MdmProject project = projectService.getById(projectId);
        if (project == null || "2".equals(project.getDelFlag())) {
            return R.failed("项目不存在");
        }
        return R.success(project);
    }

    /**
     * 新增项目。
     *
     * @param project 项目对象
     * @return 新增结果
     */
    @PostMapping("/project")
    @PreAuthorize("@ss.hasPermi('system:mdm:project:add')")
    public R<Boolean> projectAdd(@RequestBody MdmProject project) {
        boolean success = projectService.createProject(project);
        return success ? R.success(true) : R.failed("新增项目失败，请检查编码唯一性与日期区间");
    }

    /**
     * 修改项目。
     *
     * @param project 项目对象
     * @return 修改结果
     */
    @PutMapping("/project")
    @PreAuthorize("@ss.hasPermi('system:mdm:project:edit')")
    public R<Boolean> projectEdit(@RequestBody MdmProject project) {
        if (project == null || project.getProjectId() == null) {
            return R.failed("项目ID不能为空");
        }
        boolean success = projectService.updateProject(project);
        return success ? R.success(true) : R.failed("修改项目失败，请检查编码唯一性与日期区间");
    }

    /**
     * 停用项目。
     *
     * @param projectId 项目ID
     * @return 停用结果
     */
    @PostMapping("/project/disable/{projectId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:project:disable')")
    public R<Boolean> projectDisable(@PathVariable("projectId") Long projectId) {
        boolean success = projectService.disableProject(projectId);
        return success ? R.success(true) : R.failed("停用项目失败");
    }

    @PostMapping("/project/submit/{projectId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:project:edit')")
    public R<Boolean> projectSubmit(@PathVariable("projectId") Long projectId,
            @RequestBody MdmProjectWorkflowSubmitBody submitBody) {
        boolean success = dimensionWorkflowSubmitService.submitProjectDraftActivation(
                projectId,
                submitBody == null ? null : submitBody.getProcessKey(),
                submitBody == null ? null : submitBody.getRemark());
        return success ? R.success(true) : R.failed("提交项目审批失败");
    }

    @PostMapping("/project/change/{projectId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:project:edit')")
    public R<Boolean> projectSubmitChange(@PathVariable("projectId") Long projectId,
            @RequestBody MdmProjectWorkflowSubmitBody submitBody) {
        boolean success = dimensionWorkflowSubmitService.submitProjectChange(
                projectId,
                submitBody == null ? null : submitBody.getProject(),
                submitBody == null ? null : submitBody.getProcessKey(),
                submitBody == null ? null : submitBody.getRemark());
        return success ? R.success(true) : R.failed("提交项目变更审批失败");
    }

    @PostMapping("/project/disable/submit/{projectId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:project:disable')")
    public R<Boolean> projectSubmitDisable(@PathVariable("projectId") Long projectId,
            @RequestBody MdmProjectWorkflowSubmitBody submitBody) {
        boolean success = dimensionWorkflowSubmitService.submitProjectDisable(
                projectId,
                submitBody == null ? null : submitBody.getProcessKey(),
                submitBody == null ? null : submitBody.getRemark());
        return success ? R.success(true) : R.failed("提交项目停用审批失败");
    }

    /**
     * 删除项目（逻辑删除）。
     *
     * @param projectId 项目ID
     * @return 删除结果
     */
    @DeleteMapping("/project/{projectId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:project:remove')")
    public R<Boolean> projectRemove(@PathVariable("projectId") Long projectId) {
        boolean success = projectService.removeProject(projectId);
        return success ? R.success(true) : R.failed("删除项目失败，仅草稿状态允许删除");
    }
}
