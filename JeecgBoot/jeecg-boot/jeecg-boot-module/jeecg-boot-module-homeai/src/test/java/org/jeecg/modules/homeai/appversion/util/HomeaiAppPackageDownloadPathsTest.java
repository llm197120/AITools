package org.jeecg.modules.homeai.appversion.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HomeaiAppPackageDownloadPathsTest {

    @Test
    void detectsProxyDownloadUrl() {
        assertTrue(HomeaiAppPackageDownloadPaths.isProxyUrl(
                "https://example.com/jeecg-boot/homeai/app/version/package/download"));
        assertTrue(HomeaiAppPackageDownloadPaths.isProxyUrl(
                "https://example.com/jeecg-boot" + HomeaiAppPackageDownloadPaths.RESOURCE));
        assertFalse(HomeaiAppPackageDownloadPaths.isProxyUrl("https://oss.aliyuncs.com/homeai/app-version/resource-1.zip"));
        assertFalse(HomeaiAppPackageDownloadPaths.isProxyUrl(null));
        assertTrue(HomeaiAppPackageDownloadPaths.isResourceKind("resource"));
        assertFalse(HomeaiAppPackageDownloadPaths.isResourceKind("apk"));
    }
}
