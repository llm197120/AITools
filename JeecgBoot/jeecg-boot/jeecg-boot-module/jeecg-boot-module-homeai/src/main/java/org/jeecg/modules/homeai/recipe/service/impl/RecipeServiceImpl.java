package org.jeecg.modules.homeai.recipe.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.homeai.config.HomeaiFileMagicUtil;
import org.jeecg.modules.homeai.config.service.IHomeaiFileStorageService;
import org.jeecg.modules.homeai.config.service.IHomeaiFileWhitelistService;
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
import org.jeecg.modules.homeai.recipe.util.InMemoryMultipartFile;
import org.jeecg.modules.homeai.recipe.util.RecipeCoverMatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
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
    //update-begin---author:cursor---date:2026-08-22---for:【审查D】菜谱上传走全局扩展名白名单---
    @Autowired private IHomeaiFileWhitelistService whitelistService;
    //update-end---author:cursor---date:2026-08-22---for:【审查D】菜谱上传走全局扩展名白名单---
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
            //update-begin---author:cursor---date:2026-08-20---for:【审查修复】详情返回 canModify，对齐家庭可改---
            result.put("canModify", canModifyRecipe(recipe, userId, familyId));
            //update-end---author:cursor---date:2026-08-20---for:【审查修复】详情返回 canModify，对齐家庭可改---
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

    //update-begin---author:cursor ---date:2026-08-13 for：【P0 双通道一致性】家庭成员可维护家庭菜谱-----------
    @Override
    public boolean canModifyRecipe(Recipe recipe, String userId, String familyId) {
        if (recipe == null || oConvertUtils.isEmpty(userId)) {
            return false;
        }
        if (userId.equals(recipe.getUserId())) {
            return true;
        }
        // 家庭共享菜谱：家庭成员可编辑/删除（console 建的菜谱 userId 为空，需靠家庭归属授权）
        return RecipeVisibility.FAMILY.equals(recipe.getVisibility())
                && oConvertUtils.isNotEmpty(familyId)
                && familyId.equals(recipe.getFamilyId());
    }
    //update-end---author:cursor ---date:2026-08-13 for：【P0 双通道一致性】家庭成员可维护家庭菜谱-----------

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
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleFavorite(String userId, String recipeId, String familyId) {
        Recipe recipe = getById(recipeId);
        assertRecipeVisible(recipe, userId, familyId);
        LambdaQueryWrapper<RecipeFavorite> q = new LambdaQueryWrapper<>();
        q.eq(RecipeFavorite::getUserId, userId).eq(RecipeFavorite::getRecipeId, recipeId);
        RecipeFavorite existing = favoriteMapper.selectOne(q);
        if (existing != null) {
            favoriteMapper.deleteById(existing.getId());
            //update-begin---author:cursor ---date:2026-08-13 for：【并发修复】收藏计数改为原子递减，避免并发 read-modify-write 错乱-----------
            update(null, new LambdaUpdateWrapper<Recipe>()
                    .eq(Recipe::getId, recipeId)
                    .gt(Recipe::getFavoriteCount, 0)
                    .setSql("favorite_count = favorite_count - 1"));
            //update-end---author:cursor ---date:2026-08-13 for：【并发修复】收藏计数改为原子递减-----------
            return false;
        }
        RecipeFavorite fav = new RecipeFavorite();
        fav.setUserId(userId);
        fav.setRecipeId(recipeId);
        fav.setCreateTime(new Date());
        favoriteMapper.insert(fav);
        //update-begin---author:cursor ---date:2026-08-13 for：【并发修复】收藏计数改为原子递增-----------
        update(null, new LambdaUpdateWrapper<Recipe>()
                .eq(Recipe::getId, recipeId)
                .setSql("favorite_count = favorite_count + 1"));
        //update-end---author:cursor ---date:2026-08-13 for：【并发修复】收藏计数改为原子递增-----------
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

    //update-begin---author:cursor---date:2026-08-23---for:【HomeAI-R114】收藏列表可选分页---
    @Override
    public IPage<Recipe> pageFavoriteRecipes(String userId, String familyId, int pageNo, int pageSize) {
        pageNo = Math.max(pageNo, 1);
        pageSize = Math.min(Math.max(pageSize, 1), 100);
        LambdaQueryWrapper<RecipeFavorite> fq = new LambdaQueryWrapper<>();
        fq.eq(RecipeFavorite::getUserId, userId).orderByDesc(RecipeFavorite::getCreateTime);
        IPage<RecipeFavorite> favPage = favoriteMapper.selectPage(new Page<>(pageNo, pageSize), fq);
        IPage<Recipe> result = new Page<>(pageNo, pageSize, favPage.getTotal());
        if (favPage.getRecords() == null || favPage.getRecords().isEmpty()) {
            result.setRecords(Collections.emptyList());
            return result;
        }
        List<String> ids = favPage.getRecords().stream().map(RecipeFavorite::getRecipeId).toList();
        LambdaQueryWrapper<Recipe> q = new LambdaQueryWrapper<>();
        q.in(Recipe::getId, ids).eq(Recipe::getDelFlag, 0);
        applyClientVisibilityFilter(q, userId, familyId);
        List<Recipe> recipes = list(q);
        Map<String, Recipe> map = new LinkedHashMap<>();
        for (Recipe r : recipes) {
            map.put(r.getId(), r);
        }
        List<Recipe> ordered = new ArrayList<>();
        for (RecipeFavorite fav : favPage.getRecords()) {
            Recipe r = map.get(fav.getRecipeId());
            if (r != null) {
                ordered.add(r);
            }
        }
        result.setRecords(ordered);
        return result;
    }
    //update-end---author:cursor---date:2026-08-23---for:【HomeAI-R114】收藏列表可选分页---

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

    private static final int SEARCH_MAX_ROWS = 50;

    @Override
    public List<Recipe> search(String keyword) {
        LambdaQueryWrapper<Recipe> q = new LambdaQueryWrapper<>();
        q.like(Recipe::getName, keyword).eq(Recipe::getDelFlag, 0).orderByDesc(Recipe::getCreateTime);
        return page(new Page<>(1, SEARCH_MAX_ROWS), q).getRecords();
    }

    @Override
    public List<Recipe> searchVisible(String keyword, String userId, String familyId) {
        LambdaQueryWrapper<Recipe> q = new LambdaQueryWrapper<>();
        q.like(Recipe::getName, keyword).eq(Recipe::getDelFlag, 0);
        applyClientVisibilityFilter(q, userId, familyId);
        q.orderByDesc(Recipe::getCreateTime);
        return page(new Page<>(1, SEARCH_MAX_ROWS), q).getRecords();
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
        //update-begin---author:cursor ---date:2026-08-13 for：【上传优化】先校验菜谱存在，避免孤儿文件；补充扩展名/大小/魔数校验-----------
        Recipe recipe = getById(recipeId);
        if (recipe == null) {
            throw new JeecgBootException("菜谱不存在");
        }
        validateUploadFile(file, VIDEO_EXTENSIONS, MAX_VIDEO_SIZE, "视频");
        String fileName = "video_" + System.currentTimeMillis() + "." + sanitizeExtension(file);
        String videoUrl = fileStorageService.storeMultipart(file, "homeai/recipe/" + recipeId + "/" + fileName);
        recipe.setVideoUrl(videoUrl);
        updateById(recipe);
        return videoUrl;
        //update-end---author:cursor ---date:2026-08-13 for：【上传优化】先校验菜谱存在，避免孤儿文件；补充扩展名/大小/魔数校验-----------
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
        //update-begin---author:cursor ---date:2026-08-13 for：【上传优化】先校验菜谱存在，避免孤儿文件；补充扩展名/大小/魔数校验-----------
        Recipe recipe = getById(recipeId);
        if (recipe == null) {
            throw new JeecgBootException("菜谱不存在");
        }
        validateUploadFile(file, IMAGE_EXTENSIONS, MAX_IMAGE_SIZE, "封面图片");
        String fileName = "cover_" + System.currentTimeMillis() + "." + sanitizeExtension(file);
        String coverUrl = fileStorageService.storeMultipart(file, "homeai/recipe/" + recipeId + "/" + fileName);
        recipe.setCoverUrl(coverUrl);
        updateById(recipe);
        return coverUrl;
        //update-end---author:cursor ---date:2026-08-13 for：【上传优化】先校验菜谱存在，避免孤儿文件；补充扩展名/大小/魔数校验-----------
    }

    @Override
    public String uploadStepImage(MultipartFile file) {
        //update-begin---author:cursor ---date:2026-08-13 for：【上传优化】步骤图补充扩展名/大小/魔数校验-----------
        validateUploadFile(file, IMAGE_EXTENSIONS, MAX_IMAGE_SIZE, "步骤图片");
        String fileName = System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8)
                + "." + sanitizeExtension(file);
        return fileStorageService.storeMultipart(file, "homeai/recipe/steps/" + fileName);
        //update-end---author:cursor ---date:2026-08-13 for：【上传优化】步骤图补充扩展名/大小/魔数校验-----------
    }

    @Override
    public String uploadCoverFile(MultipartFile file) {
        return saveGeneric(file, "/homeai/recipe/covers/", IMAGE_EXTENSIONS, MAX_IMAGE_SIZE, "封面图片");
    }

    //update-begin---author:cursor---date:2026-08-21---for:【菜谱封面】单张/文件夹/zip 按文件名或父目录匹配导入---
    private static final int MAX_COVER_IMPORT_FILES = 500;
    private static final int MAX_ZIP_ENTRIES = 2000;
    private static final long MAX_ZIP_UNCOMPRESSED = 80L * 1024 * 1024;

    @Override
    public Map<String, Object> importCovers(List<MultipartFile> files) {
        List<Map<String, Object>> matched = new ArrayList<>();
        List<String> unmatched = new ArrayList<>();
        List<MultipartFile> expanded = new ArrayList<>();
        if (files != null) {
            for (MultipartFile f : files) {
                if (f == null || f.isEmpty()) {
                    continue;
                }
                String original = f.getOriginalFilename();
                if (RecipeCoverMatch.isZip(original)) {
                    try {
                        expanded.addAll(expandZipImages(f, unmatched));
                    } catch (Exception ex) {
                        log.warn("封面 zip 展开失败: {}", original, ex);
                        unmatched.add((original == null ? "zip" : original) + "（解压失败）");
                    }
                } else {
                    expanded.add(f);
                }
            }
        }
        if (expanded.size() > MAX_COVER_IMPORT_FILES) {
            throw new JeecgBootException("单次最多导入 " + MAX_COVER_IMPORT_FILES + " 张图片，请拆分后重试");
        }
        if (expanded.isEmpty()) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("matched", matched);
            empty.put("unmatched", unmatched.isEmpty() ? List.of("未识别到有效图片文件") : unmatched);
            return empty;
        }

        List<Recipe> nameRows = list(new LambdaQueryWrapper<Recipe>()
                .select(Recipe::getId, Recipe::getName)
                .eq(Recipe::getDelFlag, 0)
                .isNotNull(Recipe::getName));
        Map<String, String> nameIndex = new LinkedHashMap<>();
        for (Recipe r : nameRows) {
            if (r == null || oConvertUtils.isEmpty(r.getName())) {
                continue;
            }
            nameIndex.putIfAbsent(RecipeCoverMatch.normalize(r.getName()), r.getName());
        }

        Map<String, MultipartFile> bestFile = new LinkedHashMap<>();
        Map<String, Integer> bestScore = new HashMap<>();
        for (MultipartFile f : expanded) {
            String original = f.getOriginalFilename();
            if (RecipeCoverMatch.isIgnoredPath(original) || !RecipeCoverMatch.isImage(original)) {
                unmatched.add(displayName(original) + "（非图片）");
                continue;
            }
            String recipeName = RecipeCoverMatch.matchRecipeName(original, nameIndex);
            if (recipeName == null) {
                unmatched.add(displayName(original));
                continue;
            }
            int score = RecipeCoverMatch.coverScore(original, recipeName);
            Integer prev = bestScore.get(recipeName);
            if (prev == null || score > prev) {
                bestScore.put(recipeName, score);
                bestFile.put(recipeName, f);
            }
        }

        if (!bestFile.isEmpty()) {
            List<Recipe> recipes = list(new LambdaQueryWrapper<Recipe>()
                    .in(Recipe::getName, bestFile.keySet())
                    .eq(Recipe::getDelFlag, 0));
            Map<String, List<Recipe>> byName = recipes.stream().collect(Collectors.groupingBy(Recipe::getName));
            for (Map.Entry<String, MultipartFile> e : bestFile.entrySet()) {
                List<Recipe> hit = byName.get(e.getKey());
                if (hit == null || hit.isEmpty()) {
                    unmatched.add(displayName(e.getValue().getOriginalFilename()));
                    continue;
                }
                try {
                    String coverUrl = fileStorageService.resolveAccessUrl(uploadCoverFile(e.getValue()));
                    for (Recipe r : hit) {
                        r.setCoverUrl(coverUrl);
                        updateById(r);
                    }
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("fileName", displayName(e.getValue().getOriginalFilename()));
                    row.put("recipeName", e.getKey());
                    row.put("count", hit.size());
                    row.put("coverUrl", coverUrl);
                    matched.add(row);
                } catch (Exception ex) {
                    log.warn("封面导入失败: {}", e.getValue().getOriginalFilename(), ex);
                    unmatched.add(displayName(e.getValue().getOriginalFilename()) + "（上传失败）");
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("matched", matched);
        result.put("unmatched", unmatched);
        return result;
    }

    private List<MultipartFile> expandZipImages(MultipartFile zipFile, List<String> unmatched) throws IOException {
        if (zipFile.getSize() > MAX_IMAGE_SIZE) {
            throw new JeecgBootException("压缩包大小不能超过 10MB，较大目录请直接选择文件夹");
        }
        HomeaiFileMagicUtil.validate(zipFile, "zip");
        List<MultipartFile> images = new ArrayList<>();
        long uncompressed = 0;
        int entries = 0;
        try (InputStream in = zipFile.getInputStream(); ZipInputStream zis = new ZipInputStream(in)) {
            ZipEntry entry;
            byte[] buf = new byte[8192];
            while ((entry = zis.getNextEntry()) != null) {
                entries++;
                if (entries > MAX_ZIP_ENTRIES) {
                    throw new JeecgBootException("压缩包内文件过多");
                }
                String name = entry.getName();
                if (entry.isDirectory() || RecipeCoverMatch.isIgnoredPath(name)) {
                    continue;
                }
                String path = RecipeCoverMatch.originalPath(name);
                if (path.contains("..")) {
                    throw new JeecgBootException("压缩包路径不合法");
                }
                if (!RecipeCoverMatch.isImage(path)) {
                    continue;
                }
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                int n;
                boolean skip = false;
                while ((n = zis.read(buf)) > 0) {
                    uncompressed += n;
                    if (uncompressed > MAX_ZIP_UNCOMPRESSED) {
                        throw new JeecgBootException("压缩包解压后过大");
                    }
                    if (out.size() + n > MAX_IMAGE_SIZE) {
                        unmatched.add(displayName(path) + "（超过 10MB）");
                        skip = true;
                        break;
                    }
                    out.write(buf, 0, n);
                }
                if (skip || out.size() == 0) {
                    continue;
                }
                String ext = RecipeCoverMatch.extension(path);
                images.add(new InMemoryMultipartFile(path, "image/" + ("jpg".equals(ext) ? "jpeg" : ext), out.toByteArray()));
            }
        }
        if (images.isEmpty()) {
            unmatched.add(displayName(zipFile.getOriginalFilename()) + "（压缩包内无图片）");
        }
        return images;
    }

    private String displayName(String originalFilename) {
        String path = RecipeCoverMatch.originalPath(originalFilename);
        return oConvertUtils.isEmpty(path) ? "未命名" : path;
    }
    //update-end---author:cursor---date:2026-08-21---for:【菜谱封面】单张/文件夹/zip 按文件名或父目录匹配导入---

    @Override
    public String uploadVideoFile(MultipartFile file) {
        return saveGeneric(file, "/homeai/recipe/videos/", VIDEO_EXTENSIONS, MAX_VIDEO_SIZE, "视频");
    }

    private String saveGeneric(MultipartFile file, String relativeDir,
                               Set<String> allowedExts, long maxSize, String label) {
        //update-begin---author:cursor ---date:2026-08-13 for：【上传优化】通用保存补充扩展名/大小/魔数校验-----------
        validateUploadFile(file, allowedExts, maxSize, label);
        String fileName = System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8)
                + "." + sanitizeExtension(file);
        String objectKey = relativeDir.replaceFirst("^/+", "") + fileName;
        return fileStorageService.storeMultipart(file, objectKey);
        //update-end---author:cursor ---date:2026-08-13 for：【上传优化】通用保存补充扩展名/大小/魔数校验-----------
    }

    /** 菜谱上传允许的图片扩展名 */
    private static final Set<String> IMAGE_EXTENSIONS = new HashSet<>(Arrays.asList(
            "jpg", "jpeg", "png", "gif", "webp", "bmp"
    ));
    /** 菜谱上传允许的视频扩展名 */
    private static final Set<String> VIDEO_EXTENSIONS = new HashSet<>(Arrays.asList(
            "mp4", "mov", "m4v", "webm", "avi", "mkv"
    ));
    /** 图片大小上限（10MB） */
    private static final long MAX_IMAGE_SIZE = 10L * 1024 * 1024;
    /** 视频大小上限（200MB） */
    private static final long MAX_VIDEO_SIZE = 200L * 1024 * 1024;

    /** 从原始文件名中提取安全扩展名（仅保留字母数字，防路径穿越/任意字符） */
    private String sanitizeExtension(MultipartFile file) {
        String original = file.getOriginalFilename();
        String ext = original == null ? "" : original.substring(original.lastIndexOf('.') + 1);
        ext = ext.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        return ext.length() > 8 ? ext.substring(0, 8) : ext;
    }

    /** 上传前统一校验：非空 + 大小 + 扩展名白名单 + 魔数 */
    private void validateUploadFile(MultipartFile file, Set<String> allowedExts, long maxSize, String label) {
        if (file == null || file.isEmpty()) {
            throw new JeecgBootException("请选择要上传的文件");
        }
        if (file.getSize() > maxSize) {
            throw new JeecgBootException(label + "大小不能超过 " + (maxSize / 1024 / 1024) + "MB");
        }
        String ext = sanitizeExtension(file);
        if (oConvertUtils.isEmpty(ext) || !allowedExts.contains(ext)) {
            throw new JeecgBootException("不支持的" + label + "格式，仅支持: " + String.join("/", allowedExts));
        }
        //update-begin---author:cursor---date:2026-08-22---for:【审查D】菜谱上传走全局扩展名白名单---
        if (!whitelistService.isAllowedExtension(ext)) {
            throw new JeecgBootException("不支持上传该文件类型");
        }
        //update-end---author:cursor---date:2026-08-22---for:【审查D】菜谱上传走全局扩展名白名单---
        try {
            HomeaiFileMagicUtil.validate(file, ext);
        } catch (IOException e) {
            throw new JeecgBootException(e.getMessage());
        }
    }
}
