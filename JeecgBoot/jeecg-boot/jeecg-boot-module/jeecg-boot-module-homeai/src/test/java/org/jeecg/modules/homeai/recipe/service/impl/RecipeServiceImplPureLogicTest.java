package org.jeecg.modules.homeai.recipe.service.impl;

import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.homeai.recipe.constant.RecipeVisibility;
import org.jeecg.modules.homeai.recipe.entity.Recipe;
import org.jeecg.modules.homeai.recipe.entity.RecipeIngredient;
import org.jeecg.modules.homeai.recipe.entity.RecipeStep;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 菜谱服务纯逻辑回归测试
 *
 * <p>覆盖 RecipeServiceImpl 中不依赖 Spring/DB 的纯逻辑方法：
 * applyFamilyOnSave / applyAdminVisibilityOnSave / parseIngredientsFromExcel /
 * parseStepsFromExcel，以及 RecipeVisibility.isValid。
 * 无 Spring 上下文、无 Mockito，直接 new RecipeServiceImpl()。</p>
 */
class RecipeServiceImplPureLogicTest {

    private final RecipeServiceImpl service = new RecipeServiceImpl();

    private Recipe recipe(String visibility, String familyId) {
        Recipe r = new Recipe();
        r.setVisibility(visibility);
        r.setFamilyId(familyId);
        return r;
    }

    // ---------- applyFamilyOnSave ----------

    @Test
    void familyVisibilityKeepsFamilyId() {
        // 家庭共享 + 有家庭 → familyId 保留
        Recipe r = recipe(RecipeVisibility.FAMILY, "f1");
        service.applyFamilyOnSave(r, "u1", "f1");
        assertEquals("f1", r.getFamilyId());
    }

    @Test
    void familyVisibilityWithoutFamilyThrows() {
        // 家庭共享 + 无家庭（null/空串）→ 抛异常
        Recipe nullFamily = recipe(RecipeVisibility.FAMILY, null);
        JeecgBootException ex1 = assertThrows(JeecgBootException.class,
                () -> service.applyFamilyOnSave(nullFamily, "u1", null));
        assertTrue(ex1.getMessage().contains("加入家庭后才能共享菜谱"));

        Recipe emptyFamily = recipe(RecipeVisibility.FAMILY, "f1");
        JeecgBootException ex2 = assertThrows(JeecgBootException.class,
                () -> service.applyFamilyOnSave(emptyFamily, "u1", ""));
        assertTrue(ex2.getMessage().contains("加入家庭后才能共享菜谱"));
    }

    @Test
    void nonFamilyVisibilityClearsFamilyId() {
        // 非家庭共享 → familyId 清空
        Recipe r = recipe(RecipeVisibility.PRIVATE, "f1");
        service.applyFamilyOnSave(r, "u1", "f1");
        assertNull(r.getFamilyId());
    }

    // ---------- applyAdminVisibilityOnSave ----------

    @Test
    void nullVisibilityDefaultsToPublic() {
        // 管理端可见性为空 → 置为 public
        Recipe r = recipe(null, null);
        service.applyAdminVisibilityOnSave(r);
        assertEquals(RecipeVisibility.PUBLIC, r.getVisibility());
    }

    @Test
    void invalidVisibilityThrows() {
        // 非法可见性 → 抛异常
        Recipe r = recipe("invalid", null);
        JeecgBootException ex = assertThrows(JeecgBootException.class,
                () -> service.applyAdminVisibilityOnSave(r));
        assertTrue(ex.getMessage().contains("可见性参数无效"));
    }

    @Test
    void adminFamilyVisibilityWithoutFamilyThrows() {
        // 家庭共享但未选家庭 → 抛异常
        Recipe r = recipe(RecipeVisibility.FAMILY, null);
        JeecgBootException ex = assertThrows(JeecgBootException.class,
                () -> service.applyAdminVisibilityOnSave(r));
        assertTrue(ex.getMessage().contains("家庭共享菜谱请选择所属家庭"));
    }

    @Test
    void adminFamilyVisibilityKeepsFamilyId() {
        // 家庭共享 + 有家庭 → familyId 保留
        Recipe r = recipe(RecipeVisibility.FAMILY, "f1");
        service.applyAdminVisibilityOnSave(r);
        assertEquals("f1", r.getFamilyId());
    }

    @Test
    void publicVisibilityClearsFamilyId() {
        // 公开可见性 → familyId 清空
        Recipe r = recipe(RecipeVisibility.PUBLIC, "f1");
        service.applyAdminVisibilityOnSave(r);
        assertNull(r.getFamilyId());
    }

    // ---------- parseIngredientsFromExcel ----------

