package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.erp.system.domain.SysNotice;
import com.erp.system.mapper.SysNoticeMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 系统消息通知服务单元测试。
 */
@ExtendWith(MockitoExtension.class)
class SysNoticeServiceImplTest {

    @Mock
    private SysNoticeMapper noticeMapper;

    private SysNoticeServiceImpl noticeService;

    /**
     * 初始化被测服务并注册实体元数据。
     */
    @BeforeEach
    void setUp() {
        noticeService = new SysNoticeServiceImpl();
        ReflectionTestUtils.setField(noticeService, "baseMapper", noticeMapper);
        initTableInfoIfAbsent(SysNotice.class);
    }

    /**
     * 初始化实体元数据缓存，保证 LambdaQueryWrapper 在纯单测场景可用。
     *
     * @param entityClass 实体类型
     */
    private void initTableInfoIfAbsent(Class<?> entityClass) {
        if (TableInfoHelper.getTableInfo(entityClass) != null) {
            return;
        }
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant builderAssistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(builderAssistant, entityClass);
    }

    /**
     * 验证按当前用户和送达维度查询消息列表。
     */
    @Test
    void shouldSelectByCurrentUserWithDeliveryFilters() {
        when(noticeMapper.selectList(any())).thenReturn(Collections.singletonList(new SysNotice()));

        List<SysNotice> noticeList = noticeService.selectByCurrentUser(1L, "审批通知", "0", "IN_APP", "2");

        Assertions.assertEquals(1, noticeList.size());
        verify(noticeMapper).selectList(any());
    }

    /**
     * 验证标记单条消息已读时会执行更新。
     */
    @Test
    void shouldMarkRead() {
        when(noticeMapper.update(isNull(), any())).thenReturn(1);

        boolean success = noticeService.markRead(11L, 3L);

        Assertions.assertTrue(success);
        verify(noticeMapper).update(isNull(), any());
    }

    /**
     * 验证批量标记已读会返回更新条数。
     */
    @Test
    void shouldMarkAllRead() {
        when(noticeMapper.update(isNull(), any())).thenReturn(2);

        int updated = noticeService.markAllRead(3L);

        Assertions.assertEquals(2, updated);
    }

    /**
     * 验证管理端查询支持送达和业务维度过滤。
     */
    @Test
    void shouldSelectForManageWithExtendedFilters() {
        when(noticeMapper.selectList(any())).thenReturn(Collections.singletonList(new SysNotice()));

        List<SysNotice> noticeList = noticeService.selectForManage("000000", "采购", "审批通知",
                8L, "0", "SMS", "0", "流程引擎", "PO-001");

        Assertions.assertEquals(1, noticeList.size());
    }

    /**
     * 验证外部渠道新建消息时默认进入待发送状态。
     */
    @Test
    void shouldCreateNoticeWithPendingStatusForExternalChannel() {
        SysNotice notice = new SysNotice();
        notice.setTitle("外部消息");
        notice.setReceiverUserId(5L);
        notice.setDeliveryChannel("sms");
        when(noticeMapper.insert(any(SysNotice.class))).thenReturn(1);

        boolean success = noticeService.createNotice(notice);

        Assertions.assertTrue(success);
        Assertions.assertEquals("SMS", notice.getDeliveryChannel());
        Assertions.assertEquals("0", notice.getDeliveryStatus());
        Assertions.assertNull(notice.getDeliveryTime());
    }

    /**
     * 验证消息送达时会自动补齐送达时间。
     */
    @Test
    void shouldFillDeliveryTimeWhenNoticeDelivered() {
        SysNotice existedNotice = new SysNotice();
        existedNotice.setNoticeId(21L);
        existedNotice.setTenantId("000000");
        existedNotice.setTitle("审批通知");
        existedNotice.setDeliveryChannel("WECOM");
        existedNotice.setDeliveryStatus("0");
        existedNotice.setStatus("0");
        existedNotice.setCreateTime(new Date());
        when(noticeMapper.selectById(21L)).thenReturn(existedNotice);
        when(noticeMapper.updateById(any(SysNotice.class))).thenReturn(1);

        SysNotice updateBody = new SysNotice();
        updateBody.setNoticeId(21L);
        updateBody.setDeliveryStatus("2");

        boolean success = noticeService.updateNotice(updateBody);

        Assertions.assertTrue(success);
        ArgumentCaptor<SysNotice> captor = ArgumentCaptor.forClass(SysNotice.class);
        verify(noticeMapper).updateById(captor.capture());
        Assertions.assertEquals("2", captor.getValue().getDeliveryStatus());
        Assertions.assertNotNull(captor.getValue().getDeliveryTime());
    }
}
