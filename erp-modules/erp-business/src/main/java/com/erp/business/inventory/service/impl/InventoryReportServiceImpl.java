package com.erp.business.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.business.inventory.domain.InventoryBatchRecord;
import com.erp.business.inventory.domain.InventoryStockBalance;
import com.erp.business.inventory.domain.InventoryStockTxn;
import com.erp.business.inventory.domain.InventoryStocktakeOrder;
import com.erp.business.inventory.domain.InventoryStocktakeOrderLine;
import com.erp.business.inventory.domain.vo.InventoryAgeReportRow;
import com.erp.business.inventory.domain.vo.InventoryExpiryReportRow;
import com.erp.business.inventory.domain.vo.InventoryStocktakeDiffReportRow;
import com.erp.business.inventory.mapper.InventoryBatchRecordMapper;
import com.erp.business.inventory.mapper.InventoryStockBalanceMapper;
import com.erp.business.inventory.mapper.InventoryStockTxnMapper;
import com.erp.business.inventory.mapper.InventoryStocktakeOrderLineMapper;
import com.erp.business.inventory.mapper.InventoryStocktakeOrderMapper;
import com.erp.business.inventory.service.IInventoryReportService;
import com.erp.business.security.service.SecurityUserResolver;
import com.erp.business.system.domain.SysImexJob;
import com.erp.business.system.mapper.SysImexJobMapper;
import com.erp.common.core.context.TenantContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 库存报表服务实现。
 */
@Service
public class InventoryReportServiceImpl implements IInventoryReportService {

    private final InventoryStockBalanceMapper stockBalanceMapper;
    private final InventoryStockTxnMapper stockTxnMapper;
    private final InventoryBatchRecordMapper batchRecordMapper;
    private final InventoryStocktakeOrderMapper stocktakeOrderMapper;
    private final InventoryStocktakeOrderLineMapper stocktakeOrderLineMapper;
    private final SysImexJobMapper imexJobMapper;
    private final SecurityUserResolver securityUserResolver;

    public InventoryReportServiceImpl(InventoryStockBalanceMapper stockBalanceMapper,
            InventoryStockTxnMapper stockTxnMapper,
            InventoryBatchRecordMapper batchRecordMapper,
            InventoryStocktakeOrderMapper stocktakeOrderMapper,
            InventoryStocktakeOrderLineMapper stocktakeOrderLineMapper,
            SysImexJobMapper imexJobMapper,
            SecurityUserResolver securityUserResolver) {
        this.stockBalanceMapper = stockBalanceMapper;
        this.stockTxnMapper = stockTxnMapper;
        this.batchRecordMapper = batchRecordMapper;
        this.stocktakeOrderMapper = stocktakeOrderMapper;
        this.stocktakeOrderLineMapper = stocktakeOrderLineMapper;
        this.imexJobMapper = imexJobMapper;
        this.securityUserResolver = securityUserResolver;
    }

    /**
     * 查询库存汇总报表。
     *
     * @param warehouseId 仓库ID
     * @param itemId 物料ID
     * @param pageNum 页码
     * @param pageSize 页长
     * @return 分页结果
     */
    @Override
    public Page<InventoryStockBalance> selectSummaryPage(Long warehouseId, Long itemId, Long pageNum, Long pageSize) {
        Page<InventoryStockBalance> page = new Page<>(normalizePageNum(pageNum), normalizePageSize(pageSize));
        LambdaQueryWrapper<InventoryStockBalance> queryWrapper = new LambdaQueryWrapper<InventoryStockBalance>()
                .eq(InventoryStockBalance::getTenantId, currentTenantId())
                .eq(warehouseId != null, InventoryStockBalance::getWarehouseId, warehouseId)
                .eq(itemId != null, InventoryStockBalance::getItemId, itemId)
                .orderByDesc(InventoryStockBalance::getUpdateTime);
        return stockBalanceMapper.selectPage(page, queryWrapper);
    }

