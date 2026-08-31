package org.jeecg.modules.homeai.ai.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.modules.homeai.ai.entity.AiUserQuota;
import org.jeecg.modules.homeai.ai.entity.AiQuotaLog;
import org.jeecg.modules.homeai.ai.mapper.AiUserQuotaMapper;
import org.jeecg.modules.homeai.ai.mapper.AiQuotaLogMapper;
import org.jeecg.modules.homeai.ai.service.IAiQuotaService;
import org.jeecg.modules.homeai.ai.util.AiQuotaLimitUtil;
import org.jeecg.modules.homeai.ai.vo.AiQuotaUsageVO;
import org.jeecg.modules.homeai.user.entity.WxUser;
import org.jeecg.modules.homeai.user.service.IWxUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Token额度控制实现
 * 用 Redis 做原子计数，DB 做持久化记录
 */
@Slf4j
@Service
public class AiQuotaServiceImpl implements IAiQuotaService {

    /** Redis 中今日消耗 key 前缀 */
    private static final String QUOTA_DAILY_KEY = "homeai:quota:daily:";
    /** Redis 中本月消耗 key 前缀 */
    private static final String QUOTA_MONTHLY_KEY = "homeai:quota:monthly:";
    /** 默认日限额 */
    private static final int DEFAULT_DAILY_LIMIT = 10000;
    /** 默认月限额 */
    private static final int DEFAULT_MONTHLY_LIMIT = 200000;

    @Autowired
    private AiQuotaLogMapper quotaLogMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private AiUserQuotaMapper userQuotaMapper;

    @Autowired
    private IWxUserService wxUserService;

    @Override
    public Map<String, Object> checkQuota(String userId, int estimatedInputTokens, int estimatedOutputTokens) {
        AiUserQuota quota = getOrCreateUserQuota(userId);
        Date now = new Date();
        //update-begin---author:cursor---date:2026-08-22---for:【审查B】预检用真实限额，取消日限额硬编码透支-----------
        int dailyLimit = AiQuotaLimitUtil.resolveDailyLimit(quota, DEFAULT_DAILY_LIMIT, now);
        int monthlyLimit = AiQuotaLimitUtil.resolveMonthlyLimit(quota, DEFAULT_MONTHLY_LIMIT, now);
        //update-end---author:cursor---date:2026-08-22---for:【审查B】预检用真实限额，取消日限额硬编码透支-----------
        int dailyConsumed = getDailyConsumed(userId);
        int monthlyConsumed = getMonthlyConsumed(userId);
        int estimatedTotal = estimatedInputTokens + estimatedOutputTokens;

        Map<String, Object> result = new HashMap<>();
        result.put("remainingDaily", Math.max(0, dailyLimit - dailyConsumed));
        result.put("remainingMonthly", Math.max(0, monthlyLimit - monthlyConsumed));

        // 预检：预估消耗 + 已消耗 是否超过限额
        if (dailyConsumed + estimatedTotal > dailyLimit) {
            result.put("allowed", false);
            result.put("message", "今日Token额度即将用完，请缩短消息或等待额度重置");
            return result;
        }
        if (monthlyConsumed + estimatedTotal > monthlyLimit) {
            result.put("allowed", false);
            result.put("message", "本月Token额度已用完，下月自动重置");
            return result;
        }

        result.put("allowed", true);
        result.put("message", "OK");
        return result;
    }

