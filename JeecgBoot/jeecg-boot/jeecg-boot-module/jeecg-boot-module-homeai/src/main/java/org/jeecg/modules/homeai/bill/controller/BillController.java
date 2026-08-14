package org.jeecg.modules.homeai.bill.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
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
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

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

    //update-begin---author:cursor ---date:2026-08-13 for：【体验优化】账单单条查询，供编辑页按 id 加载（避免整条数据塞 URL）-----------
    @GetMapping("/entry/{id}")
    public Result<?> getEntry(@PathVariable String id, HttpServletRequest r) {
        String uid = getUserId(r);
        if (uid == null) return Result.error("未登录");
        BillEntry entry = billService.getById(id);
        if (entry == null) return Result.error("账单不存在");
        if (!uid.equals(entry.getUserId())) return Result.error("无权查看他人账单");
        return Result.OK(entry);
    }
    //update-end---author:cursor ---date:2026-08-13 for：【体验优化】账单单条查询-----------

    @GetMapping("/summary")
    public Result<?> summary(@RequestParam(required = false) String yearMonth, HttpServletRequest r) {
        String uid = getUserId(r);
        if (uid == null) return Result.error("未登录");
        return Result.OK(billService.getMonthlySummary(uid, yearMonth));
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
    @Operation(summary="账单-消费分类列表(管理端)")
    @RequiresPermissions("homeai:bill:category:list")
    public Result<?> listCategories(BillCategory cat,
        @RequestParam(defaultValue = "1") int pageNo, @RequestParam(defaultValue = "10") int pageSize, HttpServletRequest req) {
        QueryWrapper<BillCategory> qw = QueryGenerator.initQueryWrapper(cat, req.getParameterMap());
        return Result.OK(categoryService.page(new Page<>(pageNo, pageSize), qw));
    }

    @PostMapping("/category")
    @AutoLog(value="账单-新增消费分类")
    @Operation(summary="账单-新增消费分类")
    @RequiresPermissions("homeai:bill:category:add")
    public Result<?> addCat(@RequestBody BillCategory c, HttpServletRequest r) {
        String uid = getUserId(r); if (uid == null) return Result.error("未登录");
        c.setCreateBy(uid);
        categoryService.save(c); return Result.OK("OK"); }
    @PutMapping("/category")
    @AutoLog(value="账单-编辑消费分类")
    @Operation(summary="账单-编辑消费分类")
    @RequiresPermissions("homeai:bill:category:edit")
    public Result<?> editCat(@RequestBody BillCategory c, HttpServletRequest r) {
        String uid = getUserId(r); if (uid == null) return Result.error("未登录");
        categoryService.updateById(c); return Result.OK("OK"); }
    @DeleteMapping("/category/{id}")
    @AutoLog(value="账单-删除消费分类")
    @Operation(summary="账单-删除消费分类")
    @RequiresPermissions("homeai:bill:category:delete")
    public Result<?> delCat(@PathVariable String id, HttpServletRequest r) {
        String uid = getUserId(r); if (uid == null) return Result.error("未登录");
        categoryService.removeById(id); return Result.OK("OK"); }

    @GetMapping("/list")
    @Operation(summary="账单-分页列表查询(管理端)")
    @RequiresPermissions("homeai:bill:list")
    public Result<?> list(BillEntry e, @RequestParam(defaultValue = "1") int pageNo, @RequestParam(defaultValue = "10") int pageSize, HttpServletRequest req) {
        QueryWrapper<BillEntry> qw = QueryGenerator.initQueryWrapper(e, req.getParameterMap());
        qw.eq("del_flag", 0).orderByDesc("create_time");
        IPage<BillEntry> pageList = billService.page(new Page<>(pageNo, pageSize), qw);
        // 填充分类名称，便于前端展示
        billService.fillCategoryNames(pageList.getRecords());
        return Result.OK(pageList);
    }

    //update-begin---author:admin ---date:2026-07-30  for：账单管理-新增/导入/导出/回收站功能-----------
    /**
     * 新增账单（管理端）
     */
    @PostMapping("/add")
    @AutoLog(value="账单-新增(管理端)")
    @Operation(summary="账单-新增(管理端)")
    @RequiresPermissions("homeai:bill:add")
    public Result<?> addEntry(@RequestBody BillEntry entry) {
        entry.setDelFlag(0);
        billService.save(entry);
        return Result.OK(entry);
    }

    /**
     * 导出Excel
     */
    @GetMapping("/exportXls")
    @Operation(summary="账单-导出Excel")
    @RequiresPermissions("homeai:bill:exportXls")
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
    @AutoLog(value="账单-导入Excel")
    @Operation(summary="账单-导入Excel")
    @RequiresPermissions("homeai:bill:importExcel")
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
    @Operation(summary="账单-回收站列表")
    @RequiresPermissions("homeai:bill:moveToRecycleBin")
    public Result<?> recycleBin(BillEntry e, @RequestParam(defaultValue = "1") int pageNo, @RequestParam(defaultValue = "10") int pageSize, HttpServletRequest req) {
        // 原生SQL分页查询回收站，避免逻辑删除自动追加 del_flag=0 导致查不到数据
        return Result.OK(billEntryMapper.selectRecycleBinPage(new Page<>(pageNo, pageSize), e.getType(), e.getBillDate()));
    }

    /**
     * 移入回收站（软删除）
     */
    @PutMapping("/moveToRecycleBin")
    @AutoLog(value="账单-移入回收站")
    @Operation(summary="账单-移入回收站")
    @RequiresPermissions("homeai:bill:moveToRecycleBin")
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
    @AutoLog(value="账单-恢复")
    @Operation(summary="账单-恢复")
    @RequiresPermissions("homeai:bill:restore")
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
    @AutoLog(value="账单-彻底删除")
    @Operation(summary="账单-彻底删除")
    @RequiresPermissions("homeai:bill:deletePermanently")
    public Result<?> deletePermanently(@RequestBody List<String> ids) {
        billEntryMapper.deletePermanentlyByIds(ids);
        return Result.OK("彻底删除成功");
    }

    /**
     * 编辑账单（管理端）
     */
    @PutMapping("/{id}")
    @AutoLog(value="账单-编辑(管理端)")
    @Operation(summary="账单-编辑(管理端)")
    @RequiresPermissions("homeai:bill:edit")
    public Result<?> edit(@PathVariable String id, @RequestBody BillEntry entry) {
        entry.setId(id);
        billService.updateById(entry);
        return Result.OK(entry);
    }

    /**
     * 统计报表（管理端）
     * @param yearMonth 月份 YYYY-MM，为空取当月
     * @param dimension 维度: category/user/month
     */
    @GetMapping("/admin/stats")
    @Operation(summary="账单-统计报表(管理端)")
    @RequiresPermissions("homeai:bill:list")
    public Result<?> adminStats(@RequestParam(required = false) String yearMonth,
                                @RequestParam(defaultValue = "category") String dimension) {
        return Result.OK(billService.getAdminStats(yearMonth, dimension));
    }

    /**
     * 账单导入-预览解析（管理端）
     * @param file CSV 或 Excel 文件
     * @param type wechat_csv / excel
     */
    @PostMapping("/import/preview")
    @AutoLog(value="账单-导入预览")
    @Operation(summary="账单-导入预览(管理端)")
    @RequiresPermissions("homeai:bill:importExcel")
    public Result<?> importPreview(@RequestParam MultipartFile file,
                                   @RequestParam(defaultValue = "wechat_csv") String type) {
        try {
            List<Map<String, Object>> rows = "excel".equals(type)
                    ? parseExcelBill(file) : parseWechatCsvBill(file);
            return Result.OK(rows);
        } catch (Exception e) {
            log.error("账单导入解析失败", e);
            return Result.error("解析失败: " + e.getMessage());
        }
    }

    /**
     * 账单导入-确认写入（管理端）
     * body: { userId: "可选", entries: [{billDate,type,categoryId,amount,remark,paymentMethod}] }
     */
    @PostMapping("/import/confirm")
    @AutoLog(value="账单-导入确认写入")
    @Operation(summary="账单-导入确认写入(管理端)")
    @RequiresPermissions("homeai:bill:importExcel")
    public Result<?> importConfirm(@RequestBody Map<String, Object> body) {
        String userId = body.get("userId") != null ? String.valueOf(body.get("userId")) : null;
        Object entriesObj = body.get("entries");
        if (!(entriesObj instanceof List) || ((List<?>) entriesObj).isEmpty()) {
            return Result.error("没有可导入的数据");
        }
        int success = 0;
        List<String> errors = new ArrayList<>();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> entries = (List<Map<String, Object>>) entriesObj;
        for (Map<String, Object> e : entries) {
            try {
                BillEntry entry = new BillEntry();
                entry.setUserId(userId);
                entry.setBillDate(java.time.LocalDate.parse(String.valueOf(e.get("billDate"))));
                entry.setType(String.valueOf(e.get("type")));
                entry.setCategoryId(e.get("categoryId") != null ? String.valueOf(e.get("categoryId")) : null);
                entry.setAmount(new BigDecimal(String.valueOf(e.get("amount"))));
                entry.setRemark(e.get("remark") != null ? String.valueOf(e.get("remark")) : null);
                entry.setPaymentMethod(e.get("paymentMethod") != null ? String.valueOf(e.get("paymentMethod")) : "微信");
                entry.setSource("import");
                billService.save(entry);
                success++;
            } catch (Exception ex) {
                errors.add(ex.getMessage());
            }
        }
        return Result.OK("导入完成！成功: " + success + " 条, 失败: " + errors.size() + " 条");
    }

    /** 小程序端：账单导入预览 */
    @PostMapping("/app/import/preview")
    @Operation(summary = "账单-导入预览(小程序)")
    public Result<?> appImportPreview(@RequestParam MultipartFile file,
                                      @RequestParam(defaultValue = "wechat_csv") String type,
                                      HttpServletRequest r) {
        String uid = getUserId(r);
        if (uid == null) return Result.error("未登录");
        try {
            List<Map<String, Object>> rows = "excel".equals(type)
                    ? parseExcelBill(file) : parseWechatCsvBill(file);
            return Result.OK(rows);
        } catch (Exception e) {
            log.error("小程序账单导入解析失败", e);
            return Result.error("解析失败: " + e.getMessage());
        }
    }

    /** 小程序端：账单导入确认写入 */
    @PostMapping("/app/import/confirm")
    @Operation(summary = "账单-导入确认(小程序)")
    public Result<?> appImportConfirm(@RequestBody Map<String, Object> body, HttpServletRequest r) {
        String uid = getUserId(r);
        if (uid == null) return Result.error("未登录");
        body.put("userId", uid);
        return importConfirm(body);
    }

    /**
     * 解析微信支付 CSV 账单（自动定位表头行，兼容不同导出格式）
     */
    private List<Map<String, Object>> parseWechatCsvBill(MultipartFile file) throws Exception {
        List<Map<String, Object>> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            // 若为 UTF-8 BOM，跳过 BOM
            String line;
            int headerIndex = -1;
            String[] header = null;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (lineNo == 1 && line.startsWith("\uFEFF")) {
                    line = line.substring(1);
                }
                String[] cols = splitCsvLine(line);
                // 定位表头：包含“交易时间”与“收/支”
                if (headerIndex < 0 && containsAny(cols, "交易时间") && containsAny(cols, "收/支")) {
                    headerIndex = lineNo;
                    header = cols;
                    continue;
                }
                if (headerIndex > 0 && cols.length >= header.length && line.trim().length() > 0) {
                    if ("----------------------".equals(line.trim())) {
                        continue;
                    }
                    Map<String, Object> row = mapWechatRow(header, cols, rows.size() + 1);
                    if (row != null) {
                        rows.add(row);
                    }
                }
            }
        }
        if (rows.isEmpty()) {
            throw new RuntimeException("未识别到有效账单数据，请确认文件为微信支付导出的 CSV");
        }
        return rows;
    }

    private Map<String, Object> mapWechatRow(String[] header, String[] cols, int index) {
        int dateIdx = idxOf(header, "交易时间");
        int typeIdx = idxOf(header, "收/支");
        int amountIdx = idxOf(header, "金额(元)");
        int payIdx = idxOf(header, "支付方式");
        int remarkIdx = idxOf(header, "商品") >= 0 ? idxOf(header, "商品") : idxOf(header, "备注");
        if (dateIdx < 0 || typeIdx < 0 || amountIdx < 0) {
            return null;
        }
        String type = cols[typeIdx].trim();
        if (!"支出".equals(type) && !"收入".equals(type)) {
            return null;
        }
        String dateStr = cols[dateIdx].trim();
        if (dateStr.length() < 10) {
            return null;
        }
        String amountStr = cols[amountIdx].trim().replace("¥", "").replace(",", "");
        BigDecimal amount;
        try {
            amount = new BigDecimal(amountStr);
        } catch (Exception e) {
            return null;
        }
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("index", index);
        row.put("billDate", dateStr.substring(0, 10));
        row.put("type", "支出".equals(type) ? "expense" : "income");
        row.put("amount", amount);
        row.put("paymentMethod", payIdx >= 0 && cols.length > payIdx ? cols[payIdx].trim() : "微信");
        row.put("remark", remarkIdx >= 0 && cols.length > remarkIdx ? cols[remarkIdx].trim() : null);
        // 分类映射 + 去重
        BillCategory cat = matchCategory(String.valueOf(row.get("type")),
                remarkIdx >= 0 && cols.length > remarkIdx ? cols[remarkIdx].trim() : null);
        row.put("categoryId", cat != null ? cat.getId() : null);
        row.put("categoryName", cat != null ? cat.getName() : null);
        row.put("duplicate", isDuplicate(row));
        row.put("valid", true);
        return row;
    }

    /**
     * 解析 Excel 账单（首行为表头：日期/类型/分类/金额/备注/支付方式）
     */
    private List<Map<String, Object>> parseExcelBill(MultipartFile file) throws Exception {
        List<Map<String, Object>> rows = new ArrayList<>();
        org.apache.poi.ss.usermodel.Workbook workbook =
                org.apache.poi.ss.usermodel.WorkbookFactory.create(file.getInputStream());
        org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(0);
        int idx = 0;
        for (org.apache.poi.ss.usermodel.Row r : sheet) {
            if (r.getRowNum() == 0) {
                continue; // 表头
            }
            String dateStr = cellStr(r.getCell(0));
            String type = cellStr(r.getCell(1));
            String category = cellStr(r.getCell(2));
            String amountStr = cellStr(r.getCell(3));
            String remark = r.getLastCellNum() > 4 ? cellStr(r.getCell(4)) : null;
            String pay = r.getLastCellNum() > 5 ? cellStr(r.getCell(5)) : "微信";
            if (dateStr.isEmpty() || amountStr.isEmpty()) {
                continue;
            }
            BigDecimal amount;
            try {
                amount = new BigDecimal(amountStr);
            } catch (Exception e) {
                continue;
            }
            String typeNorm = type.contains("收") ? "income" : "expense";
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("index", ++idx);
            row.put("billDate", dateStr.length() >= 10 ? dateStr.substring(0, 10) : dateStr);
            row.put("type", typeNorm);
            row.put("amount", amount);
            row.put("paymentMethod", pay.isEmpty() ? "微信" : pay);
            row.put("remark", remark);
            BillCategory cat = matchCategory(typeNorm, category);
            row.put("categoryId", cat != null ? cat.getId() : null);
            row.put("categoryName", cat != null ? cat.getName() : null);
            row.put("duplicate", isDuplicate(row));
            row.put("valid", true);
            rows.add(row);
        }
        workbook.close();
        if (rows.isEmpty()) {
            throw new RuntimeException("Excel 中未解析到有效账单数据");
        }
        return rows;
    }

    private String cellStr(org.apache.poi.ss.usermodel.Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue().trim();
            case NUMERIC:
                if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                    return new java.text.SimpleDateFormat("yyyy-MM-dd").format(cell.getDateCellValue());
                }
                return String.valueOf(cell.getNumericCellValue()).trim();
            default: return "";
        }
    }

    private BillCategory matchCategory(String type, String name) {
        List<BillCategory> cats = categoryService.getEnabledCategories(type);
        if (name == null || name.isEmpty()) {
            return cats.stream().filter(c -> Integer.valueOf(1).equals(c.getIsDefault())).findFirst()
                    .orElse(cats.isEmpty() ? null : cats.get(0));
        }
        String n = name.trim();
        return cats.stream().filter(c -> n.equals(c.getName())).findFirst()
                .orElse(cats.stream().filter(c -> Integer.valueOf(1).equals(c.getIsDefault())).findFirst()
                        .orElse(cats.isEmpty() ? null : cats.get(0)));
    }

    private boolean isDuplicate(Map<String, Object> row) {
        String date = String.valueOf(row.get("billDate"));
        String amount = String.valueOf(row.get("amount"));
        String remark = row.get("remark") != null ? String.valueOf(row.get("remark")) : "";
        LambdaQueryWrapper<BillEntry> q = new LambdaQueryWrapper<>();
        q.eq(BillEntry::getDelFlag, 0)
                .eq(BillEntry::getBillDate, java.time.LocalDate.parse(date))
                .eq(BillEntry::getAmount, new BigDecimal(amount))
                .eq(oConvertUtils.isNotEmpty(remark), BillEntry::getRemark, remark)
                .last("LIMIT 1");
        return billService.count(q) > 0;
    }

    private int idxOf(String[] arr, String key) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] != null && arr[i].contains(key)) return i;
        }
        return -1;
    }

    private boolean containsAny(String[] arr, String key) {
        return idxOf(arr, key) >= 0;
    }

    private String[] splitCsvLine(String line) {
        // 简单 CSV 分割：支持引号包裹的逗号
        List<String> cols = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuote = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuote && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    sb.append('"');
                    i++;
                } else {
                    inQuote = !inQuote;
                }
            } else if (c == ',' && !inQuote) {
                cols.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        cols.add(sb.toString());
        return cols.toArray(new String[0]);
    }
    //update-end---author:admin ---date:2026-07-30  for：账单管理-新增/导入/导出/回收站功能-----------
}