    /**
     * 查询库存收发存报表。
     *
     * @param warehouseId 仓库ID
     * @param itemId 物料ID
     * @param actionType 动作类型
     * @param pageNum 页码
     * @param pageSize 页长
     * @return 分页结果
     */
    @Override
    public Page<InventoryStockTxn> selectMovementPage(Long warehouseId, Long itemId, String actionType,
            Long pageNum, Long pageSize) {
        Page<InventoryStockTxn> page = new Page<>(normalizePageNum(pageNum), normalizePageSize(pageSize));
        LambdaQueryWrapper<InventoryStockTxn> queryWrapper = new LambdaQueryWrapper<InventoryStockTxn>()
                .eq(InventoryStockTxn::getTenantId, currentTenantId())
                .eq(warehouseId != null, InventoryStockTxn::getWarehouseId, warehouseId)
                .eq(itemId != null, InventoryStockTxn::getItemId, itemId)
                .eq(StringUtils.hasText(actionType), InventoryStockTxn::getActionType,
                        actionType == null ? null : actionType.trim().toUpperCase())
                .orderByDesc(InventoryStockTxn::getCreateTime);
        return stockTxnMapper.selectPage(page, queryWrapper);
    }

    /**
     * 查询库龄报表。
     *
     * @param warehouseId 仓库ID
     * @param itemId 物料ID
     * @param pageNum 页码
     * @param pageSize 页长
     * @return 分页结果
     */
    @Override
    public Page<InventoryAgeReportRow> selectAgePage(Long warehouseId, Long itemId, Long pageNum, Long pageSize) {
        List<InventoryAgeReportRow> rows = new ArrayList<>();
        List<InventoryBatchRecord> batches = batchRecordMapper.selectList(new LambdaQueryWrapper<InventoryBatchRecord>()
                .eq(InventoryBatchRecord::getTenantId, currentTenantId())
                .eq(warehouseId != null, InventoryBatchRecord::getWarehouseId, warehouseId)
                .eq(itemId != null, InventoryBatchRecord::getItemId, itemId)
                .orderByAsc(InventoryBatchRecord::getUpdateTime));
        for (InventoryBatchRecord batch : batches) {
            InventoryAgeReportRow row = new InventoryAgeReportRow();
            row.setBatchId(batch.getBatchId());
            row.setWarehouseId(batch.getWarehouseId());
            row.setItemId(batch.getItemId());
            row.setBatchNo(batch.getBatchNo());
            row.setCurrentQty(batch.getCurrentQty());
            row.setLastTxnTime(batch.getUpdateTime());
            row.setStatus(batch.getStatus());
            Date baseTime = batch.getUpdateTime() == null ? batch.getCreateTime() : batch.getUpdateTime();
            row.setAgeDays(baseTime == null ? 0L : ChronoUnit.DAYS.between(toLocalDate(baseTime), LocalDate.now()));
            rows.add(row);
        }
        return buildManualPage(rows, pageNum, pageSize);
    }

    /**
     * 查询有效期报表。
     *
     * @param warehouseId 仓库ID
     * @param itemId 物料ID
     * @param status 状态
     * @param pageNum 页码
     * @param pageSize 页长
     * @return 分页结果
     */
    @Override
    public Page<InventoryExpiryReportRow> selectExpiryPage(Long warehouseId, Long itemId, String status,
            Long pageNum, Long pageSize) {
        List<InventoryExpiryReportRow> rows = new ArrayList<>();
        List<InventoryBatchRecord> batches = batchRecordMapper.selectList(new LambdaQueryWrapper<InventoryBatchRecord>()
                .eq(InventoryBatchRecord::getTenantId, currentTenantId())
                .eq(warehouseId != null, InventoryBatchRecord::getWarehouseId, warehouseId)
                .eq(itemId != null, InventoryBatchRecord::getItemId, itemId)
                .eq(StringUtils.hasText(status), InventoryBatchRecord::getStatus, status == null ? null : status.trim().toUpperCase())
                .orderByAsc(InventoryBatchRecord::getExpiryDate));
        for (InventoryBatchRecord batch : batches) {
            InventoryExpiryReportRow row = new InventoryExpiryReportRow();
            row.setBatchId(batch.getBatchId());
            row.setWarehouseId(batch.getWarehouseId());
            row.setItemId(batch.getItemId());
            row.setBatchNo(batch.getBatchNo());
            row.setCurrentQty(batch.getCurrentQty());
            row.setProductionDate(batch.getProductionDate());
            row.setExpiryDate(batch.getExpiryDate());
            row.setStatus(batch.getStatus());
            row.setRemainingDays(batch.getExpiryDate() == null ? null : ChronoUnit.DAYS.between(LocalDate.now(), toLocalDate(batch.getExpiryDate())));
            rows.add(row);
        }
        return buildManualPage(rows, pageNum, pageSize);
    }

