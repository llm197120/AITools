package org.jeecg.modules.homeai.ai.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.homeai.ai.constant.HomeaiAiQuotaScene;
import org.jeecg.modules.homeai.ai.service.IAiQuotaService;
import org.jeecg.modules.homeai.ai.service.IHomeaiAiQuotaPrecheckService;
import org.jeecg.modules.homeai.ai.util.HomeaiAiQuotaEstimateUtil;
import org.jeecg.modules.homeai.config.service.IHomeaiPlanConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * AI 场景配额统一预检实现
 */
@Slf4j
@Service
public class HomeaiAiQuotaPrecheckServiceImpl implements IHomeaiAiQuotaPrecheckService {

    @Autowired
    private IAiQuotaService quotaService;

    @Autowired
    private IHomeaiPlanConfigService planConfigService;

    @Override
    public Map<String, Object> precheck(String userId, String scene, String text) {
        String normalized = HomeaiAiQuotaScene.normalize(scene);
        Map<String, Object> result = new HashMap<>();
        result.put("scene", normalized);

        //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R25】统一预检按场景估算-----------
        if (HomeaiAiQuotaScene.RECIPE_GENERATE.equals(normalized)) {
            // 菜谱 AI 尚未落地：预留场景，明确未启用
            result.put("enabled", false);
            result.put("allowed", false);
            result.put("message", "菜谱 AI 生成尚未开放");
            result.put("estimatedInputTokens", 0);
            result.put("estimatedOutputTokens", 0);
            enrichLimits(userId, result);
            return result;
        }

        if (HomeaiAiQuotaScene.OFFICE_GENERATE.equals(normalized)
                && !planConfigService.isAiDocPolishEnabled()) {
            result.put("enabled", false);
            result.put("aiDocPolishEnabled", false);
            result.put("allowed", true);
            result.put("message", "AI 文档润色已关闭，本次不消耗 Token");
            result.put("estimatedInputTokens", 0);
            result.put("estimatedOutputTokens", 0);
            enrichLimits(userId, result);
            return result;
        }

        int estIn;
        int estOut;
        if (HomeaiAiQuotaScene.OFFICE_GENERATE.equals(normalized)) {
            estIn = HomeaiAiQuotaEstimateUtil.estimateInputTokens(text);
            estOut = HomeaiAiQuotaEstimateUtil.estimateOutputTokens(text);
            result.put("aiDocPolishEnabled", true);
            result.put("enabled", true);
        } else {
            // chat：与 HomeaiChatServiceImpl 历史行为对齐
            int textLen = oConvertUtils.isNotEmpty(text) ? text.trim().length() : 0;
            estIn = textLen / 2;
            estOut = oConvertUtils.isNotEmpty(text) ? 500 : 0;
            result.put("enabled", true);
        }

        Map<String, Object> quota = quotaService.checkQuota(userId, estIn, estOut);
        result.putAll(quota);
        result.put("estimatedInputTokens", estIn);
        result.put("estimatedOutputTokens", estOut);
        enrichLimits(userId, result);
        //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R25】统一预检按场景估算-----------
        return result;
    }

    @Override
    public void assertAllowed(String userId, String scene, String text) {
        Map<String, Object> result = precheck(userId, scene, text);
        if (!Boolean.TRUE.equals(result.get("allowed"))) {
            throw new JeecgBootException(String.valueOf(result.get("message")));
        }
    }

    private void enrichLimits(String userId, Map<String, Object> result) {
        Map<String, Integer> defaults = quotaService.getDefaultQuota();
        result.put("dailyLimit", defaults.get("dailyLimit"));
        result.put("monthlyLimit", defaults.get("monthlyLimit"));
        if (!result.containsKey("remainingDaily") || !result.containsKey("remainingMonthly")) {
            Map<String, Object> bare = quotaService.checkQuota(userId, 0, 0);
            result.putIfAbsent("remainingDaily", bare.get("remainingDaily"));
            result.putIfAbsent("remainingMonthly", bare.get("remainingMonthly"));
        }
    }
}
