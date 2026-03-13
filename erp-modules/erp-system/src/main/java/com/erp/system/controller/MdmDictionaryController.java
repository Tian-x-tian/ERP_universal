package com.erp.system.controller;

import com.erp.common.core.domain.PageData;
import com.erp.common.core.domain.R;
import com.erp.system.domain.MdmCurrency;
import com.erp.system.domain.MdmSettleMethod;
import com.erp.system.domain.MdmTaxRate;
import com.erp.system.domain.MdmUom;
import com.erp.system.domain.vo.MdmCurrencyWorkflowSubmitBody;
import com.erp.system.domain.vo.MdmSettleMethodWorkflowSubmitBody;
import com.erp.system.domain.vo.MdmTaxRateWorkflowSubmitBody;
import com.erp.system.domain.vo.MdmUomWorkflowSubmitBody;
import com.erp.system.service.IMdmCurrencyService;
import com.erp.system.service.IMdmDictionaryWorkflowSubmitService;
import com.erp.system.service.IMdmSettleMethodService;
import com.erp.system.service.IMdmTaxRateService;
import com.erp.system.service.IMdmUomService;
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
 * MDM 基础字典控制层。
 */
@RestController
@RequestMapping("/system/mdm/dict")
public class MdmDictionaryController {
    private final IMdmSettleMethodService settleMethodService;
    private final IMdmTaxRateService taxRateService;
    private final IMdmCurrencyService currencyService;
    private final IMdmUomService uomService;
    private final IMdmDictionaryWorkflowSubmitService dictionaryWorkflowSubmitService;

    public MdmDictionaryController(IMdmSettleMethodService settleMethodService,
            IMdmTaxRateService taxRateService,
            IMdmCurrencyService currencyService,
            IMdmUomService uomService,
            IMdmDictionaryWorkflowSubmitService dictionaryWorkflowSubmitService) {
        this.settleMethodService = settleMethodService;
        this.taxRateService = taxRateService;
        this.currencyService = currencyService;
        this.uomService = uomService;
        this.dictionaryWorkflowSubmitService = dictionaryWorkflowSubmitService;
    }

    /**
     * 查询结算方式列表。
     *
     * @param settleCode 结算方式编码
     * @param settleName 结算方式名称
     * @param status     状态
     * @return 结算方式列表
     */
    @GetMapping("/settle/list")
    @PreAuthorize("@ss.hasPermi('system:mdm:dict:list')")
    public R<PageData<MdmSettleMethod>> settleList(
            @RequestParam(value = "settleCode", required = false) String settleCode,
            @RequestParam(value = "settleName", required = false) String settleName,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") Long pageNum,
            @RequestParam(value = "pageSize", required = false, defaultValue = "20") Long pageSize) {
        return R.success(MdmPageSupport.paginate(settleMethodService.selectList(settleCode, settleName, status),
                pageNum, pageSize));
    }

    /**
     * 查询结算方式详情。
     *
     * @param settleMethodId 结算方式ID
     * @return 结算方式详情
     */
    @GetMapping("/settle/{settleMethodId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:dict:query')")
    public R<MdmSettleMethod> settleInfo(@PathVariable("settleMethodId") Long settleMethodId) {
        return R.success(settleMethodService.getById(settleMethodId));
    }

    /**
     * 新增结算方式。
     *
     * @param settleMethod 结算方式对象
     * @return 新增结果
     */
    @PostMapping("/settle")
    @PreAuthorize("@ss.hasPermi('system:mdm:dict:add')")
    public R<Boolean> settleAdd(@RequestBody MdmSettleMethod settleMethod) {
        boolean success = settleMethodService.create(settleMethod);
        return success ? R.success(true) : R.failed("新增结算方式失败，请检查编码唯一性");
    }

    /**
     * 修改结算方式。
     *
     * @param settleMethod 结算方式对象
     * @return 修改结果
     */
    @PutMapping("/settle")
    @PreAuthorize("@ss.hasPermi('system:mdm:dict:edit')")
    public R<Boolean> settleEdit(@RequestBody MdmSettleMethod settleMethod) {
        boolean success = settleMethodService.modify(settleMethod);
        return success ? R.success(true) : R.failed("修改结算方式失败，请检查编码唯一性");
    }

