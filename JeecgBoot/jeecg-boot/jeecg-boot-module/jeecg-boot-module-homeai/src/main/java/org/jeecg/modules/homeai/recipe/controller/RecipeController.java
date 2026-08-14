package org.jeecg.modules.homeai.recipe.controller;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.common.util.oConvertUtils;
import io.swagger.v3.oas.annotations.Operation;
import org.jeecg.modules.homeai.audit.service.IHomeaiAuditLogService;
import org.jeecg.modules.homeai.config.service.IHomeaiFileStorageService;
import org.jeecg.modules.homeai.config.HomeaiJwtUtil;
import org.jeecg.modules.homeai.config.HomeaiSecurityUtil;
import org.jeecg.modules.homeai.recipe.entity.Recipe;
import org.jeecg.modules.homeai.recipe.entity.RecipeIngredient;
import org.jeecg.modules.homeai.recipe.entity.RecipeStep;
import org.jeecg.modules.homeai.recipe.mapper.RecipeMapper;
import org.jeecg.modules.homeai.recipe.service.IRecipeCategoryService;
import org.jeecg.modules.homeai.recipe.service.IRecipeService;
import org.jeecg.modules.homeai.user.entity.WxUser;
import org.jeecg.modules.homeai.user.service.IWxUserService;
import org.jeecgframework.poi.excel.def.NormalExcelConstants;
import org.jeecgframework.poi.excel.entity.ExportParams;
import org.jeecgframework.poi.excel.entity.enmus.ExcelType;
import org.jeecgframework.poi.excel.view.JeecgEntityExcelView;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/homeai/recipe")
public class RecipeController {
    @Autowired private IRecipeService recipeService;
    @Autowired private IRecipeCategoryService recipeCategoryService;
    @Autowired private IWxUserService wxUserService;
    @Autowired private RecipeMapper recipeMapper;
    @Autowired private HomeaiSecurityUtil securityUtil;
    @Autowired private IHomeaiFileStorageService fileStorageService;
    //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R22】菜谱审计埋点-----------
    @Autowired private IHomeaiAuditLogService auditLogService;
    //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R22】菜谱审计埋点-----------

    private String getUserId(HttpServletRequest r) {
        // 优先从Shiro认证获取（管理端）
        try {
            if (SecurityUtils.getSubject() != null && SecurityUtils.getSubject().isAuthenticated()) {
                Object principal = SecurityUtils.getSubject().getPrincipal();
                if (principal instanceof LoginUser) {
                    return ((LoginUser) principal).getId();
                }
                return principal != null ? principal.toString() : null;
            }
        } catch (Exception ignored) {}
        // 回退到HomeaiJWT认证（小程序端）
        String tk = r.getHeader("X-Access-Token");
        String o = HomeaiJwtUtil.getOpenid(tk);
        if (o == null) return null;
        var u = wxUserService.getByOpenid(o);
        return u != null ? u.getId() : null;
    }

