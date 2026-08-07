package org.jeecg.modules.homeai.family.controller;

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
import org.jeecg.modules.homeai.config.HomeaiJwtUtil;
import org.jeecg.modules.homeai.family.entity.Family;
import org.jeecg.modules.homeai.family.entity.FamilyInviteCode;
import org.jeecg.modules.homeai.family.entity.FamilyMember;
import org.jeecg.modules.homeai.family.mapper.FamilyMapper;
import org.jeecg.modules.homeai.family.service.IFamilyInviteCodeService;
import org.jeecg.modules.homeai.family.service.IFamilyMemberService;
import org.jeecg.modules.homeai.family.service.IFamilyService;
import org.jeecg.modules.homeai.user.entity.WxUser;
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
@RequestMapping("/homeai/family")
public class FamilyController {

    @Autowired
    private IFamilyService familyService;

    @Autowired
    private IFamilyMemberService familyMemberService;

    @Autowired
    private IFamilyInviteCodeService familyInviteCodeService;

    @Autowired
    private IWxUserService wxUserService;

    @Autowired
    private FamilyMapper familyMapper;

    /**
     * 从请求中解析当前用户ID
     */
    private String getCurrentUserId(HttpServletRequest request) {
        String token = request.getHeader("X-Access-Token");
        String openid = HomeaiJwtUtil.getOpenid(token);
        if (openid == null) return null;
        WxUser user = wxUserService.getByOpenid(openid);
        return user != null ? user.getId() : null;
    }

