package org.jeecg.modules.homeai.appversion.util;

import java.util.Locale;

/**
 * APP 更新分流：仅当服务端 versionCode 大于本地 Web 版本才更新。
 */
public final class HomeaiAppUpdateDecision {

    public enum Action {
        NONE,
        APK,
        RESOURCE
    }

    private HomeaiAppUpdateDecision() {
    }

    public static Action resolve(
            boolean enabled,
            int serverCode,
            int localWebCode,
            int shellCode,
            String updateMode,
            int minShellCode) {
        if (!enabled || serverCode <= localWebCode) {
            return Action.NONE;
        }
        String mode = updateMode == null ? "apk" : updateMode.trim().toLowerCase(Locale.ROOT);
        if ("resource".equals(mode) && shellCode >= minShellCode) {
            return Action.RESOURCE;
        }
        return Action.APK;
    }
}
