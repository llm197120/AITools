package org.jeecg.modules.homeai.recipe.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.homeai.config.service.IHomeaiFileStorageService;
import org.jeecg.modules.homeai.family.entity.FamilyMember;
import org.jeecg.modules.homeai.family.service.IFamilyMemberService;
import org.jeecg.modules.homeai.plan.entity.PlanInstance;
import org.jeecg.modules.homeai.plan.mapper.PlanInstanceMapper;
import org.jeecg.modules.homeai.plan.service.IPlanService;
import org.jeecg.modules.homeai.recipe.constant.RecipeVisibility;
import org.jeecg.modules.homeai.recipe.entity.*;
import org.jeecg.modules.homeai.recipe.mapper.RecipeFavoriteMapper;
import org.jeecg.modules.homeai.recipe.mapper.RecipeIngredientMapper;
import org.jeecg.modules.homeai.recipe.mapper.RecipeMapper;
import org.jeecg.modules.homeai.recipe.mapper.RecipeStepMapper;
import org.jeecg.modules.homeai.recipe.service.IRecipeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.Month;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RecipeServiceImpl extends ServiceImpl<RecipeMapper, Recipe> implements IRecipeService {
    @Autowired private RecipeIngredientMapper ingredientMapper;
    @Autowired private RecipeStepMapper stepMapper;
    @Autowired private RecipeFavoriteMapper favoriteMapper;
    @Autowired private IHomeaiFileStorageService fileStorageService;
    @Lazy
    @Autowired private IPlanService planService;
    @Autowired private IFamilyMemberService familyMemberService;
    //update-begin---author:admin ---date:2026-08-13 for：【HomeAI-R32】做过次数统计-----------
    @Autowired private PlanInstanceMapper planInstanceMapper;
    //update-end---author:admin ---date:2026-08-13 for：【HomeAI-R32】做过次数统计-----------

    @Override
    public Recipe getDetail(String id) {
        return getById(id);
    }

    @Override
    public Map<String, Object> getDetailWithRelations(String id) {
        return getDetailWithRelations(id, null, null, false);
    }

    @Override
    public Map<String, Object> getDetailWithRelations(String id, String userId, String familyId, boolean checkVisibility) {
        Recipe recipe = getById(id);
        if (recipe == null) {
            throw new JeecgBootException("菜谱不存在");
        }
        if (checkVisibility) {
            assertRecipeVisible(recipe, userId, familyId);
            //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R26】小程序详情浏览计数-----------
            incrementViewCount(id);
            int vc = recipe.getViewCount() != null ? recipe.getViewCount() : 0;
            recipe.setViewCount(vc + 1);
            //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R26】小程序详情浏览计数-----------
        }
        Map<String, Object> result = new HashMap<>();
        result.put("recipe", recipe);
        result.put("ingredients", getIngredients(id));
        result.put("steps", getSteps(id));
        if (oConvertUtils.isNotEmpty(userId)) {
            result.put("isFavorited", isFavorited(userId, id));
        }
        return result;
    }

    //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R26】浏览计数 + 热门排行-----------
    @Override
    public void incrementViewCount(String recipeId) {
        if (oConvertUtils.isEmpty(recipeId)) {
            return;
        }
        baseMapper.incrementViewCount(recipeId);
    }

    @Override
    public List<Recipe> listHotRecipes(String userId, String familyId, int limit) {
        int size = limit <= 0 ? 20 : Math.min(limit, 50);
        LambdaQueryWrapper<Recipe> q = new LambdaQueryWrapper<>();
        applyClientVisibilityFilter(q, userId, familyId);
        q.orderByDesc(Recipe::getViewCount).orderByDesc(Recipe::getCreateTime).last("LIMIT " + size);
        return list(q);
    }

    //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R28】轻量推荐-----------
    @Override
    public List<Map<String, Object>> listRecommendRecipes(String userId, String familyId, int limit, String season) {
        int size = limit <= 0 ? 8 : Math.min(limit, 20);
        LinkedHashMap<String, Map<String, Object>> ordered = new LinkedHashMap<>();
        //update-begin---author:admin ---date:2026-08-13 for：【HomeAI-R32】做过次数加权-----------
        Map<String, Integer> cookCounts = loadCookCounts(userId, familyId);
        //update-end---author:admin ---date:2026-08-13 for：【HomeAI-R32】做过次数加权-----------

        // 1) 今日计划关联菜
        try {
            List<PlanInstance> today = planService.getInstancesByDate(userId, LocalDate.now());
            if (today != null) {
                for (PlanInstance inst : today) {
                    if (inst == null || oConvertUtils.isEmpty(inst.getRecipeId())) {
                        continue;
                    }
                    appendRecommend(ordered, inst.getRecipeId(), userId, familyId, "today_plan", size, cookCounts);
                    if (ordered.size() >= size) {
                        return new ArrayList<>(ordered.values());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("推荐：读取今日计划失败", e);
        }

        // 2) 我的收藏
        List<Recipe> myFav = listFavoriteRecipes(userId, familyId);
        if (myFav != null) {
            for (Recipe r : myFav) {
                if (r == null) continue;
                appendRecommendRecipe(ordered, r, "my_favorite", size, cookCounts);
                if (ordered.size() >= size) {
                    return new ArrayList<>(ordered.values());
                }
            }
        }

        // 3) 家庭成员收藏
        if (oConvertUtils.isNotEmpty(familyId)) {
            List<FamilyMember> members = familyMemberService.getByFamilyId(familyId);
            if (members != null) {
                Map<String, Integer> favCount = new HashMap<>();
                for (FamilyMember m : members) {
                    if (m == null || oConvertUtils.isEmpty(m.getUserId()) || userId.equals(m.getUserId())) {
                        continue;
                    }
                    LambdaQueryWrapper<RecipeFavorite> fq = new LambdaQueryWrapper<>();
                    fq.eq(RecipeFavorite::getUserId, m.getUserId());
                    List<RecipeFavorite> favs = favoriteMapper.selectList(fq);
                    if (favs == null) continue;
                    for (RecipeFavorite f : favs) {
                        if (f == null || oConvertUtils.isEmpty(f.getRecipeId())) continue;
                        favCount.merge(f.getRecipeId(), 1, Integer::sum);
                    }
                }
                favCount.entrySet().stream()
                        .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                        .forEach(e -> appendRecommend(ordered, e.getKey(), userId, familyId, "family_favorite", size, cookCounts));
                if (ordered.size() >= size) {
                    return new ArrayList<>(ordered.values());
                }
            }
        }

        //update-begin---author:admin ---date:2026-08-13 for：【HomeAI-R32】做过次数优先补位-----------
        cookCounts.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .forEach(e -> appendRecommend(ordered, e.getKey(), userId, familyId, "cooked", size, cookCounts));
        if (ordered.size() >= size) {
            return new ArrayList<>(ordered.values());
        }
        //update-end---author:admin ---date:2026-08-13 for：【HomeAI-R32】做过次数优先补位-----------

        // 4) 加权热门兜底（含季节分类 / 做过次数加权）
        Set<String> seasonCats = resolveSeasonCategories(season);
        List<Recipe> hot = listHotRecipes(userId, familyId, Math.max(size * 3, 30));
        if (hot != null) {
            hot.sort((a, b) -> Double.compare(
                    recommendScore(b, seasonCats, cookCounts), recommendScore(a, seasonCats, cookCounts)));
            for (Recipe r : hot) {
                String reason;
                int cooked = cookCounts.getOrDefault(r.getId(), 0);
                if (cooked > 0) {
                    reason = "cooked";
                } else if (r.getCategoryId() != null && seasonCats.contains(r.getCategoryId())) {
                    reason = "season";
                } else {
                    reason = "hot";
                }
                appendRecommendRecipe(ordered, r, reason, size, cookCounts);
                if (ordered.size() >= size) {
                    break;
                }
            }
        }
        return new ArrayList<>(ordered.values());
    }

    //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R30】新菜尝鲜-----------
    @Override
    public List<Recipe> listNewRecipes(String userId, String familyId, int limit, int days) {
        int size = limit <= 0 ? 8 : Math.min(limit, 20);
        int window = days <= 0 ? 30 : Math.min(days, 365);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -window);
        Date since = cal.getTime();

        LambdaQueryWrapper<Recipe> recentQ = new LambdaQueryWrapper<>();
        applyClientVisibilityFilter(recentQ, userId, familyId);
        recentQ.ge(Recipe::getCreateTime, since)
                .orderByDesc(Recipe::getCreateTime)
                .last("LIMIT " + size);
        List<Recipe> recent = list(recentQ);
        if (recent != null && recent.size() >= size) {
            return recent;
        }

        LinkedHashMap<String, Recipe> ordered = new LinkedHashMap<>();
        if (recent != null) {
            for (Recipe r : recent) {
                if (r != null && oConvertUtils.isNotEmpty(r.getId())) {
                    ordered.put(r.getId(), r);
                }
            }
        }
        LambdaQueryWrapper<Recipe> fallbackQ = new LambdaQueryWrapper<>();
        applyClientVisibilityFilter(fallbackQ, userId, familyId);
        fallbackQ.orderByDesc(Recipe::getCreateTime).last("LIMIT " + size);
        List<Recipe> fallback = list(fallbackQ);
        if (fallback != null) {
            for (Recipe r : fallback) {
                if (ordered.size() >= size) {
                    break;
                }
                if (r != null && oConvertUtils.isNotEmpty(r.getId())) {
                    ordered.putIfAbsent(r.getId(), r);
                }
            }
        }
        return new ArrayList<>(ordered.values());
    }
    //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R30】新菜尝鲜-----------

    private void appendRecommend(LinkedHashMap<String, Map<String, Object>> ordered,
                                 String recipeId, String userId, String familyId,
                                 String reason, int size, Map<String, Integer> cookCounts) {
        if (ordered.size() >= size || ordered.containsKey(recipeId)) {
            return;
        }
        Recipe recipe = getById(recipeId);
        if (recipe == null || !canViewRecipe(recipe, userId, familyId)) {
            return;
        }
        appendRecommendRecipe(ordered, recipe, reason, size, cookCounts);
    }

    private void appendRecommendRecipe(LinkedHashMap<String, Map<String, Object>> ordered,
                                       Recipe recipe, String reason, int size,
                                       Map<String, Integer> cookCounts) {
        if (recipe == null || oConvertUtils.isEmpty(recipe.getId()) || ordered.size() >= size) {
            return;
        }
        if (ordered.containsKey(recipe.getId())) {
            return;
        }
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", recipe.getId());
        row.put("name", recipe.getName());
        row.put("coverUrl", recipe.getCoverUrl());
        row.put("difficulty", recipe.getDifficulty());
        row.put("cookTime", recipe.getCookTime());
        row.put("viewCount", recipe.getViewCount());
        row.put("favoriteCount", recipe.getFavoriteCount());
        row.put("categoryId", recipe.getCategoryId());
        row.put("visibility", recipe.getVisibility());
        row.put("reason", reason);
        //update-begin---author:admin ---date:2026-08-13 for：【HomeAI-R32】推荐返回做过次数-----------
        int cooked = 0;
        if (cookCounts != null && recipe.getId() != null) {
            cooked = cookCounts.getOrDefault(recipe.getId(), 0);
        }
        row.put("cookCount", cooked);
        //update-end---author:admin ---date:2026-08-13 for：【HomeAI-R32】推荐返回做过次数-----------
        ordered.put(recipe.getId(), row);
    }

    //update-begin---author:admin ---date:2026-08-13 for：【HomeAI-R32】做过次数加权-----------
    private Map<String, Integer> loadCookCounts(String userId, String familyId) {
        LinkedHashSet<String> userIds = new LinkedHashSet<>();
        if (oConvertUtils.isNotEmpty(userId)) {
            userIds.add(userId);
        }
        if (oConvertUtils.isNotEmpty(familyId)) {
            List<FamilyMember> members = familyMemberService.getByFamilyId(familyId);
            if (members != null) {
                for (FamilyMember m : members) {
                    if (m != null && oConvertUtils.isNotEmpty(m.getUserId())) {
                        userIds.add(m.getUserId());
                    }
                }
            }
        }
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Map<String, Object>> rows = planInstanceMapper.countCompletedByRecipe(new ArrayList<>(userIds));
        Map<String, Integer> result = new HashMap<>();
        if (rows == null) {
            return result;
        }
        for (Map<String, Object> row : rows) {
            if (row == null) {
                continue;
            }
            Object rid = firstNonNull(row, "recipeId", "recipeid", "recipe_id");
            Object cnt = firstNonNull(row, "cookCount", "cookcount", "cook_count");
            if (rid == null || cnt == null) {
                continue;
            }
            try {
                result.put(String.valueOf(rid), Integer.parseInt(String.valueOf(cnt)));
            } catch (NumberFormatException ignored) {
                // skip malformed count
            }
        }
        return result;
    }

    private static Object firstNonNull(Map<String, Object> row, String... keys) {
        for (String key : keys) {
            if (row.containsKey(key) && row.get(key) != null) {
                return row.get(key);
            }
        }
        return null;
    }

    private static double recommendScore(Recipe r, Set<String> seasonCats, Map<String, Integer> cookCounts) {
        if (r == null) return 0;
        int views = r.getViewCount() != null ? r.getViewCount() : 0;
        int favs = r.getFavoriteCount() != null ? r.getFavoriteCount() : 0;
        double score = views * 0.6 + favs * 0.3;
        if (r.getCreateTime() != null) {
            long days = Math.max(0, (System.currentTimeMillis() - r.getCreateTime().getTime()) / (24L * 3600_000));
            score += Math.max(0, 1.0 - days / 90.0) * 0.1;
        }
        if (r.getCategoryId() != null && seasonCats.contains(r.getCategoryId())) {
            score += 0.15 * Math.max(views + favs, 1);
        }
        int cooked = 0;
        if (cookCounts != null && r.getId() != null) {
            cooked = cookCounts.getOrDefault(r.getId(), 0);
        }
        if (cooked > 0) {
            score += cooked * 1.5;
        }
        return score;
    }
    //update-end---author:admin ---date:2026-08-13 for：【HomeAI-R32】做过次数加权-----------

    private static Set<String> resolveSeasonCategories(String season) {
        String s = season;
        if (oConvertUtils.isEmpty(s) || "auto".equalsIgnoreCase(s)) {
            Month m = LocalDate.now().getMonth();
            int n = m.getValue();
            if (n == 12 || n <= 2) s = "winter";
            else if (n <= 5) s = "spring";
            else if (n <= 8) s = "summer";
            else s = "autumn";
        }
        switch (s.toLowerCase(Locale.ROOT)) {
            case "winter":
                return Set.of("rc_soup", "rc_hot");
            case "summer":
                return Set.of("rc_cold", "rc_drink");
            case "spring":
            case "autumn":
                return Set.of("rc_staple", "rc_hot");
            default:
                return Collections.emptySet();
        }
    }
    //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R28】轻量推荐-----------
    //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R26】浏览计数 + 热门排行-----------

    @Override
    public boolean canViewRecipe(Recipe recipe, String userId, String familyId) {
        if (recipe == null || oConvertUtils.isEmpty(userId)) {
            return false;
        }
        if (userId.equals(recipe.getUserId())) {
            return true;
        }
        //update-begin---author:cursor ---date:2026-08-12 for：【菜谱可见性】公开菜谱对所有登录用户可见-----------
        if (RecipeVisibility.PUBLIC.equals(recipe.getVisibility())) {
            return true;
        }
        //update-end---author:cursor ---date:2026-08-12 for：【菜谱可见性】公开菜谱对所有登录用户可见-----------
        return RecipeVisibility.FAMILY.equals(recipe.getVisibility())
                && oConvertUtils.isNotEmpty(familyId)
                && familyId.equals(recipe.getFamilyId());
    }

    @Override
    public void assertRecipeVisible(Recipe recipe, String userId, String familyId) {
        if (!canViewRecipe(recipe, userId, familyId)) {
            throw new JeecgBootException("无权查看该菜谱");
        }
    }

    @Override
    public void applyFamilyOnSave(Recipe recipe, String userId, String familyId) {
        //update-begin---author:cursor ---date:2026-08-12 for：【菜谱可见性】保存时规范化 visibility/familyId-----------
        if (RecipeVisibility.FAMILY.equals(recipe.getVisibility())) {
            if (oConvertUtils.isEmpty(familyId)) {
                throw new JeecgBootException("加入家庭后才能共享菜谱");
            }
            recipe.setFamilyId(familyId);
        } else {
            recipe.setFamilyId(null);
        }
        //update-end---author:cursor ---date:2026-08-12 for：【菜谱可见性】保存时规范化 visibility/familyId-----------
    }

    //update-begin---author:cursor ---date:2026-08-12 for：【菜谱可见性】管理端保存校验-----------
    @Override
    public void applyAdminVisibilityOnSave(Recipe recipe) {
        String visibility = recipe.getVisibility();
        if (oConvertUtils.isEmpty(visibility)) {
            recipe.setVisibility(RecipeVisibility.PUBLIC);
            visibility = RecipeVisibility.PUBLIC;
        }
        if (!RecipeVisibility.isValid(visibility)) {
            throw new JeecgBootException("可见性参数无效");
        }
        if (RecipeVisibility.FAMILY.equals(visibility)) {
            if (oConvertUtils.isEmpty(recipe.getFamilyId())) {
                throw new JeecgBootException("家庭共享菜谱请选择所属家庭");
            }
        } else {
            recipe.setFamilyId(null);
        }
    }
    //update-end---author:cursor ---date:2026-08-12 for：【菜谱可见性】管理端保存校验-----------

    @Override
    public boolean toggleFavorite(String userId, String recipeId, String familyId) {
        Recipe recipe = getById(recipeId);
        assertRecipeVisible(recipe, userId, familyId);
        LambdaQueryWrapper<RecipeFavorite> q = new LambdaQueryWrapper<>();
        q.eq(RecipeFavorite::getUserId, userId).eq(RecipeFavorite::getRecipeId, recipeId);
        RecipeFavorite existing = favoriteMapper.selectOne(q);
        if (existing != null) {
            favoriteMapper.deleteById(existing.getId());
            int count = recipe.getFavoriteCount() != null ? recipe.getFavoriteCount() : 0;
            recipe.setFavoriteCount(Math.max(0, count - 1));
            updateById(recipe);
            return false;
        }
        RecipeFavorite fav = new RecipeFavorite();
        fav.setUserId(userId);
        fav.setRecipeId(recipeId);
        fav.setCreateTime(new Date());
        favoriteMapper.insert(fav);
        int count = recipe.getFavoriteCount() != null ? recipe.getFavoriteCount() : 0;
        recipe.setFavoriteCount(count + 1);
        updateById(recipe);
        return true;
    }

    @Override
    public boolean isFavorited(String userId, String recipeId) {
        if (oConvertUtils.isEmpty(userId) || oConvertUtils.isEmpty(recipeId)) {
            return false;
        }
        LambdaQueryWrapper<RecipeFavorite> q = new LambdaQueryWrapper<>();
        q.eq(RecipeFavorite::getUserId, userId).eq(RecipeFavorite::getRecipeId, recipeId);
        return favoriteMapper.selectCount(q) > 0;
    }

    @Override
    public List<Recipe> listFavoriteRecipes(String userId, String familyId) {
        LambdaQueryWrapper<RecipeFavorite> fq = new LambdaQueryWrapper<>();
        fq.eq(RecipeFavorite::getUserId, userId).orderByDesc(RecipeFavorite::getCreateTime);
        List<RecipeFavorite> favorites = favoriteMapper.selectList(fq);
        if (favorites.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> ids = favorites.stream().map(RecipeFavorite::getRecipeId).toList();
        LambdaQueryWrapper<Recipe> q = new LambdaQueryWrapper<>();
        q.in(Recipe::getId, ids).eq(Recipe::getDelFlag, 0);
        applyClientVisibilityFilter(q, userId, familyId);
        List<Recipe> recipes = list(q);
        Map<String, Recipe> map = new LinkedHashMap<>();
        for (Recipe r : recipes) {
            map.put(r.getId(), r);
        }
        List<Recipe> ordered = new ArrayList<>();
        for (RecipeFavorite fav : favorites) {
            Recipe r = map.get(fav.getRecipeId());
            if (r != null) {
                ordered.add(r);
            }
        }
        return ordered;
    }

    @Override
    public List<RecipeIngredient> getIngredients(String recipeId) {
        LambdaQueryWrapper<RecipeIngredient> q = new LambdaQueryWrapper<>();
        q.eq(RecipeIngredient::getRecipeId, recipeId).orderByAsc(RecipeIngredient::getSortOrder);
        return ingredientMapper.selectList(q);
    }

    @Override
    public List<RecipeStep> getSteps(String recipeId) {
        LambdaQueryWrapper<RecipeStep> q = new LambdaQueryWrapper<>();
        q.eq(RecipeStep::getRecipeId, recipeId).orderByAsc(RecipeStep::getStepNum);
        return stepMapper.selectList(q);
    }

    @Override
    public List<Recipe> search(String keyword) {
        LambdaQueryWrapper<Recipe> q = new LambdaQueryWrapper<>();
        q.like(Recipe::getName, keyword).eq(Recipe::getDelFlag, 0).orderByDesc(Recipe::getCreateTime);
        return list(q);
    }

    @Override
    public List<Recipe> searchVisible(String keyword, String userId, String familyId) {
        LambdaQueryWrapper<Recipe> q = new LambdaQueryWrapper<>();
        q.like(Recipe::getName, keyword).eq(Recipe::getDelFlag, 0);
        applyClientVisibilityFilter(q, userId, familyId);
        q.orderByDesc(Recipe::getCreateTime);
        return list(q);
    }

    @Override
    public void applyClientVisibilityFilter(QueryWrapper<Recipe> qw, String userId, String familyId) {
        if (oConvertUtils.isEmpty(userId)) {
            qw.eq("user_id", "__none__");
            return;
        }
        //update-begin---author:cursor ---date:2026-08-12 for：【菜谱可见性】列表包含公开菜谱-----------
        qw.and(w -> {
            w.eq("user_id", userId)
                    .or()
                    .eq("visibility", RecipeVisibility.PUBLIC);
            if (oConvertUtils.isNotEmpty(familyId)) {
                w.or(n -> n.eq("visibility", RecipeVisibility.FAMILY).eq("family_id", familyId));
            }
        });
        //update-end---author:cursor ---date:2026-08-12 for：【菜谱可见性】列表包含公开菜谱-----------
    }

    @Override
    public void applyClientVisibilityFilter(LambdaQueryWrapper<Recipe> qw, String userId, String familyId) {
        if (oConvertUtils.isEmpty(userId)) {
            qw.eq(Recipe::getUserId, "__none__");
            return;
        }
        //update-begin---author:cursor ---date:2026-08-12 for：【菜谱可见性】列表包含公开菜谱-----------
        qw.and(w -> {
            w.eq(Recipe::getUserId, userId)
                    .or()
                    .eq(Recipe::getVisibility, RecipeVisibility.PUBLIC);
            if (oConvertUtils.isNotEmpty(familyId)) {
                w.or(n -> n.eq(Recipe::getVisibility, RecipeVisibility.FAMILY).eq(Recipe::getFamilyId, familyId));
            }
        });
        //update-end---author:cursor ---date:2026-08-12 for：【菜谱可见性】列表包含公开菜谱-----------
    }

    @Override
    @Transactional
    public void saveWithRelations(Recipe recipe, List<RecipeIngredient> ingredients, List<RecipeStep> steps) {
        recipe.setCreateTime(new Date());
        save(recipe);
        if (ingredients != null) {
            for (RecipeIngredient i : ingredients) { i.setRecipeId(recipe.getId()); i.setCreateTime(new Date()); ingredientMapper.insert(i); }
        }
        if (steps != null) {
            for (RecipeStep s : steps) { s.setRecipeId(recipe.getId()); s.setCreateTime(new Date()); stepMapper.insert(s); }
        }
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void updateWithRelations(Recipe recipe, List<RecipeIngredient> ingredients, List<RecipeStep> steps) {
        recipe.setUpdateTime(new Date());
        updateById(recipe);
        //update-begin---author:cursor ---date:2026-08-12 for：【菜谱可见性】非家庭共享时显式清空 family_id-----------
        if (!RecipeVisibility.FAMILY.equals(recipe.getVisibility())) {
            update(new LambdaUpdateWrapper<Recipe>()
                    .eq(Recipe::getId, recipe.getId())
                    .set(Recipe::getFamilyId, null));
        }
        //update-end---author:cursor ---date:2026-08-12 for：【菜谱可见性】非家庭共享时显式清空 family_id-----------
        // 整体替换食材
        if (ingredients != null) {
            ingredientMapper.delete(new LambdaQueryWrapper<RecipeIngredient>()
                    .eq(RecipeIngredient::getRecipeId, recipe.getId()));
            for (RecipeIngredient i : ingredients) {
                i.setId(null);
                i.setRecipeId(recipe.getId());
                i.setCreateTime(new Date());
                ingredientMapper.insert(i);
            }
        }
        // 整体替换步骤
        if (steps != null) {
            stepMapper.delete(new LambdaQueryWrapper<RecipeStep>()
                    .eq(RecipeStep::getRecipeId, recipe.getId()));
            for (RecipeStep s : steps) {
                s.setId(null);
                s.setRecipeId(recipe.getId());
                s.setCreateTime(new Date());
                stepMapper.insert(s);
            }
        }
    }

    //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R24】Excel 含子表导入导出-----------
    @Override
    public List<RecipeIngredient> parseIngredientsFromExcel(String text) {
        List<RecipeIngredient> list = new ArrayList<>();
        if (oConvertUtils.isEmpty(text)) {
            return list;
        }
        String[] parts = text.replace("\r\n", "\n").replace('\n', ';').split(";");
        int order = 1;
        for (String raw : parts) {
            String part = raw == null ? "" : raw.trim();
            if (part.isEmpty()) {
                continue;
            }
            String[] cols = part.split("\\|");
            RecipeIngredient ing = new RecipeIngredient();
            ing.setName(cols[0].trim());
            if (cols.length >= 2 && oConvertUtils.isNotEmpty(cols[1].trim())) {
                try {
                    ing.setQuantity(new java.math.BigDecimal(cols[1].trim()));
                } catch (Exception ignored) {
                    // 兼容「2个」写在数量列：整段放 unit，quantity 空
                    ing.setUnit(cols[1].trim());
                }
            }
            if (cols.length >= 3) {
                ing.setUnit(cols[2].trim());
            }
            if (oConvertUtils.isEmpty(ing.getName())) {
                continue;
            }
            ing.setSortOrder(order++);
            list.add(ing);
        }
        return list;
    }

    @Override
    public List<RecipeStep> parseStepsFromExcel(String text) {
        List<RecipeStep> list = new ArrayList<>();
        if (oConvertUtils.isEmpty(text)) {
            return list;
        }
        String[] parts = text.replace("\r\n", "\n").replace('\n', ';').split(";");
        int num = 1;
        for (String raw : parts) {
            String desc = raw == null ? "" : raw.trim();
            if (desc.isEmpty()) {
                continue;
            }
            // 去掉前缀「1.」「1、」
            desc = desc.replaceFirst("^\\d+[\\.、\\)]\\s*", "");
            RecipeStep step = new RecipeStep();
            step.setStepNum(num);
            step.setSortOrder(num);
            step.setDescription(desc);
            list.add(step);
            num++;
        }
        return list;
    }

    @Override
    public void fillExcelRelationText(Recipe recipe) {
        if (recipe == null || oConvertUtils.isEmpty(recipe.getId())) {
            return;
        }
        List<RecipeIngredient> ings = getIngredients(recipe.getId());
        if (ings != null && !ings.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (RecipeIngredient i : ings) {
                if (sb.length() > 0) {
                    sb.append(';');
                }
                sb.append(i.getName() == null ? "" : i.getName());
                sb.append('|');
                sb.append(i.getQuantity() == null ? "" : i.getQuantity().stripTrailingZeros().toPlainString());
                sb.append('|');
                sb.append(i.getUnit() == null ? "" : i.getUnit());
            }
            recipe.setIngredients(sb.toString());
        }
        List<RecipeStep> steps = getSteps(recipe.getId());
        if (steps != null && !steps.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (RecipeStep s : steps) {
                if (sb.length() > 0) {
                    sb.append(';');
                }
                sb.append(s.getDescription() == null ? "" : s.getDescription());
            }
            recipe.setSteps(sb.toString());
        }
    }
    //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R24】Excel 含子表导入导出-----------

    @Override
    public String uploadVideo(String recipeId, MultipartFile file) {
        try {
            String ext = getExtension(file.getOriginalFilename());
            String fileName = "video_" + System.currentTimeMillis() + (ext != null ? "." + ext : ".mp4");
            String videoUrl = fileStorageService.storeMultipart(file, "homeai/recipe/" + recipeId + "/" + fileName);
            Recipe recipe = getById(recipeId);
            if (recipe != null) {
                recipe.setVideoUrl(videoUrl);
                updateById(recipe);
            }
            return videoUrl;
        } catch (Exception e) {
            log.error("菜谱视频上传失败", e);
            throw new RuntimeException("视频上传失败", e);
        }
    }

    @Override
    public void deleteVideo(String recipeId) {
        Recipe recipe = getById(recipeId);
        if (recipe != null && recipe.getVideoUrl() != null) {
            fileStorageService.deleteIfExists(recipe.getVideoUrl());
            recipe.setVideoUrl(null);
            updateById(recipe);
        }
    }

    @Override
    public String uploadCover(String recipeId, MultipartFile file) {
        try {
            String ext = getExtension(file.getOriginalFilename());
            String fileName = "cover_" + System.currentTimeMillis() + (ext != null ? "." + ext : ".jpg");
            String coverUrl = fileStorageService.storeMultipart(file, "homeai/recipe/" + recipeId + "/" + fileName);
            Recipe recipe = getById(recipeId);
            if (recipe != null) {
                recipe.setCoverUrl(coverUrl);
                updateById(recipe);
            }
            return coverUrl;
        } catch (Exception e) {
            log.error("菜谱封面上传失败", e);
            throw new RuntimeException("封面上传失败", e);
        }
    }

    @Override
    public String uploadStepImage(MultipartFile file) {
        try {
            String ext = getExtension(file.getOriginalFilename());
            String fileName = System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8)
                    + (ext != null ? "." + ext : ".jpg");
            return fileStorageService.storeMultipart(file, "homeai/recipe/steps/" + fileName);
        } catch (Exception e) {
            log.error("步骤图片上传失败", e);
            throw new RuntimeException("步骤图片上传失败", e);
        }
    }

    @Override
    public String uploadCoverFile(MultipartFile file) {
        return saveGeneric(file, "/homeai/recipe/covers/");
    }

    @Override
    public String uploadVideoFile(MultipartFile file) {
        return saveGeneric(file, "/homeai/recipe/videos/");
    }

    private String saveGeneric(MultipartFile file, String relativeDir) {
        try {
            String ext = getExtension(file.getOriginalFilename());
            String fileName = System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8)
                    + (ext != null ? "." + ext : "");
            String objectKey = relativeDir.replaceFirst("^/+", "") + fileName;
            return fileStorageService.storeMultipart(file, objectKey);
        } catch (Exception e) {
            log.error("文件上传失败: {}", relativeDir, e);
            throw new RuntimeException("文件上传失败", e);
        }
    }

    private String getExtension(String filename) {
        if (filename == null) return null;
        int idx = filename.lastIndexOf('.');
        return idx >= 0 ? filename.substring(idx + 1) : null;
    }
}
