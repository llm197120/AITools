package org.jeecg.modules.homeai.bill.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.homeai.bill.entity.BillEntry;
import org.jeecg.modules.homeai.bill.entity.BillCategory;
import org.jeecg.modules.homeai.bill.mapper.BillEntryMapper;
import org.jeecg.modules.homeai.bill.service.IBillEntryService;
import org.jeecg.modules.homeai.bill.service.IBillCategoryService;
import org.jeecg.modules.homeai.config.HomeaiJwtUtil;
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
@RequestMapping("/homeai/bill")
public class BillController {
    @Autowired private IBillEntryService billService;
    @Autowired private IBillCategoryService categoryService;
    @Autowired private IWxUserService wxUserService;
    @Autowired private BillEntryMapper billEntryMapper;

    private String getUserId(HttpServletRequest r) {
        String t = r.getHeader("X-Access-Token");
        String o = HomeaiJwtUtil.getOpenid(t);
        if (o == null) return null;
        var u = wxUserService.getByOpenid(o);
        return u != null ? u.getId() : null;
    }

    @PostMapping("/entry")
    public Result<?> add(@RequestBody BillEntry entry, HttpServletRequest r) {
        String uid = getUserId(r);
        if (uid == null) return Result.error("未登录");
        entry.setUserId(uid);
        return Result.OK(billService.add(entry));
    }

    @PutMapping("/entry")
    public Result<?> edit(@RequestBody BillEntry entry, HttpServletRequest r) {
        String uid = getUserId(r);
        if (uid == null) return Result.error("未登录");
        // 校验所有权：只能修改自己的账单
        BillEntry existing = billService.getById(entry.getId());
        if (existing == null) return Result.error("账单不存在");
        if (!uid.equals(existing.getUserId())) return Result.error("无权修改他人账单");
        try { return Result.OK(billService.update(entry)); }
        catch (RuntimeException e) { return Result.error(e.getMessage()); }
    }

    @DeleteMapping("/entry/{id}")
    public Result<?> delete(@PathVariable String id, HttpServletRequest r) {
        String uid = getUserId(r);
        billService.softDelete(id, uid);
        return Result.OK("删除成功");
    }

    @GetMapping("/summary")
    public Result<?> summary(HttpServletRequest r) {
        String uid = getUserId(r);
        if (uid == null) return Result.error("未登录");
        return Result.OK(billService.getMonthlySummary(uid));
    }

    @GetMapping("/entries")
    public Result<?> entries(@RequestParam String yearMonth, HttpServletRequest r) {
        String uid = getUserId(r);
        if (uid == null) return Result.error("未登录");
        return Result.OK(billService.getMonthList(uid, yearMonth));
    }

    @GetMapping("/stats")
    public Result<?> stats(@RequestParam String yearMonth, HttpServletRequest r) {
        String uid = getUserId(r);
        if (uid == null) return Result.error("未登录");
        List<Map<String, Object>> s = billService.getCategoryStats(uid, yearMonth);
        List<BillCategory> cats = categoryService.getEnabledCategories("expense");
        Map<String, BillCategory> catMap = new HashMap<>();
        for (BillCategory c : cats) {
            catMap.put(c.getId(), c);
        }
        for (Map<String, Object> m : s) {
            String cid = (String) m.get("categoryId");
            BillCategory c = catMap.get(cid);
            if (c != null) {
                m.put("name", c.getName());
                m.put("icon", c.getIcon());
                m.put("color", c.getColor());
            }
        }
        return Result.OK(s);
    }

    @GetMapping("/categories")
    public Result<?> categories(@RequestParam(required = false) String type) {
        return Result.OK(categoryService.getEnabledCategories(type));
    }

    @GetMapping("/category-list")
    public Result<?> listCategories(BillCategory cat,
        @RequestParam(defaultValue = "1") int pageNo, @RequestParam(defaultValue = "10") int pageSize, HttpServletRequest req) {
        QueryWrapper<BillCategory> qw = QueryGenerator.initQueryWrapper(cat, req.getParameterMap());
        return Result.OK(categoryService.page(new Page<>(pageNo, pageSize), qw));
    }

    @PostMapping("/category") public Result<?> addCat(@RequestBody BillCategory c, HttpServletRequest r) {
        String uid = getUserId(r); if (uid == null) return Result.error("未登录");
        c.setCreateBy(uid);
        categoryService.save(c); return Result.OK("OK"); }
    @PutMapping("/category") public Result<?> editCat(@RequestBody BillCategory c, HttpServletRequest r) {
        String uid = getUserId(r); if (uid == null) return Result.error("未登录");
        categoryService.updateById(c); return Result.OK("OK"); }
    @DeleteMapping("/category/{id}") public Result<?> delCat(@PathVariable String id, HttpServletRequest r) {
        String uid = getUserId(r); if (uid == null) return Result.error("未登录");
        categoryService.removeById(id); return Result.OK("OK"); }