    /**
     * 停用结算方式。
     *
     * @param settleMethodId 结算方式ID
     * @return 停用结果
     */
    @PostMapping("/settle/disable/{settleMethodId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:dict:disable')")
    public R<Boolean> settleDisable(@PathVariable("settleMethodId") Long settleMethodId) {
        boolean success = settleMethodService.disable(settleMethodId);
        return success ? R.success(true) : R.failed("停用结算方式失败");
    }

    @PostMapping("/settle/submit/{settleMethodId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:dict:edit')")
    public R<Boolean> settleSubmit(@PathVariable("settleMethodId") Long settleMethodId,
            @RequestBody MdmSettleMethodWorkflowSubmitBody submitBody) {
        boolean success = dictionaryWorkflowSubmitService.submitSettleDraftActivation(
                settleMethodId,
                submitBody == null ? null : submitBody.getProcessKey(),
                submitBody == null ? null : submitBody.getRemark());
        return success ? R.success(true) : R.failed("提交结算方式审批失败");
    }

    @PostMapping("/settle/change/{settleMethodId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:dict:edit')")
    public R<Boolean> settleSubmitChange(@PathVariable("settleMethodId") Long settleMethodId,
            @RequestBody MdmSettleMethodWorkflowSubmitBody submitBody) {
        boolean success = dictionaryWorkflowSubmitService.submitSettleChange(
                settleMethodId,
                submitBody == null ? null : submitBody.getSettleMethod(),
                submitBody == null ? null : submitBody.getProcessKey(),
                submitBody == null ? null : submitBody.getRemark());
        return success ? R.success(true) : R.failed("提交结算方式变更审批失败");
    }

    @PostMapping("/settle/disable/submit/{settleMethodId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:dict:disable')")
    public R<Boolean> settleSubmitDisable(@PathVariable("settleMethodId") Long settleMethodId,
            @RequestBody MdmSettleMethodWorkflowSubmitBody submitBody) {
        boolean success = dictionaryWorkflowSubmitService.submitSettleDisable(
                settleMethodId,
                submitBody == null ? null : submitBody.getProcessKey(),
                submitBody == null ? null : submitBody.getRemark());
        return success ? R.success(true) : R.failed("提交结算方式停用审批失败");
    }

    /**
     * 删除结算方式。
     *
     * @param settleMethodId 结算方式ID
     * @return 删除结果
     */
    @DeleteMapping("/settle/{settleMethodId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:dict:remove')")
    public R<Boolean> settleRemove(@PathVariable("settleMethodId") Long settleMethodId) {
        boolean success = settleMethodService.remove(settleMethodId);
        return success ? R.success(true) : R.failed("删除结算方式失败，请先停用");
    }

    /**
     * 查询税率列表。
     *
     * @param taxCode 税率编码
     * @param taxName 税率名称
     * @param status  状态
     * @return 税率列表
     */
    @GetMapping("/tax/list")
    @PreAuthorize("@ss.hasPermi('system:mdm:dict:list')")
    public R<PageData<MdmTaxRate>> taxList(@RequestParam(value = "taxCode", required = false) String taxCode,
            @RequestParam(value = "taxName", required = false) String taxName,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") Long pageNum,
            @RequestParam(value = "pageSize", required = false, defaultValue = "20") Long pageSize) {
        return R.success(
                MdmPageSupport.paginate(taxRateService.selectList(taxCode, taxName, status), pageNum, pageSize));
    }

    /**
     * 查询税率详情。
     *
     * @param taxRateId 税率ID
     * @return 税率详情
     */
    @GetMapping("/tax/{taxRateId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:dict:query')")
    public R<MdmTaxRate> taxInfo(@PathVariable("taxRateId") Long taxRateId) {
        return R.success(taxRateService.getById(taxRateId));
    }

