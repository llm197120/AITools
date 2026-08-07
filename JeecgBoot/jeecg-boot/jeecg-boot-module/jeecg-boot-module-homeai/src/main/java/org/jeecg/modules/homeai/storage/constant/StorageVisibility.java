package org.jeecg.modules.homeai.storage.constant;

/**
 * 资料存储可见性级别
 */
public final class StorageVisibility {

    public static final String PRIVATE = "private";
    public static final String FAMILY = "family";
    public static final String PUBLIC = "public";

    private StorageVisibility() {
    }

    public static boolean isValid(String visibility) {
        return PRIVATE.equals(visibility) || FAMILY.equals(visibility) || PUBLIC.equals(visibility);
    }
}
