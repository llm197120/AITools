package org.jeecg.modules.homeai.ai.util;

/**
 * AI Token 用量估算（与 LLM 调用逻辑保持一致）
 */
public final class HomeaiAiQuotaEstimateUtil {

    private static final int SYSTEM_PROMPT_LEN = 80;

    private HomeaiAiQuotaEstimateUtil() {
    }

    public static int estimateInputTokens(String instruction) {
        int textLen = instruction != null ? instruction.trim().length() : 0;
        return (SYSTEM_PROMPT_LEN + textLen) / 2;
    }

    public static int estimateOutputTokens(String instruction) {
        int textLen = instruction != null ? instruction.trim().length() : 0;
        return Math.max(500, textLen);
    }
}
