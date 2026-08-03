package org.jeecg.modules.homeai.recipe.service;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.homeai.recipe.entity.Recipe;
import org.jeecg.modules.homeai.recipe.entity.RecipeIngredient;
import org.jeecg.modules.homeai.recipe.entity.RecipeStep;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

public interface IRecipeService extends IService<Recipe> {
    Recipe getDetail(String id);
    /** 菜谱详情（含食材、步骤） */
    Map<String, Object> getDetailWithRelations(String id);
    /** 根据菜谱 ID 查询食材 */
    List<RecipeIngredient> getIngredients(String recipeId);
    /** 根据菜谱 ID 查询步骤 */
    List<RecipeStep> getSteps(String recipeId);
    List<Recipe> search(String keyword);
    void saveWithRelations(Recipe recipe, List<RecipeIngredient> ingredients, List<RecipeStep> steps);
    String uploadVideo(String recipeId, MultipartFile file);
    void deleteVideo(String recipeId);
}
