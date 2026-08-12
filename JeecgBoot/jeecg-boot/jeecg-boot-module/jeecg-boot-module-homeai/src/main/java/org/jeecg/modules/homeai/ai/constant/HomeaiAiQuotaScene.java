package org.jeecg.modules.homeai.ai.constant;

/**
 * AI Token 配额场景标识（与 deductQuota 的 conversationId/scene 字段对齐）
 */
public final class HomeaiAiQuotaScene {

    public static final String CHAT = "chat";
    public static final String OFFICE_GENERATE = "storage:ai_generate";
    /** 预留：菜谱 AI 生成尚未落地 */
    public static final String RECIPE_GENERATE = "recipe:ai_generate";

    private HomeaiAiQuotaScene() {
    }

    public static String normalize(String scene) {
        if (scene == null) {
            return CHAT;
        }
        String s = scene.trim().toLowerCase();
        if ("office".equals(s) || "office_generate".equals(s) || "storage:ai_generate".equals(s)) {
            return OFFICE_GENERATE;
        }
        if ("recipe".equals(s) || "recipe_generate".equals(s) || "recipe:ai_generate".equals(s)) {
            return RECIPE_GENERATE;
        }
        return CHAT;
    }
}
