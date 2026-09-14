package org.jeecg.modules.homeai.ai.util;

/**
 * 对话列表标题：未提问前用占位名，首条用户问题改成摘要以便区分。
 */
public final class HomeaiConversationTitle {

    public static final String PLACEHOLDER = "新对话";
    private static final int MAX_LEN = 24;

    private HomeaiConversationTitle() {
    }

    public static boolean isPlaceholder(String title) {
        if (title == null) {
            return true;
        }
        String t = title.trim();
        return t.isEmpty() || PLACEHOLDER.equals(t);
    }

    /** 由用户首条消息生成列表/导航标题；图片或附件占位不改名。 */
    public static String fromUserMessage(String content) {
        if (content == null) {
            return PLACEHOLDER;
        }
        String clean = content.replaceAll("[\\n\\r]+", " ").trim();
        if (clean.isEmpty() || "[图片]".equals(clean) || "[附件]".equals(clean)) {
            return PLACEHOLDER;
        }
        if (clean.length() > MAX_LEN) {
            return clean.substring(0, MAX_LEN) + "…";
        }
        return clean;
    }
}
