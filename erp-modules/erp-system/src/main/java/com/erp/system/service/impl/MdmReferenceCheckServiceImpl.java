package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.common.core.domain.ResultCode;
import com.erp.common.core.exception.ServiceException;
import com.erp.system.domain.MdmCustomer;
import com.erp.system.domain.MdmCurrency;
import com.erp.system.domain.MdmEmployee;
import com.erp.system.domain.MdmItem;
import com.erp.system.domain.MdmProject;
import com.erp.system.domain.MdmSupplier;
import com.erp.system.domain.MdmWarehouse;
import com.erp.system.domain.SysWorkflowInstance;
import com.erp.system.mapper.MdmCustomerMapper;
import com.erp.system.mapper.MdmCurrencyMapper;
import com.erp.system.mapper.MdmEmployeeMapper;
import com.erp.system.mapper.InventoryReferenceMapper;
import com.erp.system.mapper.MdmItemMapper;
import com.erp.system.mapper.MdmProjectMapper;
import com.erp.system.mapper.MdmSupplierMapper;
import com.erp.system.mapper.MdmWarehouseMapper;
import com.erp.system.mapper.SysWorkflowInstanceMapper;
import com.erp.system.service.IMdmReferenceCheckService;
import com.erp.system.support.MdmDomainTypeSupport;
import com.erp.system.support.MdmWorkflowBusinessSupport;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * MDM 主数据引用检查服务实现类。
 */
@Service
public class MdmReferenceCheckServiceImpl implements IMdmReferenceCheckService {

    private static final String DEL_FLAG_EXIST = "0";
    private static final String WORKFLOW_STATUS_RUNNING = "0";

    private final MdmProjectMapper projectMapper;
    private final MdmCustomerMapper customerMapper;
    private final MdmEmployeeMapper employeeMapper;
    private final MdmWarehouseMapper warehouseMapper;
    private final MdmSupplierMapper supplierMapper;
    private final MdmItemMapper itemMapper;
    private final MdmCurrencyMapper currencyMapper;
    private final SysWorkflowInstanceMapper workflowInstanceMapper;
    private final InventoryReferenceMapper inventoryReferenceMapper;

    public MdmReferenceCheckServiceImpl(
            MdmProjectMapper projectMapper,
            MdmCustomerMapper customerMapper,
            MdmEmployeeMapper employeeMapper,
            MdmWarehouseMapper warehouseMapper,
            MdmSupplierMapper supplierMapper,
            MdmItemMapper itemMapper,
            MdmCurrencyMapper currencyMapper,
            SysWorkflowInstanceMapper workflowInstanceMapper,
            InventoryReferenceMapper inventoryReferenceMapper) {
        this.projectMapper = projectMapper;
        this.customerMapper = customerMapper;
        this.employeeMapper = employeeMapper;
        this.warehouseMapper = warehouseMapper;
        this.supplierMapper = supplierMapper;
        this.itemMapper = itemMapper;
        this.currencyMapper = currencyMapper;
        this.workflowInstanceMapper = workflowInstanceMapper;
        this.inventoryReferenceMapper = inventoryReferenceMapper;
    }

