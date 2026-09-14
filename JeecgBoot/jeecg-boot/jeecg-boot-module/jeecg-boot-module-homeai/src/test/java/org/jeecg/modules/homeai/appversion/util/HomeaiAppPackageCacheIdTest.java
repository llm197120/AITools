package org.jeecg.modules.homeai.appversion.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HomeaiAppPackageCacheIdTest {

    @Test
    void sameShaMeansUnchangedPackage() {
        String a = HomeaiAppPackageCacheId.of("ABCDEF12" + "aa".repeat(28), "oss:old-key.apk");
        String b = HomeaiAppPackageCacheId.of("abcdef12" + "aa".repeat(28), "oss:new-key.apk");
        assertEquals(a, b);
        assertEquals("pkg-" + a + ".apk", HomeaiAppPackageCacheId.fileName(a, "apk"));
    }

    @Test
    void differentShaMeansUpdatedPackage() {
        String a = HomeaiAppPackageCacheId.of("a".repeat(64), "oss:same.apk");
        String b = HomeaiAppPackageCacheId.of("b".repeat(64), "oss:same.apk");
        assertNotEquals(a, b);
    }

    @Test
    void missingShaFallsBackToStoredReference() {
        String a = HomeaiAppPackageCacheId.of(null, "oss:homeai/app-version/apk-1.apk");
        String b = HomeaiAppPackageCacheId.of("", "oss:homeai/app-version/apk-1.apk");
        String c = HomeaiAppPackageCacheId.of(null, "oss:homeai/app-version/apk-2.apk");
        assertEquals(a, b);
        assertNotEquals(a, c);
        assertTrue(a.startsWith("ref-"));
    }
}
