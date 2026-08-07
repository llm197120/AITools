package org.jeecg.modules.homeai.plan.util;

import org.jeecg.common.util.oConvertUtils;

import java.time.LocalDate;

/**
 * 重复计划日期匹配工具
 * repeatRule: none / daily / weekly / monthly
 */
public final class PlanRepeatUtil {

    public static final int DEFAULT_HORIZON_DAYS = 90;

    private PlanRepeatUtil() {
    }

    public static boolean isRepeatMaster(String repeatRule) {
        return oConvertUtils.isNotEmpty(repeatRule) && !"none".equalsIgnoreCase(repeatRule.trim());
    }

    public static boolean matchesDate(String repeatRule, LocalDate anchor, LocalDate date) {
        if (anchor == null || date == null) {
            return false;
        }
        if (date.isBefore(anchor)) {
            return false;
        }
        if (!isRepeatMaster(repeatRule)) {
            return date.equals(anchor);
        }
        switch (repeatRule.trim().toLowerCase()) {
            case "daily":
                return true;
            case "weekly":
                return date.getDayOfWeek() == anchor.getDayOfWeek();
            case "monthly":
                return date.getDayOfMonth() == anchor.getDayOfMonth();
            default:
                return date.equals(anchor);
        }
    }
}
