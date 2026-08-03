package org.jeecg.modules.homeai.storage.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jeecg.common.api.CommonAPI;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import io.swagger.v3.oas.annotations.Operation;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.common.system.util.JwtUtil;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.common.util.TokenUtils;
import org.jeecg.modules.homeai.config.HomeaiFileUrlUtil;
import org.jeecg.modules.homeai.config.HomeaiJwtUtil;
import org.jeecg.modules.homeai.storage.entity.StorageConvertTask;
import org.jeecg.modules.homeai.storage.service.IStorageConvertTaskService;
import org.jeecg.modules.homeai.user.service.IWxUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Office处理（格式转换/AI生成）
 */
@Slf4j
@RestController
@RequestMapping("/homeai/storage/office")
public class StorageOfficeController {

    @Autowired
    private IStorageConvertTaskService taskService;

    @Autowired
    private IWxUserService wxUserService;

    @Lazy
    @Autowired
    private CommonAPI commonApi;

    @Autowired
    private RedisUtil redisUtil;

    /**
     * 从 Token 解析用户ID（支持管理端 JeecgBoot JWT 与小程序端 HomeaiJWT）
     */
    private String getUserId(HttpServletRequest request) {
        try {
            if (SecurityUtils.getSubject() != null && SecurityUtils.getSubject().isAuthenticated()) {
                Object principal = SecurityUtils.getSubject().getPrincipal();
                if (principal instanceof LoginUser) {
                    return ((LoginUser) principal).getId();
                }
                return principal != null ? principal.toString() : null;
            }
        } catch (Exception ignored) {}

        String token = request.getHeader("X-Access-Token");
        if (token == null || token.trim().isEmpty()) {
            return null;
        }

        // 尝试解析 JeecgBoot 标准 JWT（管理端）
        try {
            String username = JwtUtil.getUsername(token);
            if (username != null && TokenUtils.verifyToken(token, commonApi, redisUtil)) {
                LoginUser loginUser = TokenUtils.getLoginUser(username, commonApi, redisUtil);
                if (loginUser != null) {
                    return loginUser.getId();
                }
            }
        } catch (Exception ignored) {}

        // 回退到 HomeaiJWT 认证（小程序端）
        String openid = HomeaiJwtUtil.getOpenid(token);
        if (openid == null) return null;
        var user = wxUserService.getByOpenid(openid);
        return user != null ? user.getId() : null;
    }

    /**
     * 提交格式转换任务
     */
    @PostMapping("/convert")
    public Result<?> submitConvert(@RequestParam String fileId,
                                   @RequestParam String sourceFormat,
                                   @RequestParam String targetFormat,
                                   HttpServletRequest request) {
        String userId = getUserId(request);
        if (userId == null) return Result.error("未登录");
        StorageConvertTask task = taskService.submitConvertTask(userId, fileId, sourceFormat, targetFormat);
        return Result.OK(task);
    }

    /**
     * 提交AI生成任务
     */
    @PostMapping("/generate")
    public Result<?> submitGenerate(@RequestParam String fileId,
                                    @RequestParam String instruction,
                                    HttpServletRequest request) {
        String userId = getUserId(request);
        if (userId == null) return Result.error("未登录");
        StorageConvertTask task = taskService.submitGenerateTask(userId, fileId, instruction);
        return Result.OK(task);
    }

    /**
     * 查询任务状态（轮询用）
     */
    @GetMapping("/tasks/{id}")
    public Result<?> getTaskStatus(@PathVariable String id) {
        StorageConvertTask task = taskService.getTaskStatus(id);
        resolveResultUrl(task);
        return Result.OK(task);
    }

    /**
     * 用户处理历史
     */
    @GetMapping("/history")
    public Result<?> getHistory(HttpServletRequest request) {
        String userId = getUserId(request);
        if (userId == null) return Result.error("未登录");
        List<StorageConvertTask> history = taskService.getUserHistory(userId);
        if (history != null) {
            for (StorageConvertTask task : history) {
                resolveResultUrl(task);
            }
        }
        return Result.OK(history);
    }

    /**
     * 管理端分页列表
     */
    @GetMapping("/list")
    @Operation(summary="Office处理-记录列表(管理端)")
    @RequiresPermissions("homeai:storage:history:list")
    public Result<?> list(StorageConvertTask task,
                          @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                          @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                          HttpServletRequest req) {
        QueryWrapper<StorageConvertTask> queryWrapper = QueryGenerator.initQueryWrapper(task, req.getParameterMap());
        Page<StorageConvertTask> page = new Page<>(pageNo, pageSize);
        IPage<StorageConvertTask> pageList = taskService.page(page, queryWrapper);
        if (pageList.getRecords() != null) {
            for (StorageConvertTask t : pageList.getRecords()) {
                resolveResultUrl(t);
            }
        }
        return Result.OK(pageList);
    }

    /** 兼容历史相对地址数据：转换 resultFileUrl 为绝对访问地址 */
    private void resolveResultUrl(StorageConvertTask task) {
        if (task != null && task.getResultFileUrl() != null && !task.getResultFileUrl().startsWith("http")) {
            task.setResultFileUrl(HomeaiFileUrlUtil.toAbsoluteUrl(task.getResultFileUrl()));
        }
    }

    /**
     * 处理记录详情（管理端，含结果文件地址）
     */
    @GetMapping("/{id}")
    @Operation(summary="Office处理-记录详情(管理端)")
    @RequiresPermissions("homeai:storage:history:list")
    public Result<?> detail(@PathVariable String id) {
        StorageConvertTask task = taskService.getById(id);
        resolveResultUrl(task);
        return Result.OK(task);
    }

    /**
     * 删除处理记录（管理端）
     */
    @DeleteMapping("/{id}")
    @AutoLog(value="Office处理-删除记录")
    @Operation(summary="Office处理-删除记录(管理端)")
    @RequiresPermissions("homeai:storage:history:delete")
    public Result<?> delete(@PathVariable String id) {
        taskService.removeById(id);
        return Result.OK("删除成功");
    }
}
