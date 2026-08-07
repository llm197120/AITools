package org.jeecg.modules.homeai.recipe.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.homeai.config.service.IHomeaiFileStorageService;
import org.jeecg.modules.homeai.recipe.entity.*;
import org.jeecg.modules.homeai.recipe.mapper.RecipeFavoriteMapper;
import org.jeecg.modules.homeai.recipe.mapper.RecipeIngredientMapper;
import org.jeecg.modules.homeai.recipe.mapper.RecipeMapper;
import org.jeecg.modules.homeai.recipe.mapper.RecipeStepMapper;
import org.jeecg.modules.homeai.recipe.service.IRecipeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;

@Slf4j
@Service
public class RecipeServiceImpl extends ServiceImpl<RecipeMapper, Recipe> implements IRecipeService {
    @Autowired private RecipeIngredientMapper ingredientMapper;
    @Autowired private RecipeStepMapper stepMapper;
    @Autowired private RecipeFavoriteMapper favoriteMapper;
    @Autowired private IHomeaiFileStorageService fileStorageService;

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

    @Override
    public boolean canViewRecipe(Recipe recipe, String userId, String familyId) {
        if (recipe == null || oConvertUtils.isEmpty(userId)) {
            return false;
        }
        if (userId.equals(recipe.getUserId())) {
            return true;
        }
        return "family".equals(recipe.getVisibility())
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
        if ("family".equals(recipe.getVisibility())) {
            if (oConvertUtils.isEmpty(familyId)) {
                throw new JeecgBootException("加入家庭后才能共享菜谱");
            }
            recipe.setFamilyId(familyId);
        } else {
            recipe.setFamilyId(null);
        }
    }

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
        qw.and(w -> {
            w.eq("user_id", userId);
            if (oConvertUtils.isNotEmpty(familyId)) {
                w.or(n -> n.eq("visibility", "family").eq("family_id", familyId));
            }
        });
    }

    @Override
    public void applyClientVisibilityFilter(LambdaQueryWrapper<Recipe> qw, String userId, String familyId) {
        if (oConvertUtils.isEmpty(userId)) {
            qw.eq(Recipe::getUserId, "__none__");
            return;
        }
        qw.and(w -> {
            w.eq(Recipe::getUserId, userId);
            if (oConvertUtils.isNotEmpty(familyId)) {
                w.or(n -> n.eq(Recipe::getVisibility, "family").eq(Recipe::getFamilyId, familyId));
            }
        });
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
