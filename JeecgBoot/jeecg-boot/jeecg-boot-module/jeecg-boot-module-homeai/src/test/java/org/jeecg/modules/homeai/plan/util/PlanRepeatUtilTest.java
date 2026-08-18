package org.jeecg.modules.homeai.plan.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 重复计划日期匹配工具纯逻辑测试
 *
 * <p>覆盖 PlanRepeatUtil.isRepeatMaster 与 matchesDate 的
 * none/daily/weekly/monthly 规则及未知规则兜底行为。
 * 无 Spring 上下文、无 Mockito。</p>
 */
class PlanRepeatUtilTest {

    /** 锚点日期：2026-08-18（周二） */
    private static final LocalDate ANCHOR = LocalDate.of(2026, 8, 18);

    // ---------- isRepeatMaster ----------

    @Test
    void nonRepeatRulesRejected() {
        // null / 空串 / none（大小写不敏感）→ 非重复主计划
        assertFalse(PlanRepeatUtil.isRepeatMaster(null));
        assertFalse(PlanRepeatUtil.isRepeatMaster(""));
        assertFalse(PlanRepeatUtil.isRepeatMaster("none"));
        assertFalse(PlanRepeatUtil.isRepeatMaster("NONE"));
    }

    @Test
    void repeatRulesAccepted() {
        // daily/weekly/monthly → 重复主计划；带首尾空白也接受
        assertTrue(PlanRepeatUtil.isRepeatMaster("daily"));
        assertTrue(PlanRepeatUtil.isRepeatMaster("weekly"));
        assertTrue(PlanRepeatUtil.isRepeatMaster("monthly"));
        assertTrue(PlanRepeatUtil.isRepeatMaster(" daily "));
    }

    // ---------- matchesDate ----------

    @Test
    void nullAnchorOrDateRejected() {
        // anchor 或 date 为 null → false
        assertFalse(PlanRepeatUtil.matchesDate("daily", null, LocalDate.of(2026, 8, 19)));
        assertFalse(PlanRepeatUtil.matchesDate("daily", ANCHOR, null));
    }

    @Test
    void dateBeforeAnchorRejected() {
        // date 早于 anchor → false
        assertFalse(PlanRepeatUtil.matchesDate("daily", ANCHOR, LocalDate.of(2026, 8, 17)));
    }

    @Test
    void noneRuleMatchesOnlyAnchorDay() {
        // none：仅 anchor 当天匹配
        assertTrue(PlanRepeatUtil.matchesDate("none", ANCHOR, ANCHOR));
        assertFalse(PlanRepeatUtil.matchesDate("none", ANCHOR, LocalDate.of(2026, 8, 19)));
    }

    @Test
    void dailyRuleMatchesAnyDateAfterAnchor() {
        // daily：anchor 之后任意日期匹配
        assertTrue(PlanRepeatUtil.matchesDate("daily", ANCHOR, LocalDate.of(2026, 8, 19)));
        assertTrue(PlanRepeatUtil.matchesDate("daily", ANCHOR, LocalDate.of(2026, 9, 1)));
    }

    @Test
    void weeklyRuleMatchesSameWeekday() {
        // weekly：同星期几匹配（2026-08-18 周二 → 2026-08-25 周二）
        assertTrue(PlanRepeatUtil.matchesDate("weekly", ANCHOR, LocalDate.of(2026, 8, 25)));
        // 不同星期几不匹配（2026-08-19 周三）
        assertFalse(PlanRepeatUtil.matchesDate("weekly", ANCHOR, LocalDate.of(2026, 8, 19)));
    }

    @Test
    void monthlyRuleMatchesSameDayOfMonth() {
        // monthly：同日匹配（08-18 → 09-18）
        assertTrue(PlanRepeatUtil.matchesDate("monthly", ANCHOR, LocalDate.of(2026, 9, 18)));
        // 不同日不匹配（09-19）
        assertFalse(PlanRepeatUtil.matchesDate("monthly", ANCHOR, LocalDate.of(2026, 9, 19)));
    }

    @Test
    void unknownRuleFallsBackToAnchorDay() {
        // 未知规则 yearly：仅 anchor 当天匹配，其他日期不匹配
        assertTrue(PlanRepeatUtil.matchesDate("yearly", ANCHOR, ANCHOR));
        assertFalse(PlanRepeatUtil.matchesDate("yearly", ANCHOR, LocalDate.of(2026, 8, 19)));
    }
}