package org.jeecg.modules.homeai.dashboard.controller;

import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.homeai.dashboard.service.IHomeaiDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * HomeAI 管理端综合仪表盘
 */
@Slf4j
@RestController
@RequestMapping("/homeai/dashboard")
public class HomeaiDashboardController {

    @Autowired
    private IHomeaiDashboardService dashboardService;

    //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R26】计划+学习交叉统计 API-----------
    @GetMapping("/plan-learn")
    @Operation(summary = "综合统计-计划完成率与学习时长")
    @RequiresPermissions("homeai:dashboard:view")
    public Result<?> planLearn(@RequestParam(required = false) String yearMonth,
                               @RequestParam(defaultValue = "30") int days,
                               @RequestParam(required = false) String userId) {
        return Result.OK(dashboardService.planLearn(yearMonth, days, userId));
    }
    //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R26】计划+学习交叉统计 API-----------
}
