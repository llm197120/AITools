package org.jeecg.modules.homeai.config.service.impl;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.modules.homeai.config.dto.HomeaiStorageConfigDto;
import org.jeecg.modules.homeai.config.service.IHomeaiStorageConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class HomeaiStorageConfigServiceImpl implements IHomeaiStorageConfigService {

    private static final String REDIS_KEY = "homeai:config:storage";
    private static final long ONE_GB = 1024L * 1024L * 1024L;

    @Value("${homeai.storage.default-user-limit-bytes:1073741824}")
    private long defaultUserLimitBytes;

    //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R28】家庭默认配额-----------
    @Value("${homeai.storage.default-family-limit-bytes:5368709120}")
    private long defaultFamilyLimitBytes;
    //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R28】家庭默认配额-----------

    @Value("${homeai.storage.warn-percent:80}")
    private int defaultWarnPercent;

    @Autowired
    private RedisUtil redisUtil;

    @Override
    public HomeaiStorageConfigDto getConfig() {
        HomeaiStorageConfigDto defaults = buildDefaults();
        Object cached = redisUtil.get(REDIS_KEY);
        if (cached == null) {
            return defaults;
        }
        try {
            HomeaiStorageConfigDto stored = JSON.parseObject(String.valueOf(cached), HomeaiStorageConfigDto.class);
            if (stored == null) {
                return defaults;
            }
            if (stored.getDefaultUserLimitBytes() == null) {
                stored.setDefaultUserLimitBytes(defaults.getDefaultUserLimitBytes());
            }
            //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R28】家庭默认配额-----------
            if (stored.getDefaultFamilyLimitBytes() == null) {
                stored.setDefaultFamilyLimitBytes(defaults.getDefaultFamilyLimitBytes());
            }
            //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R28】家庭默认配额-----------
            if (stored.getWarnPercent() == null) {
                stored.setWarnPercent(defaults.getWarnPercent());
            }
            return stored;
        } catch (Exception e) {
            log.warn("解析存储配额配置失败，使用默认值", e);
            return defaults;
        }
    }

    @Override
    public void saveConfig(HomeaiStorageConfigDto config) {
        HomeaiStorageConfigDto toSave = config != null ? config : new HomeaiStorageConfigDto();
        normalize(toSave);
        redisUtil.set(REDIS_KEY, JSON.toJSONString(toSave));
    }

    @Override
    public long getDefaultUserLimitBytes() {
        Long v = getConfig().getDefaultUserLimitBytes();
        long limit = v != null ? v : defaultUserLimitBytes;
        return clampLimit(limit);
    }

    //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R28】家庭默认配额-----------
    @Override
    public long getDefaultFamilyLimitBytes() {
        Long v = getConfig().getDefaultFamilyLimitBytes();
        long limit = v != null ? v : defaultFamilyLimitBytes;
        return clampLimit(limit);
    }
    //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R28】家庭默认配额-----------

    //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R30】家庭级配额覆盖-----------
    private static final String FAMILY_LIMIT_PREFIX = "homeai:config:storage:family:";

    @Override
    public long getFamilyLimitBytes(String familyId) {
        if (familyId == null || familyId.isBlank()) {
            return getDefaultFamilyLimitBytes();
        }
        Object cached = redisUtil.get(FAMILY_LIMIT_PREFIX + familyId);
        if (cached != null) {
            try {
                long v = Long.parseLong(String.valueOf(cached).trim());
                if (v > 0) {
                    return clampLimit(v);
                }
            } catch (Exception e) {
                log.warn("解析家庭配额覆盖失败 familyId={}", familyId, e);
            }
        }
        return getDefaultFamilyLimitBytes();
    }

    @Override
    public boolean hasFamilyLimitOverride(String familyId) {
        if (familyId == null || familyId.isBlank()) {
            return false;
        }
        Object cached = redisUtil.get(FAMILY_LIMIT_PREFIX + familyId);
        if (cached == null) {
            return false;
        }
        try {
            return Long.parseLong(String.valueOf(cached).trim()) > 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void setFamilyLimitBytes(String familyId, Long limitBytes) {
        if (familyId == null || familyId.isBlank()) {
            throw new IllegalArgumentException("familyId 不能为空");
        }
        if (limitBytes == null || limitBytes <= 0) {
            clearFamilyLimitBytes(familyId);
            return;
        }
        redisUtil.set(FAMILY_LIMIT_PREFIX + familyId, String.valueOf(clampLimit(limitBytes)));
    }

    @Override
    public void clearFamilyLimitBytes(String familyId) {
        if (familyId == null || familyId.isBlank()) {
            return;
        }
        redisUtil.del(FAMILY_LIMIT_PREFIX + familyId);
    }
    //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R30】家庭级配额覆盖-----------

    @Override
    public int getWarnPercent() {
        Integer v = getConfig().getWarnPercent();
        int p = v != null ? v : defaultWarnPercent;
        return Math.max(50, Math.min(p, 99));
    }

    private HomeaiStorageConfigDto buildDefaults() {
        HomeaiStorageConfigDto dto = new HomeaiStorageConfigDto();
        dto.setDefaultUserLimitBytes(defaultUserLimitBytes > 0 ? defaultUserLimitBytes : ONE_GB);
        //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R28】家庭默认配额-----------
        dto.setDefaultFamilyLimitBytes(defaultFamilyLimitBytes > 0 ? defaultFamilyLimitBytes : ONE_GB * 5);
        //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R28】家庭默认配额-----------
        dto.setWarnPercent(defaultWarnPercent > 0 ? defaultWarnPercent : 80);
        return dto;
    }

    private void normalize(HomeaiStorageConfigDto dto) {
        long userLimit = dto.getDefaultUserLimitBytes() != null ? dto.getDefaultUserLimitBytes() : defaultUserLimitBytes;
        dto.setDefaultUserLimitBytes(clampLimit(userLimit));
        //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R28】家庭默认配额-----------
        long familyLimit = dto.getDefaultFamilyLimitBytes() != null ? dto.getDefaultFamilyLimitBytes() : defaultFamilyLimitBytes;
        dto.setDefaultFamilyLimitBytes(clampLimit(familyLimit));
        //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R28】家庭默认配额-----------
        int warn = dto.getWarnPercent() != null ? dto.getWarnPercent() : defaultWarnPercent;
        dto.setWarnPercent(Math.max(50, Math.min(warn, 99)));
    }

    private static long clampLimit(long limit) {
        return Math.max(ONE_GB / 100, Math.min(limit, ONE_GB * 100));
    }
}
