package org.jeecg.modules.homeai.recipe.util;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecipeCategoryResolveTest {

    private static final Set<String> IDS = Set.of(
            "rc_hot", "rc_cold", "rc_soup", "rc_staple", "rc_bake", "rc_drink", "rc_snack", "rc_other"
    );

    private Map<String, String> names() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("热菜", "rc_hot");
        m.put("凉菜", "rc_cold");
        m.put("汤羹", "rc_soup");
        m.put("主食", "rc_staple");
        m.put("烘焙", "rc_bake");
        m.put("饮品", "rc_drink");
        m.put("小食", "rc_snack");
        m.put("其他", "rc_other");
        return m;
    }

    @Test
    void nameInferenceOverridesCoarseFolder() {
        assertEquals("rc_staple", RecipeCategoryResolve.resolve("rc_soup", "小米粥", IDS, names()));
        assertEquals("rc_staple", RecipeCategoryResolve.resolve("rc_hot", "叉烧包", IDS, names()));
        assertEquals("rc_staple", RecipeCategoryResolve.resolve("rc_hot", "台式卤肉饭", IDS, names()));
        assertEquals("rc_soup", RecipeCategoryResolve.resolve("rc_hot", "西红柿豆腐汤羹", IDS, names()));
        assertEquals("rc_cold", RecipeCategoryResolve.resolve("rc_hot", "凉拌黄瓜", IDS, names()));
        assertEquals("rc_hot", RecipeCategoryResolve.resolve("rc_soup", "上汤娃娃菜", IDS, names()));
        assertEquals("rc_hot", RecipeCategoryResolve.resolve("rc_soup", "酸汤肥牛", IDS, names()));
    }

    @Test
    void rawIdOrChineseName() {
        assertEquals("rc_cold", RecipeCategoryResolve.resolve("凉菜", "拍黄瓜", IDS, names()));
        assertEquals("rc_drink", RecipeCategoryResolve.resolve("饮品", "柠檬水", IDS, names()));
        assertEquals("rc_hot", RecipeCategoryResolve.resolve("rc_hot", "红烧肉", IDS, names()));
        assertEquals("rc_cold", RecipeCategoryResolve.resolve("凉菜", "红烧肉", IDS, names()));
    }

    @Test
    void emptyFallsBackToHotOrOther() {
        assertEquals("rc_hot", RecipeCategoryResolve.resolve("", "回锅肉", IDS, names()));
        assertEquals("rc_hot", RecipeCategoryResolve.resolve(null, null, IDS, names()));
    }
}
