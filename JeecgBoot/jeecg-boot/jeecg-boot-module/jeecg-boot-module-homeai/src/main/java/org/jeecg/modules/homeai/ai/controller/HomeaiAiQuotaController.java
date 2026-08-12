package org.jeecg.modules.homeai.ai.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.homeai.ai.service.IHomeaiAiQuotaPrecheckService;
import org.jeecg.modules.homeai.config.HomeaiSecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 配额统一预检
 */
@Slf4j
@RestController
@RequestMapping("/homeai/ai/quota")
public class HomeaiAiQuotaController {

    @Autowired
    private IHomeaiAiQuotaPrecheckService precheckService;

    @Autowired
    private HomeaiSecurityUtil securityUtil;

    //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R25】统一配额预检 API-----------
    @GetMapping("/precheck")
    @Operation(summary = "AI配额-按场景预检")
    public Result<?> precheck(@RequestParam(defaultValue = "chat") String scene,
                              @RequestParam(required = false) String text,
                              HttpServletRequest request) {
        String userId = securityUtil.getCurrentUserId(request);
        if (userId == null) {
            return Result.error("未登录");
        }
        return Result.OK(precheckService.precheck(userId, scene, text));
    }
    //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R25】统一配额预检 API-----------
}
