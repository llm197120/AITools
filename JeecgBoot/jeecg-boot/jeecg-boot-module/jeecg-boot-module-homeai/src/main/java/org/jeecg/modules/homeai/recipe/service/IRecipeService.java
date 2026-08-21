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

    /** 管理端保存：规范化可见性；家庭共享时必须带 familyId */
    void applyAdminVisibilityOnSave(Recipe recipe);

    /** 详情（含可见性校验与收藏状态） */
    Map<String, Object> getDetailWithRelations(String id, String userId, String familyId, boolean checkVisibility);

    /** 切换收藏，返回切换后是否已收藏 */
    boolean toggleFavorite(String userId, String recipeId, String familyId);

    boolean isFavorited(String userId, String recipeId);

    /** 我的收藏列表（仍受可见性约束） */
    List<Recipe> listFavoriteRecipes(String userId, String familyId);

    //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R26】浏览计数 + 热门排行-----------
    /** 浏览计数 +1（详情可见后调用） */
    void incrementViewCount(String recipeId);

    /** 热门菜谱（按 view_count 降序，受可见性约束） */
    List<Recipe> listHotRecipes(String userId, String familyId, int limit);

    //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R28】轻量推荐-----------
    /**
     * 推荐菜谱：今日计划 > 我的收藏 > 家庭收藏 > 加权热门（含季节分类加权）
     * @return 每项含 recipe 字段 + reason
     */
    List<Map<String, Object>> listRecommendRecipes(String userId, String familyId, int limit, String season);
    //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R28】轻量推荐-----------

    //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R30】新菜尝鲜-----------
    /**
     * 新菜尝鲜：近 days 日新增（按 createTime），优先排除用户近期计划做过的菜
     */
    List<Recipe> listNewRecipes(String userId, String familyId, int limit, int days);
    //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R30】新菜尝鲜-----------
    //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R26】浏览计数 + 热门排行-----------
    void saveWithRelations(Recipe recipe, List<RecipeIngredient> ingredients, List<RecipeStep> steps);

    /** 编辑菜谱（更新主表并整体替换食材/步骤） */
    void updateWithRelations(Recipe recipe, List<RecipeIngredient> ingredients, List<RecipeStep> steps);

    //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R24】Excel 含子表导入导出-----------
    /** 从 Excel 文本列解析食材 */
    List<RecipeIngredient> parseIngredientsFromExcel(String text);

    /** 从 Excel 文本列解析步骤 */
    List<RecipeStep> parseStepsFromExcel(String text);

    /** 导出前把子表填回 Excel 文本列 */
    void fillExcelRelationText(Recipe recipe);
    //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R24】Excel 含子表导入导出-----------

    String uploadVideo(String recipeId, MultipartFile file);
    void deleteVideo(String recipeId);
    /** 上传菜谱封面图并更新菜谱记录 */
    String uploadCover(String recipeId, MultipartFile file);
    /** 上传烹饪步骤图片，返回图片地址 */
    String uploadStepImage(MultipartFile file);
    /** 通用上传封面图片，返回图片地址（不绑定菜谱） */
    String uploadCoverFile(MultipartFile file);

    //update-begin---author:cursor---date:2026-08-21---for:【菜谱封面】单张/文件夹/zip 按文件名或父目录匹配导入---
    /**
     * 批量导入封面。按文件名或父目录名匹配菜谱名称；zip 会展开后同样匹配。
     * 返回 { matched: [...], unmatched: [...] }
     */
    Map<String, Object> importCovers(List<MultipartFile> files);
    //update-end---author:cursor---date:2026-08-21---for:【菜谱封面】单张/文件夹/zip 按文件名或父目录匹配导入---
    /** 通用上传做菜视频，返回视频地址（不绑定菜谱） */
    String uploadVideoFile(MultipartFile file);

    //update-begin---author:cursor ---date:2026-08-13 for：【P0 双通道一致性】家庭成员可维护家庭菜谱（console 建的菜谱 userId 为空，家庭成员可编辑/删除）-----------
    /** 是否有权修改菜谱：创建者本人，或家庭共享菜谱的家庭成员 */
    boolean canModifyRecipe(Recipe recipe, String userId, String familyId);
    //update-end---author:cursor ---date:2026-08-13 for：【P0 双通道一致性】家庭成员可维护家庭菜谱-----------
}