    /**
     * 查询盘点差异报表。
     *
     * @param warehouseId 仓库ID
     * @param status 状态
     * @param pageNum 页码
     * @param pageSize 页长
     * @return 分页结果
     */
    @Override
    public Page<InventoryStocktakeDiffReportRow> selectStocktakeDiffPage(Long warehouseId, String status,
            Long pageNum, Long pageSize) {
        List<InventoryStocktakeDiffReportRow> rows = new ArrayList<>();
        List<InventoryStocktakeOrder> orders = stocktakeOrderMapper.selectList(new LambdaQueryWrapper<InventoryStocktakeOrder>()
                .eq(InventoryStocktakeOrder::getTenantId, currentTenantId())
                .eq(warehouseId != null, InventoryStocktakeOrder::getWarehouseId, warehouseId)
                .eq(StringUtils.hasText(status), InventoryStocktakeOrder::getStatus, status == null ? null : status.trim().toUpperCase())
                .orderByDesc(InventoryStocktakeOrder::getCreateTime));
        for (InventoryStocktakeOrder order : orders) {
            List<InventoryStocktakeOrderLine> lines = stocktakeOrderLineMapper.selectList(new LambdaQueryWrapper<InventoryStocktakeOrderLine>()
                    .eq(InventoryStocktakeOrderLine::getOrderId, order.getOrderId())
                    .orderByAsc(InventoryStocktakeOrderLine::getLineNo));
            for (InventoryStocktakeOrderLine line : lines) {
                InventoryStocktakeDiffReportRow row = new InventoryStocktakeDiffReportRow();
                row.setOrderId(order.getOrderId());
                row.setBillNo(order.getBillNo());
                row.setStatus(order.getStatus());
                row.setWarehouseId(order.getWarehouseId());
                row.setItemId(line.getItemId());
                row.setAreaId(line.getAreaId());
                row.setLocationId(line.getLocationId());
                row.setSnapshotQty(line.getSnapshotQty());
                row.setCountedQty(line.getCountedQty());
                row.setDiffQty(line.getDiffQty());
                row.setCreateTime(order.getCreateTime());
                rows.add(row);
            }
        }
        return buildManualPage(rows, pageNum, pageSize);
    }

    /**
     * 导出报表并创建异步任务。
     *
     * @param reportType 报表类型
     * @param warehouseId 仓库ID
     * @param itemId 物料ID
     * @param actionType 动作类型
     * @param status 状态
     * @return 导出任务ID
     */
    @Override
    public Long exportReport(String reportType, Long warehouseId, Long itemId, String actionType, String status) {
        SysImexJob job = createJob(reportType);
        CompletableFuture.runAsync(() -> generateReportFile(job.getJobId(), reportType, warehouseId, itemId, actionType, status));
        return job.getJobId();
    }

    /**
     * 创建导出任务。
     *
     * @param reportType 报表类型
     * @return 任务对象
     */
    private SysImexJob createJob(String reportType) {
        Date now = new Date();
        String normalizedReportType = reportType == null ? "summary" : reportType.trim().toLowerCase();
        SysImexJob job = new SysImexJob();
        job.setTenantId(currentTenantId());
        job.setJobNo("EX" + System.currentTimeMillis());
        job.setJobType("EXPORT");
        job.setModuleCode("INVENTORY_" + normalizedReportType.toUpperCase());
        job.setFileName(normalizedReportType + "_" + System.currentTimeMillis() + ".csv");
        job.setStatus("PENDING");
        job.setProgress(0);
        job.setTriggerType("REPORT");
        job.setCreateBy(resolveOperator());
        job.setUpdateBy(resolveOperator());
        job.setCreateTime(now);
        job.setUpdateTime(now);
        imexJobMapper.insert(job);
        return job;
    }

