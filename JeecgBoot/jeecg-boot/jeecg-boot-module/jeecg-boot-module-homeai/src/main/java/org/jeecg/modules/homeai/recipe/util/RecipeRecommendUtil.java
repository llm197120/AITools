package org.jeecg.modules.homeai.recipe.util;

import org.jeecg.modules.homeai.recipe.entity.Recipe;

import java.util.Collections;
import java.util.Set;

/**
 * 菜谱推荐打分：热度/季节加分，常做菜降权，避免总推同一道。
 */
public final class RecipeRecommendUtil {

    /** 「做过」补位最多占几个推荐位 */
    public static final int MAX_COOKED_FILL = 2;

    /** 做过太多次则不再作为「做过」补位（改走热门池并降权） */
    public static final int COOKED_FILL_MAX_COUNT = 2;

    private RecipeRecommendUtil() {
    }

    public static boolean eligibleCookedFill(int cookCount) {
        return cookCount >= 1 && cookCount <= COOKED_FILL_MAX_COUNT;
    }

    public static double recommendScore(Recipe r, Set<String> seasonCats, java.util.Map<String, Integer> cookCounts) {
        if (r == null) {
            return 0;
        }
        int cooked = 0;
        if (cookCounts != null && r.getId() != null) {
            cooked = cookCounts.getOrDefault(r.getId(), 0);
        }
        return recommendScore(r, seasonCats == null ? Collections.emptySet() : seasonCats, cooked);
    }

    public static double recommendScore(Recipe r, Set<String> seasonCats, int cooked) {
        if (r == null) {
            return 0;
        }
        int views = r.getViewCount() != null ? r.getViewCount() : 0;
        int favs = r.getFavoriteCount() != null ? r.getFavoriteCount() : 0;
        double score = views * 0.6 + favs * 0.3;
        if (r.getCreateTime() != null) {
            long days = Math.max(0, (System.currentTimeMillis() - r.getCreateTime().getTime()) / (24L * 3600_000));
            score += Math.max(0, 1.0 - days / 90.0) * 0.1;
        }
        if (r.getCategoryId() != null && seasonCats != null && seasonCats.contains(r.getCategoryId())) {
            score += 0.15 * Math.max(views + favs, 1);
        }
        // 做过 1～2 次轻微加分；做过更多则降权，避免总推同一道
        if (cooked == 1) {
            score += 0.4;
        } else if (cooked == 2) {
            score += 0.15;
        } else if (cooked >= 3) {
            score -= Math.min(cooked * 0.8, 8.0);
        }
        return score;
    }
}