    @Override
    public void check(String domainType, Long id) {
        String normalizedDomainType = MdmWorkflowBusinessSupport.normalizeDomainType(domainType);
        if (!StringUtils.hasText(normalizedDomainType) || id == null || id < 1) {
            throw new ServiceException("主数据域类型和业务主键不能为空", (int) ResultCode.VALIDATE_FAILED.getCode());
        }
        List<String> referenceDetails = new ArrayList<>();
        switch (normalizedDomainType) {
            case MdmDomainTypeSupport.CUSTOMER:
                addReferenceDetail(referenceDetails,
                        projectMapper.selectCount(new LambdaQueryWrapper<MdmProject>()
                        .eq(MdmProject::getCustomerId, id)
                        .eq(MdmProject::getDelFlag, DEL_FLAG_EXIST)),
                        "基础资料: 项目档案已引用当前客户");
                break;
            case MdmDomainTypeSupport.SUPPLIER:
                break;
            case MdmDomainTypeSupport.ITEM:
                break;
            case MdmDomainTypeSupport.EMPLOYEE:
                addReferenceDetail(referenceDetails,
                        projectMapper.selectCount(new LambdaQueryWrapper<MdmProject>()
                        .eq(MdmProject::getManagerEmpId, id)
                        .eq(MdmProject::getDelFlag, DEL_FLAG_EXIST)),
                        "基础资料: 项目档案负责人已引用当前员工");
                addReferenceDetail(referenceDetails,
                        warehouseMapper.selectCount(new LambdaQueryWrapper<MdmWarehouse>()
                        .eq(MdmWarehouse::getManagerEmpId, id)
                        .eq(MdmWarehouse::getDelFlag, DEL_FLAG_EXIST)),
                        "基础资料: 仓库负责人已引用当前员工");
                break;
            case MdmDomainTypeSupport.WAREHOUSE:
                addReferenceDetail(referenceDetails,
                        inventoryReferenceMapper.countWarehouseAvailableStock(id),
                        "库存模块: 仓库存在可用库存");
                addReferenceDetail(referenceDetails,
                        inventoryReferenceMapper.countWarehouseOpenInboundOrders(id),
                        "库存模块: 仓库存在未完成入库单");
                addReferenceDetail(referenceDetails,
                        inventoryReferenceMapper.countWarehouseOpenOutboundOrders(id),
                        "库存模块: 仓库存在未完成出库单");
                break;
            case MdmDomainTypeSupport.ORG:
                addReferenceDetail(referenceDetails,
                        projectMapper.selectCount(new LambdaQueryWrapper<MdmProject>()
                        .eq(MdmProject::getOrgId, id)
                        .eq(MdmProject::getDelFlag, DEL_FLAG_EXIST)),
                        "基础资料: 项目档案已引用当前组织");
                addReferenceDetail(referenceDetails,
                        customerMapper.selectCount(new LambdaQueryWrapper<MdmCustomer>()
                        .eq(MdmCustomer::getOrgId, id)
                        .eq(MdmCustomer::getDelFlag, DEL_FLAG_EXIST)),
                        "基础资料: 客户档案已引用当前组织");
                addReferenceDetail(referenceDetails,
                        employeeMapper.selectCount(new LambdaQueryWrapper<MdmEmployee>()
                        .eq(MdmEmployee::getOrgId, id)
                        .eq(MdmEmployee::getDelFlag, DEL_FLAG_EXIST)),
                        "基础资料: 员工档案已引用当前组织");
                addReferenceDetail(referenceDetails,
                        warehouseMapper.selectCount(new LambdaQueryWrapper<MdmWarehouse>()
                        .eq(MdmWarehouse::getOrgId, id)
                        .eq(MdmWarehouse::getDelFlag, DEL_FLAG_EXIST)),
                        "基础资料: 仓库档案已引用当前组织");
                break;
            case MdmDomainTypeSupport.COST_CENTER:
                addReferenceDetail(referenceDetails,
                        employeeMapper.selectCount(new LambdaQueryWrapper<MdmEmployee>()
                        .eq(MdmEmployee::getCostCenterId, id)
                        .eq(MdmEmployee::getDelFlag, DEL_FLAG_EXIST)),
                        "基础资料: 员工档案已引用当前成本中心");
                break;
            case MdmDomainTypeSupport.PROJECT:
                break;
            case MdmDomainTypeSupport.SETTLE_METHOD:
                addReferenceDetail(referenceDetails,
                        customerMapper.selectCount(new LambdaQueryWrapper<MdmCustomer>()
                        .eq(MdmCustomer::getSettleMethodId, id)
                        .eq(MdmCustomer::getDelFlag, DEL_FLAG_EXIST)),
                        "基础资料: 客户档案已引用当前结算方式");
                break;
            case MdmDomainTypeSupport.TAX_RATE:
                addReferenceDetail(referenceDetails,
                        itemMapper.selectCount(new LambdaQueryWrapper<MdmItem>()
                        .eq(MdmItem::getTaxRateId, id)
                        .eq(MdmItem::getDelFlag, DEL_FLAG_EXIST)),
                        "基础资料: 物料档案已引用当前税率");
                break;
            case MdmDomainTypeSupport.CURRENCY:
                MdmCurrency currency = currencyMapper.selectById(id);
                if (currency != null && currency.getCurrencyCode() != null) {
                    addReferenceDetail(referenceDetails,
                            customerMapper.selectCount(new LambdaQueryWrapper<MdmCustomer>()
                            .eq(MdmCustomer::getDefaultCurrency, currency.getCurrencyCode())
                            .eq(MdmCustomer::getDelFlag, DEL_FLAG_EXIST)),
                            "基础资料: 客户档案已引用当前币种");
                    addReferenceDetail(referenceDetails,
                            supplierMapper.selectCount(new LambdaQueryWrapper<MdmSupplier>()
                            .eq(MdmSupplier::getDefaultCurrency, currency.getCurrencyCode())
                            .eq(MdmSupplier::getDelFlag, DEL_FLAG_EXIST)),
                            "基础资料: 供应商档案已引用当前币种");
                }
                break;
            case MdmDomainTypeSupport.UOM:
                addReferenceDetail(referenceDetails,
                        itemMapper.selectCount(new LambdaQueryWrapper<MdmItem>()
                        .eq(MdmItem::getUnitId, id)
                        .eq(MdmItem::getDelFlag, DEL_FLAG_EXIST)),
                        "基础资料: 物料档案已引用当前计量单位");
                break;
            default:
                throw new ServiceException("暂不支持该主数据类型的引用检查: " + normalizedDomainType,
                        (int) ResultCode.VALIDATE_FAILED.getCode());
        }
        collectWorkflowReference(normalizedDomainType, id, referenceDetails);
        if (!referenceDetails.isEmpty()) {
            throw new ServiceException("当前主数据已被引用，无法删除或停用（" + String.join("；", referenceDetails) + "）",
                    (int) ResultCode.CONFLICT.getCode());
        }
    }

    /**
     * 收集运行中的流程实例占用情况。
     *
     * @param domainType        MDM 域类型
     * @param id                业务主键
     * @param referenceDetails  引用明细
     */
    private void collectWorkflowReference(String domainType, Long id, List<String> referenceDetails) {
        String businessType = MdmWorkflowBusinessSupport.resolveBusinessType(domainType);
        String businessNo = MdmWorkflowBusinessSupport.buildBusinessNo(domainType, id);
        if (!StringUtils.hasText(businessType) || !StringUtils.hasText(businessNo)) {
            return;
        }
        addReferenceDetail(referenceDetails,
                workflowInstanceMapper.selectCount(new LambdaQueryWrapper<SysWorkflowInstance>()
                        .eq(SysWorkflowInstance::getBusinessType, businessType)
                        .eq(SysWorkflowInstance::getBusinessNo, businessNo)
                        .eq(SysWorkflowInstance::getStatus, WORKFLOW_STATUS_RUNNING)),
                "流程中心: 当前主数据存在运行中的审批流程");
    }

    /**
     * 若存在引用则追加明细描述。
     *
     * @param referenceDetails 引用明细
     * @param count            引用数量
     * @param detail           引用说明
     */
    private void addReferenceDetail(List<String> referenceDetails, Long count, String detail) {
        if (count != null && count > 0 && StringUtils.hasText(detail)) {
            referenceDetails.add(detail);
        }
    }
}
