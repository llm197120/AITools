package org.jeecg.modules.homeai.recipe.service;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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

    /** 小程序端：按可见性搜索 */
    List<Recipe> searchVisible(String keyword, String userId, String familyId);

    /** 小程序端：列表/搜索可见性过滤 */
    void applyClientVisibilityFilter(QueryWrapper<Recipe> qw, String userId, String familyId);

    void applyClientVisibilityFilter(LambdaQueryWrapper<Recipe> qw, String userId, String familyId);

    /** 小程序端：是否可查看该菜谱 */
    boolean canViewRecipe(Recipe recipe, String userId, String familyId);

    /** 小程序端：校验可见性，不可见则抛异常 */
    void assertRecipeVisible(Recipe recipe, String userId, String familyId);

    /** 创建/编辑时写入家庭 ID（visibility=family 时） */
    void applyFamilyOnSave(Recipe recipe, String userId, String familyId);

    /** 详情（含可见性校验与收藏状态） */
    Map<String, Object> getDetailWithRelations(String id, String userId, String familyId, boolean checkVisibility);

    /** 切换收藏，返回切换后是否已收藏 */
    boolean toggleFavorite(String userId, String recipeId, String familyId);

    boolean isFavorited(String userId, String recipeId);

    /** 我的收藏列表（仍受可见性约束） */
    List<Recipe> listFavoriteRecipes(String userId, String familyId);
    void saveWithRelations(Recipe recipe, List<RecipeIngredient> ingredients, List<RecipeStep> steps);

    /** 编辑菜谱（更新主表并整体替换食材/步骤） */
    void updateWithRelations(Recipe recipe, List<RecipeIngredient> ingredients, List<RecipeStep> steps);
    String uploadVideo(String recipeId, MultipartFile file);
    void deleteVideo(String recipeId);
    /** 上传菜谱封面图并更新菜谱记录 */
    String uploadCover(String recipeId, MultipartFile file);
    /** 上传烹饪步骤图片，返回图片地址 */
    String uploadStepImage(MultipartFile file);
    /** 通用上传封面图片，返回图片地址（不绑定菜谱） */
    String uploadCoverFile(MultipartFile file);
    /** 通用上传做菜视频，返回视频地址（不绑定菜谱） */
    String uploadVideoFile(MultipartFile file);
}
