package com.erp.business.hr.service.impl;

import com.erp.business.hr.domain.vo.HrEmployeeArchiveBody;
import com.erp.business.hr.service.IHrEmployeeChangeService;
import com.erp.business.hr.service.IHrEmployeeWorkflowBridgeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 员工工作流扩展桥接服务实现。
 */
@Service
public class HrEmployeeWorkflowBridgeServiceImpl implements IHrEmployeeWorkflowBridgeService {
    private final IHrEmployeeChangeService employeeChangeService;
    private final ObjectMapper objectMapper;

    public HrEmployeeWorkflowBridgeServiceImpl(IHrEmployeeChangeService employeeChangeService) {
        this.employeeChangeService = employeeChangeService;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 审批通过后回写 HR 扩展数据。
     *
     * @param changeRecordId 异动记录ID
     * @param archivePayloadJson 审批通过后的扩展档案JSON
     * @param operator 操作人
     */
    @Override
    public void onChangeApproved(Long changeRecordId, String archivePayloadJson, String operator) {
        if (changeRecordId == null) {
            return;
        }
        HrEmployeeArchiveBody archiveBody = null;
        if (StringUtils.hasText(archivePayloadJson)) {
            try {
                archiveBody = objectMapper.readValue(archivePayloadJson, HrEmployeeArchiveBody.class);
            } catch (Exception ex) {
                throw new IllegalStateException("审批档案快照解析失败", ex);
            }
        }
        employeeChangeService.approveChange(changeRecordId, archiveBody, operator);
    }

    /**
     * 审批驳回或撤回后回写 HR 异动状态。
     *
     * @param changeRecordId 异动记录ID
     * @param operator 操作人
     */
    @Override
    public void onChangeRejected(Long changeRecordId, String operator) {
        if (changeRecordId == null) {
            return;
        }
        employeeChangeService.rejectChange(changeRecordId, operator);
    }
}