    /**
     * 获取当前用户家庭信息
     */
    @GetMapping("/info")
    public Result<?> getInfo(HttpServletRequest request) {
        String userId = getCurrentUserId(request);
        if (userId == null) return Result.error("未登录");

        Family family = familyService.getByUserId(userId);
        if (family == null) {
            return Result.OK(Collections.singletonMap("hasFamily", false));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("hasFamily", true);
        result.put("family", family);

        // 查询当前用户的角色
        FamilyMember member = familyMemberService.getByUserId(userId);
        result.put("myRole", member != null ? member.getRole() : "member");

        return Result.OK(result);
    }

    /**
     * 创建家庭
     */
    @PostMapping
    public Result<?> create(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String userId = getCurrentUserId(request);
        if (userId == null) return Result.error("未登录");

        String name = body.get("name");
        if (name == null || name.trim().isEmpty()) {
            return Result.error("家庭名称不能为空");
        }

        try {
            Family family = familyService.createFamily(name.trim(), userId);
            return Result.OK(family);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 修改家庭信息
     */
    @PutMapping
    public Result<?> update(@RequestBody Family family, HttpServletRequest request) {
        String userId = getCurrentUserId(request);
        if (userId == null) return Result.error("未登录");

        // 校验管理员权限
        FamilyMember member = familyMemberService.getByUserId(userId);
        if (member == null || !"admin".equals(member.getRole())) {
            return Result.error("仅管理员可修改家庭信息");
        }

        // 防止跨家庭越权：管理员只能修改自己所在家庭
        if (family.getId() == null || !member.getFamilyId().equals(family.getId())) {
            return Result.error("无权修改该家庭的信息");
        }

        Family existing = familyService.getById(family.getId());
        if (existing == null) {
            return Result.error("家庭不存在");
        }

        existing.setName(family.getName());
        familyService.updateById(existing);
        return Result.OK("修改成功");
    }

    /**
     * 生成6位邀请码
     */
    @PostMapping("/invite-code")
    public Result<?> generateInviteCode(HttpServletRequest request) {
        String userId = getCurrentUserId(request);
        if (userId == null) return Result.error("未登录");

        FamilyMember member = familyMemberService.getByUserId(userId);
        if (member == null) {
            return Result.error("您不在任何家庭中");
        }

        FamilyInviteCode code = familyInviteCodeService.generateCode(member.getFamilyId(), userId);
        return Result.OK(code.getInviteCode());
    }

    /**
     * 通过邀请码加入家庭
     */
    @PostMapping("/members")
    public Result<?> joinByCode(@RequestParam String code, HttpServletRequest request) {
        String userId = getCurrentUserId(request);
        if (userId == null) return Result.error("未登录");

        FamilyInviteCode inviteCode = familyInviteCodeService.validateCode(code);
        if (inviteCode == null) {
            return Result.error("邀请码无效或已过期");
        }

        Family existing = familyService.getByUserId(userId);
        if (existing != null) {
            return Result.error("您已有家庭，不能重复加入");
        }

        familyService.joinFamily(userId, inviteCode.getFamilyId(), inviteCode.getId());
        return Result.OK("加入成功");
    }

    /**
     * 家庭成员列表
     */
    @GetMapping("/members")
    public Result<?> members(HttpServletRequest request) {
        String userId = getCurrentUserId(request);
        if (userId == null) return Result.error("未登录");

        FamilyMember myMember = familyMemberService.getByUserId(userId);
        if (myMember == null) {
            return Result.OK(Collections.emptyList());
        }

        List<FamilyMember> members = familyMemberService.getByFamilyId(myMember.getFamilyId());
        // 关联用户信息
        List<Map<String, Object>> result = new ArrayList<>();
        for (FamilyMember fm : members) {
            WxUser user = wxUserService.getById(fm.getUserId());
            Map<String, Object> item = new HashMap<>();
            item.put("memberId", fm.getId());
            item.put("userId", fm.getUserId());
            item.put("nickname", user != null ? user.getNickname() : "未知");
            item.put("avatarUrl", user != null ? user.getAvatarUrl() : null);
            item.put("role", fm.getRole());
            item.put("joinedAt", fm.getJoinedAt());
            result.add(item);
        }

        return Result.OK(result);
    }

    /**
     * 移除成员
     */
    @DeleteMapping("/member/{id}")
    public Result<?> removeMember(@PathVariable String id, HttpServletRequest request) {
        String userId = getCurrentUserId(request);
        if (userId == null) return Result.error("未登录");

        // 校验操作人是否为管理员
        FamilyMember operator = familyMemberService.getByUserId(userId);
        if (operator == null || !"admin".equals(operator.getRole())) {
            return Result.error("仅管理员可移除成员");
        }

        FamilyMember target = familyMemberService.getById(id);
        if (target == null) {
            return Result.error("成员不存在");
        }

        // 防止跨家庭越权：只能移除自己家庭内的成员
        if (!operator.getFamilyId().equals(target.getFamilyId())) {
            return Result.error("无权移除其他家庭的成员");
        }
        // 不能移除自己
        if (target.getUserId().equals(userId)) {
            return Result.error("不能移除自己，请使用退出家庭");
        }

        familyMemberService.removeById(id);
        familyService.refreshMemberCount(target.getFamilyId());
        // 同步用户表缓存字段
        WxUser user = wxUserService.getById(target.getUserId());
        if (user != null && target.getFamilyId().equals(user.getFamilyId())) {
            user.setFamilyId(null);
            wxUserService.updateById(user);
        }

        return Result.OK("移除成功");
    }

    /**
     * 主动退出家庭
     */
    @DeleteMapping("/leave")
    public Result<?> leave(HttpServletRequest request) {
        String userId = getCurrentUserId(request);
        if (userId == null) return Result.error("未登录");

        FamilyMember member = familyMemberService.getByUserId(userId);
        if (member == null) {
            return Result.error("您不在任何家庭中");
        }

        // 管理员退出前需转让
        if ("admin".equals(member.getRole())) {
            List<FamilyMember> members = familyMemberService.getByFamilyId(member.getFamilyId());
            boolean hasOtherMember = members.stream().anyMatch(m -> !m.getId().equals(member.getId()));
            if (hasOtherMember) {
                return Result.error("管理员需先转让身份给其他成员后再退出");
            }
        }

        familyMemberService.removeById(member.getId());
        familyService.refreshMemberCount(member.getFamilyId());
        // 同步用户表缓存字段
        WxUser user = wxUserService.getById(userId);
        if (user != null) {
            user.setFamilyId(null);
            wxUserService.updateById(user);
        }

        return Result.OK("退出成功");
    }

    /**
     * 解散家庭
     */
    @DeleteMapping("/disband")
    public Result<?> disband(HttpServletRequest request) {
        String userId = getCurrentUserId(request);
        if (userId == null) return Result.error("未登录");

        FamilyMember member = familyMemberService.getByUserId(userId);
        if (member == null) {
            return Result.error("您不在任何家庭中");
        }

        try {
            familyService.disband(member.getFamilyId(), userId);
            return Result.OK("解散成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 修改成员角色
     */
    @PutMapping("/member/{id}/role")
    public Result<?> updateRole(@PathVariable String id, @RequestParam String role,
                                 HttpServletRequest request) {
        String userId = getCurrentUserId(request);
        if (userId == null) return Result.error("未登录");

        // 校验操作人是否为管理员
        FamilyMember operator = familyMemberService.getByUserId(userId);
        if (operator == null || !"admin".equals(operator.getRole())) {
            return Result.error("仅管理员可修改成员角色");
        }

        FamilyMember target = familyMemberService.getById(id);
        if (target == null) {
            return Result.error("成员不存在");
        }

        // 防止跨家庭越权：只能修改自己家庭内的成员
        if (!operator.getFamilyId().equals(target.getFamilyId())) {
            return Result.error("无权修改其他家庭的成员");
        }
        // 角色值白名单校验
        if (!Arrays.asList("admin", "member", "restricted").contains(role)) {
            return Result.error("非法的角色类型");
        }
        // 唯一管理员不能被降级
        if ("admin".equals(target.getRole()) && !"admin".equals(role)) {
            long adminCount = familyMemberService.getByFamilyId(target.getFamilyId())
                    .stream().filter(m -> "admin".equals(m.getRole())).count();
            if (adminCount <= 1) {
                return Result.error("家庭至少需要一名管理员，请先转让管理员身份");
            }
        }
        target.setRole(role);
        familyMemberService.updateById(target);
        return Result.OK("修改成功");
    }

    /**
     * 转让管理员
     */
    @PostMapping("/transfer")
    public Result<?> transfer(@RequestParam String targetUserId, HttpServletRequest request) {
        String userId = getCurrentUserId(request);
        if (userId == null) return Result.error("未登录");

        FamilyMember member = familyMemberService.getByUserId(userId);
        if (member == null) {
            return Result.error("您不在任何家庭中");
        }

        try {
            familyService.transferAdmin(member.getFamilyId(), userId, targetUserId);
            return Result.OK("转让成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 家庭列表（管理端）
     */
    @GetMapping("/list")
    @Operation(summary="家庭-分页列表查询")
    @RequiresPermissions("homeai:family:list")
    public Result<?> list(Family family,
                          @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                          @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                          HttpServletRequest req) {
        QueryWrapper<Family> queryWrapper = QueryGenerator.initQueryWrapper(family, req.getParameterMap());
        Page<Family> page = new Page<>(pageNo, pageSize);
        IPage<Family> pageList = familyService.page(page, queryWrapper);
        return Result.OK(pageList);
    }

    //update-begin---author:admin ---date:2026-07-31  for：家庭管理-成员列表/添加/移除/改角色-----------
    /**
     * 家庭成员列表（管理端）
     * @param familyId 家庭ID
     */
    @GetMapping("/admin/members")
    @Operation(summary="家庭-成员列表(管理端)")
    @RequiresPermissions("homeai:family:list")
    public Result<?> adminMembers(@RequestParam String familyId) {
        Family family = familyService.getById(familyId);
        if (family == null) {
            return Result.error("家庭不存在");
        }
        List<FamilyMember> members = familyMemberService.getByFamilyId(familyId);
        // 关联用户信息
        List<Map<String, Object>> result = new ArrayList<>();
        for (FamilyMember fm : members) {
            WxUser user = wxUserService.getById(fm.getUserId());
            Map<String, Object> item = new HashMap<>();
            item.put("memberId", fm.getId());
            item.put("userId", fm.getUserId());
            item.put("nickname", user != null ? user.getNickname() : "未知");
            item.put("phone", user != null ? user.getPhone() : null);
            item.put("avatarUrl", user != null ? user.getAvatarUrl() : null);
            item.put("role", fm.getRole());
            item.put("joinedAt", fm.getJoinedAt());
            result.add(item);
        }
        return Result.OK(result);
    }

    /**
     * 添加家庭成员（管理端）
     * body: { userId: "用户ID", role: "admin/member/restricted 可选" }
     */
    @PostMapping("/admin/members")
    @AutoLog(value="家庭-添加成员(管理端)")
    @Operation(summary="家庭-添加成员(管理端)")
    @RequiresPermissions("homeai:family:add")
    public Result<?> adminAddMember(@RequestBody Map<String, String> body) {
        String familyId = body.get("familyId");
        String userId = body.get("userId");
        String role = body.get("role");
        if (oConvertUtils.isEmpty(familyId)) {
            return Result.error("家庭ID不能为空");
        }
        if (oConvertUtils.isEmpty(userId)) {
            return Result.error("请选择用户");
        }
        Family family = familyService.getById(familyId);
        if (family == null || Integer.valueOf(1).equals(family.getDelFlag())) {
            return Result.error("家庭不存在或已解散");
        }
        WxUser user = wxUserService.getById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        // 已有关联则更新家庭，无关联则新增（一个用户只能属于一个家庭）
        String oldFamilyId = null;
        FamilyMember existing = familyMemberService.getByUserId(userId);
        if (existing != null) {
            oldFamilyId = existing.getFamilyId();
        }
        familyMemberService.setUserFamily(userId, familyId, role);
        // 同步用户表缓存字段
        user.setFamilyId(familyId);
        wxUserService.updateById(user);
        familyService.refreshMemberCount(familyId);
        if (oConvertUtils.isNotEmpty(oldFamilyId) && !oldFamilyId.equals(familyId)) {
            familyService.refreshMemberCount(oldFamilyId);
        }
        return Result.OK("添加成功");
    }

    /**
     * 移除家庭成员（管理端）
     */
    @DeleteMapping("/admin/member/{id}")
    @AutoLog(value="家庭-移除成员(管理端)")
    @Operation(summary="家庭-移除成员(管理端)")
    @RequiresPermissions("homeai:family:delete")
    public Result<?> adminRemoveMember(@PathVariable String id) {
        FamilyMember target = familyMemberService.getById(id);
        if (target == null) {
            return Result.error("成员不存在");
        }
        // 家庭至少保留一名管理员
        if ("admin".equals(target.getRole())) {
            List<FamilyMember> admins = familyMemberService.getByFamilyId(target.getFamilyId())
                    .stream().filter(m -> "admin".equals(m.getRole())).collect(Collectors.toList());
            if (admins.size() <= 1) {
                return Result.error("家庭至少需要一名管理员，请先转让管理员身份");
            }
        }
        familyMemberService.removeById(id);
        familyService.refreshMemberCount(target.getFamilyId());
        // 同步用户表缓存字段
        WxUser user = wxUserService.getById(target.getUserId());
        if (user != null && target.getFamilyId().equals(user.getFamilyId())) {
            user.setFamilyId(null);
            wxUserService.updateById(user);
        }
        return Result.OK("移除成功");
    }

    /**
     * 修改成员角色（管理端）
     * body: { role: "admin/member/restricted" }
     */
    @PutMapping("/admin/member/{id}/role")
    @AutoLog(value="家庭-修改成员角色(管理端)")
    @Operation(summary="家庭-修改成员角色(管理端)")
    @RequiresPermissions("homeai:family:edit")
    public Result<?> adminUpdateRole(@PathVariable String id, @RequestBody Map<String, String> body) {
        String role = body.get("role");
        if (oConvertUtils.isEmpty(role)) {
            return Result.error("角色不能为空");
        }
        if (!Arrays.asList("admin", "member", "restricted").contains(role)) {
            return Result.error("非法的角色类型");
        }
        FamilyMember target = familyMemberService.getById(id);
        if (target == null) {
            return Result.error("成员不存在");
        }
        // 唯一管理员不能被降级
        if ("admin".equals(target.getRole()) && !"admin".equals(role)) {
            List<FamilyMember> admins = familyMemberService.getByFamilyId(target.getFamilyId())
                    .stream().filter(m -> "admin".equals(m.getRole())).collect(Collectors.toList());
            if (admins.size() <= 1) {
                return Result.error("家庭至少需要一名管理员，请先转让管理员身份");
            }
        }
        target.setRole(role);
        familyMemberService.updateById(target);
        // 同步用户表角色缓存字段
        WxUser user = wxUserService.getById(target.getUserId());
        if (user != null) {
            user.setFamilyRoleType(role);
            wxUserService.updateById(user);
        }
        return Result.OK("修改成功");
    }
    //update-end---author:admin ---date:2026-07-31  for：家庭管理-成员列表/添加/移除/改角色-----------

    //update-begin---author:admin ---date:2026-07-30  for：家庭管理-新增/导入/导出/回收站功能-----------
    /**
     * 新增家庭（管理端）
     */
    @PostMapping("/add")
    @AutoLog(value="家庭-新增(管理端)")
    @Operation(summary="家庭-新增(管理端)")
    @RequiresPermissions("homeai:family:add")
    public Result<?> add(@RequestBody Family family, HttpServletRequest request) {
        if (family.getStatus() == null) {
            family.setStatus("normal");
        }
        // 管理端新增家庭时，从 Shiro 获取当前管理员 ID 作为创建者
        if (family.getCreatorId() == null) {
            try {
                if (SecurityUtils.getSubject() != null && SecurityUtils.getSubject().isAuthenticated()) {
                    Object principal = SecurityUtils.getSubject().getPrincipal();
                    if (principal instanceof LoginUser) {
                        family.setCreatorId(((LoginUser) principal).getId());
                    }
                }
            } catch (Exception ignored) {}
        }
        familyService.save(family);
        // 若创建者已存在，自动建立创建者-家庭关联（管理员），保证关联表数据一致
        if (oConvertUtils.isNotEmpty(family.getCreatorId())) {
            WxUser creator = wxUserService.getById(family.getCreatorId());
            if (creator != null) {
                familyMemberService.setUserFamily(creator.getId(), family.getId(), "admin");
                creator.setFamilyId(family.getId());
                wxUserService.updateById(creator);
                family.setMemberCount(1);
                familyService.updateById(family);
            }
        }
        return Result.OK("新增成功");
    }

    /**
     * 编辑家庭（管理端）
     */
    @PutMapping("/{id}")
    @AutoLog(value="家庭-编辑(管理端)")
    @Operation(summary="家庭-编辑(管理端)")
    @RequiresPermissions("homeai:family:edit")
    public Result<?> edit(@PathVariable String id, @RequestBody Family family) {
        Family existing = familyService.getById(id);
        if (existing == null) {
            return Result.error("家庭不存在");
        }
        if (family.getName() != null) {
            existing.setName(family.getName());
        }
        familyService.updateById(existing);
        return Result.OK("编辑成功");
    }

    /**
     * 导出Excel
     */
    @GetMapping("/exportXls")
    @Operation(summary="家庭-导出Excel")
    @RequiresPermissions("homeai:family:exportXls")
    public ModelAndView exportXls(HttpServletRequest request, Family family) {
        QueryWrapper<Family> queryWrapper = QueryGenerator.initQueryWrapper(family, request.getParameterMap());
        List<Family> pageList = familyService.list(queryWrapper);
        ModelAndView mv = new ModelAndView(new JeecgEntityExcelView());
        String selections = request.getParameter("selections");
        if (oConvertUtils.isEmpty(selections)) {
            mv.addObject(NormalExcelConstants.DATA_LIST, pageList);
        } else {
            List<String> selectionList = Arrays.asList(selections.split(","));
            List<Family> exportList = pageList.stream().filter(item -> selectionList.contains(item.getId())).collect(Collectors.toList());
            mv.addObject(NormalExcelConstants.DATA_LIST, exportList);
        }
        mv.addObject(NormalExcelConstants.FILE_NAME, "家庭列表");
        mv.addObject(NormalExcelConstants.CLASS, Family.class);
        LoginUser user = null;
        try {
            Object principal = SecurityUtils.getSubject().getPrincipal();
            if (principal instanceof LoginUser) {
                user = (LoginUser) principal;
            }
        } catch (Exception ignored) {}
        mv.addObject(NormalExcelConstants.PARAMS, new ExportParams("家庭列表数据", "导出人:" + (user != null ? user.getRealname() : "系统"), "导出信息", ExcelType.XSSF));
        return mv;
    }

    /**
     * 导出Excel模板
     */
    @GetMapping("/exportTemplate")
    @Operation(summary="家庭-导出导入模板")
    @RequiresPermissions("homeai:family:exportXls")
    public ModelAndView exportTemplate() {
        ModelAndView mv = new ModelAndView(new JeecgEntityExcelView());
        mv.addObject(NormalExcelConstants.DATA_LIST, new ArrayList<Family>());
        mv.addObject(NormalExcelConstants.FILE_NAME, "家庭导入模板");
        mv.addObject(NormalExcelConstants.CLASS, Family.class);
        mv.addObject(NormalExcelConstants.PARAMS, new ExportParams("家庭导入模板", "模板", "导入模板", ExcelType.XSSF));
        return mv;
    }

    /**
     * 导入Excel
     */
    @PostMapping("/importExcel")
    @AutoLog(value="家庭-导入Excel")
    @Operation(summary="家庭-导入Excel")
    @RequiresPermissions("homeai:family:importExcel")
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
                    List<Family> list = ExcelImportUtil.importExcel(file.getInputStream(), Family.class, params);
                    for (Family item : list) {
                        try {
                            familyService.save(item);
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
    @Operation(summary="家庭-回收站列表")
    @RequiresPermissions("homeai:family:moveToRecycleBin")
    public Result<?> recycleBin(Family family,
                                @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                HttpServletRequest req) {
        // 原生SQL分页查询回收站，避免逻辑删除自动追加 del_flag=0 导致查不到数据
        IPage<Family> pageList = familyMapper.selectRecycleBinPage(new Page<>(pageNo, pageSize), family.getName());
        return Result.OK(pageList);
    }

    //update-begin---author:admin ---date:2026-07-31  for：修复软删除/恢复失效问题（@TableLogic 字段不参与 updateById）-----------
    /**
     * 移入回收站（软删除）
     */
    @PutMapping("/moveToRecycleBin")
    @AutoLog(value="家庭-移入回收站")
    @Operation(summary="家庭-移入回收站")
    @RequiresPermissions("homeai:family:moveToRecycleBin")
    public Result<?> moveToRecycleBin(@RequestBody List<String> ids) {
        for (String id : ids) {
            // @TableLogic 字段不参与 updateById，需通过 update wrapper 显式设置
            familyService.update(new LambdaUpdateWrapper<Family>()
                    .eq(Family::getId, id)
                    .set(Family::getDelFlag, 1)
                    .set(Family::getDeletedAt, new Date()));
        }
        return Result.OK("移入回收站成功");
    }

    /**
     * 从回收站恢复
     */
    @PutMapping("/restore")
    @AutoLog(value="家庭-恢复")
    @Operation(summary="家庭-恢复")
    @RequiresPermissions("homeai:family:restore")
    public Result<?> restore(@RequestBody List<String> ids) {
        for (String id : ids) {
            // 自定义原生 SQL 恢复，绕开逻辑删除拦截器自动注入的 del_flag=0 条件
            familyMapper.restoreById(id);
        }
        return Result.OK("恢复成功");
    }
    //update-end---author:admin ---date:2026-07-31  for：修复软删除/恢复失效问题（@TableLogic 字段不参与 updateById）-----------

    /**
     * 彻底删除
     */
    @DeleteMapping("/deletePermanently")
    @AutoLog(value="家庭-彻底删除")
    @Operation(summary="家庭-彻底删除")
    @RequiresPermissions("homeai:family:deletePermanently")
    public Result<?> deletePermanently(@RequestBody List<String> ids) {
        for (String id : ids) {
            // 清理该家庭所有成员关联
            List<FamilyMember> members = familyMemberService.getByFamilyId(id);
            for (FamilyMember m : members) {
                WxUser u = wxUserService.getById(m.getUserId());
                if (u != null && id.equals(u.getFamilyId())) {
                    u.setFamilyId(null);
                    wxUserService.updateById(u);
                }
            }
            familyMemberService.removeByIds(members.stream().map(FamilyMember::getId).collect(Collectors.toList()));
        }
        familyMapper.deletePermanentlyByIds(ids);
        return Result.OK("彻底删除成功");
    }
    //update-end---author:admin ---date:2026-07-30  for：家庭管理-新增/导入/导出/回收站功能-----------
}
