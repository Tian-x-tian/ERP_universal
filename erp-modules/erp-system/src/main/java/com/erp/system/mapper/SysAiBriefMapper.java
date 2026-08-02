package com.erp.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.system.domain.SysAiBrief;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.Date;

/**
 * AI 每日简报Mapper接口
 */
@Mapper
public interface SysAiBriefMapper extends BaseMapper<SysAiBrief> {

    /**
     * 抢占一条已存在的简报用于重新生成。
     *
     * <p>条件更新 + 行锁保证多实例下只有一个调用方能抢到生成权：
     * 只有「当前不是生成中」或「生成中但已陈旧（上一个生成方大概率已经挂了）」才允许抢占。</p>
     *
     * @param briefId     简报ID
     * @param staleBefore 陈旧判定时间点
     * @return 影响行数，1 表示抢占成功
     */
    @Update("UPDATE sys_ai_brief SET status = 'PENDING', update_time = NOW() "
            + "WHERE brief_id = #{briefId} "
            + "AND (status <> 'PENDING' OR update_time < #{staleBefore})")
    int claimExisting(@Param("briefId") Long briefId, @Param("staleBefore") Date staleBefore);
}
