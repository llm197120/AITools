package org.jeecg.modules.homeai.config.service.impl;



import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

import org.jeecg.common.exception.JeecgBootException;

import org.jeecg.common.util.RedisUtil;

import org.jeecg.common.util.oConvertUtils;

import org.jeecg.modules.homeai.config.HomeaiFileUrlUtil;

import org.jeecg.modules.homeai.config.entity.HomeaiFileWhitelist;

import org.jeecg.modules.homeai.config.mapper.HomeaiFileWhitelistMapper;

import org.jeecg.modules.homeai.config.service.IHomeaiFileWhitelistService;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;



import java.util.*;

import java.util.stream.Collectors;



@Slf4j

@Service

public class HomeaiFileWhitelistServiceImpl extends ServiceImpl<HomeaiFileWhitelistMapper, HomeaiFileWhitelist>

        implements IHomeaiFileWhitelistService {



    private static final String CACHE_KEY = "homeai:cache:file:whitelist";

    private static final long CACHE_TTL = 300;



    @Autowired

    private RedisUtil redisUtil;



    @Override

    @SuppressWarnings("unchecked")

    public List<String> getEnabledExtensions() {

        Object cached = redisUtil.get(CACHE_KEY);

        if (cached instanceof List) {

            return (List<String>) cached;

        }

        LambdaQueryWrapper<HomeaiFileWhitelist> q = new LambdaQueryWrapper<>();

        q.eq(HomeaiFileWhitelist::getIsEnabled, 1).orderByAsc(HomeaiFileWhitelist::getSortOrder);

        List<String> extensions = list(q).stream()

                .map(HomeaiFileWhitelist::getExtension)

                .filter(oConvertUtils::isNotEmpty)

                .map(String::toLowerCase)

                .distinct()

                .collect(Collectors.toList());

        if (extensions.isEmpty()) {

            extensions = defaultExtensions();

        }

        redisUtil.set(CACHE_KEY, extensions, CACHE_TTL);

        return extensions;

    }



    @Override

    public boolean isAllowedExtension(String extension) {

        if (oConvertUtils.isEmpty(extension)) {

            return false;

        }

        String ext = extension.toLowerCase();

        if (!HomeaiFileUrlUtil.passBlacklist(ext)) {

            return false;

        }

        return getEnabledExtensions().contains(ext);

    }



    @Override

    @Transactional(rollbackFor = Exception.class)

    public void replaceAll(List<HomeaiFileWhitelist> items) {

        if (items == null || items.isEmpty()) {

            throw new JeecgBootException("白名单不能为空");

        }

        Set<String> seen = new HashSet<>();

        for (HomeaiFileWhitelist item : items) {

            if (item == null || oConvertUtils.isEmpty(item.getExtension())) {

                throw new JeecgBootException("扩展名不能为空");

            }

            String ext = item.getExtension().trim().toLowerCase().replace(".", "");

            if (!seen.add(ext)) {

                throw new JeecgBootException("扩展名重复: " + ext);

            }

            if (!HomeaiFileUrlUtil.passBlacklist(ext)) {

                throw new JeecgBootException("禁止配置危险扩展名: " + ext);

            }

            item.setExtension(ext);

            if (item.getIsEnabled() == null) item.setIsEnabled(1);

            if (item.getSortOrder() == null) item.setSortOrder(0);

            if (oConvertUtils.isEmpty(item.getCategory())) item.setCategory("other");

        }

        remove(new LambdaQueryWrapper<>());

        Date now = new Date();

        for (HomeaiFileWhitelist item : items) {

            item.setId(null);

            item.setCreateTime(now);

            item.setUpdateTime(now);

        }

        saveBatch(items);

        evictCache();

    }



    @Override

    public void evictCache() {

        redisUtil.del(CACHE_KEY);

    }



    private List<String> defaultExtensions() {

        return Arrays.asList(

                "jpg", "jpeg", "png", "gif", "bmp",

                "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",

                "mp4", "avi", "mov", "mkv",

                "zip", "rar", "7z",

                "txt", "csv", "md"

        );

    }

}