    @GetMapping("/list")
    public Result<?> list(BillEntry e, @RequestParam(defaultValue = "1") int pageNo, @RequestParam(defaultValue = "10") int pageSize, HttpServletRequest req) {
        QueryWrapper<BillEntry> qw = QueryGenerator.initQueryWrapper(e, req.getParameterMap());
        qw.eq("del_flag", 0).orderByDesc("create_time");
        return Result.OK(billService.page(new Page<>(pageNo, pageSize), qw));
    }

    //update-begin---author:admin ---date:2026-07-30  for：账单管理-新增/导入/导出/回收站功能-----------
    /**
     * 新增账单（管理端）
     */
    @PostMapping("/add")
    public Result<?> addEntry(@RequestBody BillEntry entry) {
        entry.setDelFlag(0);
        billService.save(entry);
        return Result.OK("新增成功");
    }

    /**
     * 导出Excel
     */
    @GetMapping("/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, BillEntry billEntry) {
        QueryWrapper<BillEntry> queryWrapper = QueryGenerator.initQueryWrapper(billEntry, request.getParameterMap());
        List<BillEntry> pageList = billService.list(queryWrapper);
        ModelAndView mv = new ModelAndView(new JeecgEntityExcelView());
        String selections = request.getParameter("selections");
        if (oConvertUtils.isEmpty(selections)) {
            mv.addObject(NormalExcelConstants.DATA_LIST, pageList);
        } else {
            List<String> selectionList = Arrays.asList(selections.split(","));
            List<BillEntry> exportList = pageList.stream().filter(item -> selectionList.contains(item.getId())).collect(Collectors.toList());
            mv.addObject(NormalExcelConstants.DATA_LIST, exportList);
        }
        mv.addObject(NormalExcelConstants.FILE_NAME, "账单列表");
        mv.addObject(NormalExcelConstants.CLASS, BillEntry.class);
        LoginUser user = null;
        try {
            Object principal = SecurityUtils.getSubject().getPrincipal();
            if (principal instanceof LoginUser) {
                user = (LoginUser) principal;
            }
        } catch (Exception ignored) {}
        mv.addObject(NormalExcelConstants.PARAMS, new ExportParams("账单列表数据", "导出人:" + (user != null ? user.getRealname() : "系统"), "导出信息", ExcelType.XSSF));
        return mv;
    }

    /**
     * 导入Excel
     */
    @PostMapping("/importExcel")
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
                    List<BillEntry> list = ExcelImportUtil.importExcel(file.getInputStream(), BillEntry.class, params);
                    for (BillEntry item : list) {
                        try {
                            billService.save(item);
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
    public Result<?> recycleBin(BillEntry e, @RequestParam(defaultValue = "1") int pageNo, @RequestParam(defaultValue = "10") int pageSize, HttpServletRequest req) {
        // 原生SQL分页查询回收站，避免逻辑删除自动追加 del_flag=0 导致查不到数据
        return Result.OK(billEntryMapper.selectRecycleBinPage(new Page<>(pageNo, pageSize), e.getType(), e.getBillDate()));
    }

    /**
     * 移入回收站（软删除）
     */
    @PutMapping("/moveToRecycleBin")
    public Result<?> moveToRecycleBin(@RequestBody List<String> ids) {
        for (String id : ids) {
            billService.softDelete(id, null);
        }
        return Result.OK("移入回收站成功");
    }

    //update-begin---author:admin ---date:2026-07-31  for：修复软删除/恢复失效问题（@TableLogic 字段不参与 updateById）-----------
    /**
     * 从回收站恢复
     */
    @PutMapping("/restore")
    public Result<?> restore(@RequestBody List<String> ids) {
        for (String id : ids) {
            // 自定义原生 SQL 恢复，绕开逻辑删除拦截器自动注入的 del_flag=0 条件
            billEntryMapper.restoreById(id);
        }
        return Result.OK("恢复成功");
    }
    //update-end---author:admin ---date:2026-07-31  for：修复软删除/恢复失效问题（@TableLogic 字段不参与 updateById）-----------

    /**
     * 彻底删除
     */
    @DeleteMapping("/deletePermanently")
    public Result<?> deletePermanently(@RequestBody List<String> ids) {
        billEntryMapper.deletePermanentlyByIds(ids);
        return Result.OK("彻底删除成功");
    }

    /**
     * 编辑账单（管理端）
     */
    @PutMapping("/{id}")
    public Result<?> edit(@PathVariable String id, @RequestBody BillEntry entry) {
        entry.setId(id);
        billService.updateById(entry);
        return Result.OK("编辑成功");
    }
    //update-end---author:admin ---date:2026-07-30  for：账单管理-新增/导入/导出/回收站功能-----------
}
