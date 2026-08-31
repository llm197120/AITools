package org.jeecg.modules.homeai.ai.util;

import org.jeecg.modules.homeai.ai.entity.AiUserQuota;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 个性化 Token 限额：过期回落默认；Redis 计数按 Number 读取。
 */
class AiQuotaLimitUtilTest {

    @Test
    void useCustomWhenNotExpired() {
        AiUserQuota quota = new AiUserQuota();
        quota.setDailyLimit(3000);
        quota.setMonthlyLimit(50000);
        quota.setEffectiveEnd(new Date(System.currentTimeMillis() + 86_400_000L));
        Date now = new Date();
        assertEquals(3000, AiQuotaLimitUtil.resolveDailyLimit(quota, 10000, now));
        assertEquals(50000, AiQuotaLimitUtil.resolveMonthlyLimit(quota, 200000, now));
        assertFalse(AiQuotaLimitUtil.isCustomQuotaExpired(quota, now));
    }

    @Test
    void fallbackWhenExpired() {
        AiUserQuota quota = new AiUserQuota();
        quota.setDailyLimit(3000);
        quota.setMonthlyLimit(50000);
        quota.setEffectiveEnd(new Date(System.currentTimeMillis() - 1000L));
        Date now = new Date();
        assertTrue(AiQuotaLimitUtil.isCustomQuotaExpired(quota, now));
        assertEquals(10000, AiQuotaLimitUtil.resolveDailyLimit(quota, 10000, now));
        assertEquals(200000, AiQuotaLimitUtil.resolveMonthlyLimit(quota, 200000, now));
    }

    @Test
    void defaultWhenQuotaMissing() {
        Date now = new Date();
        assertEquals(10000, AiQuotaLimitUtil.resolveDailyLimit(null, 10000, now));
        assertEquals(200000, AiQuotaLimitUtil.resolveMonthlyLimit(null, 200000, now));
    }

    @Test
    void consumedAcceptsLongFromRedisIncr() {
        assertEquals(1200, AiQuotaLimitUtil.toConsumed(1200L));
        assertEquals(80, AiQuotaLimitUtil.toConsumed(80));
        assertEquals(0, AiQuotaLimitUtil.toConsumed(null));
    }
}
