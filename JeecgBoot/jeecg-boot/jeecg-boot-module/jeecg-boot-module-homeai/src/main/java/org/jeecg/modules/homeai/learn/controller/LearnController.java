package org.jeecg.modules.homeai.learn.controller;
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
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.oConvertUtils;
import io.swagger.v3.oas.annotations.Operation;
import org.jeecg.modules.homeai.config.service.IHomeaiFileStorageService;
import org.jeecg.modules.homeai.config.HomeaiJwtUtil;
import org.jeecg.modules.homeai.config.HomeaiSecurityUtil;
import org.jeecg.modules.homeai.learn.service.ILearnCategoryService;
import org.jeecg.modules.homeai.recipe.entity.LearnMaterial;
import org.jeecg.modules.homeai.recipe.mapper.LearnMaterialMapper;
import org.jeecg.modules.homeai.recipe.service.ILearnService;
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
@RequestMapping("/homeai/learn")
public class LearnController {
    @Autowired private ILearnService learnService;
    @Autowired private ILearnCategoryService learnCategoryService;
    @Autowired private IWxUserService wxUserService;
    @Autowired private LearnMaterialMapper learnMaterialMapper;
    @Autowired private HomeaiSecurityUtil securityUtil;
    @Autowired private IHomeaiFileStorageService fileStorageService;

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

