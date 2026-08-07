package org.jeecg.modules.homeai.config.service.impl;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.modules.homeai.config.dto.HomeaiPlanConfigDto;
import org.jeecg.modules.homeai.config.service.IHomeaiPlanConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class HomeaiPlanConfigServiceImpl implements IHomeaiPlanConfigService {

    private static final String REDIS_KEY = "homeai:config:plan";

    @Value("${homeai.plan.repeat-horizon-days:90}")
    private int defaultRepeatHorizonDays;

    @Value("${homeai.plan.instance-cleanup-days:30}")
    private int defaultInstanceCleanupDays;

    @Value("${homeai.plan.remind-enabled:true}")
    private boolean defaultRemindEnabled;

    @Value("${homeai.plan.ai-doc-polish-enabled:true}")
    private boolean defaultAiDocPolishEnabled;

    @Autowired
    private RedisUtil redisUtil;

    @Override
    public HomeaiPlanConfigDto getConfig() {
        HomeaiPlanConfigDto defaults = buildDefaults();
        Object cached = redisUtil.get(REDIS_KEY);
        if (cached == null) {
            return defaults;
        }
        try {
            HomeaiPlanConfigDto stored = JSON.parseObject(String.valueOf(cached), HomeaiPlanConfigDto.class);
            if (stored == null) {
                return defaults;
            }
            if (stored.getRepeatHorizonDays() == null) {
                stored.setRepeatHorizonDays(defaults.getRepeatHorizonDays());
            }
            if (stored.getInstanceCleanupDays() == null) {
                stored.setInstanceCleanupDays(defaults.getInstanceCleanupDays());
            }
            if (stored.getRemindEnabled() == null) {
                stored.setRemindEnabled(defaults.getRemindEnabled());
            }
            if (stored.getAiDocPolishEnabled() == null) {
                stored.setAiDocPolishEnabled(defaults.getAiDocPolishEnabled());
            }
            return stored;
        } catch (Exception e) {
            log.warn("解析计划配置缓存失败，使用默认值", e);
            return defaults;
        }
    }

    @Override
    public void saveConfig(HomeaiPlanConfigDto config) {
        HomeaiPlanConfigDto toSave = config != null ? config : new HomeaiPlanConfigDto();
        normalize(toSave);
        redisUtil.set(REDIS_KEY, JSON.toJSONString(toSave));
    }

    @Override
    public int getRepeatHorizonDays() {
        return normalizeInt(getConfig().getRepeatHorizonDays(), defaultRepeatHorizonDays, 7, 365);
    }

    @Override
    public int getInstanceCleanupDays() {
        return normalizeInt(getConfig().getInstanceCleanupDays(), defaultInstanceCleanupDays, 7, 180);
    }

    @Override
    public boolean isRemindEnabled() {
        Boolean v = getConfig().getRemindEnabled();
        return v == null ? defaultRemindEnabled : v;
    }

    @Override
    public boolean isAiDocPolishEnabled() {
        Boolean v = getConfig().getAiDocPolishEnabled();
        return v == null ? defaultAiDocPolishEnabled : v;
    }

    private HomeaiPlanConfigDto buildDefaults() {
        HomeaiPlanConfigDto dto = new HomeaiPlanConfigDto();
        dto.setRepeatHorizonDays(defaultRepeatHorizonDays);
        dto.setInstanceCleanupDays(defaultInstanceCleanupDays);
        dto.setRemindEnabled(defaultRemindEnabled);
        dto.setAiDocPolishEnabled(defaultAiDocPolishEnabled);
        return dto;
    }

    private void normalize(HomeaiPlanConfigDto dto) {
        dto.setRepeatHorizonDays(normalizeInt(dto.getRepeatHorizonDays(), defaultRepeatHorizonDays, 7, 365));
        dto.setInstanceCleanupDays(normalizeInt(dto.getInstanceCleanupDays(), defaultInstanceCleanupDays, 7, 180));
        if (dto.getRemindEnabled() == null) {
            dto.setRemindEnabled(defaultRemindEnabled);
        }
        if (dto.getAiDocPolishEnabled() == null) {
            dto.setAiDocPolishEnabled(defaultAiDocPolishEnabled);
        }
    }

    private int normalizeInt(Integer value, int fallback, int min, int max) {
        int v = value != null ? value : fallback;
        if (v < min) {
            return min;
        }
        if (v > max) {
            return max;
        }
        return v;
    }
}
