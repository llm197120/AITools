package org.jeecg.modules.homeai.storage.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import io.swagger.v3.oas.annotations.Operation;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.homeai.ai.constant.HomeaiAiQuotaScene;
import org.jeecg.modules.homeai.ai.service.IHomeaiAiQuotaPrecheckService;
import org.jeecg.modules.homeai.config.HomeaiSecurityUtil;
import org.jeecg.modules.homeai.config.service.IHomeaiPlanConfigService;
import org.jeecg.modules.homeai.config.service.IHomeaiFileStorageService;
import org.jeecg.modules.homeai.storage.entity.StorageConvertTask;
import org.jeecg.modules.homeai.storage.service.IStorageConvertTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
    private HomeaiSecurityUtil securityUtil;

    @Autowired
    private IHomeaiAiQuotaPrecheckService precheckService;

    @Autowired
    private IHomeaiPlanConfigService planConfigService;

    @Autowired
    private IHomeaiFileStorageService fileStorageService;

    /**
     * 从 Token 解析用户ID（管理端控制台 JWT 或 HomeAI APP JWT）
     */
    private String getUserId(HttpServletRequest request) {
        //update-begin---author:cursor---date:2026-08-20---for:【Android体验】业务接口统一走 SecurityUtil 解析手机号 JWT-----------
        return securityUtil.getCurrentUserId(request);
        //update-end---author:cursor---date:2026-08-20---for:【Android体验】业务接口统一走 SecurityUtil 解析手机号 JWT-----------
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
        //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R25】Office 生成走统一预检-----------
        if (planConfigService.isAiDocPolishEnabled()) {
            try {
                precheckService.assertAllowed(userId, HomeaiAiQuotaScene.OFFICE_GENERATE, instruction);
            } catch (org.jeecg.common.exception.JeecgBootException e) {
                return Result.error(e.getMessage());
            }
        }
        //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R25】Office 生成走统一预检-----------
        StorageConvertTask task = taskService.submitGenerateTask(userId, fileId, instruction);
        return Result.OK(task);
    }

    /**
     * AI 生成前 Token 配额预检
     */
    @GetMapping("/generate/quota-check")
    @Operation(summary = "Office AI生成-配额预检")
    public Result<?> checkGenerateQuota(@RequestParam(required = false) String instruction,
                                        HttpServletRequest request) {
        String userId = getUserId(request);
        if (userId == null) return Result.error("未登录");
        //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R25】Office 预检委托统一服务-----------
        return Result.OK(precheckService.precheck(userId, HomeaiAiQuotaScene.OFFICE_GENERATE, instruction));
        //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R25】Office 预检委托统一服务-----------
    }

    /**
     * 查询任务状态（轮询用）
     */
    @GetMapping("/tasks/{id}")
    public Result<?> getTaskStatus(@PathVariable String id, HttpServletRequest request) {
        //update-begin---author:cursor ---date:2026-08-13 for：【IDOR修复】任务状态查询补登录与归属校验，防止遍历他人任务-----------
        String userId = getUserId(request);
        if (userId == null) {
            return Result.error("未登录");
        }
        StorageConvertTask task = taskService.getTaskStatus(id);
        if (task == null) {
            return Result.error("任务不存在");
        }
        if (oConvertUtils.isNotEmpty(task.getUserId()) && !userId.equals(task.getUserId())) {
            return Result.error("无权查看该任务");
        }
        resolveResultUrl(task);
        return Result.OK(task);
        //update-end---author:cursor ---date:2026-08-13 for：【IDOR修复】任务状态查询补登录与归属校验-----------
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
        if (task != null && task.getResultFileUrl() != null) {
            task.setResultFileUrl(fileStorageService.resolveAccessUrl(task.getResultFileUrl()));
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
