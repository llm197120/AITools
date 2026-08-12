package org.jeecg.modules.homeai.recipe.constant;

/**
 * 菜谱可见性级别
 */
public final class RecipeVisibility {

    public static final String PRIVATE = "private";
    public static final String FAMILY = "family";
    /** 公开：所有已登录小程序用户可见（管理端运营菜谱） */
    public static final String PUBLIC = "public";

    private RecipeVisibility() {
    }

    public static boolean isValid(String visibility) {
        return PRIVATE.equals(visibility) || FAMILY.equals(visibility) || PUBLIC.equals(visibility);
    }
}
