package org.jeecg.modules.homeai.audit.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.homeai.audit.entity.HomeaiAuditLog;
import org.jeecg.modules.homeai.audit.service.IHomeaiAuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/homeai/audit")
public class HomeaiAuditLogController {

    @Autowired
    private IHomeaiAuditLogService auditLogService;

    @GetMapping("/logs")
    @Operation(summary = "审计日志-分页查询(管理端)")
    @RequiresPermissions("homeai:plan:list")
    public Result<?> logs(@RequestParam(required = false) String module,
                          @RequestParam(required = false) String actionType,
                          @RequestParam(defaultValue = "1") int pageNo,
                          @RequestParam(defaultValue = "10") int pageSize) {
        LambdaQueryWrapper<HomeaiAuditLog> q = new LambdaQueryWrapper<>();
        if (oConvertUtils.isNotEmpty(module)) {
            q.eq(HomeaiAuditLog::getModule, module);
        }
        if (oConvertUtils.isNotEmpty(actionType)) {
            q.eq(HomeaiAuditLog::getActionType, actionType);
        }
        q.orderByDesc(HomeaiAuditLog::getCreateTime);
        IPage<HomeaiAuditLog> page = auditLogService.page(new Page<>(pageNo, pageSize), q);
        return Result.OK(page);
    }
}
