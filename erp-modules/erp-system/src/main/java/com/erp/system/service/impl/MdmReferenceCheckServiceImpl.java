package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.common.core.exception.ServiceException;
import com.erp.system.domain.MdmCustomer;
import com.erp.system.domain.MdmEmployee;
import com.erp.system.domain.MdmItem;
import com.erp.system.domain.MdmProject;
import com.erp.system.domain.MdmSupplier;
import com.erp.system.domain.MdmWarehouse;
import com.erp.system.mapper.MdmCustomerMapper;
import com.erp.system.mapper.MdmEmployeeMapper;
import com.erp.system.mapper.MdmItemMapper;
import com.erp.system.mapper.MdmProjectMapper;
import com.erp.system.mapper.MdmSupplierMapper;
import com.erp.system.mapper.MdmWarehouseMapper;
import com.erp.system.service.IMdmReferenceCheckService;
import com.erp.system.support.MdmDomainTypeSupport;
import org.springframework.stereotype.Service;

/**
 * MDM 主数据引用检查服务实现类。
 */
@Service
public class MdmReferenceCheckServiceImpl implements IMdmReferenceCheckService {

    private static final String DEL_FLAG_EXIST = "0";

    private final MdmProjectMapper projectMapper;
    private final MdmCustomerMapper customerMapper;
    private final MdmEmployeeMapper employeeMapper;
    private final MdmWarehouseMapper warehouseMapper;

    public MdmReferenceCheckServiceImpl(
            MdmProjectMapper projectMapper,
            MdmCustomerMapper customerMapper,
            MdmEmployeeMapper employeeMapper,
            MdmWarehouseMapper warehouseMapper) {
        this.projectMapper = projectMapper;
        this.customerMapper = customerMapper;
        this.employeeMapper = employeeMapper;
        this.warehouseMapper = warehouseMapper;
    }

    @Override
    public void check(String domainType, Long id) {
        if (id == null || domainType == null) {
            return;
        }

        boolean isReferenced = false;
        String referenceDetails = "";

        switch (domainType) {
            case MdmDomainTypeSupport.CUSTOMER:
                Long projectCustomerCount = projectMapper.selectCount(new LambdaQueryWrapper<MdmProject>()
                        .eq(MdmProject::getCustomerId, id)
                        .eq(MdmProject::getDelFlag, DEL_FLAG_EXIST));
                if (projectCustomerCount != null && projectCustomerCount > 0) {
                    isReferenced = true;
                    referenceDetails = "存在关联的项目记录";
                }
                break;
            case MdmDomainTypeSupport.SUPPLIER:
                // TODO: 检查采购订单等采购域凭证 (假设有其他表引用)
                break;
            case MdmDomainTypeSupport.ITEM:
                // TODO: 检查库存、采购、销售等凭证
                break;
            case MdmDomainTypeSupport.EMPLOYEE:
                Long projectEmpCount = projectMapper.selectCount(new LambdaQueryWrapper<MdmProject>()
                        .eq(MdmProject::getManagerEmpId, id)
                        .eq(MdmProject::getDelFlag, DEL_FLAG_EXIST));
                if (projectEmpCount != null && projectEmpCount > 0) {
                    isReferenced = true;
                    referenceDetails = "作为负责人关联了项目记录";
                }

                Long warehouseManagerCount = warehouseMapper.selectCount(new LambdaQueryWrapper<MdmWarehouse>()
                        .eq(MdmWarehouse::getManagerEmpId, id)
                        .eq(MdmWarehouse::getDelFlag, DEL_FLAG_EXIST));
                if (warehouseManagerCount != null && warehouseManagerCount > 0) {
                    isReferenced = true;
                    referenceDetails = "作为负责人关联了仓库记录";
                }
                break;
            case MdmDomainTypeSupport.WAREHOUSE:
                // TODO: 检查库存记录、出入库单
                break;
            case MdmDomainTypeSupport.ORG:
                Long projectOrgCount = projectMapper.selectCount(new LambdaQueryWrapper<MdmProject>()
                        .eq(MdmProject::getOrgId, id)
                        .eq(MdmProject::getDelFlag, DEL_FLAG_EXIST));
                if (projectOrgCount != null && projectOrgCount > 0) {
                    isReferenced = true;
                    referenceDetails = "存在关联的项目记录";
                }
                Long customerOrgCount = customerMapper.selectCount(new LambdaQueryWrapper<MdmCustomer>()
                        .eq(MdmCustomer::getOrgId, id)
                        .eq(MdmCustomer::getDelFlag, DEL_FLAG_EXIST));
                if (customerOrgCount != null && customerOrgCount > 0) {
                    isReferenced = true;
                    referenceDetails = "存在关联的客户记录";
                }
                Long empOrgCount = employeeMapper.selectCount(new LambdaQueryWrapper<MdmEmployee>()
                        .eq(MdmEmployee::getOrgId, id)
                        .eq(MdmEmployee::getDelFlag, DEL_FLAG_EXIST));
                if (empOrgCount != null && empOrgCount > 0) {
                    isReferenced = true;
                    referenceDetails = "存在关联的员工记录";
                }
                Long whOrgCount = warehouseMapper.selectCount(new LambdaQueryWrapper<MdmWarehouse>()
                        .eq(MdmWarehouse::getOrgId, id)
                        .eq(MdmWarehouse::getDelFlag, DEL_FLAG_EXIST));
                if (whOrgCount != null && whOrgCount > 0) {
                    isReferenced = true;
                    referenceDetails = "存在关联的仓库记录";
                }
                break;
            default:
                break;
        }

        if (isReferenced) {
            // 40901: Conflict - 数据存在引用冲突
            throw new ServiceException("当前主数据已被引用，无法删除或停用 (" + referenceDetails + ")", 40901);
        }
    }
}