    /**
     * 新增税率。
     *
     * @param taxRate 税率对象
     * @return 新增结果
     */
    @PostMapping("/tax")
    @PreAuthorize("@ss.hasPermi('system:mdm:dict:add')")
    public R<Boolean> taxAdd(@RequestBody MdmTaxRate taxRate) {
        boolean success = taxRateService.create(taxRate);
        return success ? R.success(true) : R.failed("新增税率失败，请检查编码、税率值与生效区间");
    }

    /**
     * 修改税率。
     *
     * @param taxRate 税率对象
     * @return 修改结果
     */
    @PutMapping("/tax")
    @PreAuthorize("@ss.hasPermi('system:mdm:dict:edit')")
    public R<Boolean> taxEdit(@RequestBody MdmTaxRate taxRate) {
        boolean success = taxRateService.modify(taxRate);
        return success ? R.success(true) : R.failed("修改税率失败，请检查编码、税率值与生效区间");
    }

    /**
     * 停用税率。
     *
     * @param taxRateId 税率ID
     * @return 停用结果
     */
    @PostMapping("/tax/disable/{taxRateId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:dict:disable')")
    public R<Boolean> taxDisable(@PathVariable("taxRateId") Long taxRateId) {
        boolean success = taxRateService.disable(taxRateId);
        return success ? R.success(true) : R.failed("停用税率失败");
    }

    @PostMapping("/tax/submit/{taxRateId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:dict:edit')")
    public R<Boolean> taxSubmit(@PathVariable("taxRateId") Long taxRateId,
            @RequestBody MdmTaxRateWorkflowSubmitBody submitBody) {
        boolean success = dictionaryWorkflowSubmitService.submitTaxDraftActivation(
                taxRateId,
                submitBody == null ? null : submitBody.getProcessKey(),
                submitBody == null ? null : submitBody.getRemark());
        return success ? R.success(true) : R.failed("提交税率审批失败");
    }

    @PostMapping("/tax/change/{taxRateId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:dict:edit')")
    public R<Boolean> taxSubmitChange(@PathVariable("taxRateId") Long taxRateId,
            @RequestBody MdmTaxRateWorkflowSubmitBody submitBody) {
        boolean success = dictionaryWorkflowSubmitService.submitTaxChange(
                taxRateId,
                submitBody == null ? null : submitBody.getTaxRate(),
                submitBody == null ? null : submitBody.getProcessKey(),
                submitBody == null ? null : submitBody.getRemark());
        return success ? R.success(true) : R.failed("提交税率变更审批失败");
    }

    @PostMapping("/tax/disable/submit/{taxRateId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:dict:disable')")
    public R<Boolean> taxSubmitDisable(@PathVariable("taxRateId") Long taxRateId,
            @RequestBody MdmTaxRateWorkflowSubmitBody submitBody) {
        boolean success = dictionaryWorkflowSubmitService.submitTaxDisable(
                taxRateId,
                submitBody == null ? null : submitBody.getProcessKey(),
                submitBody == null ? null : submitBody.getRemark());
        return success ? R.success(true) : R.failed("提交税率停用审批失败");
    }

    /**
     * 删除税率。
     *
     * @param taxRateId 税率ID
     * @return 删除结果
     */
    @DeleteMapping("/tax/{taxRateId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:dict:remove')")
    public R<Boolean> taxRemove(@PathVariable("taxRateId") Long taxRateId) {
        boolean success = taxRateService.remove(taxRateId);
        return success ? R.success(true) : R.failed("删除税率失败，请先停用");
    }

    /**
     * 查询币种列表。
     *
     * @param currencyCode 币种编码
     * @param currencyName 币种名称
     * @param status       状态
     * @return 币种列表
     */
    @GetMapping("/currency/list")
    @PreAuthorize("@ss.hasPermi('system:mdm:dict:list')")
    public R<PageData<MdmCurrency>> currencyList(
            @RequestParam(value = "currencyCode", required = false) String currencyCode,
            @RequestParam(value = "currencyName", required = false) String currencyName,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") Long pageNum,
            @RequestParam(value = "pageSize", required = false, defaultValue = "20") Long pageSize) {
        return R.success(MdmPageSupport.paginate(currencyService.selectList(currencyCode, currencyName, status),
                pageNum, pageSize));
    }

