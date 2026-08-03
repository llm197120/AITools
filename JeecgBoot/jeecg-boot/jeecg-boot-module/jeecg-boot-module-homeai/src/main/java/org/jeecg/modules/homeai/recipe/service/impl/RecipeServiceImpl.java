package org.jeecg.modules.homeai.recipe.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.homeai.config.HomeaiFileUrlUtil;
import org.jeecg.modules.homeai.recipe.entity.*;
import org.jeecg.modules.homeai.recipe.mapper.RecipeIngredientMapper;
import org.jeecg.modules.homeai.recipe.mapper.RecipeMapper;
import org.jeecg.modules.homeai.recipe.mapper.RecipeStepMapper;
import org.jeecg.modules.homeai.recipe.service.IRecipeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@Slf4j
@Service
public class RecipeServiceImpl extends ServiceImpl<RecipeMapper, Recipe> implements IRecipeService {
    @Autowired private RecipeIngredientMapper ingredientMapper;
    @Autowired private RecipeStepMapper stepMapper;

    @Value("${jeecg.path.upload:./upload}")
    private String uploadPath;

    @Override
    public Recipe getDetail(String id) {
        return getById(id);
    }

    @Override
    public Map<String, Object> getDetailWithRelations(String id) {
        Recipe recipe = getById(id);
        Map<String, Object> result = new HashMap<>();
        result.put("recipe", recipe);
        result.put("ingredients", getIngredients(id));
        result.put("steps", getSteps(id));
        return result;
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
    public String uploadVideo(String recipeId, MultipartFile file) {
        try {
            String dir = uploadPath + "/homeai/recipe/" + recipeId + "/";
            Files.createDirectories(Path.of(dir));
            String ext = getExtension(file.getOriginalFilename());
            String fileName = "video_" + System.currentTimeMillis() + (ext != null ? "." + ext : ".mp4");
            Path targetPath = Path.of(dir + fileName);
            file.transferTo(targetPath.toFile());
            String videoUrl = HomeaiFileUrlUtil.toAbsoluteUrl("/upload/homeai/recipe/" + recipeId + "/" + fileName);
            Recipe recipe = getById(recipeId);
            if (recipe != null) {
                recipe.setVideoUrl(videoUrl);
                updateById(recipe);
            }
            return videoUrl;
        } catch (IOException e) {
            log.error("菜谱视频上传失败", e);
            throw new RuntimeException("视频上传失败", e);
        }
    }

    @Override
    public void deleteVideo(String recipeId) {
        Recipe recipe = getById(recipeId);
        if (recipe != null && recipe.getVideoUrl() != null) {
            try {
                // videoUrl 可能是绝对地址或相对地址，统一提取相对路径定位物理文件
                String relativeUrl = HomeaiFileUrlUtil.toRelativeUrl(recipe.getVideoUrl());
                String filePath = uploadPath + relativeUrl.replace("/upload", "");
                Files.deleteIfExists(Path.of(filePath));
            } catch (IOException e) {
                log.warn("删除视频文件失败", e);
            }
            recipe.setVideoUrl(null);
            updateById(recipe);
        }
    }

    private String getExtension(String filename) {
        if (filename == null) return null;
        int idx = filename.lastIndexOf('.');
        return idx >= 0 ? filename.substring(idx + 1) : null;
    }
}
