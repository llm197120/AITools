package org.jeecg.modules.homeai.user.controller;

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
import org.jeecg.common.constant.CommonConstant;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.common.util.oConvertUtils;
import io.swagger.v3.oas.annotations.Operation;
import org.jeecg.modules.homeai.config.HomeaiJwtUtil;
import org.jeecg.modules.homeai.family.entity.Family;
import org.jeecg.modules.homeai.family.entity.FamilyMember;
import org.jeecg.modules.homeai.family.service.IFamilyMemberService;
import org.jeecg.modules.homeai.family.service.IFamilyService;
import org.jeecg.modules.homeai.user.entity.WxUser;
import org.jeecg.modules.homeai.user.mapper.WxUserMapper;
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

/**
 * 微信用户管理
 */
@Slf4j
@RestController
@RequestMapping("/homeai/user")
public class WxUserController {

    @Autowired
    private IWxUserService wxUserService;

    @Autowired
    private WxUserMapper wxUserMapper;

    @Autowired
    private IFamilyService familyService;

    @Autowired
    private IFamilyMemberService familyMemberService;

    /**
     * 微信登录（code 换 JWT）
     */
    @PostMapping("/login")
    public Result<?> login(@RequestParam String code) {
        try {
            Map<String, Object> result = wxUserService.login(code);
            return Result.OK(result);
        } catch (Exception e) {
            return Result.error("登录失败: " + e.getMessage());
        }
    }

    /**
     * 刷新 Token
     */
    @PostMapping("/refresh-token")
    public Result<?> refreshToken(@RequestParam String refreshToken) {
        try {
            Map<String, Object> result = wxUserService.refreshToken(refreshToken);
            return Result.OK(result);
        } catch (Exception e) {
            return Result.error("Token 刷新失败: " + e.getMessage());
        }
    }

    /**
     * 获取当前用户信息
     * 从请求头 X-Access-Token 解析用户
     */
    @GetMapping("/info")
    public Result<?> getInfo(HttpServletRequest request) {
        String token = request.getHeader("X-Access-Token");
        if (token == null || token.isEmpty()) {
            return Result.error("未登录");
        }
        String openid = HomeaiJwtUtil.getOpenid(token);
        if (openid == null) {
            return Result.error("Token 无效");
        }
        WxUser user = wxUserService.getByOpenid(openid);
        return Result.OK(user);
    }

    /**
     * 用户列表（管理端）
     */
    @GetMapping("/list")
    @Operation(summary="微信用户-分页列表查询")
    @RequiresPermissions("homeai:user:list")
    public Result<?> list(WxUser wxUser,
                          @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                          @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                          HttpServletRequest req) {
        QueryWrapper<WxUser> queryWrapper = QueryGenerator.initQueryWrapper(wxUser, req.getParameterMap());
        Page<WxUser> page = new Page<>(pageNo, pageSize);
        IPage<WxUser> pageList = wxUserService.page(page, queryWrapper);
        fillFamilyName(pageList.getRecords());
        return Result.OK(pageList);
    }

    /** 用户下拉选项（管理端，日历筛选等） */
    @GetMapping("/options")
    @Operation(summary = "微信用户-下拉选项")
    @RequiresPermissions("homeai:plan:list")
    public Result<?> options() {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<WxUser> q =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        q.select(WxUser::getId, WxUser::getNickname, WxUser::getPhone)
                .orderByDesc(WxUser::getCreateTime)
                .last("LIMIT 200");
        List<WxUser> users = wxUserService.list(q);
        List<Map<String, String>> options = users.stream().map(u -> {
            Map<String, String> row = new LinkedHashMap<>();
            row.put("value", u.getId());
            String label = u.getNickname();
            if (oConvertUtils.isEmpty(label)) {
                label = u.getPhone();
            }
            if (oConvertUtils.isEmpty(label)) {
                label = u.getId();
            }
            row.put("label", label);
            return row;
        }).collect(Collectors.toList());
        return Result.OK(options);
    }

    /**
     * 用户详情（管理端）
     */
    @GetMapping("/{id}")
    @Operation(summary="微信用户-详情查询")
    @RequiresPermissions("homeai:user:list")
    public Result<?> getById(@PathVariable String id) {
        WxUser user = wxUserService.getById(id);
        return Result.OK(user);
    }

