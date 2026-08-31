package org.jeecg.modules.homeai.ai.util;

import org.jeecg.modules.homeai.ai.entity.AiUserQuota;

import java.util.Date;

/**
 * Token 限额解析：过期个性化配额回落到默认值。
 */
public final class AiQuotaLimitUtil {

    private AiQuotaLimitUtil() {
    }

    public static boolean isCustomQuotaExpired(AiUserQuota quota, Date now) {
        if (quota == null || quota.getEffectiveEnd() == null || now == null) {
            return false;
        }
        return !quota.getEffectiveEnd().after(now);
    }

    public static int resolveDailyLimit(AiUserQuota quota, int defaultDaily, Date now) {
        if (quota == null || isCustomQuotaExpired(quota, now)) {
            return defaultDaily;
        }
        return quota.getDailyLimit() != null ? quota.getDailyLimit() : defaultDaily;
    }

    public static int resolveMonthlyLimit(AiUserQuota quota, int defaultMonthly, Date now) {
        if (quota == null || isCustomQuotaExpired(quota, now)) {
            return defaultMonthly;
        }
        return quota.getMonthlyLimit() != null ? quota.getMonthlyLimit() : defaultMonthly;
    }

    public static int toConsumed(Object cached) {
        if (cached instanceof Number number) {
            return number.intValue();
        }
        return 0;
    }
}
