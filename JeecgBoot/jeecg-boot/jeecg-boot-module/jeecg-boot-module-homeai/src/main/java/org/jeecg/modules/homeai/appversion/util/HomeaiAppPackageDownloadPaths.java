package org.jeecg.modules.homeai.appversion.util;

import org.jeecg.common.util.oConvertUtils;

/**
 * APP 安装包 / 热更新 zip 的后端代理下载路径。
 * OSS 默认域名禁止预签名直链分发部分类型；客户端统一打这个接口。
 */
public final class HomeaiAppPackageDownloadPaths {

    public static final String APK = "/homeai/app/version/package/download";
    public static final String RESOURCE = "/homeai/app/version/package/download?kind=resource";

    private HomeaiAppPackageDownloadPaths() {
    }

    public static boolean isProxyUrl(String url) {
        return oConvertUtils.isNotEmpty(url) && url.contains("/homeai/app/version/package/download");
    }

    public static boolean isResourceKind(String kind) {
        return "resource".equalsIgnoreCase(kind);
    }
}
