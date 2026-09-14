package org.jeecg.modules.homeai.storage.util;

import org.jeecg.common.util.oConvertUtils;

import java.net.URI;

/**
 * Gotenberg LibreOffice 转换入口规范化。
 */
public final class HomeaiGotenbergConvert {

    private HomeaiGotenbergConvert() {
    }

    public static boolean enabled(String baseUrl) {
        String base = trimBase(baseUrl);
        return base != null && HomeaiLocalHttpUrl.isLoopbackOrPrivate(base);
    }

    public static boolean handlesTarget(String targetFormat) {
        String t = ConvertRuleFormatUtil.normalize(targetFormat);
        return "pdf".equals(t);
    }

    public static URI convertUri(String baseUrl) {
        String base = trimBase(baseUrl);
        if (base == null) {
            throw new IllegalArgumentException("Gotenberg 地址为空");
        }
        return URI.create(base + "/forms/libreoffice/convert");
    }

    static String trimBase(String baseUrl) {
        if (oConvertUtils.isEmpty(baseUrl)) {
            return null;
        }
        String base = baseUrl.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (!base.startsWith("http://") && !base.startsWith("https://")) {
            return null;
        }
        if (!HomeaiLocalHttpUrl.isLoopbackOrPrivate(base)) {
            return null;
        }
        return base;
    }
}
