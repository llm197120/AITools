package org.jeecg.modules.homeai.storage.util;

import org.jeecg.common.util.oConvertUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 拼 kkFileView 在线预览地址：{@code /onlinePreview?url=} + Base64(源文件 URL)。
 */
public final class HomeaiKkFileViewUrl {

    private HomeaiKkFileViewUrl() {
    }

    public static String build(String kkBaseUrl, String fileUrl, String fileName) {
        if (oConvertUtils.isEmpty(kkBaseUrl) || oConvertUtils.isEmpty(fileUrl)) {
            return "";
        }
        String base = kkBaseUrl.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (!base.startsWith("http://") && !base.startsWith("https://")) {
            return "";
        }
        if (!HomeaiLocalHttpUrl.isLoopbackOrPrivate(base)) {
            return "";
        }
        String src = fileUrl.trim();
        if (oConvertUtils.isNotEmpty(fileName)) {
            String sep = src.contains("?") ? "&" : "?";
            src = src + sep + "fullfilename=" + URLEncoder.encode(fileName.trim(), StandardCharsets.UTF_8);
        }
        String b64 = Base64.getEncoder().encodeToString(src.getBytes(StandardCharsets.UTF_8));
        return base + "/onlinePreview?url=" + URLEncoder.encode(b64, StandardCharsets.UTF_8);
    }
}
