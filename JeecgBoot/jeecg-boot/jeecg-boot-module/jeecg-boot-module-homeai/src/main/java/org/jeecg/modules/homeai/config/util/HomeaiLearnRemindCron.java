package org.jeecg.modules.homeai.config.util;

import org.jeecg.common.util.oConvertUtils;
import org.springframework.scheduling.support.CronExpression;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 学习提醒 cron：保存前校验；定时任务按分钟对齐触发。
 */
public final class HomeaiLearnRemindCron {

    private HomeaiLearnRemindCron() {
    }

    public static boolean isValid(String cron) {
        if (oConvertUtils.isEmpty(cron)) {
            return false;
        }
        try {
            CronExpression.parse(cron.trim());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean matchesMinute(String cron, LocalDateTime now) {
        if (!isValid(cron) || now == null) {
            return false;
        }
        LocalDateTime truncated = now.truncatedTo(ChronoUnit.MINUTES);
        LocalDateTime next = CronExpression.parse(cron.trim()).next(truncated.minusSeconds(1));
        return next != null && next.truncatedTo(ChronoUnit.MINUTES).equals(truncated);
    }
}