    /**
     * 编辑用户信息（管理端）
     */
    @PutMapping("/{id}")
    @AutoLog(value="微信用户-编辑")
    @Operation(summary="微信用户-编辑")
    @RequiresPermissions("homeai:user:edit")
    public Result<?> edit(@PathVariable String id, @RequestBody WxUser wxUser) {
        wxUser.setId(id);
        // 家庭关联不在此处处理，由专属接口 /{id}/family 维护，避免编辑其他字段时误操作
        wxUser.setFamilyId(null);
        wxUserService.updateById(wxUser);
        return Result.OK("编辑成功");
    }

    /**
     * 设置用户所属家庭（管理端）
     * body: { familyId: "xx" 或 "" 表示解除关联, role: "admin/member/restricted" 可选 }
     */
    @PutMapping("/{id}/family")
    @AutoLog(value="微信用户-设置所属家庭")
    @Operation(summary="微信用户-设置所属家庭")
    @RequiresPermissions("homeai:user:edit")
    public Result<?> setFamily(@PathVariable String id, @RequestBody Map<String, String> body) {
        WxUser user = wxUserService.getById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }
        String familyId = body.get("familyId");
        String role = body.get("role");
        // 记录原关联家庭（用于刷新成员数）
        String oldFamilyId = null;
        FamilyMember existing = familyMemberService.getByUserId(id);
        if (existing != null) {
            oldFamilyId = existing.getFamilyId();
        }
        if (oConvertUtils.isNotEmpty(familyId)) {
            // 校验家庭存在且未解散
            Family family = familyService.getById(familyId);
            if (family == null || Integer.valueOf(1).equals(family.getDelFlag())) {
                return Result.error("家庭不存在或已解散");
            }
            // 已有关联则更新，无关联则新增
            familyMemberService.setUserFamily(id, familyId, role);
            familyService.refreshMemberCount(familyId);
        } else {
            // 解除关联
            familyMemberService.setUserFamily(id, null, null);
        }
        if (oConvertUtils.isNotEmpty(oldFamilyId) && !oldFamilyId.equals(familyId)) {
            familyService.refreshMemberCount(oldFamilyId);
        }
        // 同步用户表缓存字段
        user.setFamilyId(familyId);
        wxUserService.updateById(user);
        return Result.OK("设置成功");
    }

    /**
     * 注销用户账号（物理删除）
     */
    @DeleteMapping("/{id}")
    @AutoLog(value="微信用户-注销")
    @Operation(summary="微信用户-注销")
    @RequiresPermissions("homeai:user:delete")
    public Result<?> delete(@PathVariable String id) {
        // 清理家庭关联
        String oldFamilyId = familyMemberService.removeUserFamily(id);
        if (oConvertUtils.isNotEmpty(oldFamilyId)) {
            familyService.refreshMemberCount(oldFamilyId);
        }
        wxUserService.removeById(id);
        return Result.OK("注销成功");
    }

    /**
     * 启用/禁用用户
     */
    @PutMapping("/{id}/status")
    @AutoLog(value="微信用户-启用/禁用")
    @Operation(summary="微信用户-启用/禁用")
    @RequiresPermissions("homeai:user:edit")
    public Result<?> updateStatus(@PathVariable String id, @RequestParam String status) {
        WxUser user = wxUserService.getById(id);
        if (user != null) {
            user.setStatus(status);
            wxUserService.updateById(user);
        }
        return Result.OK("操作成功");
    }

    //update-begin---author:admin ---date:2026-07-30  for：用户管理-新增/导入/导出/回收站功能-----------
    /**
     * 新增用户（管理端）
     */
    @PostMapping
    @AutoLog(value="微信用户-新增")
    @Operation(summary="微信用户-新增")
    @RequiresPermissions("homeai:user:add")
    public Result<?> add(@RequestBody WxUser wxUser) {
        wxUser.setDelFlag(0);
        if (wxUser.getStatus() == null) {
            wxUser.setStatus("1");
        }
        boolean saved = wxUserService.save(wxUser);
        if (!saved) {
            return Result.error("新增失败，请检查用户数据是否完整");
        }
        // 同步用户-家庭关联
        syncUserFamily(wxUser.getId(), wxUser.getFamilyId(), wxUser.getFamilyRoleType());
        return Result.OK("新增成功");
    }

    /**
     * 导出Excel
     */
    @GetMapping("/exportXls")
    @Operation(summary="微信用户-导出Excel")
    @RequiresPermissions("homeai:user:exportXls")
    public ModelAndView exportXls(HttpServletRequest request, WxUser wxUser) {
        QueryWrapper<WxUser> queryWrapper = QueryGenerator.initQueryWrapper(wxUser, request.getParameterMap());
        List<WxUser> pageList = wxUserService.list(queryWrapper);
        ModelAndView mv = new ModelAndView(new JeecgEntityExcelView());
        String selections = request.getParameter("selections");
        if (oConvertUtils.isEmpty(selections)) {
            mv.addObject(NormalExcelConstants.DATA_LIST, pageList);
        } else {
            List<String> selectionList = Arrays.asList(selections.split(","));
            List<WxUser> exportList = pageList.stream().filter(item -> selectionList.contains(item.getId())).collect(Collectors.toList());
            mv.addObject(NormalExcelConstants.DATA_LIST, exportList);
        }
        mv.addObject(NormalExcelConstants.FILE_NAME, "微信用户列表");
        mv.addObject(NormalExcelConstants.CLASS, WxUser.class);
        LoginUser user = null;
        try {
            Object principal = SecurityUtils.getSubject().getPrincipal();
            if (principal instanceof LoginUser) {
                user = (LoginUser) principal;
            }
        } catch (Exception ignored) {}
        mv.addObject(NormalExcelConstants.PARAMS, new ExportParams("微信用户列表数据", "导出人:" + (user != null ? user.getRealname() : "系统"), "导出信息", ExcelType.XSSF));
        return mv;
    }

    /**
     * 导出Excel模板
     */
    @GetMapping("/exportTemplate")
    @Operation(summary="微信用户-导出导入模板")
    @RequiresPermissions("homeai:user:exportXls")
    public ModelAndView exportTemplate() {
        ModelAndView mv = new ModelAndView(new JeecgEntityExcelView());
        mv.addObject(NormalExcelConstants.DATA_LIST, new ArrayList<WxUser>());
        mv.addObject(NormalExcelConstants.FILE_NAME, "微信用户导入模板");
        mv.addObject(NormalExcelConstants.CLASS, WxUser.class);
        mv.addObject(NormalExcelConstants.PARAMS, new ExportParams("微信用户导入模板", "模板", "导入模板", ExcelType.XSSF));
        return mv;
    }

    /**
     * 导入Excel
     */
    @PostMapping("/importExcel")
    @AutoLog(value="微信用户-导入Excel")
    @Operation(summary="微信用户-导入Excel")
    @RequiresPermissions("homeai:user:importExcel")
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
                    List<WxUser> list = ExcelImportUtil.importExcel(file.getInputStream(), WxUser.class, params);
                    for (WxUser item : list) {
                        try {
                            wxUserService.save(item);
                            successLines++;
                        } catch (Exception e) {
                            errorLines++;
                            errorMessage.add("第" + (successLines + errorLines) + "行: " + e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    log.error("导入失败", e);
                    return Result.error("导入失败: " + e.getMessage());
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
    @Operation(summary="微信用户-回收站列表")
    @RequiresPermissions("homeai:user:moveToRecycleBin")
    public Result<?> recycleBin(WxUser wxUser,
                                @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                HttpServletRequest req) {
        // 原生SQL分页查询回收站，避免逻辑删除自动追加 del_flag=0 导致查不到数据
        IPage<WxUser> pageList = wxUserMapper.selectRecycleBinPage(new Page<>(pageNo, pageSize), wxUser.getNickname(), wxUser.getPhone());
        return Result.OK(pageList);
    }

    //update-begin---author:admin ---date:2026-07-31  for：修复软删除/恢复失效问题（@TableLogic 字段不参与 updateById）-----------
    /**
     * 移入回收站（软删除）
     */
    @PutMapping("/moveToRecycleBin")
    @AutoLog(value="微信用户-移入回收站")
    @Operation(summary="微信用户-移入回收站")
    @RequiresPermissions("homeai:user:moveToRecycleBin")
    public Result<?> moveToRecycleBin(@RequestBody List<String> ids) {
        for (String id : ids) {
            // 解除家庭关联
            String oldFamilyId = familyMemberService.removeUserFamily(id);
            if (oConvertUtils.isNotEmpty(oldFamilyId)) {
                familyService.refreshMemberCount(oldFamilyId);
            }
            // @TableLogic 字段不参与 updateById，需通过 update wrapper 显式设置
            wxUserService.update(new LambdaUpdateWrapper<WxUser>()
                    .eq(WxUser::getId, id)
                    .set(WxUser::getDelFlag, 1)
                    .set(WxUser::getFamilyId, null));
        }
        return Result.OK("移入回收站成功");
    }

    /**
     * 从回收站恢复
     */
    @PutMapping("/restore")
    @AutoLog(value="微信用户-恢复")
    @Operation(summary="微信用户-恢复")
    @RequiresPermissions("homeai:user:restore")
    public Result<?> restore(@RequestBody List<String> ids) {
        for (String id : ids) {
            // 自定义原生 SQL 恢复，绕开逻辑删除拦截器自动注入的 del_flag=0 条件
            wxUserMapper.restoreById(id);
        }
        return Result.OK("恢复成功");
    }
    //update-end---author:admin ---date:2026-07-31  for：修复软删除/恢复失效问题（@TableLogic 字段不参与 updateById）-----------

    /**
     * 彻底删除
     */
    @DeleteMapping("/deletePermanently")
    @AutoLog(value="微信用户-彻底删除")
    @Operation(summary="微信用户-彻底删除")
    @RequiresPermissions("homeai:user:deletePermanently")
    public Result<?> deletePermanently(@RequestBody List<String> ids) {
        for (String id : ids) {
            // 清理家庭关联
            String oldFamilyId = familyMemberService.removeUserFamily(id);
            if (oConvertUtils.isNotEmpty(oldFamilyId)) {
                familyService.refreshMemberCount(oldFamilyId);
            }
        }
        wxUserMapper.deletePermanentlyByIds(ids);
        return Result.OK("彻底删除成功");
    }
    //update-end---author:admin ---date:2026-07-30  for：用户管理-新增/导入/导出/回收站功能-----------

    /**
     * 新增用户时同步家庭关联（add 场景）
     */
    private void syncUserFamily(String userId, String familyId, String role) {
        if (oConvertUtils.isEmpty(familyId)) {
            return;
        }
        Family family = familyService.getById(familyId);
        if (family != null && !Integer.valueOf(1).equals(family.getDelFlag())) {
            familyMemberService.setUserFamily(userId, familyId, role);
            familyService.refreshMemberCount(familyId);
        }
    }

    /**
     * 填充用户列表的家庭名称
     * 以 homeai_family_member 关联表为权威，兼容 wx_user.family_id 缓存字段与关联表不一致的历史数据
     */
    private void fillFamilyName(List<WxUser> users) {
        if (users == null || users.isEmpty()) {
            return;
        }
        List<String> userIds = users.stream().map(WxUser::getId).collect(Collectors.toList());
        // 1. 从关联表获取用户-家庭映射（权威）
        QueryWrapper<FamilyMember> memberQuery = new QueryWrapper<>();
        memberQuery.in("user_id", userIds);
        List<FamilyMember> members = familyMemberService.list(memberQuery);
        Map<String, String> userFamilyMap = new HashMap<>();
        for (FamilyMember m : members) {
            if (oConvertUtils.isNotEmpty(m.getFamilyId())) {
                userFamilyMap.put(m.getUserId(), m.getFamilyId());
            }
        }
        // 2. 收集家庭ID并批量查询名称
        Set<String> familyIds = new HashSet<>(userFamilyMap.values());
        // 兜底：wx_user.family_id 有值但关联表缺失时，以缓存字段为准
        for (WxUser user : users) {
            if (oConvertUtils.isNotEmpty(user.getFamilyId())) {
                familyIds.add(user.getFamilyId());
            }
        }
        if (familyIds.isEmpty()) {
            return;
        }
        List<Family> families = familyService.listByIds(familyIds);
        Map<String, String> familyNameMap = new HashMap<>();
        for (Family family : families) {
            if (family != null) {
                familyNameMap.put(family.getId(), family.getName());
            }
        }
        for (WxUser user : users) {
            // 优先使用关联表映射，其次使用 wx_user.family_id 缓存
            String familyId = userFamilyMap.getOrDefault(user.getId(), user.getFamilyId());
            if (oConvertUtils.isNotEmpty(familyId)) {
                user.setFamilyId(familyId);
                user.setFamilyName(familyNameMap.get(familyId));
            }
        }
    }
}
