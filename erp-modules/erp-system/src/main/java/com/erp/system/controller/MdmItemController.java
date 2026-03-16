package com.erp.system.controller;

import com.erp.common.core.domain.PageData;
import com.erp.common.core.domain.R;
import com.erp.system.domain.MdmItem;
import com.erp.system.domain.vo.MdmItemWorkflowSubmitBody;
import com.erp.system.domain.vo.MdmVersionActionBody;
import com.erp.system.service.IMdmItemService;
import com.erp.system.service.IMdmItemWorkflowSubmitService;
import com.erp.system.support.MdmResponseSupport;
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
 * MDM 物料主数据控制层。
 */
@RestController
@RequestMapping("/system/mdm/item")
public class MdmItemController {
    private final IMdmItemService itemService;
    private final IMdmItemWorkflowSubmitService itemWorkflowSubmitService;

    public MdmItemController(IMdmItemService itemService,
                             IMdmItemWorkflowSubmitService itemWorkflowSubmitService) {
        this.itemService = itemService;
        this.itemWorkflowSubmitService = itemWorkflowSubmitService;
    }

    /**
     * 查询物料列表。
     *
     * @param itemCode 物料编码
     * @param itemName 物料名称
     * @param status   状态
     * @return 物料列表
     */
    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('system:mdm:item:list')")
    public R<PageData<MdmItem>> list(@RequestParam(value = "itemCode", required = false) String itemCode,
            @RequestParam(value = "itemName", required = false) String itemName,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") Long pageNum,
            @RequestParam(value = "pageSize", required = false, defaultValue = "20") Long pageSize) {
        return MdmResponseSupport.page(itemService.selectItemList(itemCode, itemName, status), pageNum, pageSize);
    }

    /**
     * 查询物料详情。
     *
     * @param itemId 物料ID
     * @return 物料详情
     */
    @GetMapping("/{itemId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:item:query')")
    public R<MdmItem> getInfo(@PathVariable("itemId") Long itemId) {
        MdmItem item = itemService.getById(itemId);
        if (item == null || "2".equals(item.getDelFlag())) {
            return R.failed("物料不存在");
        }
        return R.success(item);
    }

    /**
     * 新增物料。
     *
     * @param item 物料对象
     * @return 新增结果
     */
    @PostMapping
    @PreAuthorize("@ss.hasPermi('system:mdm:item:add')")
    public R<Boolean> add(@RequestBody MdmItem item) {
        boolean success = itemService.createItem(item);
        return success ? R.success(true) : R.failed("新增物料失败，请检查编码唯一性与关键字段");
    }

    /**
     * 修改物料。
     *
     * @param item 物料对象
     * @return 修改结果
     */
    @PutMapping
    @PreAuthorize("@ss.hasPermi('system:mdm:item:edit')")
    public R<Boolean> edit(@RequestBody MdmItem item) {
        if (item == null || item.getItemId() == null) {
            return R.failed("物料ID不能为空");
        }
        boolean success = itemService.updateItem(item);
        return success ? R.success(true) : R.failed("修改物料失败，可能触发编码不可改或批次/序列回退限制");
    }

    /**
     * 停用物料。
     *
     * @param itemId 物料ID
     * @return 停用结果
     */
    @PostMapping("/disable/{itemId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:item:disable')")
    public R<Boolean> disable(@PathVariable("itemId") Long itemId,
                              @RequestBody(required = false) MdmVersionActionBody actionBody) {
        boolean success = itemService.disableItem(itemId, actionBody == null ? null : actionBody.getVersionNo());
        return success ? R.success(true) : R.failed("停用物料失败");
    }

    /**
     * 提交物料草稿生效审批。
     *
     * @param itemId     物料ID
     * @param submitBody 审批提交参数
     * @return 提交结果
     */
    @PostMapping("/submit/{itemId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:item:edit')")
    public R<Boolean> submit(@PathVariable("itemId") Long itemId,
                             @RequestBody MdmItemWorkflowSubmitBody submitBody) {
        boolean success = itemWorkflowSubmitService.submitDraftActivation(
                itemId,
                submitBody == null ? null : submitBody.getVersionNo(),
                submitBody == null ? null : submitBody.getProcessKey(),
                submitBody == null ? null : submitBody.getRemark());
        return success ? R.success(true) : R.failed("提交物料审批失败");
    }

    /**
     * 提交物料变更审批。
     *
     * @param itemId     物料ID
     * @param submitBody 审批提交参数
     * @return 提交结果
     */
    @PostMapping("/change/{itemId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:item:edit')")
    public R<Boolean> submitChange(@PathVariable("itemId") Long itemId,
                                   @RequestBody MdmItemWorkflowSubmitBody submitBody) {
        boolean success = itemWorkflowSubmitService.submitChange(
                itemId,
                submitBody == null ? null : submitBody.getVersionNo(),
                submitBody == null ? null : submitBody.getItem(),
                submitBody == null ? null : submitBody.getProcessKey(),
                submitBody == null ? null : submitBody.getRemark());
        return success ? R.success(true) : R.failed("提交物料变更审批失败");
    }

    /**
     * 提交物料停用审批。
     *
     * @param itemId     物料ID
     * @param submitBody 审批提交参数
     * @return 提交结果
     */
    @PostMapping("/disable/submit/{itemId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:item:disable')")
    public R<Boolean> submitDisable(@PathVariable("itemId") Long itemId,
                                    @RequestBody MdmItemWorkflowSubmitBody submitBody) {
        boolean success = itemWorkflowSubmitService.submitDisable(
                itemId,
                submitBody == null ? null : submitBody.getVersionNo(),
                submitBody == null ? null : submitBody.getProcessKey(),
                submitBody == null ? null : submitBody.getRemark());
        return success ? R.success(true) : R.failed("提交物料停用审批失败");
    }

    /**
     * 删除物料（逻辑删除）。
     *
     * @param itemId 物料ID
     * @return 删除结果
     */
    @DeleteMapping("/{itemId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:item:remove')")
    public R<Boolean> remove(@PathVariable("itemId") Long itemId,
                             @RequestBody(required = false) MdmVersionActionBody actionBody) {
        boolean success = itemService.removeItem(itemId, actionBody == null ? null : actionBody.getVersionNo());
        return success ? R.success(true) : R.failed("删除物料失败，仅草稿状态允许删除");
    }
}