    /**
     * 查询币种详情。
     *
     * @param currencyId 币种ID
     * @return 币种详情
     */
    @GetMapping("/currency/{currencyId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:dict:query')")
    public R<MdmCurrency> currencyInfo(@PathVariable("currencyId") Long currencyId) {
        return R.success(currencyService.getById(currencyId));
    }

    /**
     * 新增币种。
     *
     * @param currency 币种对象
     * @return 新增结果
     */
    @PostMapping("/currency")
    @PreAuthorize("@ss.hasPermi('system:mdm:dict:add')")
    public R<Boolean> currencyAdd(@RequestBody MdmCurrency currency) {
        boolean success = currencyService.create(currency);
        return success ? R.success(true) : R.failed("新增币种失败，请检查编码唯一性与生效区间");
    }

    /**
     * 修改币种。
     *
     * @param currency 币种对象
     * @return 修改结果
     */
    @PutMapping("/currency")
    @PreAuthorize("@ss.hasPermi('system:mdm:dict:edit')")
    public R<Boolean> currencyEdit(@RequestBody MdmCurrency currency) {
        boolean success = currencyService.modify(currency);
        return success ? R.success(true) : R.failed("修改币种失败，请检查编码唯一性与生效区间");
    }

    /**
     * 停用币种。
     *
     * @param currencyId 币种ID
     * @return 停用结果
     */
    @PostMapping("/currency/disable/{currencyId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:dict:disable')")
    public R<Boolean> currencyDisable(@PathVariable("currencyId") Long currencyId) {
        boolean success = currencyService.disable(currencyId);
        return success ? R.success(true) : R.failed("停用币种失败");
    }

    @PostMapping("/currency/submit/{currencyId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:dict:edit')")
    public R<Boolean> currencySubmit(@PathVariable("currencyId") Long currencyId,
            @RequestBody MdmCurrencyWorkflowSubmitBody submitBody) {
        boolean success = dictionaryWorkflowSubmitService.submitCurrencyDraftActivation(
                currencyId,
                submitBody == null ? null : submitBody.getProcessKey(),
                submitBody == null ? null : submitBody.getRemark());
        return success ? R.success(true) : R.failed("提交币种审批失败");
    }

    @PostMapping("/currency/change/{currencyId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:dict:edit')")
    public R<Boolean> currencySubmitChange(@PathVariable("currencyId") Long currencyId,
            @RequestBody MdmCurrencyWorkflowSubmitBody submitBody) {
        boolean success = dictionaryWorkflowSubmitService.submitCurrencyChange(
                currencyId,
                submitBody == null ? null : submitBody.getCurrency(),
                submitBody == null ? null : submitBody.getProcessKey(),
                submitBody == null ? null : submitBody.getRemark());
        return success ? R.success(true) : R.failed("提交币种变更审批失败");
    }

    @PostMapping("/currency/disable/submit/{currencyId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:dict:disable')")
    public R<Boolean> currencySubmitDisable(@PathVariable("currencyId") Long currencyId,
            @RequestBody MdmCurrencyWorkflowSubmitBody submitBody) {
        boolean success = dictionaryWorkflowSubmitService.submitCurrencyDisable(
                currencyId,
                submitBody == null ? null : submitBody.getProcessKey(),
                submitBody == null ? null : submitBody.getRemark());
        return success ? R.success(true) : R.failed("提交币种停用审批失败");
    }

    /**
     * 删除币种。
     *
     * @param currencyId 币种ID
     * @return 删除结果
     */
    @DeleteMapping("/currency/{currencyId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:dict:remove')")
    public R<Boolean> currencyRemove(@PathVariable("currencyId") Long currencyId) {
        boolean success = currencyService.remove(currencyId);
        return success ? R.success(true) : R.failed("删除币种失败，请先停用");
    }