    /**
     * 生成导出文件。
     *
     * @param jobId 任务ID
     * @param reportType 报表类型
     * @param warehouseId 仓库ID
     * @param itemId 物料ID
     * @param actionType 动作类型
     * @param status 状态
     */
    private void generateReportFile(Long jobId, String reportType, Long warehouseId, Long itemId, String actionType, String status) {
        try {
            updateJob(jobId, "RUNNING", 10, null, null);
            Path dir = Paths.get(System.getProperty("user.dir"), "upload", "imex");
            Files.createDirectories(dir);
            SysImexJob job = imexJobMapper.selectById(jobId);
            Path filePath = dir.resolve(job.getFileName());
            writeCsv(reportType, warehouseId, itemId, actionType, status, filePath);
            updateJob(jobId, "SUCCESS", 100, filePath.toAbsolutePath().toString(), "导出成功");
        } catch (Exception ex) {
            updateJob(jobId, "FAILED", 100, null, ex.getMessage());
        }
    }

    /**
     * 写出 CSV 文件。
     *
     * @param reportType 报表类型
     * @param warehouseId 仓库ID
     * @param itemId 物料ID
     * @param actionType 动作类型
     * @param status 状态
     * @param filePath 文件路径
     * @throws IOException IO 异常
     */
    private void writeCsv(String reportType, Long warehouseId, Long itemId, String actionType, String status,
            Path filePath) throws IOException {
        String normalizedReportType = reportType == null ? "summary" : reportType.trim().toLowerCase();
        try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            if ("movement".equals(normalizedReportType)) {
                writer.write("billNo,actionType,itemId,warehouseId,changeQty,createTime");
                writer.newLine();
                for (InventoryStockTxn item : selectMovementPage(warehouseId, itemId, actionType, 1L, 5000L).getRecords()) {
                    writer.write(csv(item.getBillNo()) + "," + csv(item.getActionType()) + "," + item.getItemId() + ","
                            + item.getWarehouseId() + "," + safeQty(item.getChangeQty()) + "," + csv(String.valueOf(item.getCreateTime())));
                    writer.newLine();
                }
                return;
            }
            if ("age".equals(normalizedReportType)) {
                writer.write("batchNo,itemId,warehouseId,currentQty,lastTxnTime,ageDays,status");
                writer.newLine();
                for (InventoryAgeReportRow item : selectAgePage(warehouseId, itemId, 1L, 5000L).getRecords()) {
                    writer.write(csv(item.getBatchNo()) + "," + item.getItemId() + "," + item.getWarehouseId() + ","
                            + safeQty(item.getCurrentQty()) + "," + csv(String.valueOf(item.getLastTxnTime())) + ","
                            + item.getAgeDays() + "," + csv(item.getStatus()));
                    writer.newLine();
                }
                return;
            }
            if ("expiry".equals(normalizedReportType)) {
                writer.write("batchNo,itemId,warehouseId,currentQty,productionDate,expiryDate,remainingDays,status");
                writer.newLine();
                for (InventoryExpiryReportRow item : selectExpiryPage(warehouseId, itemId, status, 1L, 5000L).getRecords()) {
                    writer.write(csv(item.getBatchNo()) + "," + item.getItemId() + "," + item.getWarehouseId() + ","
                            + safeQty(item.getCurrentQty()) + "," + csv(String.valueOf(item.getProductionDate())) + ","
                            + csv(String.valueOf(item.getExpiryDate())) + "," + item.getRemainingDays() + "," + csv(item.getStatus()));
                    writer.newLine();
                }
                return;
            }
            if ("stocktake-diff".equals(normalizedReportType)) {
                writer.write("billNo,status,warehouseId,itemId,areaId,locationId,snapshotQty,countedQty,diffQty,createTime");
                writer.newLine();
                for (InventoryStocktakeDiffReportRow item : selectStocktakeDiffPage(warehouseId, status, 1L, 5000L).getRecords()) {
                    writer.write(csv(item.getBillNo()) + "," + csv(item.getStatus()) + "," + item.getWarehouseId() + ","
                            + item.getItemId() + "," + item.getAreaId() + "," + item.getLocationId() + ","
                            + safeQty(item.getSnapshotQty()) + "," + safeQty(item.getCountedQty()) + ","
                            + safeQty(item.getDiffQty()) + "," + csv(String.valueOf(item.getCreateTime())));
                    writer.newLine();
                }
                return;
            }
            writer.write("warehouseId,itemId,batchNo,serialNo,onHandQty,availableQty,frozenQty,lastTxnTime");
            writer.newLine();
            for (InventoryStockBalance item : selectSummaryPage(warehouseId, itemId, 1L, 5000L).getRecords()) {
                writer.write(item.getWarehouseId() + "," + item.getItemId() + "," + csv(item.getBatchNo()) + ","
                        + csv(item.getSerialNo()) + "," + safeQty(item.getOnHandQty()) + "," + safeQty(item.getAvailableQty())
                        + "," + safeQty(item.getFrozenQty()) + "," + csv(String.valueOf(item.getLastTxnTime())));
                writer.newLine();
            }
        }
    }

    /**
     * 更新导出任务状态。
     *
     * @param jobId 任务ID
     * @param status 状态
     * @param progress 进度
     * @param filePath 文件路径
     * @param message 任务消息
     */
    private void updateJob(Long jobId, String status, Integer progress, String filePath, String message) {
        SysImexJob updateEntity = new SysImexJob();
        updateEntity.setJobId(jobId);
        updateEntity.setStatus(status);
        updateEntity.setProgress(progress);
        updateEntity.setFilePath(filePath);
        updateEntity.setMessage(message);
        updateEntity.setUpdateBy(resolveOperator());
        updateEntity.setUpdateTime(new Date());
        imexJobMapper.updateById(updateEntity);
    }

    /**
     * 构建手工分页结果。
     *
     * @param rows 全量数据
     * @param pageNum 页码
     * @param pageSize 页长
     * @param <T> 行类型
     * @return 分页结果
     */
    private <T> Page<T> buildManualPage(List<T> rows, Long pageNum, Long pageSize) {
        long normalizedPageNum = normalizePageNum(pageNum);
        long normalizedPageSize = normalizePageSize(pageSize);
        int fromIndex = (int) ((normalizedPageNum - 1) * normalizedPageSize);
        int toIndex = Math.min(rows.size(), fromIndex + (int) normalizedPageSize);
        List<T> pageRows = fromIndex >= rows.size() ? new ArrayList<>() : rows.subList(fromIndex, toIndex);
        Page<T> page = new Page<>(normalizedPageNum, normalizedPageSize);
        page.setRecords(pageRows);
        page.setTotal(rows.size());
        return page;
    }

    /**
     * CSV 字段转义。
     *
     * @param value 原始值
     * @return 转义值
     */
    private String csv(String value) {
        String safeValue = value == null ? "" : value.replace("\"", "\"\"");
        return "\"" + safeValue + "\"";
    }

    /**
     * 日期转本地日期。
     *
     * @param date 日期对象
     * @return 本地日期
     */
    private LocalDate toLocalDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /**
     * 安全处理数量。
     *
     * @param value 原始数量
     * @return 标准数量
     */
    private BigDecimal safeQty(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * 获取当前租户编号。
     *
     * @return 租户编号
     */
    private String currentTenantId() {
        String tenantId = TenantContextHolder.getTenantId();
        if (!StringUtils.hasText(tenantId)) {
            throw new IllegalStateException("当前租户上下文缺失");
        }
        return tenantId.trim();
    }

    /**
     * 获取当前操作人。
     *
     * @return 操作人账号
     */
    private String resolveOperator() {
        String username = securityUserResolver.getCurrentUsername();
        return StringUtils.hasText(username) ? username.trim() : "system";
    }

    /**
     * 规范化页码。
     *
     * @param pageNum 原始页码
     * @return 标准页码
     */
    private long normalizePageNum(Long pageNum) {
        return pageNum == null || pageNum < 1 ? 1L : pageNum;
    }

    /**
     * 规范化页长。
     *
     * @param pageSize 原始页长
     * @return 标准页长
     */
    private long normalizePageSize(Long pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 20L;
        }
        return Math.min(pageSize, 5000L);
    }
}
