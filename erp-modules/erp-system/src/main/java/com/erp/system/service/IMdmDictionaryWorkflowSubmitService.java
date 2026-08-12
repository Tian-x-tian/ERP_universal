package com.erp.system.service;

import com.erp.system.domain.MdmCurrency;
import com.erp.system.domain.MdmSettleMethod;
import com.erp.system.domain.MdmTaxRate;
import com.erp.system.domain.MdmUom;

/**
 * 字典主数据审批提交流程服务接口。
 */
public interface IMdmDictionaryWorkflowSubmitService {
    boolean submitSettleDraftActivation(Long settleMethodId, String processKey, String remark);
    boolean submitSettleChange(Long settleMethodId, MdmSettleMethod targetSettleMethod, String processKey, String remark);
    boolean submitSettleDisable(Long settleMethodId, String processKey, String remark);
    boolean submitTaxDraftActivation(Long taxRateId, String processKey, String remark);
    boolean submitTaxChange(Long taxRateId, MdmTaxRate targetTaxRate, String processKey, String remark);
    boolean submitTaxDisable(Long taxRateId, String processKey, String remark);
    boolean submitCurrencyDraftActivation(Long currencyId, String processKey, String remark);
    boolean submitCurrencyChange(Long currencyId, MdmCurrency targetCurrency, String processKey, String remark);
    boolean submitCurrencyDisable(Long currencyId, String processKey, String remark);
    boolean submitUomDraftActivation(Long uomId, String processKey, String remark);
    boolean submitUomChange(Long uomId, MdmUom targetUom, String processKey, String remark);
    boolean submitUomDisable(Long uomId, String processKey, String remark);
}