    private Result<?> syncLearnCategory(LearnMaterial m) {
        try {
            if (oConvertUtils.isNotEmpty(m.getCategoryId())) {
                learnCategoryService.validateCategoryId(m.getCategoryId());
                String name = learnCategoryService.resolveCategoryName(m.getCategoryId());
                if (name != null) {
                    m.setCategory(name);
                }
            }
            return null;
        } catch (JeecgBootException e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/materials")
    @Operation(summary="学习资料-分页列表查询")
    public Result<?> materials(LearnMaterial m, @RequestParam(defaultValue = "1") int pageNo, @RequestParam(defaultValue = "10") int pageSize, HttpServletRequest req) {
        QueryWrapper<LearnMaterial> qw = QueryGenerator.initQueryWrapper(m, req.getParameterMap());
        qw.eq("del_flag", "0").orderByDesc("create_time");
        IPage<LearnMaterial> result = learnService.page(new Page<>(pageNo, pageSize), qw);
        // 兼容历史相对地址数据：统一转换为绝对访问地址
        if (result.getRecords() != null) {
            for (LearnMaterial item : result.getRecords()) {
                if (item.getFileUrl() != null) {
                    item.setFileUrl(fileStorageService.resolveAccessUrl(item.getFileUrl()));
                }
                if (item.getCoverUrl() != null && !item.getCoverUrl().startsWith("data:")) {
                    item.setCoverUrl(fileStorageService.resolveAccessUrl(item.getCoverUrl()));
                }
            }
        }
        return Result.OK(result);
    }

    @PostMapping("/start")
    public Result<?> start(@RequestParam String materialId, HttpServletRequest r) {
        String uid = getUserId(r);
        if (uid == null) return Result.error("未登录");
        learnService.startLearn(uid, materialId);
        return Result.OK("OK");
    }

    @PostMapping("/stop")
    public Result<?> stop(@RequestParam String materialId, HttpServletRequest r) {
        String uid = getUserId(r);
        if (uid == null) return Result.error("未登录");
        return Result.OK(learnService.stopLearn(uid, materialId));
    }

    @GetMapping("/session/active")
    @Operation(summary = "学习-当前进行中的计时会话")
    public Result<?> activeSession(HttpServletRequest r) {
        String uid = getUserId(r);
        if (uid == null) return Result.error("未登录");
        return Result.OK(learnService.getActiveSession(uid));
    }

    @GetMapping("/records")
    public Result<?> records(HttpServletRequest r) {
        String uid = getUserId(r);
        if (uid == null) return Result.error("未登录");
        return Result.OK(learnService.getUserRecords(uid));
    }

    /**
     * 学习记录查看（管理端分页）
     */
    @GetMapping("/admin/records")
    @Operation(summary="学习-记录查看(管理端)")
    @RequiresPermissions("homeai:learn:material:list")
    public Result<?> adminRecords(@RequestParam(defaultValue = "1") int pageNo,
                                  @RequestParam(defaultValue = "10") int pageSize,
                                  @RequestParam(required = false) String userId) {
        return Result.OK(learnService.adminListRecords(pageNo, pageSize, userId));
    }

    /**
     * 学习统计（管理端）
     */
    @GetMapping("/admin/stats")
    @Operation(summary="学习-统计(管理端)")
    @RequiresPermissions("homeai:learn:material:list")
    public Result<?> adminStats() {
        return Result.OK(learnService.adminStats());
    }

    @GetMapping("/admin/stats/trend")
    @Operation(summary = "学习-近N日趋势(管理端)")
    @RequiresPermissions("homeai:learn:material:list")
    public Result<?> adminStatsTrend(@RequestParam(defaultValue = "30") int days) {
        return Result.OK(learnService.adminStatsTrend(days));
    }

    /**
     * 学习统计（小程序端）
     */
    @GetMapping("/statistics")
    public Result<?> statistics(HttpServletRequest r) {
        String uid = getUserId(r);
        if (uid == null) return Result.error("未登录");
        return Result.OK(learnService.getUserStatistics(uid));
    }

    /** 学习日历：某月有学习记录的日期 */
    @GetMapping("/calendar")
    @Operation(summary = "学习-日历(小程序)")
    public Result<?> calendar(@RequestParam String yearMonth, HttpServletRequest r) {
        String uid = getUserId(r);
        if (uid == null) return Result.error("未登录");
        return Result.OK(learnService.getLearnCalendarDates(uid, yearMonth));
    }

    /**
     * 手动记录学习（小程序端）
     * body: { materialId, duration(秒), recordType }
     */
    @PostMapping("/record")
    public Result<?> record(@RequestBody Map<String, Object> body, HttpServletRequest r) {
        String uid = getUserId(r);
        if (uid == null) return Result.error("未登录");
        String materialId = body.get("materialId") != null ? String.valueOf(body.get("materialId")) : null;
        if (oConvertUtils.isEmpty(materialId)) {
            return Result.error("请选择学习资料");
        }
        int duration = body.get("duration") != null
                ? Integer.parseInt(String.valueOf(body.get("duration"))) : 0;
        String recordType = body.get("recordType") != null
                ? String.valueOf(body.get("recordType")) : "timer";
        return Result.OK(learnService.addRecord(uid, materialId, duration, recordType));
    }

    @PostMapping("/material")
    public Result<?> createMat(@RequestBody LearnMaterial m, HttpServletRequest r) {
        String uid = getUserId(r);
        if (uid == null) return Result.error("未登录");
        Result<?> categoryError = syncLearnCategory(m);
        if (categoryError != null) return categoryError;
        m.setUserId(uid);
        learnService.save(m);
        return Result.OK(m);
    }
    @PutMapping("/material")
    public Result<?> editMat(@RequestBody LearnMaterial m, HttpServletRequest r) {
        if (m == null || m.getId() == null) return Result.error("参数异常");
        LearnMaterial existing = learnService.getById(m.getId());
        if (existing == null) return Result.error("资料不存在");
        Result<?> categoryError = syncLearnCategory(m);
        if (categoryError != null) return categoryError;
        // 管理端控制台可编辑任意资料；小程序端只能编辑自己的资料
        if (!securityUtil.isConsoleAuthenticated(r)) {
            String uid = getUserId(r);
            if (uid == null) return Result.error("未登录");
            if (!uid.equals(existing.getUserId())) return Result.error("无权编辑该资料");
        }
        learnService.updateById(m);
        return Result.OK("OK");
    }
    @DeleteMapping("/material/{id}")
    public Result<?> delMat(@PathVariable String id, HttpServletRequest r) {
        LearnMaterial existing = learnService.getById(id);
        if (existing == null) return Result.error("资料不存在");
        if (!securityUtil.isConsoleAuthenticated(r)) {
            String uid = getUserId(r);
            if (uid == null) return Result.error("未登录");
            if (!uid.equals(existing.getUserId())) return Result.error("无权删除该资料");
        }
        learnService.removeById(id);
        return Result.OK("OK");
    }

    //update-begin---author:admin ---date:2026-07-30  for：学习资料-新增/导入/导出/回收站功能-----------
    /**
     * 新增学习资料（管理端）
     */
    @PostMapping("/addMaterial")
    @AutoLog(value="学习资料-新增(管理端)")
    @Operation(summary="学习资料-新增(管理端)")
    @RequiresPermissions("homeai:learn:addMaterial")
    public Result<?> addMaterial(@RequestBody LearnMaterial m) {
        Result<?> categoryError = syncLearnCategory(m);
        if (categoryError != null) return categoryError;
        m.setDelFlag(0);
        learnService.save(m);
        return Result.OK("新增成功");
    }

    /**
     * 导出Excel
     */
    @GetMapping("/exportXls")
    @Operation(summary="学习资料-导出Excel")
    @RequiresPermissions("homeai:learn:exportXls")
    public ModelAndView exportXls(HttpServletRequest request, LearnMaterial learnMaterial) {
        QueryWrapper<LearnMaterial> queryWrapper = QueryGenerator.initQueryWrapper(learnMaterial, request.getParameterMap());
        List<LearnMaterial> pageList = learnService.list(queryWrapper);
        ModelAndView mv = new ModelAndView(new JeecgEntityExcelView());
        String selections = request.getParameter("selections");
        if (oConvertUtils.isEmpty(selections)) {
            mv.addObject(NormalExcelConstants.DATA_LIST, pageList);
        } else {
            List<String> selectionList = Arrays.asList(selections.split(","));
            List<LearnMaterial> exportList = pageList.stream().filter(item -> selectionList.contains(item.getId())).collect(Collectors.toList());
            mv.addObject(NormalExcelConstants.DATA_LIST, exportList);
        }
        mv.addObject(NormalExcelConstants.FILE_NAME, "学习资料列表");
        mv.addObject(NormalExcelConstants.CLASS, LearnMaterial.class);
        LoginUser user = null;
        try {
            Object principal = SecurityUtils.getSubject().getPrincipal();
            if (principal instanceof LoginUser) {
                user = (LoginUser) principal;
            }
        } catch (Exception ignored) {}
        mv.addObject(NormalExcelConstants.PARAMS, new ExportParams("学习资料列表数据", "导出人:" + (user != null ? user.getRealname() : "系统"), "导出信息", ExcelType.XSSF));
        return mv;
    }

    /**
     * 导入Excel
     */
    @PostMapping("/importExcel")
    @AutoLog(value="学习资料-导入Excel")
    @Operation(summary="学习资料-导入Excel")
    @RequiresPermissions("homeai:learn:importExcel")
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
                    List<LearnMaterial> list = ExcelImportUtil.importExcel(file.getInputStream(), LearnMaterial.class, params);
                    for (LearnMaterial item : list) {
                        try {
                            learnService.save(item);
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
    @Operation(summary="学习资料-回收站列表")
    @RequiresPermissions("homeai:learn:moveToRecycleBin")
    public Result<?> recycleBin(LearnMaterial m, @RequestParam(defaultValue = "1") int pageNo, @RequestParam(defaultValue = "10") int pageSize, HttpServletRequest req) {
        // 原生SQL分页查询回收站，避免逻辑删除自动追加 del_flag=0 导致查不到数据
        return Result.OK(learnMaterialMapper.selectRecycleBinPage(new Page<>(pageNo, pageSize), m.getTitle()));
    }

    //update-begin---author:admin ---date:2026-07-31  for：修复软删除/恢复失效问题（@TableLogic 字段不参与 updateById）-----------
    /**
     * 移入回收站（软删除）
     */
    @PutMapping("/moveToRecycleBin")
    @AutoLog(value="学习资料-移入回收站")
    @Operation(summary="学习资料-移入回收站")
    @RequiresPermissions("homeai:learn:moveToRecycleBin")
    public Result<?> moveToRecycleBin(@RequestBody List<String> ids) {
        for (String id : ids) {
            // @TableLogic 字段不参与 updateById，需通过 update wrapper 显式设置
            learnService.update(new LambdaUpdateWrapper<LearnMaterial>()
                    .eq(LearnMaterial::getId, id)
                    .set(LearnMaterial::getDelFlag, 1));
        }
        return Result.OK("移入回收站成功");
    }

    /**
     * 从回收站恢复
     */
    @PutMapping("/restore")
    @AutoLog(value="学习资料-恢复")
    @Operation(summary="学习资料-恢复")
    @RequiresPermissions("homeai:learn:restore")
    public Result<?> restore(@RequestBody List<String> ids) {
        for (String id : ids) {
            // 自定义原生 SQL 恢复，绕开逻辑删除拦截器自动注入的 del_flag=0 条件
            learnMaterialMapper.restoreById(id);
        }
        return Result.OK("恢复成功");
    }
    //update-end---author:admin ---date:2026-07-31  for：修复软删除/恢复失效问题（@TableLogic 字段不参与 updateById）-----------

    /**
     * 彻底删除
     */
    @DeleteMapping("/deletePermanently")
    @AutoLog(value="学习资料-彻底删除")
    @Operation(summary="学习资料-彻底删除")
    @RequiresPermissions("homeai:learn:deletePermanently")
    public Result<?> deletePermanently(@RequestBody List<String> ids) {
        learnMaterialMapper.deletePermanentlyByIds(ids);
        return Result.OK("彻底删除成功");
    }

    /**
     * 编辑学习资料（管理端）
     */
    @PutMapping("/material/{id}")
    @AutoLog(value="学习资料-编辑(管理端)")
    @Operation(summary="学习资料-编辑(管理端)")
    @RequiresPermissions("homeai:learn:edit")
    public Result<?> editMaterial(@PathVariable String id, @RequestBody LearnMaterial m) {
        m.setId(id);
        Result<?> categoryError = syncLearnCategory(m);
        if (categoryError != null) return categoryError;
        learnService.updateById(m);
        return Result.OK("编辑成功");
    }

    //update-begin---author:admin ---date:2026-08-04  for：学习资料预上传API-----------
    /**
     * 学习资料预上传（新增前上传文件）
     */
    @PostMapping("/upload")
    @Operation(summary = "学习资料-预上传文件")
    public Result<?> uploadTemp(@RequestParam MultipartFile file,
                                @RequestParam String type,
                                HttpServletRequest r) {
        if (getUserId(r) == null) return Result.error("未登录");
        try {
            String fileUrl = fileStorageService.resolveAccessUrl(learnService.uploadTempFile(file, type));
            return Result.OK("上传成功", fileUrl);
        } catch (JeecgBootException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("文件上传失败", e);
            return Result.error("文件上传失败: " + e.getMessage());
        }
    }
    //update-end---author:admin ---date:2026-08-04  for：学习资料预上传API-----------

    //update-begin---author:admin ---date:2026-07-31  for：A4-学习资料文件上传API-----------
    /**
     * 学习资料文件上传
     */
    @PostMapping("/materials/{id}/upload")
    public Result<?> uploadMaterialFile(@PathVariable String id, @RequestParam MultipartFile file) {
        LearnMaterial material = learnService.getById(id);
        if (material == null) return Result.error("学习资料不存在");
        try {
            String fileUrl = learnService.uploadMaterialFile(id, file);
            material.setFileUrl(fileUrl);
            learnService.updateById(material);
            return Result.OK("上传成功", fileStorageService.resolveAccessUrl(fileUrl));
        } catch (JeecgBootException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("文件上传失败", e);
            return Result.error("文件上传失败: " + e.getMessage());
        }
    }
    //update-end---author:admin ---date:2026-07-31  for：A4-学习资料文件上传API-----------
    //update-end---author:admin ---date:2026-07-30  for：学习资料-新增/导入/导出/回收站功能-----------
}