    /**
     * 校验当前用户是否有权修改菜谱媒体（管理端可改任意；小程序端仅创建者可改）
     */
    //update-begin---author:cursor ---date:2026-08-13 for：【菜谱导入】按文件名批量导入封面图-----------
    /**
     * 批量导入菜谱封面（按文件名去扩展名匹配菜谱名称）
     * 返回：{ matched: [{fileName, recipeName, count, coverUrl}], unmatched: [文件名...] }
     */
    @PostMapping("/import-covers")
    @AutoLog(value="菜谱-批量导入封面")
    @Operation(summary="菜谱-批量导入封面(按文件名匹配)")
    @RequiresPermissions("homeai:recipe:importExcel")
    public Result<?> importCovers(HttpServletRequest request) {
        try {
            if (!(request instanceof MultipartHttpServletRequest)) {
                return Result.error("请求格式不正确，请使用multipart/form-data格式上传文件");
            }
            MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
            Map<String, MultipartFile> fileMap = multipartRequest.getFileMap();
            if (fileMap.isEmpty()) {
                return Result.error("未检测到上传文件");
            }
            // 文件名（去扩展名、去空白）→ 文件
            Map<String, MultipartFile> byName = new LinkedHashMap<>();
            for (MultipartFile f : fileMap.values()) {
                if (f == null || f.isEmpty()) {
                    continue;
                }
                String original = f.getOriginalFilename();
                String base = original == null ? "" : original.substring(0, original.lastIndexOf('.') < 0 ? original.length() : original.lastIndexOf('.')).trim();
                if (oConvertUtils.isEmpty(base)) {
                    continue;
                }
                byName.putIfAbsent(base, f);
            }
            if (byName.isEmpty()) {
                return Result.error("未识别到有效图片文件");
            }
            // 按名称批量匹配菜谱（同名称的菜谱都会更新为该封面）
            List<Recipe> recipes = recipeService.list(new LambdaQueryWrapper<Recipe>()
                    .in(Recipe::getName, byName.keySet())
                    .eq(Recipe::getDelFlag, 0));
            Map<String, List<Recipe>> byRecipeName = recipes.stream()
                    .collect(Collectors.groupingBy(Recipe::getName));
            List<Map<String, Object>> matched = new ArrayList<>();
            List<String> unmatched = new ArrayList<>();
            for (Map.Entry<String, MultipartFile> e : byName.entrySet()) {
                List<Recipe> hit = byRecipeName.get(e.getKey());
                if (hit == null || hit.isEmpty()) {
                    unmatched.add(e.getValue().getOriginalFilename());
                    continue;
                }
                try {
                    String coverUrl = fileStorageService.resolveAccessUrl(recipeService.uploadCoverFile(e.getValue()));
                    for (Recipe r : hit) {
                        r.setCoverUrl(coverUrl);
                        recipeService.updateById(r);
                    }
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("fileName", e.getValue().getOriginalFilename());
                    row.put("recipeName", e.getKey());
                    row.put("count", hit.size());
                    row.put("coverUrl", coverUrl);
                    matched.add(row);
                } catch (Exception ex) {
                    log.warn("封面导入失败: {}", e.getValue().getOriginalFilename(), ex);
                    unmatched.add(e.getValue().getOriginalFilename() + "（上传失败）");
                }
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("matched", matched);
            result.put("unmatched", unmatched);
            return Result.OK(result);
        } catch (Exception e) {
            log.error("封面批量导入异常", e);
            return Result.error("封面导入失败: " + e.getMessage());
        }
    }
    //update-end---author:cursor ---date:2026-08-13 for：【菜谱导入】按文件名批量导入封面图-----------

    //update-begin---author:cursor ---date:2026-08-13 for：【IDOR修复】菜谱媒体归属校验-----------
    private Result<?> checkRecipeOwner(String recipeId, HttpServletRequest req) {
        Recipe existing = recipeService.getById(recipeId);
        if (existing == null) {
            return Result.error("菜谱不存在");
        }
        if (securityUtil.isConsoleAuthenticated(req)) {
            return null;
        }
        String uid = getUserId(req);
        if (uid == null) {
            return Result.error("未登录");
        }
        WxUser user = wxUserService.getById(uid);
        if (!recipeService.canModifyRecipe(existing, uid, user != null ? user.getFamilyId() : null)) {
            return Result.error("无权修改该菜谱");
        }
        return null;
    }
    //update-end---author:cursor ---date:2026-08-13 for：【IDOR修复】菜谱媒体归属校验-----------

    @GetMapping("/list")
    @Operation(summary="菜谱-分页列表查询")
    public Result<?> list(Recipe r, @RequestParam(defaultValue = "1") int pageNo, @RequestParam(defaultValue = "10") int pageSize, HttpServletRequest req) {
        QueryWrapper<Recipe> qw = QueryGenerator.initQueryWrapper(r, req.getParameterMap());
        qw.eq("del_flag", "0").orderByDesc("create_time");
        if (!securityUtil.isConsoleAuthenticated(req)) {
            String uid = getUserId(req);
            if (uid == null) {
                return Result.error("未登录");
            }
            WxUser user = wxUserService.getById(uid);
            recipeService.applyClientVisibilityFilter(qw, uid, user != null ? user.getFamilyId() : null);
        }
        IPage<Recipe> result = recipeService.page(new Page<>(pageNo, pageSize), qw);
        // 兼容历史相对地址数据：统一转换为绝对访问地址
        if (result.getRecords() != null) {
            for (Recipe item : result.getRecords()) {
                resolveRecipeUrls(item);
            }
        }
        return Result.OK(result);
    }

    @GetMapping("/search")
    public Result<?> search(@RequestParam String keyword, HttpServletRequest req) {
        List<Recipe> list;
        if (securityUtil.isConsoleAuthenticated(req)) {
            list = recipeService.search(keyword);
        } else {
            String uid = getUserId(req);
            if (uid == null) {
                return Result.error("未登录");
            }
            WxUser user = wxUserService.getById(uid);
            list = recipeService.searchVisible(keyword, uid, user != null ? user.getFamilyId() : null);
        }
        if (list != null) {
            for (Recipe item : list) {
                resolveRecipeUrls(item);
            }
        }
        return Result.OK(list);
    }

    @GetMapping("/favorites")
    @Operation(summary = "菜谱-我的收藏")
    public Result<?> favorites(HttpServletRequest req) {
        String uid = getUserId(req);
        if (uid == null) return Result.error("未登录");
        WxUser user = wxUserService.getById(uid);
        List<Recipe> list = recipeService.listFavoriteRecipes(uid, user != null ? user.getFamilyId() : null);
        if (list != null) {
            for (Recipe item : list) {
                resolveRecipeUrls(item);
            }
        }
        return Result.OK(list);
    }

    //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R26】热门菜谱-----------
    @GetMapping("/hot")
    @Operation(summary = "菜谱-热门排行")
    public Result<?> hot(@RequestParam(defaultValue = "20") int limit, HttpServletRequest req) {
        String uid = getUserId(req);
        if (uid == null) return Result.error("未登录");
        WxUser user = wxUserService.getById(uid);
        List<Recipe> list = recipeService.listHotRecipes(uid, user != null ? user.getFamilyId() : null, limit);
        if (list != null) {
            for (Recipe item : list) {
                resolveRecipeUrls(item);
            }
        }
        return Result.OK(list);
    }
    //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R26】热门菜谱-----------

    //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R28】轻量推荐-----------
    @GetMapping("/recommend")
    @Operation(summary = "菜谱-为你推荐")
    public Result<?> recommend(@RequestParam(defaultValue = "8") int limit,
                               @RequestParam(defaultValue = "auto") String season,
                               HttpServletRequest req) {
        String uid = getUserId(req);
        if (uid == null) return Result.error("未登录");
        WxUser user = wxUserService.getById(uid);
        List<Map<String, Object>> list = recipeService.listRecommendRecipes(
                uid, user != null ? user.getFamilyId() : null, limit, season);
        if (list != null) {
            for (Map<String, Object> item : list) {
                Object cover = item.get("coverUrl");
                if (cover != null && !String.valueOf(cover).startsWith("data:")) {
                    item.put("coverUrl", fileStorageService.resolveAccessUrl(String.valueOf(cover)));
                }
            }
        }
        return Result.OK(list);
    }
    //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R28】轻量推荐-----------

    //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R30】新菜尝鲜-----------
    @GetMapping("/new")
    @Operation(summary = "菜谱-新菜尝鲜")
    public Result<?> newest(@RequestParam(defaultValue = "8") int limit,
                            @RequestParam(defaultValue = "30") int days,
                            HttpServletRequest req) {
        String uid = getUserId(req);
        if (uid == null) return Result.error("未登录");
        WxUser user = wxUserService.getById(uid);
        List<Recipe> list = recipeService.listNewRecipes(
                uid, user != null ? user.getFamilyId() : null, limit, days);
        if (list != null) {
            for (Recipe item : list) {
                resolveRecipeUrls(item);
            }
        }
        return Result.OK(list);
    }
    //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R30】新菜尝鲜-----------

    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable String id, HttpServletRequest req) {
        try {
            if (securityUtil.isConsoleAuthenticated(req)) {
                Map<String, Object> detail = recipeService.getDetailWithRelations(id);
                resolveRecipeDetail(detail);
                return Result.OK(detail);
            }
            String uid = getUserId(req);
            if (uid == null) return Result.error("未登录");
            WxUser user = wxUserService.getById(uid);
            Map<String, Object> detail = recipeService.getDetailWithRelations(
                    id, uid, user != null ? user.getFamilyId() : null, true);
            resolveRecipeDetail(detail);
            return Result.OK(detail);
        } catch (JeecgBootException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/{id}/favorite")
    @Operation(summary = "菜谱-切换收藏")
    public Result<?> toggleFavorite(@PathVariable String id, HttpServletRequest req) {
        String uid = getUserId(req);
        if (uid == null) return Result.error("未登录");
        try {
            WxUser user = wxUserService.getById(uid);
            boolean favorited = recipeService.toggleFavorite(uid, id, user != null ? user.getFamilyId() : null);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("favorited", favorited);
            return Result.OK(result);
        } catch (JeecgBootException e) {
            return Result.error(e.getMessage());
        }
    }

    /** 兼容历史相对地址数据：转换 coverUrl / videoUrl 为绝对访问地址 */
    private void resolveRecipeUrls(Recipe recipe) {
        if (recipe == null) return;
        if (recipe.getCoverUrl() != null && !recipe.getCoverUrl().startsWith("data:")) {
            recipe.setCoverUrl(fileStorageService.resolveAccessUrl(recipe.getCoverUrl()));
        }
        if (recipe.getVideoUrl() != null) {
            recipe.setVideoUrl(fileStorageService.resolveAccessUrl(recipe.getVideoUrl()));
        }
    }

    @SuppressWarnings("unchecked")
    private void resolveRecipeDetail(Map<String, Object> detail) {
        if (detail == null) return;
        resolveRecipeUrls((Recipe) detail.get("recipe"));
        List<RecipeStep> steps = (List<RecipeStep>) detail.get("steps");
        if (steps != null) {
            for (RecipeStep step : steps) {
                if (step.getImageUrl() != null) {
                    step.setImageUrl(fileStorageService.resolveAccessUrl(step.getImageUrl()));
                }
            }
        }
    }

    private Result<?> validateRecipeCategory(Recipe recipe) {
        try {
            recipeCategoryService.validateCategoryId(recipe.getCategoryId());
            return null;
        } catch (JeecgBootException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping
    public Result<?> create(@RequestBody Map<String, Object> body, HttpServletRequest r) {
        String uid = getUserId(r);
        if (uid == null) return Result.error("未登录");
        Recipe recipe = JSON.parseObject(JSON.toJSONString(body), Recipe.class);
        Result<?> categoryError = validateRecipeCategory(recipe);
        if (categoryError != null) return categoryError;
        recipe.setUserId(uid);
        WxUser user = wxUserService.getById(uid);
        try {
            recipeService.applyFamilyOnSave(recipe, uid, user != null ? user.getFamilyId() : null);
        } catch (JeecgBootException e) {
            return Result.error(e.getMessage());
        }
        List<RecipeIngredient> ingredients = JSON.parseArray(
                JSON.toJSONString(body.get("ingredients")), RecipeIngredient.class);
        List<RecipeStep> steps = JSON.parseArray(
                JSON.toJSONString(body.get("steps")), RecipeStep.class);
        recipeService.saveWithRelations(recipe, ingredients, steps);
        return Result.OK(recipe);
    }

    @PutMapping
    public Result<?> edit(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        Recipe r = JSON.parseObject(JSON.toJSONString(body), Recipe.class);
        if (r == null || r.getId() == null) return Result.error("参数异常");
        Result<?> categoryError = validateRecipeCategory(r);
        if (categoryError != null) return categoryError;
        Recipe existing = recipeService.getById(r.getId());
        if (existing == null) return Result.error("菜谱不存在");
        // 管理端控制台可编辑任意菜谱；小程序端只能编辑自己的或家庭共享菜谱
        if (!securityUtil.isConsoleAuthenticated(req)) {
            String uid = getUserId(req);
            if (uid == null) return Result.error("未登录");
            WxUser user = wxUserService.getById(uid);
            if (!recipeService.canModifyRecipe(existing, uid, user != null ? user.getFamilyId() : null)) {
                return Result.error("无权编辑该菜谱");
            }
            try {
                recipeService.applyFamilyOnSave(r, uid, user != null ? user.getFamilyId() : null);
            } catch (JeecgBootException e) {
                return Result.error(e.getMessage());
            }
        }
        List<RecipeIngredient> ingredients = JSON.parseArray(
                JSON.toJSONString(body.get("ingredients")), RecipeIngredient.class);
        List<RecipeStep> steps = JSON.parseArray(
                JSON.toJSONString(body.get("steps")), RecipeStep.class);
        recipeService.updateWithRelations(r, ingredients, steps);
        return Result.OK("OK");
    }
    @DeleteMapping("/{id}") public Result<?> delete(@PathVariable String id, HttpServletRequest req) {
        Recipe existing = recipeService.getById(id);
        if (existing == null) return Result.error("菜谱不存在");
        if (!securityUtil.isConsoleAuthenticated(req)) {
            String uid = getUserId(req);
            if (uid == null) return Result.error("未登录");
            WxUser user = wxUserService.getById(uid);
            if (!recipeService.canModifyRecipe(existing, uid, user != null ? user.getFamilyId() : null)) {
                return Result.error("无权删除该菜谱");
            }
        }
        recipeService.removeById(id);
        return Result.OK("OK");
    }

    //update-begin---author:admin ---date:2026-07-30  for：菜谱管理-新增/导入/导出/回收站功能-----------
    /**
     * 新增菜谱（管理端）
     */
    @PostMapping("/add")
    @AutoLog(value="菜谱-新增(管理端)")
    @Operation(summary="菜谱-新增(管理端)")
    @RequiresPermissions("homeai:recipe:add")
    public Result<?> add(@RequestBody Map<String, Object> body) {
        Recipe recipe = JSON.parseObject(JSON.toJSONString(body), Recipe.class);
        Result<?> categoryError = validateRecipeCategory(recipe);
        if (categoryError != null) return categoryError;
        //update-begin---author:cursor ---date:2026-08-12 for：【菜谱可见性】管理端新增写入可见性/家庭-----------
        try {
            recipeService.applyAdminVisibilityOnSave(recipe);
        } catch (JeecgBootException e) {
            return Result.error(e.getMessage());
        }
        //update-end---author:cursor ---date:2026-08-12 for：【菜谱可见性】管理端新增写入可见性/家庭-----------
        recipe.setDelFlag(0);
        List<RecipeIngredient> ingredients = JSON.parseArray(
                JSON.toJSONString(body.get("ingredients")), RecipeIngredient.class);
        List<RecipeStep> steps = JSON.parseArray(
                JSON.toJSONString(body.get("steps")), RecipeStep.class);
        recipeService.saveWithRelations(recipe, ingredients, steps);
        return Result.OK(recipe);
    }

    /**
     * 导出Excel
     */
    @GetMapping("/exportXls")
    @Operation(summary="菜谱-导出Excel")
    @RequiresPermissions("homeai:recipe:exportXls")
    public ModelAndView exportXls(HttpServletRequest request, Recipe recipe) {
        QueryWrapper<Recipe> queryWrapper = QueryGenerator.initQueryWrapper(recipe, request.getParameterMap());
        List<Recipe> pageList = recipeService.list(queryWrapper);
        //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R24】导出含食材/步骤文本-----------
        for (Recipe r : pageList) {
            recipeService.fillExcelRelationText(r);
        }
        //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R24】导出含食材/步骤文本-----------
        ModelAndView mv = new ModelAndView(new JeecgEntityExcelView());
        String selections = request.getParameter("selections");
        if (oConvertUtils.isEmpty(selections)) {
            mv.addObject(NormalExcelConstants.DATA_LIST, pageList);
        } else {
            List<String> selectionList = Arrays.asList(selections.split(","));
            List<Recipe> exportList = pageList.stream().filter(item -> selectionList.contains(item.getId())).collect(Collectors.toList());
            mv.addObject(NormalExcelConstants.DATA_LIST, exportList);
        }
        mv.addObject(NormalExcelConstants.FILE_NAME, "菜谱列表");
        mv.addObject(NormalExcelConstants.CLASS, Recipe.class);
        LoginUser user = null;
        try {
            Object principal = SecurityUtils.getSubject().getPrincipal();
            if (principal instanceof LoginUser) {
                user = (LoginUser) principal;
            }
        } catch (Exception ignored) {}
        mv.addObject(NormalExcelConstants.PARAMS, new ExportParams("菜谱列表数据", "导出人:" + (user != null ? user.getRealname() : "系统"), "导出信息", ExcelType.XSSF));
        return mv;
    }

    //update-begin---author:admin ---date:2026-08-13 for：【HomeAI-R24】菜谱导入模板与 @Excel 列对齐-----------
    @GetMapping("/exportTemplate")
    @Operation(summary = "菜谱-导出导入模板")
    @RequiresPermissions("homeai:recipe:exportXls")
    public ModelAndView exportTemplate() {
        ModelAndView mv = new ModelAndView(new JeecgEntityExcelView());
        mv.addObject(NormalExcelConstants.DATA_LIST, new ArrayList<Recipe>());
        mv.addObject(NormalExcelConstants.FILE_NAME, "菜谱导入模板");
        mv.addObject(NormalExcelConstants.CLASS, Recipe.class);
        mv.addObject(NormalExcelConstants.PARAMS, new ExportParams("菜谱导入模板", "模板", "导入模板", ExcelType.XSSF));
        return mv;
    }
    //update-end---author:admin ---date:2026-08-13 for：【HomeAI-R24】菜谱导入模板与 @Excel 列对齐-----------

    /**
     * 导入Excel
     */
    @PostMapping("/importExcel")
    @AutoLog(value="菜谱-导入Excel")
    @Operation(summary="菜谱-导入Excel")
    @RequiresPermissions("homeai:recipe:importExcel")
    public Result<?> importExcel(HttpServletRequest request, HttpServletResponse response) {
        try {
            if (!(request instanceof MultipartHttpServletRequest)) {
                return Result.error("请求格式不正确，请使用multipart/form-data格式上传文件");
            }
            MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
            Map<String, MultipartFile> fileMap = multipartRequest.getFileMap();
            if (fileMap.isEmpty()) {
                return Result.error("未检测到上传文件");
            }
            int successLines = 0, errorLines = 0;
            List<String> errorMessage = new ArrayList<>();
            for (Map.Entry<String, MultipartFile> entity : fileMap.entrySet()) {
                MultipartFile file = entity.getValue();
                if (file.isEmpty()) continue;
                try {
                    List<Recipe> list = parseRecipeExcel(file.getInputStream());
                    for (Recipe item : list) {
                        try {
                            //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R24】Excel 含子表导入-----------
                            if (oConvertUtils.isEmpty(item.getName())) {
                                throw new JeecgBootException("菜谱名称不能为空");
                            }
                            if (oConvertUtils.isEmpty(item.getVisibility())) {
                                item.setVisibility("private");
                            }
                            //update-begin---author:cursor ---date:2026-08-13 for：【菜谱导入】封面图片地址格式校验-----------
                            if (oConvertUtils.isNotEmpty(item.getCoverUrl()) && !isValidImageUrl(item.getCoverUrl().trim())) {
                                throw new JeecgBootException("封面图片地址格式不正确（仅支持 http/https 或 /upload 相对地址）: " + item.getCoverUrl());
                            }
                            //update-end---author:cursor ---date:2026-08-13 for：【菜谱导入】封面图片地址格式校验-----------
                            item.setDelFlag(0);
                            item.setId(null);
                            List<RecipeIngredient> ingredients = recipeService.parseIngredientsFromExcel(item.getIngredients());
                            List<RecipeStep> steps = recipeService.parseStepsFromExcel(item.getSteps());
                            // 清空文本列，避免被当成主表字段误解
                            item.setIngredients(null);
                            item.setSteps(null);
                            recipeService.applyAdminVisibilityOnSave(item);
                            recipeService.saveWithRelations(item, ingredients, steps);
                            //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R24】Excel 含子表导入-----------
                            successLines++;
                        } catch (Exception ex) {
                            errorLines++;
                            errorMessage.add("第" + (successLines + errorLines) + "行: " + ex.getMessage());
                        }
                    }
                } catch (Exception ex) {
                    log.error("导入失败", ex);
                    return Result.error("导入失败: " + ex.getMessage());
                }
            }
            return Result.OK("导入完成！成功: " + successLines + " 条, 失败: " + errorLines + " 条");
        } catch (Exception e) {
            log.error("文件导入异常", e);
            return Result.error("文件导入失败: " + e.getMessage());
        }
    }

    /**
     * 回收站列表
     */
    @GetMapping("/recycleBin")
    @Operation(summary="菜谱-回收站列表")
    @RequiresPermissions("homeai:recipe:moveToRecycleBin")
    public Result<?> recycleBin(Recipe r, @RequestParam(defaultValue = "1") int pageNo, @RequestParam(defaultValue = "10") int pageSize, HttpServletRequest req) {
        // 原生SQL分页查询回收站，避免逻辑删除自动追加 del_flag=0 导致查不到数据
        return Result.OK(recipeMapper.selectRecycleBinPage(new Page<>(pageNo, pageSize), r.getName()));
    }

    //update-begin---author:admin ---date:2026-07-31  for：修复软删除/恢复失效问题（@TableLogic 字段不参与 updateById）-----------
    /**
     * 移入回收站（软删除）
     */
    @PutMapping("/moveToRecycleBin")
    @AutoLog(value="菜谱-移入回收站")
    @Operation(summary="菜谱-移入回收站")
    @RequiresPermissions("homeai:recipe:moveToRecycleBin")
    public Result<?> moveToRecycleBin(@RequestBody List<String> ids) {
        for (String id : ids) {
            // @TableLogic 字段不参与 updateById，需通过 update wrapper 显式设置
            recipeService.update(new LambdaUpdateWrapper<Recipe>()
                    .eq(Recipe::getId, id)
                    .set(Recipe::getDelFlag, 1));
        }
        return Result.OK("移入回收站成功");
    }

    /**
     * 从回收站恢复
     */
    @PutMapping("/restore")
    @AutoLog(value="菜谱-恢复")
    @Operation(summary="菜谱-恢复")
    @RequiresPermissions("homeai:recipe:restore")
    public Result<?> restore(@RequestBody List<String> ids) {
        for (String id : ids) {
            // 自定义原生 SQL 恢复，绕开逻辑删除拦截器自动注入的 del_flag=0 条件
            recipeMapper.restoreById(id);
        }
        return Result.OK("恢复成功");
    }
    //update-end---author:admin ---date:2026-07-31  for：修复软删除/恢复失效问题（@TableLogic 字段不参与 updateById）-----------

    /**
     * 彻底删除
     */
    @DeleteMapping("/deletePermanently")
    @AutoLog(value="菜谱-彻底删除")
    @Operation(summary="菜谱-彻底删除")
    @RequiresPermissions("homeai:recipe:deletePermanently")
    public Result<?> deletePermanently(@RequestBody List<String> ids, HttpServletRequest request) {
        recipeMapper.deletePermanentlyByIds(ids);
        //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R22】菜谱彻底删除审计-----------
        auditLogService.record(
                getUserId(request),
                "recipe_delete_permanently",
                "recipe",
                ids != null && ids.size() == 1 ? ids.get(0) : null,
                "彻底删除菜谱 " + (ids == null ? 0 : ids.size()) + " 个",
                Collections.singletonMap("ids", ids),
                "success",
                request.getRemoteAddr());
        //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R22】菜谱彻底删除审计-----------
        return Result.OK("彻底删除成功");
    }

    /**
     * 编辑菜谱（管理端）
     */
    @PutMapping("/{id}")
    @AutoLog(value="菜谱-编辑(管理端)")
    @Operation(summary="菜谱-编辑(管理端)")
    @RequiresPermissions("homeai:recipe:edit")
    public Result<?> edit(@PathVariable String id, @RequestBody Map<String, Object> body) {
        Recipe recipe = JSON.parseObject(JSON.toJSONString(body), Recipe.class);
        recipe.setId(id);
        Result<?> categoryError = validateRecipeCategory(recipe);
        if (categoryError != null) return categoryError;
        //update-begin---author:cursor ---date:2026-08-12 for：【菜谱可见性】管理端编辑写入可见性/家庭-----------
        try {
            recipeService.applyAdminVisibilityOnSave(recipe);
        } catch (JeecgBootException e) {
            return Result.error(e.getMessage());
        }
        //update-end---author:cursor ---date:2026-08-12 for：【菜谱可见性】管理端编辑写入可见性/家庭-----------
        List<RecipeIngredient> ingredients = JSON.parseArray(
                JSON.toJSONString(body.get("ingredients")), RecipeIngredient.class);
        List<RecipeStep> steps = JSON.parseArray(
                JSON.toJSONString(body.get("steps")), RecipeStep.class);
        recipeService.updateWithRelations(recipe, ingredients, steps);
        return Result.OK(recipe);
    }
    //update-end---author:admin ---date:2026-07-30  for：菜谱管理-新增/导入/导出/回收站功能-----------

    //update-begin---author:admin ---date:2026-07-31  for：A5-菜谱视频上传/删除API-----------
    /**
     * 上传菜谱视频
     */
    @PostMapping("/{id}/video")
    public Result<?> uploadVideo(@PathVariable String id, @RequestParam MultipartFile file, HttpServletRequest req) {
        //update-begin---author:cursor ---date:2026-08-13 for：【IDOR修复】上传/删除菜谱视频、封面补归属校验-----------
        Result<?> permError = checkRecipeOwner(id, req);
        if (permError != null) return permError;
        //update-end---author:cursor ---date:2026-08-13 for：【IDOR修复】上传/删除菜谱视频、封面补归属校验-----------
        try {
            String videoUrl = fileStorageService.resolveAccessUrl(recipeService.uploadVideo(id, file));
            return Result.OK("视频上传成功", videoUrl);
        } catch (Exception e) {
            log.error("视频上传失败", e);
            return Result.error("视频上传失败: " + e.getMessage());
        }
    }

    /**
     * 删除菜谱视频
     */
    @DeleteMapping("/{id}/video")
    public Result<?> deleteVideo(@PathVariable String id, HttpServletRequest req) {
        //update-begin---author:cursor ---date:2026-08-13 for：【IDOR修复】上传/删除菜谱视频、封面补归属校验-----------
        Result<?> permError = checkRecipeOwner(id, req);
        if (permError != null) return permError;
        //update-end---author:cursor ---date:2026-08-13 for：【IDOR修复】上传/删除菜谱视频、封面补归属校验-----------
        recipeService.deleteVideo(id);
        return Result.OK("视频删除成功");
    }
    //update-end---author:admin ---date:2026-07-31  for：A5-菜谱视频上传/删除API-----------

    /**
     * 上传菜谱封面图
     */
    @PostMapping("/{id}/cover")
    public Result<?> uploadCover(@PathVariable String id, @RequestParam MultipartFile file, HttpServletRequest req) {
        //update-begin---author:cursor ---date:2026-08-13 for：【IDOR修复】上传/删除菜谱视频、封面补归属校验-----------
        Result<?> permError = checkRecipeOwner(id, req);
        if (permError != null) return permError;
        //update-end---author:cursor ---date:2026-08-13 for：【IDOR修复】上传/删除菜谱视频、封面补归属校验-----------
        try {
            String url = fileStorageService.resolveAccessUrl(recipeService.uploadCover(id, file));
            return Result.OK(url);
        } catch (Exception e) {
            log.error("封面上传失败", e);
            return Result.error("封面上传失败: " + e.getMessage());
        }
    }

    /**
     * 上传烹饪步骤图片（返回图片地址，由步骤记录保存）
     */
    @PostMapping("/step-image")
    public Result<?> uploadStepImage(@RequestParam MultipartFile file, HttpServletRequest r) {
        String uid = getUserId(r);
        if (uid == null) return Result.error("未登录");
        try {
            return Result.OK(fileStorageService.resolveAccessUrl(recipeService.uploadStepImage(file)));
        } catch (Exception e) {
            log.error("步骤图片上传失败", e);
            return Result.error("步骤图片上传失败: " + e.getMessage());
        }
    }

    /**
     * 通用上传封面图片（保存菜谱前先获取图片地址）
     */
    @PostMapping("/cover")
    public Result<?> uploadCoverFile(@RequestParam MultipartFile file, HttpServletRequest r) {
        String uid = getUserId(r);
        if (uid == null) return Result.error("未登录");
        try {
            return Result.OK(fileStorageService.resolveAccessUrl(recipeService.uploadCoverFile(file)));
        } catch (Exception e) {
            log.error("封面上传失败", e);
            return Result.error("封面上传失败: " + e.getMessage());
        }
    }

    /**
     * 通用上传做菜视频（保存菜谱前先获取视频地址）
     */
    @PostMapping("/video")
    public Result<?> uploadVideoFile(@RequestParam MultipartFile file, HttpServletRequest r) {
        String uid = getUserId(r);
        if (uid == null) return Result.error("未登录");
        try {
            return Result.OK(fileStorageService.resolveAccessUrl(recipeService.uploadVideoFile(file)));
        } catch (Exception e) {
            log.error("视频上传失败", e);
            return Result.error("视频上传失败: " + e.getMessage());
        }
    }

    //update-begin---author:admin ---date:2026-08-13 for：【HomeAI-R24】修复 openpyxl 内联字符串 Excel 导入文本列为空 ----------
    /** Excel 列名与实体字段映射（基于 @Excel 注解，保持与模板列一致） */
    private static final Map<String, Field> RECIPE_EXCEL_FIELDS = new HashMap<>();

    static {
        for (Field f : Recipe.class.getDeclaredFields()) {
            org.jeecgframework.poi.excel.annotation.Excel ex = f.getAnnotation(org.jeecgframework.poi.excel.annotation.Excel.class);
            if (ex != null) {
                RECIPE_EXCEL_FIELDS.put(ex.name(), f);
            }
        }
    }

    /**
     * 直接使用 POI 解析上传的菜谱 Excel。
     * 说明：AutoPoi 2.0.5 读取字符串单元格时会调用 cell.setCellType(STRING)，
     * 对 openpyxl 生成的内联字符串(inlineStr)单元格会清空内容，导致所有文本列导入为空；
     * 这里改为直接读取单元格值，不再经过 AutoPoi 的类型转换。
     */
    private List<Recipe> parseRecipeExcel(InputStream in) throws Exception {
        List<Recipe> list = new ArrayList<>();
        try (Workbook book = WorkbookFactory.create(in)) {
            Sheet sheet = book.getSheetAt(0);
            int titleRows = 2;
            int headRows = 1;
            Row headerRow = sheet.getRow(titleRows);
            if (headerRow == null) {
                throw new JeecgBootException("未识别到表头行，请使用“下载模板”导出的模板填写");
            }
            Map<Integer, String> titleMap = new HashMap<>();
            int matchedColumns = 0;
            for (Cell cell : headerRow) {
                String v = readCellText(cell);
                if (oConvertUtils.isNotEmpty(v)) {
                    String title = v.trim();
                    titleMap.put(cell.getColumnIndex(), title);
                    if (RECIPE_EXCEL_FIELDS.containsKey(title)) {
                        matchedColumns++;
                    }
                }
            }
            if (titleMap.isEmpty() || matchedColumns == 0) {
                throw new JeecgBootException("Excel 表头与模板不匹配，请使用“下载模板”导出的模板填写");
            }
            for (int r = titleRows + headRows; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }
                Recipe item = new Recipe();
                boolean hasData = false;
                for (Map.Entry<Integer, String> entry : titleMap.entrySet()) {
                    String value = readCellText(row.getCell(entry.getKey()));
                    if (oConvertUtils.isEmpty(value)) {
                        continue;
                    }
                    hasData = true;
                    Field field = RECIPE_EXCEL_FIELDS.get(entry.getValue());
                    if (field == null) {
                        continue;
                    }
                    Object converted = value.trim();
                    if (field.getType() == Integer.class || field.getType() == int.class) {
                        converted = parseInteger((String) converted);
                        if (converted == null) {
                            continue;
                        }
                    }
                    field.setAccessible(true);
                    field.set(item, converted);
                }
                if (hasData) {
                    list.add(item);
                }
            }
        }
        return list;
    }

    /** 读取单元格文本，不改变单元格类型（避免 inlineStr 单元格内容被清空） */
    private String readCellText(Cell cell) {
        if (cell == null) {
            return null;
        }
        try {
            CellType type = cell.getCellType();
            switch (type) {
                case STRING:
                    return cell.getStringCellValue();
                case NUMERIC:
                    double d = cell.getNumericCellValue();
                    if (d == Math.floor(d) && !Double.isInfinite(d)) {
                        return String.valueOf((long) d);
                    }
                    return BigDecimal.valueOf(d).stripTrailingZeros().toPlainString();
                case BOOLEAN:
                    return String.valueOf(cell.getBooleanCellValue());
                case FORMULA:
                    try {
                        return cell.getStringCellValue();
                    } catch (Exception e) {
                        return String.valueOf(cell.getNumericCellValue());
                    }
                default:
                    return null;
            }
        } catch (Exception e) {
            log.warn("读取 Excel 单元格失败: {}", e.getMessage());
            return null;
        }
    }

    //update-begin---author:cursor ---date:2026-08-13 for：【菜谱导入】封面图片地址格式校验-----------
    /** 校验封面图片地址：http/https、/upload 相对地址或 data:image */
    private boolean isValidImageUrl(String url) {
        String v = url.trim();
        return v.startsWith("http://") || v.startsWith("https://")
                || v.startsWith("/upload/") || v.startsWith("data:image/");
    }
    //update-end---author:cursor ---date:2026-08-13 for：【菜谱导入】封面图片地址格式校验-----------

    private Integer parseInteger(String value) {
        try {
            return new BigDecimal(value.trim()).intValue();
        } catch (Exception e) {
            return null;
        }
    }
    //update-end---author:admin ---date:2026-08-13 for：【HomeAI-R24】修复 openpyxl 内联字符串 Excel 导入文本列为空 ----------
}
