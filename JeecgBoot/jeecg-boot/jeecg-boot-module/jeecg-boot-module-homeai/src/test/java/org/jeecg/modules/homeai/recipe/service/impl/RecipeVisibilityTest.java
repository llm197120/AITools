package org.jeecg.modules.homeai.recipe.service.impl;

import org.jeecg.modules.homeai.recipe.constant.RecipeVisibility;
import org.jeecg.modules.homeai.recipe.entity.Recipe;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 菜谱可见性 / 可修改性授权逻辑回归测试
 *
 * <p>覆盖 P0 双通道一致性修复：console 创建的菜谱 userId 为空，
 * 家庭成员应可查看/维护家庭共享菜谱；私有菜谱仅创建者可维护。</p>
 */
class RecipeVisibilityTest {

    private final RecipeServiceImpl service = new RecipeServiceImpl();

    private Recipe recipe(String userId, String visibility, String familyId) {
        Recipe r = new Recipe();
        r.setUserId(userId);
        r.setVisibility(visibility);
        r.setFamilyId(familyId);
        return r;
    }

    @Test
    void creatorCanViewOwnRecipe() {
        Recipe r = recipe("u1", RecipeVisibility.PRIVATE, null);
        assertTrue(service.canViewRecipe(r, "u1", null));
        assertFalse(service.canViewRecipe(r, "u2", null));
    }

    @Test
    void publicRecipeVisibleToAnyLoggedInUser() {
        Recipe r = recipe(null, RecipeVisibility.PUBLIC, null);
        assertTrue(service.canViewRecipe(r, "u2", null));
        // 未登录用户不可见
        assertFalse(service.canViewRecipe(r, null, null));
    }

    @Test
    void familyRecipeVisibleOnlyToThatFamily() {
        Recipe r = recipe(null, RecipeVisibility.FAMILY, "f1");
        assertTrue(service.canViewRecipe(r, "u2", "f1"));
        assertFalse(service.canViewRecipe(r, "u2", "f2"));
        assertFalse(service.canViewRecipe(r, "u2", null));
    }

    @Test
    void consoleCreatedFamilyRecipeModifiableByFamilyMember() {
        // console 建的菜谱 userId 为空，家庭成员应可编辑/删除
        Recipe r = recipe(null, RecipeVisibility.FAMILY, "f1");
        assertTrue(service.canModifyRecipe(r, "u2", "f1"));
        assertFalse(service.canModifyRecipe(r, "u2", "f2"));
        assertFalse(service.canModifyRecipe(r, "u2", null));
    }

    @Test
    void creatorModifiableRegardlessOfVisibility() {
        Recipe r = recipe("u1", RecipeVisibility.PUBLIC, null);
        assertTrue(service.canModifyRecipe(r, "u1", null));
        // 非本人、非家庭成员的公开菜谱不可改
        assertFalse(service.canModifyRecipe(r, "u2", null));
    }

    @Test
    void privateConsoleRecipeNotModifiableByMiniUser() {
        // console 建的私有菜谱，小程序端无人可改（语义：仅管理端可见）
        Recipe r = recipe(null, RecipeVisibility.PRIVATE, null);
        assertFalse(service.canModifyRecipe(r, "u2", null));
    }
}
