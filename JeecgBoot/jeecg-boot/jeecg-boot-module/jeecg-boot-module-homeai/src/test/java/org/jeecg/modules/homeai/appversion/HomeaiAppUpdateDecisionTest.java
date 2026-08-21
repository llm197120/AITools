package org.jeecg.modules.homeai.appversion;

import org.jeecg.modules.homeai.appversion.util.HomeaiAppUpdateDecision;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.jeecg.modules.homeai.appversion.util.HomeaiAppUpdateDecision.Action;

class HomeaiAppUpdateDecisionTest {

    @Test
    void disabledOrNotNewerSkips() {
        assertEquals(Action.NONE, HomeaiAppUpdateDecision.resolve(false, 101, 100, 100, "apk", 100));
        assertEquals(Action.NONE, HomeaiAppUpdateDecision.resolve(true, 100, 100, 100, "apk", 100));
        assertEquals(Action.NONE, HomeaiAppUpdateDecision.resolve(true, 99, 100, 100, "apk", 100));
    }

    @Test
    void apkModeAlwaysApkWhenNewer() {
        assertEquals(Action.APK, HomeaiAppUpdateDecision.resolve(true, 101, 100, 100, "apk", 100));
        assertEquals(Action.APK, HomeaiAppUpdateDecision.resolve(true, 102, 101, 100, "apk", 100));
    }

    @Test
    void resourceWhenShellNewEnough() {
        assertEquals(Action.RESOURCE, HomeaiAppUpdateDecision.resolve(true, 101, 100, 100, "resource", 100));
        assertEquals(Action.APK, HomeaiAppUpdateDecision.resolve(true, 101, 100, 99, "resource", 100));
    }
}