    @Override
    public void deductQuota(String userId, String conversationId, String modelName,
                            int inputTokens, int outputTokens) {
        int total = inputTokens + outputTokens;
        if (total <= 0) {
            return;
        }

        AiUserQuota quota = getOrCreateUserQuota(userId);
        Date now = new Date();
        int dailyLimit = AiQuotaLimitUtil.resolveDailyLimit(quota, DEFAULT_DAILY_LIMIT, now);
        int monthlyLimit = AiQuotaLimitUtil.resolveMonthlyLimit(quota, DEFAULT_MONTHLY_LIMIT, now);

        // Redis 原子计数
        String dailyKey = QUOTA_DAILY_KEY + userId;
        String monthlyKey = QUOTA_MONTHLY_KEY + userId;

        // 设置过期时间（今日剩余秒数 / 本月剩余秒数）
        LocalDateTime localNow = LocalDateTime.now();
        long dailyExpire = LocalDateTime.of(localNow.toLocalDate(), LocalTime.MAX).atZone(ZoneId.systemDefault()).toEpochSecond()
                - localNow.atZone(ZoneId.systemDefault()).toEpochSecond();
        long monthlyExpire = LocalDateTime.of(localNow.getYear(), localNow.getMonth(), 1, 0, 0)
                .plusMonths(1).atZone(ZoneId.systemDefault()).toEpochSecond()
                - localNow.atZone(ZoneId.systemDefault()).toEpochSecond();

        //update-begin---author:cursor---date:2026-08-22---for:【审查B】配额原子扣减后二次比较，超额回滚-----------
        long afterDaily = redisUtil.incr(dailyKey, total);
        redisUtil.expire(dailyKey, dailyExpire);
        if (afterDaily > dailyLimit) {
            redisUtil.decr(dailyKey, total);
            throw new JeecgBootException("今日Token额度已用完，请缩短消息或等待额度重置");
        }
        long afterMonthly = redisUtil.incr(monthlyKey, total);
        redisUtil.expire(monthlyKey, monthlyExpire);
        if (afterMonthly > monthlyLimit) {
            redisUtil.decr(monthlyKey, total);
            redisUtil.decr(dailyKey, total);
            throw new JeecgBootException("本月Token额度已用完，下月自动重置");
        }
        //update-end---author:cursor---date:2026-08-22---for:【审查B】配额原子扣减后二次比较，超额回滚-----------

        // DB 持久化记录
        AiQuotaLog logRecord = new AiQuotaLog();
        logRecord.setUserId(userId);
        logRecord.setConversationId(conversationId);
        logRecord.setModelName(modelName);
        logRecord.setInputTokens(inputTokens);
        logRecord.setOutputTokens(outputTokens);
        logRecord.setTotalTokens(total);
        logRecord.setCostType("daily");
        logRecord.setCreateTime(new Date());
        quotaLogMapper.insert(logRecord);
    }

    @Override
    public int getDailyConsumed(String userId) {
        Object cached = redisUtil.get(QUOTA_DAILY_KEY + userId);
        if (cached instanceof Number) {
            return AiQuotaLimitUtil.toConsumed(cached);
        }
        // 从 DB 计算今日消耗并回填 Redis
        LocalDate today = LocalDate.now();
        Date start = Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date end = Date.from(today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());

        Integer total = getDailySumFromDb(userId, start, end);
        if (total > 0) {
            long dailyExpire = LocalDateTime.of(today, LocalTime.MAX).atZone(ZoneId.systemDefault()).toEpochSecond()
                    - LocalDateTime.now().atZone(ZoneId.systemDefault()).toEpochSecond();
            redisUtil.set(QUOTA_DAILY_KEY + userId, total, dailyExpire);
        }
        return total != null ? total : 0;
    }