    /**
     * 查询计量单位列表。
     *
     * @param uomCode 单位编码
     * @param uomName 单位名称
     * @param status  状态
     * @return 单位列表
     */
    @GetMapping("/uom/list")
    @PreAuthorize("@ss.hasPermi('system:mdm:dict:list')")
    public R<PageData<MdmUom>> uomList(@RequestParam(value = "uomCode", required = false) String uomCode,
            @RequestParam(value = "uomName", required = false) String uomName,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") Long pageNum,
            @RequestParam(value = "pageSize", required = false, defaultValue = "20") Long pageSize) {
        return R.success(MdmPageSupport.paginate(uomService.selectList(uomCode, uomName, status), pageNum, pageSize));
    }

    /**
     * 查询计量单位详情。
     *
     * @param uomId 单位ID
     * @return 单位详情
     */
    @GetMapping("/uom/{uomId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:dict:query')")
    public R<MdmUom> uomInfo(@PathVariable("uomId") Long uomId) {
        return R.success(uomService.getById(uomId));
    }

    /**
     * 新增计量单位。
     *
     * @param uom 单位对象
     * @return 新增结果
     */
    @PostMapping("/uom")
    @PreAuthorize("@ss.hasPermi('system:mdm:dict:add')")
    public R<Boolean> uomAdd(@RequestBody MdmUom uom) {
        boolean success = uomService.create(uom);
        return success ? R.success(true) : R.failed("新增计量单位失败，请检查编码唯一性");
    }

    /**
     * 修改计量单位。
     *
     * @param uom 单位对象
     * @return 修改结果
     */
    @PutMapping("/uom")
    @PreAuthorize("@ss.hasPermi('system:mdm:dict:edit')")
    public R<Boolean> uomEdit(@RequestBody MdmUom uom) {
        boolean success = uomService.modify(uom);
        return success ? R.success(true) : R.failed("修改计量单位失败，请检查编码唯一性");
    }

    /**
     * 停用计量单位。
     *
     * @param uomId 单位ID
     * @return 停用结果
     */
    @PostMapping("/uom/disable/{uomId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:dict:disable')")
    public R<Boolean> uomDisable(@PathVariable("uomId") Long uomId) {
        boolean success = uomService.disable(uomId);
        return success ? R.success(true) : R.failed("停用计量单位失败");
    }

    @PostMapping("/uom/submit/{uomId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:dict:edit')")
    public R<Boolean> uomSubmit(@PathVariable("uomId") Long uomId,
            @RequestBody MdmUomWorkflowSubmitBody submitBody) {
        boolean success = dictionaryWorkflowSubmitService.submitUomDraftActivation(
                uomId,
                submitBody == null ? null : submitBody.getProcessKey(),
                submitBody == null ? null : submitBody.getRemark());
        return success ? R.success(true) : R.failed("提交计量单位审批失败");
    }

    @PostMapping("/uom/change/{uomId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:dict:edit')")
    public R<Boolean> uomSubmitChange(@PathVariable("uomId") Long uomId,
            @RequestBody MdmUomWorkflowSubmitBody submitBody) {
        boolean success = dictionaryWorkflowSubmitService.submitUomChange(
                uomId,
                submitBody == null ? null : submitBody.getUom(),
                submitBody == null ? null : submitBody.getProcessKey(),
                submitBody == null ? null : submitBody.getRemark());
        return success ? R.success(true) : R.failed("提交计量单位变更审批失败");
    }

    @PostMapping("/uom/disable/submit/{uomId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:dict:disable')")
    public R<Boolean> uomSubmitDisable(@PathVariable("uomId") Long uomId,
            @RequestBody MdmUomWorkflowSubmitBody submitBody) {
        boolean success = dictionaryWorkflowSubmitService.submitUomDisable(
                uomId,
                submitBody == null ? null : submitBody.getProcessKey(),
                submitBody == null ? null : submitBody.getRemark());
        return success ? R.success(true) : R.failed("提交计量单位停用审批失败");
    }

    /**
     * 删除计量单位。
     *
     * @param uomId 单位ID
     * @return 删除结果
     */
    @DeleteMapping("/uom/{uomId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:dict:remove')")
    public R<Boolean> uomRemove(@PathVariable("uomId") Long uomId) {
        boolean success = uomService.remove(uomId);
        return success ? R.success(true) : R.failed("删除计量单位失败，请先停用");
    }
}
