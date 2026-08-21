package org.jeecg.modules.homeai.recipe.util;

import org.jeecg.common.util.oConvertUtils;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 菜谱导入分类解析：Excel 可填分类 ID 或中文名，并按菜名纠正过粗的文件夹分类。
 */
public final class RecipeCategoryResolve {

    public static final String HOT = "rc_hot";
    public static final String COLD = "rc_cold";
    public static final String SOUP = "rc_soup";
    public static final String STAPLE = "rc_staple";
    public static final String BAKE = "rc_bake";
    public static final String DRINK = "rc_drink";
    public static final String SNACK = "rc_snack";
    public static final String OTHER = "rc_other";

    private static final Map<String, String> ALIASES = new LinkedHashMap<>();

    static {
        putAliases(HOT, "rc_hot", "热菜", "炒菜", "荤菜", "素菜", "炖菜", "蒸菜", "红烧");
        putAliases(COLD, "rc_cold", "凉菜", "凉拌", "冷菜", "冷盘");
        putAliases(SOUP, "rc_soup", "汤羹", "汤", "煲汤");
        putAliases(STAPLE, "rc_staple", "主食", "早餐", "米饭", "面食");
        putAliases(BAKE, "rc_bake", "烘焙", "甜点", "甜品");
        putAliases(DRINK, "rc_drink", "饮品", "饮料");
        putAliases(SNACK, "rc_snack", "小食", "小吃", "零食");
        putAliases(OTHER, "rc_other", "其他", "其它");
    }

    private RecipeCategoryResolve() {
    }

    /**
     * @param rawCategory Excel「分类ID」列（可为 rc_hot / 热菜）
     * @param recipeName  菜谱名称，用于细分类
     * @param validIds    库中有效分类 ID
     * @param nameToId    分类中文名 → ID
     */
    public static String resolve(String rawCategory, String recipeName, Set<String> validIds, Map<String, String> nameToId) {
        String inferred = inferFromName(recipeName);
        String fromRaw = resolveRaw(rawCategory, validIds, nameToId);
        if (inferred != null && isValid(inferred, validIds)) {
            return inferred;
        }
        if (fromRaw != null) {
            return fromRaw;
        }
        if (isValid(HOT, validIds)) {
            return HOT;
        }
        if (isValid(OTHER, validIds)) {
            return OTHER;
        }
        throw new IllegalArgumentException("未配置菜谱分类，请先初始化默认分类");
    }

    public static String inferFromName(String recipeName) {
        if (oConvertUtils.isEmpty(recipeName)) {
            return null;
        }
        String text = recipeName.trim();
        if (containsAny(text, "凉拌", "拍黄", "白切", "口水鸡", "凉皮", "沙拉", "冷盘", "皮蛋豆腐")) {
            return COLD;
        }
        if (containsAny(text, "蛋糕", "饼干", "面包", "烘焙", "马芬", "曲奇", "泡芙", "蛋挞", "司康", "发糕", "蛋黄酥", "提拉米苏")) {
            return BAKE;
        }
        if (containsAny(text, "奶茶", "豆浆", "咖啡", "果汁", "柠檬水", "酸梅汤", "饮品", "汽水")) {
            return DRINK;
        }
        if (containsAny(text, "薯条", "炸鸡", "小吃", "零食")) {
            return SNACK;
        }
        if (containsAny(text, "粥", "炒饭", "盖饭", "卤肉饭", "面条", "拌面", "炒面", "挂面", "拉面", "刀削面", "焖面",
                "饼", "包子", "灌汤包", "饺子", "馒头", "花卷", "馄饨", "米线", "米粉", "烧麦", "小笼", "肉夹馍", "凉粉")) {
            return STAPLE;
        }
        if (text.contains("汤圆") || text.endsWith("面") || text.endsWith("饭") || text.endsWith("粥")
                || (text.endsWith("包") && !text.contains("荷包"))) {
            return STAPLE;
        }
        if (containsAny(text, "上汤", "酸汤")) {
            return HOT;
        }
        if (containsAny(text, "汤", "羹", "煲")) {
            return SOUP;
        }
        return null;
    }

    private static String resolveRaw(String rawCategory, Set<String> validIds, Map<String, String> nameToId) {
        if (oConvertUtils.isEmpty(rawCategory)) {
            return null;
        }
        String v = rawCategory.trim();
        if (isValid(v, validIds)) {
            return v;
        }
        if (nameToId != null) {
            String byName = nameToId.get(v);
            if (isValid(byName, validIds)) {
                return byName;
            }
        }
        String alias = ALIASES.get(v.toLowerCase(Locale.ROOT));
        if (alias == null) {
            alias = ALIASES.get(v);
        }
        return isValid(alias, validIds) ? alias : null;
    }

    private static boolean isValid(String id, Set<String> validIds) {
        return oConvertUtils.isNotEmpty(id) && validIds != null && validIds.contains(id);
    }

    private static boolean containsAny(String text, String... keys) {
        for (String k : keys) {
            if (text.contains(k)) {
                return true;
            }
        }
        return false;
    }

    private static void putAliases(String id, String... keys) {
        for (String k : keys) {
            ALIASES.put(k, id);
            ALIASES.put(k.toLowerCase(Locale.ROOT), id);
        }
    }
}