    @Override
    public int getMonthlyConsumed(String userId) {
        Object cached = redisUtil.get(QUOTA_MONTHLY_KEY + userId);
        if (cached instanceof Number) {
            return AiQuotaLimitUtil.toConsumed(cached);
        }
        LocalDate now = LocalDate.now();
        Date start = Date.from(now.withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date end = Date.from(now.plusMonths(1).withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant());

        Integer total = getDailySumFromDb(userId, start, end);
        if (total > 0) {
            long monthlyExpire = LocalDateTime.of(now.getYear(), now.getMonth(), 1, 0, 0)
                    .plusMonths(1).atZone(ZoneId.systemDefault()).toEpochSecond()
                    - LocalDateTime.now().atZone(ZoneId.systemDefault()).toEpochSecond();
            redisUtil.set(QUOTA_MONTHLY_KEY + userId, total, monthlyExpire);
        }
        return total != null ? total : 0;
    }

    @Override
    public Map<String, Integer> getDefaultQuota() {
        Map<String, Integer> quota = new HashMap<>();
        quota.put("dailyLimit", DEFAULT_DAILY_LIMIT);
        quota.put("monthlyLimit", DEFAULT_MONTHLY_LIMIT);
        return quota;
    }

    //update-begin---author:admin ---date:2026-07-31  for：修复Token额度配置页未登录问题，管理端按用户分组统计Token消耗-----------
    @Override
    public IPage<AiQuotaUsageVO> getUsageStats(Integer pageNo, Integer pageSize, String userId) {
        Page<AiQuotaUsageVO> page = new Page<>(pageNo, pageSize);
        LocalDate today = LocalDate.now();
        Date todayStart = Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date monthStart = Date.from(today.withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        return quotaLogMapper.selectUsageStats(page, userId, todayStart, monthStart);
    }
    //update-end---author:admin ---date:2026-07-31  for：修复Token额度配置页未登录问题，管理端按用户分组统计Token消耗-----------

    @Override
    public AiUserQuota getOrCreateUserQuota(String userId) {
        LambdaQueryWrapper<AiUserQuota> q = new LambdaQueryWrapper<>();
        q.eq(AiUserQuota::getUserId, userId).last("LIMIT 1");
        AiUserQuota quota = userQuotaMapper.selectOne(q);
        if (quota == null) {
            quota = new AiUserQuota();
            quota.setUserId(userId);
            quota.setDailyLimit(DEFAULT_DAILY_LIMIT);
            quota.setMonthlyLimit(DEFAULT_MONTHLY_LIMIT);
            quota.setCreateTime(new Date());
            userQuotaMapper.insert(quota);
        }
        return quota;
    }

    @Override
    public boolean updateUserQuota(String userId, Integer dailyLimit, Integer monthlyLimit, String effectiveEnd) {
        AiUserQuota quota = getOrCreateUserQuota(userId);
        if (dailyLimit != null) quota.setDailyLimit(dailyLimit);
        if (monthlyLimit != null) quota.setMonthlyLimit(monthlyLimit);
        if (effectiveEnd != null && !effectiveEnd.isEmpty()) {
            try {
                quota.setEffectiveEnd(java.sql.Timestamp.valueOf(effectiveEnd.replace("T", " ")));
            } catch (Exception e) {
                log.warn("有效期格式非法: {}", effectiveEnd);
            }
        }
        quota.setUpdateTime(new Date());
        return userQuotaMapper.updateById(quota) > 0;
    }

    @Override
    public IPage<Map<String, Object>> getUserQuotaPage(Integer pageNo, Integer pageSize, String userId) {
        IPage<AiQuotaUsageVO> usagePage = getUsageStats(pageNo, pageSize, userId);
        IPage<Map<String, Object>> result = new Page<>(pageNo, pageSize, usagePage.getTotal());
        List<Map<String, Object>> rows = new java.util.ArrayList<>();
        for (AiQuotaUsageVO vo : usagePage.getRecords()) {
            AiUserQuota quota = getOrCreateUserQuota(vo.getUserId());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("userId", vo.getUserId());
            row.put("nickname", vo.getNickname());
            row.put("phone", vo.getPhone());
            row.put("dailyLimit", quota.getDailyLimit() != null ? quota.getDailyLimit() : DEFAULT_DAILY_LIMIT);
            row.put("monthlyLimit", quota.getMonthlyLimit() != null ? quota.getMonthlyLimit() : DEFAULT_MONTHLY_LIMIT);
            row.put("dailyUsage", vo.getDailyUsage());
            row.put("monthlyUsage", vo.getMonthlyUsage());
            row.put("effectiveEnd", quota.getEffectiveEnd());
            row.put("lastActiveTime", vo.getLastActiveTime());
            rows.add(row);
        }
        result.setRecords(rows);
        return result;
    }

    @Override
    public Map<String, Object> getQuotaOverview() {
        Map<String, Object> overview = new HashMap<>();
        LocalDate today = LocalDate.now();
        Date todayStart = Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date monthStart = Date.from(today.withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        // 总消耗（今日/本月）
        int todayTotal = 0, monthTotal = 0;
        LambdaQueryWrapper<AiQuotaLog> q = new LambdaQueryWrapper<>();
        q.select(AiQuotaLog::getTotalTokens, AiQuotaLog::getCreateTime, AiQuotaLog::getModelName);
        List<AiQuotaLog> logs = quotaLogMapper.selectList(q);
        java.util.Set<String> activeUsers = new java.util.HashSet<>();
        Map<String, Integer> modelTokens = new LinkedHashMap<>();
        for (AiQuotaLog log : logs) {
            int total = log.getTotalTokens() != null ? log.getTotalTokens() : 0;
            Date t = log.getCreateTime();
            if (t == null) continue;
            if (!t.before(todayStart)) {
                todayTotal += total;
                activeUsers.add(log.getUserId());
            }
            if (!t.before(monthStart)) {
                monthTotal += total;
            }
            if (log.getModelName() != null) {
                modelTokens.merge(log.getModelName(), total, Integer::sum);
            }
        }
        overview.put("todayTotal", todayTotal);
        overview.put("monthTotal", monthTotal);
        overview.put("activeUserCount", activeUsers.size());
        overview.put("modelTokens", modelTokens);
        return overview;
    }

    /**
     * 从 DB 中统计指定时间范围内的总 Token 消耗
     */
    private Integer getDailySumFromDb(String userId, Date start, Date end) {
        // 使用 MyBatis-Plus 的 selectCount 无法聚合，这里简化用原始 SQL
        // 实际项目应使用 @Select("SELECT COALESCE(SUM(total_tokens),0) FROM homeai_ai_quota_log WHERE user_id=#{userId} AND create_time BETWEEN #{start} AND #{end}")
        // 这里使用 Mapper 的自定义方法
        // 简化版：直接返回0，后续通过 Mapper XML 实现
        return quotaLogMapper.selectTotalTokens(userId, start, end);
    }
}
