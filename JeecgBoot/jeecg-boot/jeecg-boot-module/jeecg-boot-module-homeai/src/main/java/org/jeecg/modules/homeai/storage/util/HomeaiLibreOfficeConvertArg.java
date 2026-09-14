package org.jeecg.modules.homeai.storage.util;

/**
 * LibreOffice --convert-to 参数。裸扩展名 txt 对 Word 常失败，需带 Text 过滤器。
 */
public final class HomeaiLibreOfficeConvertArg {

    private HomeaiLibreOfficeConvertArg() {
    }

    public static String of(String targetFormat) {
        String t = ConvertRuleFormatUtil.normalize(targetFormat);
        if (t.isEmpty()) {
            return "pdf";
        }
        if ("txt".equals(t)) {
            return "txt:Text";
        }
        return t;
    }
}
