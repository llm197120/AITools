package org.jeecg.modules.homeai.ai.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * AI Token 用量估算工具纯逻辑测试
 *
 * <p>覆盖 HomeaiAiQuotaEstimateUtil 输入/输出 token 估算公式：
 * 输入 = (80 + trim 后长度) / 2，输出 = max(500, trim 后长度)。
 * 无 Spring 上下文、无 Mockito。</p>
 */
class HomeaiAiQuotaEstimateUtilTest {

    @Test
    void nullInputTokens() {
        // null → 按 0 长度计算：(80+0)/2 = 40
        assertEquals(40, HomeaiAiQuotaEstimateUtil.estimateInputTokens(null));
    }

    @Test
    void emptyInputTokens() {
        // 空串 → 40
        assertEquals(40, HomeaiAiQuotaEstimateUtil.estimateInputTokens(""));
    }

    @Test
    void shortInputTokens() {
        // "hello" 5 字符 → (80+5)/2 = 42（整数除法）
        assertEquals(42, HomeaiAiQuotaEstimateUtil.estimateInputTokens("hello"));
    }

    @Test
    void inputTokensTrimmedBeforeCount() {
        // 首尾空白先 trim 再计数 → 仍为 42
        assertEquals(42, HomeaiAiQuotaEstimateUtil.estimateInputTokens("  hello  "));
    }

    @Test
    void nullOutputTokens() {
        // null → 兜底 500
        assertEquals(500, HomeaiAiQuotaEstimateUtil.estimateOutputTokens(null));
    }

    @Test
    void emptyOutputTokens() {
        // 空串 → 500
        assertEquals(500, HomeaiAiQuotaEstimateUtil.estimateOutputTokens(""));
    }

    @Test
    void shortOutputTokensFloorAt500() {
        // 长度不足 500 → 取 max(500, 5) = 500
        assertEquals(500, HomeaiAiQuotaEstimateUtil.estimateOutputTokens("short"));
    }

    @Test
    void longOutputTokensUseActualLength() {
        // 600 个字符 → 600
        assertEquals(600, HomeaiAiQuotaEstimateUtil.estimateOutputTokens("x".repeat(600)));
    }
}