    @Test
    void emptyIngredientTextReturnsEmptyList() {
        // null / 空串 → 空列表
        assertTrue(service.parseIngredientsFromExcel(null).isEmpty());
        assertTrue(service.parseIngredientsFromExcel("").isEmpty());
    }

    @Test
    void fullIngredientRowParsed() {
        // "土豆|2|个" → name/quantity/unit/sortOrder 齐全
        List<RecipeIngredient> list = service.parseIngredientsFromExcel("土豆|2|个");
        assertEquals(1, list.size());
        RecipeIngredient ing = list.get(0);
        assertEquals("土豆", ing.getName());
        assertEquals(new BigDecimal("2"), ing.getQuantity());
        assertEquals("个", ing.getUnit());
        assertEquals(1, ing.getSortOrder());
    }

    @Test
    void nonNumericQuantityFallsBackToUnit() {
        // "土豆|2个" → BigDecimal 解析失败，整段进 unit，quantity 为 null
        List<RecipeIngredient> list = service.parseIngredientsFromExcel("土豆|2个");
        assertEquals(1, list.size());
        RecipeIngredient ing = list.get(0);
        assertNull(ing.getQuantity());
        assertEquals("2个", ing.getUnit());
    }

    @Test
    void nameOnlyIngredientParsed() {
        // "土豆" → 仅 name，quantity/unit 为 null
        List<RecipeIngredient> list = service.parseIngredientsFromExcel("土豆");
        assertEquals(1, list.size());
        RecipeIngredient ing = list.get(0);
        assertEquals("土豆", ing.getName());
        assertNull(ing.getQuantity());
        assertNull(ing.getUnit());
    }

    @Test
    void multipleIngredientsOrdered() {
        // 多条食材 → sortOrder 依次 1、2
        List<RecipeIngredient> list = service.parseIngredientsFromExcel("土豆|2|个;鸡蛋|3|枚");
        assertEquals(2, list.size());
        assertEquals(1, list.get(0).getSortOrder());
        assertEquals(2, list.get(1).getSortOrder());
        assertEquals("鸡蛋", list.get(1).getName());
    }

    @Test
    void emptySegmentSkipped() {
        // 空段跳过 → 仍 2 条
        List<RecipeIngredient> list = service.parseIngredientsFromExcel("土豆|2|个;;鸡蛋");
        assertEquals(2, list.size());
    }

    @Test
    void emptyNameIngredientSkipped() {
        // name 为空 → 跳过 → 空列表
        assertTrue(service.parseIngredientsFromExcel("|2|个").isEmpty());
    }

    // ---------- parseStepsFromExcel ----------

    @Test
    void emptyStepTextReturnsEmptyList() {
        // null / 空串 → 空列表
        assertTrue(service.parseStepsFromExcel(null).isEmpty());
        assertTrue(service.parseStepsFromExcel("").isEmpty());
    }

    @Test
    void numberedStepsParsedWithPrefixStripped() {
        // 前缀数字+分隔符（. 、 )）被剥除，stepNum/sortOrder 依次递增
        List<RecipeStep> list = service.parseStepsFromExcel("1. 洗菜;2、切菜;3) 炒菜");
        assertEquals(3, list.size());
        assertEquals("洗菜", list.get(0).getDescription());
        assertEquals("切菜", list.get(1).getDescription());
        assertEquals("炒菜", list.get(2).getDescription());
        assertEquals(1, list.get(0).getStepNum());
        assertEquals(2, list.get(1).getStepNum());
        assertEquals(3, list.get(2).getStepNum());
        assertEquals(1, list.get(0).getSortOrder());
        assertEquals(3, list.get(2).getSortOrder());
    }

    @Test
    void emptyStepSegmentSkipped() {
        // 空段跳过 → 2 条
        List<RecipeStep> list = service.parseStepsFromExcel("洗菜;;切菜");
        assertEquals(2, list.size());
        assertEquals("洗菜", list.get(0).getDescription());
        assertEquals("切菜", list.get(1).getDescription());
    }

    // ---------- RecipeVisibility.isValid ----------

    @Test
    void validVisibilitiesAccepted() {
        // private/family/public → 合法
        assertTrue(RecipeVisibility.isValid(RecipeVisibility.PRIVATE));
        assertTrue(RecipeVisibility.isValid(RecipeVisibility.FAMILY));
        assertTrue(RecipeVisibility.isValid(RecipeVisibility.PUBLIC));
    }

    @Test
    void invalidVisibilitiesRejected() {
        // 非法值 / null → 不合法
        assertFalse(RecipeVisibility.isValid("invalid"));
        assertFalse(RecipeVisibility.isValid(null));
    }
}