package org.jeecg.modules.homeai.plan.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
import org.jeecg.modules.homeai.config.HomeaiSecurityUtil;
import org.jeecg.modules.homeai.config.HomeaiJwtUtil;
import org.jeecg.modules.homeai.audit.service.IHomeaiAuditLogService;
import org.jeecg.modules.homeai.plan.entity.PlanCategory;
import org.jeecg.modules.homeai.plan.entity.PlanMaster;
import org.jeecg.modules.homeai.plan.entity.PlanInstance;
import org.jeecg.modules.homeai.plan.mapper.PlanMasterMapper;
import org.jeecg.modules.homeai.plan.service.IPlanCategoryService;
import org.jeecg.modules.homeai.plan.service.IPlanService;
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

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/homeai/plan")
public class PlanController {

    @Autowired
    private IPlanService planService;

    @Autowired
    private IWxUserService wxUserService;

    @Autowired
    private PlanMasterMapper planMasterMapper;

    @Autowired
    private HomeaiSecurityUtil securityUtil;

    @Autowired
    private IPlanCategoryService planCategoryService;

    @Autowired
    private IHomeaiAuditLogService auditLogService;

    private static final String ACTION_PLAN_ROLL_FORWARD = "plan_repeat_roll_forward";

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
        String t = r.getHeader("X-Access-Token");
        String o = HomeaiJwtUtil.getOpenid(t);
        if (o == null) return null;
        var u = wxUserService.getByOpenid(o);
        return u != null ? u.getId() : null;
    }

    private String getOperatorId(HttpServletRequest r) {
        try {
            if (SecurityUtils.getSubject() != null && SecurityUtils.getSubject().isAuthenticated()) {
                Object principal = SecurityUtils.getSubject().getPrincipal();
                if (principal instanceof LoginUser) {
                    return ((LoginUser) principal).getId();
                }
            }
        } catch (Exception ignored) {
        }
        return getUserId(r);
    }

    private String clientIp(HttpServletRequest r) {
        if (r == null) {
            return null;
        }
        String ip = r.getHeader("X-Forwarded-For");
        if (oConvertUtils.isNotEmpty(ip)) {
            return ip.split(",")[0].trim();
        }
        return r.getRemoteAddr();
    }

    /** 创建计划 */
    @PostMapping
    public Result<?> create(@RequestBody PlanMaster plan, HttpServletRequest r) {
        String uid = getUserId(r);
        if (uid == null) return Result.error("未登录");
        plan.setUserId(uid);
        return Result.OK(planService.createPlan(plan));
    }

    /** 获取日历概览 */
    @GetMapping("/calendar")
    public Result<?> calendar(@RequestParam String yearMonth, HttpServletRequest r) {
        String uid = getUserId(r);
        if (uid == null) return Result.error("未登录");
        return Result.OK(planService.getCalendarSummary(uid, yearMonth));
    }

    /** 获取某日计划 */
    @GetMapping("/date/{date}")
    public Result<?> byDate(@PathVariable String date, HttpServletRequest r) {
        String uid = getUserId(r);
        if (uid == null) return Result.error("未登录");
        return Result.OK(planService.getInstancesByDate(uid, LocalDate.parse(date)));
    }

    /** 切换完成状态 */
    @PutMapping("/instance/{id}/toggle")
    public Result<?> toggle(@PathVariable String id, HttpServletRequest r) {
        // 管理端控制台可操作任意计划；小程序端只能操作自己的计划
        PlanInstance instance = planService.getInstanceById(id);
        if (instance == null) {
            return Result.error("计划不存在");
        }
        PlanMaster master = planService.getById(instance.getMasterId());
        if (master == null) {
            return Result.error("计划不存在");
        }
        if (!securityUtil.isConsoleAuthenticated(r)) {
            String uid = getUserId(r);
            if (uid == null) return Result.error("未登录");
            if (!uid.equals(master.getUserId())) {
                return Result.error("无权操作该计划");
            }
        }
        if ("expired".equals(instance.getStatus())) {
            return Result.error("已过期计划不可切换");
        }
        try {
            planService.toggleInstanceStatus(id);
        } catch (org.jeecg.common.exception.JeecgBootException e) {
            return Result.error(e.getMessage());
        }
        return Result.OK("OK");
    }

    /** 管理端分页 */
    @GetMapping("/list")
    @Operation(summary="计划-分页列表查询(管理端)")
    @RequiresPermissions("homeai:plan:list")
    public Result<?> list(PlanMaster m, @RequestParam(defaultValue = "1") int pageNo,
                          @RequestParam(defaultValue = "10") int pageSize, HttpServletRequest req) {
        QueryWrapper<PlanMaster> qw = QueryGenerator.initQueryWrapper(m, req.getParameterMap());
        qw.eq("del_flag", 0).orderByDesc("create_time");
        return Result.OK(planService.page(new Page<>(pageNo, pageSize), qw));
    }

    /**
     * 计划完成率统计（管理端）
     */
    @GetMapping("/admin/completion")
    @Operation(summary="计划-完成率统计(管理端)")
    @RequiresPermissions("homeai:plan:list")
    public Result<?> completion(@RequestParam(required = false) String userId,
                                @RequestParam(required = false) String yearMonth) {
        return Result.OK(planService.getCompletionStats(userId, yearMonth));
    }

    /** 管理端日历：某月计划摘要（实例维度） */
    @GetMapping("/admin/calendar")
    @Operation(summary = "计划-日历概览(管理端)")
    @RequiresPermissions("homeai:plan:list")
    public Result<?> adminCalendar(@RequestParam String yearMonth,
                                   @RequestParam(required = false) String userId) {
        return Result.OK(planService.getAdminCalendarSummary(yearMonth, userId));
    }

    /** 管理端日历：某日计划实例 */
    @GetMapping("/admin/date/{date}")
    @Operation(summary = "计划-某日列表(管理端)")
    @RequiresPermissions("homeai:plan:list")
    public Result<?> adminByDate(@PathVariable String date,
                                 @RequestParam(required = false) String userId) {
        return Result.OK(planService.getAdminInstancesByDate(LocalDate.parse(date), userId));
    }

    /** 管理端：手动补跑重复计划实例 */
    @PostMapping("/admin/repeat/roll-forward")
    @AutoLog(value = "计划-补跑重复实例")
    @Operation(summary = "计划-补跑重复实例(管理端)")
    @RequiresPermissions("homeai:plan:edit")
    public Result<?> rollForwardRepeat(@RequestParam(required = false) String masterId,
                                       HttpServletRequest request) {
        try {
            Map<String, Object> data = planService.rollForwardRepeatInstances(masterId);
            String summary = "all".equals(data.get("scope"))
                    ? "全量补跑重复计划，新建 " + data.get("created") + " 条实例"
                    : "补跑计划「" + data.get("masterTitle") + "」，新建 " + data.get("created") + " 条实例";
            auditLogService.record(
                    getOperatorId(request),
                    ACTION_PLAN_ROLL_FORWARD,
                    "plan",
                    masterId,
                    summary,
                    data,
                    "success",
                    clientIp(request));
            return Result.OK(data);
        } catch (org.jeecg.common.exception.JeecgBootException e) {
            auditLogService.record(
                    getOperatorId(request),
                    ACTION_PLAN_ROLL_FORWARD,
                    "plan",
                    masterId,
                    e.getMessage(),
                    Collections.singletonMap("masterId", masterId),
                    "fail",
                    clientIp(request));
            return Result.error(e.getMessage());
        }
    }

    /** 管理端：补跑操作日志 */
    @GetMapping("/admin/repeat/roll-forward/logs")
    @Operation(summary = "计划-补跑日志(管理端)")
    @RequiresPermissions("homeai:plan:list")
    public Result<?> rollForwardLogs(@RequestParam(defaultValue = "1") int pageNo,
                                     @RequestParam(defaultValue = "10") int pageSize) {
        return Result.OK(auditLogService.pageByAction(ACTION_PLAN_ROLL_FORWARD, pageNo, pageSize));
    }

    //update-begin---author:admin ---date:2026-07-30  for：计划管理-新增/导入/导出/回收站功能-----------
    /**
     * 新增计划（管理端）
     */
    @PostMapping("/add")
    @AutoLog(value="计划-新增(管理端)")
    @Operation(summary="计划-新增(管理端)")
    @RequiresPermissions("homeai:plan:add")
    public Result<?> add(@RequestBody PlanMaster plan) {
        plan.setDelFlag(0);
        planService.save(plan);
        return Result.OK("新增成功");
    }

    /**
     * 编辑计划（管理端）
     */
    @PutMapping("/{id}")
    @AutoLog(value="计划-编辑(管理端)")
    @Operation(summary="计划-编辑(管理端)")
    @RequiresPermissions("homeai:plan:edit")
    public Result<?> edit(@PathVariable String id, @RequestBody PlanMaster plan) {
        plan.setId(id);
        planService.updateById(plan);
        return Result.OK("编辑成功");
    }

    /**
     * 导出Excel
     */
    @GetMapping("/exportXls")
    @Operation(summary="计划-导出Excel")
    @RequiresPermissions("homeai:plan:exportXls")
    public ModelAndView exportXls(HttpServletRequest request, PlanMaster planMaster) {
        QueryWrapper<PlanMaster> queryWrapper = QueryGenerator.initQueryWrapper(planMaster, request.getParameterMap());
        List<PlanMaster> pageList = planService.list(queryWrapper);
        ModelAndView mv = new ModelAndView(new JeecgEntityExcelView());
        String selections = request.getParameter("selections");
        if (oConvertUtils.isEmpty(selections)) {
            mv.addObject(NormalExcelConstants.DATA_LIST, pageList);
        } else {
            List<String> selectionList = Arrays.asList(selections.split(","));
            List<PlanMaster> exportList = pageList.stream().filter(item -> selectionList.contains(item.getId())).collect(Collectors.toList());
            mv.addObject(NormalExcelConstants.DATA_LIST, exportList);
        }
        mv.addObject(NormalExcelConstants.FILE_NAME, "计划列表");
        mv.addObject(NormalExcelConstants.CLASS, PlanMaster.class);
        LoginUser user = null;
        try {
            Object principal = SecurityUtils.getSubject().getPrincipal();
            if (principal instanceof LoginUser) {
                user = (LoginUser) principal;
            }
        } catch (Exception ignored) {}
        mv.addObject(NormalExcelConstants.PARAMS, new ExportParams("计划列表数据", "导出人:" + (user != null ? user.getRealname() : "系统"), "导出信息", ExcelType.XSSF));
        return mv;
    }

    /**
     * 导入Excel
     */
    @PostMapping("/importExcel")
    @AutoLog(value="计划-导入Excel")
    @Operation(summary="计划-导入Excel")
    @RequiresPermissions("homeai:plan:importExcel")
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
                    List<PlanMaster> list = ExcelImportUtil.importExcel(file.getInputStream(), PlanMaster.class, params);
                    for (PlanMaster item : list) {
                        try {
                            planService.save(item);
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
    @Operation(summary="计划-回收站列表")
    @RequiresPermissions("homeai:plan:moveToRecycleBin")
    public Result<?> recycleBin(PlanMaster m, @RequestParam(defaultValue = "1") int pageNo,
                                @RequestParam(defaultValue = "10") int pageSize, HttpServletRequest req) {
        // 原生SQL分页查询回收站，避免逻辑删除自动追加 del_flag=0 导致查不到数据
        return Result.OK(planMasterMapper.selectRecycleBinPage(new Page<>(pageNo, pageSize), m.getTitle(), m.getCategory()));
    }

    //update-begin---author:admin ---date:2026-07-31  for：修复软删除/恢复失效问题（@TableLogic 字段不参与 updateById）-----------
    /**
     * 移入回收站（软删除）
     */
    @PutMapping("/moveToRecycleBin")
    @AutoLog(value="计划-移入回收站")
    @Operation(summary="计划-移入回收站")
    @RequiresPermissions("homeai:plan:moveToRecycleBin")
    public Result<?> moveToRecycleBin(@RequestBody List<String> ids) {
        for (String id : ids) {
            // @TableLogic 字段不参与 updateById，需通过 update wrapper 显式设置
            planService.update(new LambdaUpdateWrapper<PlanMaster>()
                    .eq(PlanMaster::getId, id)
                    .set(PlanMaster::getDelFlag, 1));
        }
        return Result.OK("移入回收站成功");
    }

    /**
     * 从回收站恢复
     */
    @PutMapping("/restore")
    @AutoLog(value="计划-恢复")
    @Operation(summary="计划-恢复")
    @RequiresPermissions("homeai:plan:restore")
    public Result<?> restore(@RequestBody List<String> ids) {
        for (String id : ids) {
            // 自定义原生 SQL 恢复，绕开逻辑删除拦截器自动注入的 del_flag=0 条件
            planMasterMapper.restoreById(id);
        }
        return Result.OK("恢复成功");
    }
    //update-end---author:admin ---date:2026-07-31  for：修复软删除/恢复失效问题（@TableLogic 字段不参与 updateById）-----------

    /**
     * 彻底删除
     */
    @DeleteMapping("/deletePermanently")
    @AutoLog(value="计划-彻底删除")
    @Operation(summary="计划-彻底删除")
    @RequiresPermissions("homeai:plan:deletePermanently")
    public Result<?> deletePermanently(@RequestBody List<String> ids) {
        planMasterMapper.deletePermanentlyByIds(ids);
        return Result.OK("彻底删除成功");
    }
    //update-end---author:admin ---date:2026-07-30  for：计划管理-新增/导入/导出/回收站功能-----------

    //update-begin---author:admin ---date:2026-08-04  for：计划分类独立管理-----------
    /** 获取启用的计划分类（下拉选项） */
    @GetMapping("/categories")
    @Operation(summary = "计划分类-启用列表")
    public Result<?> categories() {
        return Result.OK(planCategoryService.getEnabledCategories());
    }

    /** 计划分类分页列表（管理端） */
    @GetMapping("/category-list")
    @Operation(summary = "计划分类-分页列表(管理端)")
    @RequiresPermissions("homeai:plan:category:list")
    public Result<?> categoryList(PlanCategory c,
                                  @RequestParam(defaultValue = "1") int pageNo,
                                  @RequestParam(defaultValue = "10") int pageSize,
                                  HttpServletRequest req) {
        QueryWrapper<PlanCategory> qw = QueryGenerator.initQueryWrapper(c, req.getParameterMap());
        qw.orderByAsc("sort_order");
        return Result.OK(planCategoryService.page(new Page<>(pageNo, pageSize), qw));
    }

    @PostMapping("/category")
    @AutoLog(value = "计划分类-新增")
    @Operation(summary = "计划分类-新增")
    @RequiresPermissions("homeai:plan:category:add")
    public Result<?> addCategory(@RequestBody PlanCategory c) {
        if (c.getIsEnabled() == null) c.setIsEnabled(1);
        if (c.getSortOrder() == null) c.setSortOrder(0);
        planCategoryService.save(c);
        return Result.OK("OK");
    }

    @PutMapping("/category")
    @AutoLog(value = "计划分类-编辑")
    @Operation(summary = "计划分类-编辑")
    @RequiresPermissions("homeai:plan:category:edit")
    public Result<?> editCategory(@RequestBody PlanCategory c) {
        planCategoryService.updateById(c);
        return Result.OK("OK");
    }

    @DeleteMapping("/category/{id}")
    @AutoLog(value = "计划分类-删除")
    @Operation(summary = "计划分类-删除")
    @RequiresPermissions("homeai:plan:category:delete")
    public Result<?> deleteCategory(@PathVariable String id) {
        planCategoryService.removeById(id);
        return Result.OK("OK");
    }
    //update-end---author:admin ---date:2026-08-04  for：计划分类独立管理-----------
}
