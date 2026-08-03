package org.jeecg.modules.homeai.ai.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.jeecg.modules.homeai.ai.entity.AiUserQuota;
import org.jeecg.modules.homeai.ai.vo.AiQuotaUsageVO;

import java.util.List;
import java.util.Map;

/**
 * Token额度 Service
 */
public interface IAiQuotaService {

    /**
     * 检查用户是否有足够的Token额度
     * @param userId 用户ID
     * @param estimatedInputTokens 预估输入Token
     * @param estimatedOutputTokens 预估输出Token
     * @return 检查结果 { allowed: boolean, message: string, remainingDaily: int, remainingMonthly: int }
     */
    Map<String, Object> checkQuota(String userId, int estimatedInputTokens, int estimatedOutputTokens);

    /**
     * 记录Token消耗并扣减额度
     */
    void deductQuota(String userId, String conversationId, String modelName,
                     int inputTokens, int outputTokens);

    /**
     * 获取用户今日已消耗Token
     */
    int getDailyConsumed(String userId);

    /**
     * 获取用户本月已消耗Token
     */
    int getMonthlyConsumed(String userId);

    /**
     * 获取默认配额配置
     */
    Map<String, Integer> getDefaultQuota();

    //update-begin---author:admin ---date:2026-07-31  for：修复Token额度配置页未登录问题，管理端按用户分组统计Token消耗-----------
    /**
     * 分页查询用户Token消耗统计（管理端）
     * @param pageNo 页码
     * @param pageSize 每页条数
     * @param userId 可选，按用户ID筛选
     */
    IPage<AiQuotaUsageVO> getUsageStats(Integer pageNo, Integer pageSize, String userId);
    //update-end---author:admin ---date:2026-07-31  for：修复Token额度配置页未登录问题，管理端按用户分组统计Token消耗-----------

    /**
     * 获取用户额度配置（不存在则按默认值创建）
     */
    AiUserQuota getOrCreateUserQuota(String userId);

    /**
     * 更新用户额度配置
     */
    boolean updateUserQuota(String userId, Integer dailyLimit, Integer monthlyLimit, String effectiveEnd);

    /**
     * 管理端：用户额度+消耗列表（合并用户信息）
     */
    IPage<Map<String, Object>> getUserQuotaPage(Integer pageNo, Integer pageSize, String userId);

    /**
     * 管理端：额度使用概览（总消耗、活跃用户数、各模型占比）
     */
    Map<String, Object> getQuotaOverview();
}
