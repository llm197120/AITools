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
import org.jeecg.modules.homeai.audit.service.IHomeaiAuditLogService;
import org.jeecg.modules.homeai.config.service.IHomeaiFileStorageService;
import org.jeecg.modules.homeai.config.HomeaiImageProcess;
import org.jeecg.modules.homeai.config.HomeaiHttpDeny;
import org.jeecg.modules.homeai.config.HomeaiSecurityUtil;
import org.jeecg.modules.homeai.learn.service.ILearnCategoryService;
import org.jeecg.modules.homeai.preview.HomeaiFileMime;
import org.jeecg.modules.homeai.preview.HomeaiFilePreviewDto;
import org.jeecg.modules.homeai.preview.HomeaiPreviewKind;
import org.jeecg.modules.homeai.preview.IHomeaiFilePreviewService;
import org.jeecg.modules.homeai.recipe.entity.LearnMaterial;
import org.jeecg.modules.homeai.recipe.mapper.LearnMaterialMapper;
import org.jeecg.modules.homeai.recipe.service.ILearnService;
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
    @Autowired private LearnMaterialMapper learnMaterialMapper;
    @Autowired private HomeaiSecurityUtil securityUtil;
    @Autowired private IHomeaiFileStorageService fileStorageService;
    //update-begin---author:cursor---date:2026-08-21---for:【HomeAI-R63】学习资料预览-----------
    @Autowired private IHomeaiFilePreviewService filePreviewService;
    //update-end---author:cursor---date:2026-08-21---for:【HomeAI-R63】学习资料预览-----------
    //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R22】学习审计埋点-----------
    @Autowired private IHomeaiAuditLogService auditLogService;
    //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R22】学习审计埋点-----------

    private String getUserId(HttpServletRequest r) {
        //update-begin---author:cursor---date:2026-08-22---for:【审查B】APP 业务归属只认 HomeAI 用户-----------
        return securityUtil.getWxUserId(r);
        //update-end---author:cursor---date:2026-08-22---for:【审查B】APP 业务归属只认 HomeAI 用户-----------
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
    public Result<?> materials(LearnMaterial m, @RequestParam(defaultValue = "1") int pageNo, @RequestParam(defaultValue = "10") int pageSize,
                               @RequestParam(required = false) String keyword, HttpServletRequest req) {
        QueryWrapper<LearnMaterial> qw = QueryGenerator.initQueryWrapper(m, req.getParameterMap());
        qw.eq("del_flag", "0").orderByDesc("create_time");
        //update-begin---author:cursor---date:2026-08-22---for:【审查E】学习资料按标题检索---
        if (oConvertUtils.isNotEmpty(keyword)) {
            qw.like("title", keyword.trim());
        }
        //update-end---author:cursor---date:2026-08-22---for:【审查E】学习资料按标题检索---
        //update-begin---author:cursor---date:2026-08-20---for:【审查修复】APP 只看自己的资料；管理端控制台仍看全部---
        if (!securityUtil.canConsoleViewAll(req, "homeai:learn:material:list")) {
            String uid = getUserId(req);
            if (uid == null) return Result.error("未登录");
            qw.eq("user_id", uid);
        }
        //update-end---author:cursor---date:2026-08-20---for:【审查修复】APP 只看自己的资料；管理端控制台仍看全部---
        IPage<LearnMaterial> result = learnService.page(new Page<>(pageNo, pageSize), qw);
        // 兼容历史相对地址数据：统一转换为绝对访问地址
        if (result.getRecords() != null) {
            for (LearnMaterial item : result.getRecords()) {
                //update-begin---author:cursor---date:2026-08-22---for:【APP流量】学习封面/图片压缩---
                if (item.getFileUrl() != null) {
                    String process = "image".equals(item.getType()) ? HomeaiImageProcess.DISPLAY : null;
                    item.setFileUrl(fileStorageService.resolveAccessUrl(item.getFileUrl(), process));
                }
                if (item.getCoverUrl() != null && !item.getCoverUrl().startsWith("data:")) {
                    item.setCoverUrl(fileStorageService.resolveAccessUrl(item.getCoverUrl(), HomeaiImageProcess.THUMB));
                }
                //update-end---author:cursor---date:2026-08-22---for:【APP流量】学习封面/图片压缩---
                if (item.getPreviewPdfUrl() != null) {
                    item.setPreviewPdfUrl(fileStorageService.resolveAccessUrl(item.getPreviewPdfUrl()));
                }
            }
        }
        return Result.OK(result);
    }

    //update-begin---author:cursor---date:2026-08-21---for:【HomeAI-R63】学习资料预览-----------
    @GetMapping("/materials/{id}/preview")
    @Operation(summary = "学习资料-预览描述")
    public Result<?> previewMaterial(@PathVariable String id, HttpServletRequest req) {
        LearnMaterial material = learnService.getById(id);
        Result<?> denied = assertCanReadMaterial(material, req);
        if (denied != null) return denied;
        return Result.OK(filePreviewService.previewLearn(material));
    }

    //update-begin---author:cursor---date:2026-08-22---for:【HomeAI-R81】APP 鉴权下载原文件-----------
    @GetMapping("/materials/{id}/content")
    @Operation(summary = "学习资料-下载原文件")
    public void downloadMaterialContent(@PathVariable String id, HttpServletRequest req,
                                        HttpServletResponse response) throws java.io.IOException {
        LearnMaterial material = learnService.getById(id);
        Result<?> denied = assertCanReadMaterial(material, req);
        if (denied != null) {
            HomeaiHttpDeny.write(response, denied);
            return;
        }
        try {
            String ext = HomeaiPreviewKind.extensionOfNameOrUrl(material.getTitle());
            if (ext.isEmpty()) {
                ext = HomeaiPreviewKind.extensionOfNameOrUrl(material.getFileUrl());
            }
            java.nio.file.Path path = fileStorageService.resolveLocalPath(material.getFileUrl());
            HomeaiFileMime.writeLocalFile(response, path, material.getTitle(), ext);
        } catch (Exception e) {
            log.warn("学习资料下载失败 id={}", id, e);
            if (!response.isCommitted()) {
                response.sendError(HttpServletResponse.SC_BAD_GATEWAY, "文件读取失败");
            }
        }
    }
    //update-end---author:cursor---date:2026-08-22---for:【HomeAI-R81】APP 鉴权下载原文件-----------

    @PostMapping("/materials/{id}/preview-pdf")
    @Operation(summary = "学习资料-Office 转 PDF 预览")
    public Result<?> previewMaterialPdf(@PathVariable String id, HttpServletRequest req) {
        LearnMaterial material = learnService.getById(id);
        Result<?> denied = assertCanReadMaterial(material, req);
        if (denied != null) return denied;
        String uid = getUserId(req);
        try {
            HomeaiFilePreviewDto dto = filePreviewService.ensureLearnPreviewPdf(uid, material);
            return Result.OK(dto);
        } catch (JeecgBootException e) {
            return Result.error(e.getMessage());
        }
    }

    private Result<?> assertCanReadMaterial(LearnMaterial material, HttpServletRequest req) {
        if (material == null) return Result.error("学习资料不存在");
        if (securityUtil.isConsoleAuthenticated(req)) {
            return null;
        }
        String uid = getUserId(req);
        if (uid == null) return Result.error("未登录");
        if (!uid.equals(material.getUserId())) return Result.error("无权查看该资料");
        return null;
    }
    //update-end---author:cursor---date:2026-08-21---for:【HomeAI-R63】学习资料预览-----------

    //update-begin---author:cursor---date:2026-08-21---for:【HomeAI-R64】计时/记录校验资料归属-----------
    private Result<?> requireUsableMaterial(String materialId, HttpServletRequest r) {
        LearnMaterial material = learnService.getById(materialId);
        return assertCanReadMaterial(material, r);
    }
    //update-end---author:cursor---date:2026-08-21---for:【HomeAI-R64】计时/记录校验资料归属-----------

    @PostMapping("/start")
    public Result<?> start(@RequestParam String materialId, HttpServletRequest r) {
        String uid = getUserId(r);
        if (uid == null) return Result.error("未登录");
        //update-begin---author:cursor---date:2026-08-21---for:【HomeAI-R64】计时/记录校验资料归属-----------
        Result<?> denied = requireUsableMaterial(materialId, r);
        if (denied != null) return denied;
        try {
            learnService.startLearn(uid, materialId);
            return Result.OK("OK");
        } catch (JeecgBootException e) {
            return Result.error(e.getMessage());
        }
        //update-end---author:cursor---date:2026-08-21---for:【HomeAI-R64】计时/记录校验资料归属-----------
    }

    @PostMapping("/stop")
    public Result<?> stop(@RequestParam String materialId, HttpServletRequest r) {
        String uid = getUserId(r);
        if (uid == null) return Result.error("未登录");
        //update-begin---author:cursor---date:2026-08-21---for:【HomeAI-R64】计时/记录校验资料归属-----------
        Result<?> denied = requireUsableMaterial(materialId, r);
        if (denied != null) return denied;
        try {
            return Result.OK(learnService.stopLearn(uid, materialId));
        } catch (JeecgBootException e) {
            return Result.error(e.getMessage());
        }
        //update-end---author:cursor---date:2026-08-21---for:【HomeAI-R64】计时/记录校验资料归属-----------
    }

    @GetMapping("/session/active")
    @Operation(summary = "学习-当前进行中的计时会话")
    public Result<?> activeSession(HttpServletRequest r) {
        String uid = getUserId(r);
        if (uid == null) return Result.error("未登录");
        return Result.OK(learnService.getActiveSession(uid));
    }

    @GetMapping("/records")
    public Result<?> records(
            @RequestParam(required = false) String yearMonth,
            @RequestParam(required = false) String studyDate,
            HttpServletRequest r) {
        String uid = getUserId(r);
        if (uid == null) return Result.error("未登录");
        //update-begin---author:cursor---date:2026-08-22---for:【HomeAI-R83】学习记录按月查询-----------
        //update-begin---author:cursor---date:2026-08-23---for:【HomeAI-R117】学习记录按日查询---
        return Result.OK(learnService.getUserRecords(uid, yearMonth, studyDate));
        //update-end---author:cursor---date:2026-08-23---for:【HomeAI-R117】学习记录按日查询---
        //update-end---author:cursor---date:2026-08-22---for:【HomeAI-R83】学习记录按月查询-----------
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
    public Result<?> adminStats(@RequestParam(defaultValue = "0") int days,
                                @RequestParam(required = false) String userId) {
        //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R29】多维统计参数-----------
        return Result.OK(learnService.adminStats(days, userId));
        //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R29】多维统计参数-----------
    }

    @GetMapping("/admin/stats/trend")
    @Operation(summary = "学习-近N日趋势(管理端)")
    @RequiresPermissions("homeai:learn:material:list")
    public Result<?> adminStatsTrend(@RequestParam(defaultValue = "30") int days) {
        return Result.OK(learnService.adminStatsTrend(days));
    }

    //update-begin---author:copilot ---date:2026-08-12 for：【第15轮】学习按分类统计-----------
    @GetMapping("/admin/stats/category")
    @Operation(summary = "按分类学习统计")
    @RequiresPermissions("homeai:learn:material:list")
    public Result<?> adminStatsByCategory(@RequestParam(defaultValue = "0") int days,
                                          @RequestParam(required = false) String userId) {
        //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R29】分类统计参数-----------
        return Result.OK(learnService.getAdminStatsByCategory(days, userId));
        //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R29】分类统计参数-----------
    }
    //update-end---author:copilot ---date:2026-08-12 for：【第15轮】学习按分类统计-----------

    //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R29】按用户统计-----------
    @GetMapping("/admin/stats/user")
    @Operation(summary = "按用户学习统计")
    @RequiresPermissions("homeai:learn:material:list")
    public Result<?> adminStatsByUser(@RequestParam(defaultValue = "30") int days) {
        return Result.OK(learnService.adminStatsByUser(days));
    }
    //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R29】按用户统计-----------

    //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R31】学习统计导出-----------
    @GetMapping("/admin/stats/export")
    @Operation(summary = "学习多维统计导出Excel")
    @RequiresPermissions("homeai:learn:material:list")
    public void exportAdminStats(@RequestParam(defaultValue = "30") int days,
                                 @RequestParam(required = false) String userId,
                                 HttpServletResponse response) {
        try {
            Map<String, Object> summary = learnService.adminStats(days, userId);
            List<Map<String, Object>> byCategory = learnService.getAdminStatsByCategory(days, userId);
            int userDays = days > 0 ? days : 30;
            List<Map<String, Object>> byUser = learnService.adminStatsByUser(userDays);
            if (oConvertUtils.isNotEmpty(userId) && byUser != null) {
                byUser = byUser.stream()
                        .filter(u -> userId.equals(String.valueOf(u.get("userId"))))
                        .collect(Collectors.toList());
            }
            List<Map<String, Object>> trend = days > 0 ? learnService.adminStatsTrend(days) : Collections.emptyList();

            org.apache.poi.xssf.usermodel.XSSFWorkbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
            writeSummarySheet(wb, summary, days, userId);
            writeMapSheet(wb, "按分类", byCategory, new String[]{
                    "categoryName", "materialCount", "recordCount", "totalDuration"
            }, new String[]{"分类", "资料数", "记录数", "时长(分钟)"});
            writeMapSheet(wb, "按用户", byUser, new String[]{
                    "nickname", "userId", "recordCount", "durationMinutes", "activeDays"
            }, new String[]{"昵称", "用户ID", "记录数", "时长(分钟)", "活跃天"});
            writeMapSheet(wb, "趋势", trend, new String[]{
                    "date", "recordCount", "durationMinutes"
            }, new String[]{"日期", "记录数", "时长(分钟)"});

            String fileName = java.net.URLEncoder.encode("学习统计导出.xlsx", java.nio.charset.StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName);
            wb.write(response.getOutputStream());
            wb.close();
        } catch (Exception e) {
            log.error("学习统计导出失败", e);
            throw new JeecgBootException("学习统计导出失败: " + e.getMessage());
        }
    }

    private void writeSummarySheet(org.apache.poi.xssf.usermodel.XSSFWorkbook wb,
                                   Map<String, Object> summary, int days, String userId) {
        org.apache.poi.ss.usermodel.Sheet sheet = wb.createSheet("汇总");
        String[][] rows = {
                {"统计范围(天)", days > 0 ? String.valueOf(days) : "全部"},
                {"用户过滤", oConvertUtils.isNotEmpty(userId) ? userId : "全部"},
                {"学习记录总数", String.valueOf(summary != null ? summary.getOrDefault("totalRecords", 0) : 0)},
                {"总学习时长(分钟)", String.valueOf(summary != null ? summary.getOrDefault("totalDurationMinutes", 0) : 0)},
                {"活跃用户数", String.valueOf(summary != null ? summary.getOrDefault("activeUserCount", 0) : 0)},
                {"活跃天数", String.valueOf(summary != null ? summary.getOrDefault("activeDayCount", 0) : 0)},
        };
        for (int i = 0; i < rows.length; i++) {
            org.apache.poi.ss.usermodel.Row row = sheet.createRow(i);
            row.createCell(0).setCellValue(rows[i][0]);
            row.createCell(1).setCellValue(rows[i][1]);
        }
        sheet.setColumnWidth(0, 20 * 256);
        sheet.setColumnWidth(1, 24 * 256);
    }

    private void writeMapSheet(org.apache.poi.xssf.usermodel.XSSFWorkbook wb, String sheetName,
                               List<Map<String, Object>> rows, String[] keys, String[] headers) {
        org.apache.poi.ss.usermodel.Sheet sheet = wb.createSheet(sheetName);
        org.apache.poi.ss.usermodel.Row head = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            head.createCell(i).setCellValue(headers[i]);
            sheet.setColumnWidth(i, 16 * 256);
        }
        if (rows == null) {
            return;
        }
        int rIdx = 1;
        for (Map<String, Object> item : rows) {
            org.apache.poi.ss.usermodel.Row row = sheet.createRow(rIdx++);
            for (int c = 0; c < keys.length; c++) {
                Object v = item != null ? item.get(keys[c]) : null;
                row.createCell(c).setCellValue(v == null ? "" : String.valueOf(v));
            }
        }
    }
    //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R31】学习统计导出-----------

    /**
     * 学习统计（小程序端）
     */
    @GetMapping("/statistics")
    public Result<?> statistics(HttpServletRequest r) {
        String uid = getUserId(r);
        if (uid == null) return Result.error("未登录");
        return Result.OK(learnService.getUserStatistics(uid));
    }

    //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R29】每日目标 API-----------
    @GetMapping("/goal")
    @Operation(summary = "学习-每日目标与今日进度")
    public Result<?> goal(HttpServletRequest r) {
        String uid = getUserId(r);
        if (uid == null) return Result.error("未登录");
        return Result.OK(learnService.getTodayProgress(uid));
    }

    @PutMapping("/goal")
    @Operation(summary = "学习-设置每日目标(分钟)")
    public Result<?> setGoal(@RequestParam int minutes, HttpServletRequest r) {
        String uid = getUserId(r);
        if (uid == null) return Result.error("未登录");
        learnService.setDailyGoalMinutes(uid, minutes);
        return Result.OK(learnService.getTodayProgress(uid));
    }
    //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R29】每日目标 API-----------

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
        //update-begin---author:cursor---date:2026-08-21---for:【HomeAI-R64】计时/记录校验资料归属-----------
        Result<?> denied = requireUsableMaterial(materialId, r);
        if (denied != null) return denied;
        try {
            return Result.OK(learnService.addRecord(uid, materialId, duration, recordType));
        } catch (JeecgBootException e) {
            return Result.error(e.getMessage());
        }
        //update-end---author:cursor---date:2026-08-21---for:【HomeAI-R64】计时/记录校验资料归属-----------
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
    public Result<?> deletePermanently(@RequestBody List<String> ids, HttpServletRequest request) {
        learnMaterialMapper.deletePermanentlyByIds(ids);
        //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R22】学习彻底删除审计-----------
        auditLogService.record(
                getUserId(request),
                "learn_delete_permanently",
                "learn",
                ids != null && ids.size() == 1 ? ids.get(0) : null,
                "彻底删除学习资料 " + (ids == null ? 0 : ids.size()) + " 个",
                Collections.singletonMap("ids", ids),
                "success",
                request.getRemoteAddr());
        //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R22】学习彻底删除审计-----------
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
    public Result<?> uploadMaterialFile(@PathVariable String id, @RequestParam MultipartFile file, HttpServletRequest r) {
        LearnMaterial material = learnService.getById(id);
        if (material == null) return Result.error("学习资料不存在");
        //update-begin---author:cursor ---date:2026-08-13 for：【IDOR修复】学习资料文件上传补归属校验，防止覆盖他人资料-----------
        if (!securityUtil.isConsoleAuthenticated(r)) {
            String uid = getUserId(r);
            if (uid == null) return Result.error("未登录");
            if (!uid.equals(material.getUserId())) return Result.error("无权修改该资料");
        }
        //update-end---author:cursor ---date:2026-08-13 for：【IDOR修复】学习资料文件上传补归属校验-----------
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
