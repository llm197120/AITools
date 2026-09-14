package org.jeecg.modules.homeai.config.controller;

import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.homeai.config.entity.HomeaiDocConfig;
import org.jeecg.modules.homeai.config.mapper.HomeaiDocConfigMapper;
import org.jeecg.modules.homeai.config.util.HomeaiDocHtmlUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

/**
 * 用户协议 / 隐私政策富文本配置：
 * GET 公开（APP 拉取展示）；PUT 管理端维护（homeai:app:version:edit）。
 */
@Slf4j
@RestController
@RequestMapping("/homeai/config/doc")
public class HomeaiDocConfigController {

    private static final String TYPE_AGREEMENT = "agreement";
    private static final String TYPE_PRIVACY = "privacy";

    @Autowired
    private HomeaiDocConfigMapper mapper;

    private boolean validType(String type) {
        return TYPE_AGREEMENT.equals(type) || TYPE_PRIVACY.equals(type);
    }

    @GetMapping("/{type}")
    @Operation(summary = "协议/隐私内容（APP 公开拉取）")
    public Result<?> get(@PathVariable String type) {
        if (!validType(type)) {
            return Result.error("type 只能是 agreement 或 privacy");
        }
        HomeaiDocConfig row = mapper.selectById(type);
        if (row == null) {
            return Result.OK("");
        }
        return Result.OK(oConvertUtils.getString(row.getContent(), ""));
    }

    @PutMapping("/{type}/admin")
    @Operation(summary = "协议/隐私内容（管理端富文本保存）")
    @RequiresPermissions("homeai:app:version:edit")
    public Result<?> save(@PathVariable String type, @RequestBody HomeaiDocConfig body) {
        if (!validType(type)) {
            throw new JeecgBootException("type 只能是 agreement 或 privacy");
        }
        //update-begin---author:cursor---date:2026-09-03---for:【HomeAI-R125】协议富文本长度与 XSS 过滤-----------
        String content = HomeaiDocHtmlUtil.sanitize(body == null ? null : body.getContent());
        //update-end---author:cursor---date:2026-09-03---for:【HomeAI-R125】协议富文本长度与 XSS 过滤-----------
        HomeaiDocConfig row = mapper.selectById(type);
        Date now = new Date();
        if (row == null) {
            row = new HomeaiDocConfig();
            row.setId(type);
            row.setContent(content);
            row.setCreateTime(now);
            row.setUpdateTime(now);
            mapper.insert(row);
        } else {
            row.setContent(content);
            row.setUpdateTime(now);
            mapper.updateById(row);
        }
        return Result.OK("保存成功");
    }
}