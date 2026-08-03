package org.jeecg.modules.homeai.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.jeecg.modules.homeai.ai.entity.AiQuotaLog;
import org.jeecg.modules.homeai.ai.vo.AiQuotaUsageVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;

/**
 * Token额度日志 Mapper
 */
public interface AiQuotaLogMapper extends BaseMapper<AiQuotaLog> {

    /**
     * 统计指定时间范围内的总 Token 消耗
     */
    @Select("SELECT COALESCE(SUM(total_tokens), 0) FROM homeai_ai_quota_log " +
            "WHERE user_id = #{userId} AND create_time BETWEEN #{start} AND #{end}")
    Integer selectTotalTokens(@Param("userId") String userId,
                              @Param("start") Date start,
                              @Param("end") Date end);

    //update-begin---author:admin ---date:2026-07-31  for：修复Token额度配置页未登录问题，管理端按用户分组统计Token消耗-----------
    /**
     * 按用户分组统计今日/本月Token消耗（管理端分页查询）
     */
    @Select("SELECT l.user_id AS userId, u.nickname AS nickname, u.phone AS phone, " +
            "COALESCE(SUM(CASE WHEN l.create_time >= #{todayStart} THEN l.total_tokens END), 0) AS dailyUsage, " +
            "COALESCE(SUM(CASE WHEN l.create_time >= #{monthStart} THEN l.total_tokens END), 0) AS monthlyUsage, " +
            "MAX(l.create_time) AS lastActiveTime " +
            "FROM homeai_ai_quota_log l " +
            "LEFT JOIN homeai_wx_user u ON u.id = l.user_id " +
            "WHERE (#{userId} IS NULL OR l.user_id = #{userId}) " +
            "GROUP BY l.user_id, u.nickname, u.phone " +
            "ORDER BY monthlyUsage DESC")
    IPage<AiQuotaUsageVO> selectUsageStats(Page<AiQuotaUsageVO> page,
                                           @Param("userId") String userId,
                                           @Param("todayStart") Date todayStart,
                                           @Param("monthStart") Date monthStart);
    //update-end---author:admin ---date:2026-07-31  for：修复Token额度配置页未登录问题，管理端按用户分组统计Token消耗-----------
}
