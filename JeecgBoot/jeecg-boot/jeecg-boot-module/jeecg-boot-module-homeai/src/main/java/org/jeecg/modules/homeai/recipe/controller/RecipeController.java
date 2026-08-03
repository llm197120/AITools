package org.jeecg.modules.homeai.recipe.controller;
import com.alibaba.fastjson.JSON;
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
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.common.util.oConvertUtils;
import io.swagger.v3.oas.annotations.Operation;
import org.jeecg.modules.homeai.config.HomeaiFileUrlUtil;
import org.jeecg.modules.homeai.config.HomeaiJwtUtil;
import org.jeecg.modules.homeai.config.HomeaiSecurityUtil;
import org.jeecg.modules.homeai.recipe.entity.Recipe;
import org.jeecg.modules.homeai.recipe.entity.RecipeIngredient;
import org.jeecg.modules.homeai.recipe.entity.RecipeStep;
import org.jeecg.modules.homeai.recipe.mapper.RecipeMapper;
import org.jeecg.modules.homeai.recipe.service.IRecipeService;
import org.jeecg.modules.homeai.user.service.IWxUserService;
import org.jeecgframework.poi.excel.ExcelImportUtil;
import org.jeecgframework.poi.excel.def.NormalExcelConstants;
import org.jeecgframework.poi.excel.entity.ExportParams;
import org.jeecgframework.poi.excel.entity.ImportParams;
import org.jeecgframework.poi.excel.entity.enmus.ExcelType;
import org.jeecgframework.poi.excel.view.JeecgEntityExcelView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/homeai/recipe")
public class RecipeController {
    @Autowired private IRecipeService recipeService;
    @Autowired private IWxUserService wxUserService;
    @Autowired private RecipeMapper recipeMapper;
    @Autowired private HomeaiSecurityUtil securityUtil;

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

    @GetMapping("/list")
    @Operation(summary="菜谱-分页列表查询")
    public Result<?> list(Recipe r, @RequestParam(defaultValue = "1") int pageNo, @RequestParam(defaultValue = "10") int pageSize, HttpServletRequest req) {
        QueryWrapper<Recipe> qw = QueryGenerator.initQueryWrapper(r, req.getParameterMap());
        qw.eq("del_flag", "0").orderByDesc("create_time");
        IPage<Recipe> result = recipeService.page(new Page<>(pageNo, pageSize), qw);
        // 兼容历史相对地址数据：统一转换为绝对访问地址
        if (result.getRecords() != null) {
            for (Recipe item : result.getRecords()) {
                resolveRecipeUrls(item);
            }
        }
        return Result.OK(result);
    }

    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable String id) {
        Map<String, Object> detail = recipeService.getDetailWithRelations(id);
        Recipe recipe = (Recipe) detail.get("recipe");
        resolveRecipeUrls(recipe);
        return Result.OK(detail);
    }

    @GetMapping("/search")
    public Result<?> search(@RequestParam String keyword) {
        List<Recipe> list = recipeService.search(keyword);
        if (list != null) {
            for (Recipe item : list) {
                resolveRecipeUrls(item);
            }
        }
        return Result.OK(list);
    }

    /** 兼容历史相对地址数据：转换 coverUrl / videoUrl 为绝对访问地址 */
    private void resolveRecipeUrls(Recipe recipe) {
        if (recipe == null) return;
        if (recipe.getCoverUrl() != null && !recipe.getCoverUrl().startsWith("http") && !recipe.getCoverUrl().startsWith("data:")) {
            recipe.setCoverUrl(HomeaiFileUrlUtil.toAbsoluteUrl(recipe.getCoverUrl()));
        }
        if (recipe.getVideoUrl() != null && !recipe.getVideoUrl().startsWith("http")) {
            recipe.setVideoUrl(HomeaiFileUrlUtil.toAbsoluteUrl(recipe.getVideoUrl()));
        }
    }

    @PostMapping
    public Result<?> create(@RequestBody Map<String, Object> body, HttpServletRequest r) {
        String uid = getUserId(r);
        if (uid == null) return Result.error("未登录");
        Recipe recipe = JSON.parseObject(JSON.toJSONString(body), Recipe.class);
        recipe.setUserId(uid);
        List<RecipeIngredient> ingredients = JSON.parseArray(
                JSON.toJSONString(body.get("ingredients")), RecipeIngredient.class);
        List<RecipeStep> steps = JSON.parseArray(
                JSON.toJSONString(body.get("steps")), RecipeStep.class);
        recipeService.saveWithRelations(recipe, ingredients, steps);
        return Result.OK(recipe);
    }

    @PutMapping public Result<?> edit(@RequestBody Recipe r, HttpServletRequest req) {
        if (r == null || r.getId() == null) return Result.error("参数异常");
        Recipe existing = recipeService.getById(r.getId());
        if (existing == null) return Result.error("菜谱不存在");
        // 管理端控制台可编辑任意菜谱；小程序端只能编辑自己的菜谱
        if (!securityUtil.isConsoleAuthenticated(req)) {
            String uid = getUserId(req);
            if (uid == null) return Result.error("未登录");
            if (!uid.equals(existing.getUserId())) return Result.error("无权编辑该菜谱");
        }
        recipeService.updateById(r);
        return Result.OK("OK");
    }
    @DeleteMapping("/{id}") public Result<?> delete(@PathVariable String id, HttpServletRequest req) {
        Recipe existing = recipeService.getById(id);
        if (existing == null) return Result.error("菜谱不存在");
        if (!securityUtil.isConsoleAuthenticated(req)) {
            String uid = getUserId(req);
            if (uid == null) return Result.error("未登录");
            if (!uid.equals(existing.getUserId())) return Result.error("无权删除该菜谱");
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
    public Result<?> add(@RequestBody Recipe recipe) {
        recipe.setDelFlag(0);
        recipeService.save(recipe);
        return Result.OK("新增成功");
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
                ImportParams params = new ImportParams();
                params.setTitleRows(2);
                params.setHeadRows(1);
                params.setNeedSave(true);
                try {
                    List<Recipe> list = ExcelImportUtil.importExcel(file.getInputStream(), Recipe.class, params);
                    for (Recipe item : list) {
                        try {
                            recipeService.save(item);
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
    public Result<?> deletePermanently(@RequestBody List<String> ids) {
        recipeMapper.deletePermanentlyByIds(ids);
        return Result.OK("彻底删除成功");
    }

    /**
     * 编辑菜谱（管理端）
     */
    @PutMapping("/{id}")
    @AutoLog(value="菜谱-编辑(管理端)")
    @Operation(summary="菜谱-编辑(管理端)")
    @RequiresPermissions("homeai:recipe:edit")
    public Result<?> edit(@PathVariable String id, @RequestBody Recipe recipe) {
        recipe.setId(id);
        recipeService.updateById(recipe);
        return Result.OK("编辑成功");
    }
    //update-end---author:admin ---date:2026-07-30  for：菜谱管理-新增/导入/导出/回收站功能-----------

    //update-begin---author:admin ---date:2026-07-31  for：A5-菜谱视频上传/删除API-----------
    /**
     * 上传菜谱视频
     */
    @PostMapping("/{id}/video")
    public Result<?> uploadVideo(@PathVariable String id, @RequestParam MultipartFile file) {
        try {
            String videoUrl = recipeService.uploadVideo(id, file);
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
    public Result<?> deleteVideo(@PathVariable String id) {
        recipeService.deleteVideo(id);
        return Result.OK("视频删除成功");
    }
    //update-end---author:admin ---date:2026-07-31  for：A5-菜谱视频上传/删除API-----------
}
