package org.jeecg.modules.homeai.recipe.util;

import org.jeecg.modules.homeai.recipe.entity.Recipe;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 推荐多样性：常做菜降权；做过 1～2 次才允许补位。
 */
class RecipeRecommendUtilTest {

    private Recipe hot() {
        Recipe r = new Recipe();
        r.setId("r1");
        r.setViewCount(100);
        r.setFavoriteCount(10);
        return r;
    }

    @Test
    void highCookCountScoresLowerThanNeverCooked() {
        Recipe r = hot();
        double never = RecipeRecommendUtil.recommendScore(r, Collections.emptySet(), 0);
        double often = RecipeRecommendUtil.recommendScore(r, Collections.emptySet(), 8);
        assertTrue(often < never);
    }

    @Test
    void cookedFillOnlyOnceOrTwice() {
        assertTrue(RecipeRecommendUtil.eligibleCookedFill(1));
        assertTrue(RecipeRecommendUtil.eligibleCookedFill(2));
        assertFalse(RecipeRecommendUtil.eligibleCookedFill(0));
        assertFalse(RecipeRecommendUtil.eligibleCookedFill(3));
    }
}
