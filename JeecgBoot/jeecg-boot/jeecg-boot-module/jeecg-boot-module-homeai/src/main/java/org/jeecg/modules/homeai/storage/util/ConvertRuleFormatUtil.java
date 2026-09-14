package org.jeecg.modules.homeai.storage.util;

/**
 * 转换规则源/目标格式规范化。
 */
public final class ConvertRuleFormatUtil {

    private ConvertRuleFormatUtil() {
    }

    public static String normalize(String format) {
        if (format == null) {
            return "";
        }
        String s = format.trim().toLowerCase();
        if (s.startsWith(".")) {
            s = s.substring(1);
        }
        return s;
    }

    public static boolean samePair(String source, String target, String ruleSource, String ruleTarget) {
        return normalize(source).equals(normalize(ruleSource))
                && normalize(target).equals(normalize(ruleTarget));
    }
}
