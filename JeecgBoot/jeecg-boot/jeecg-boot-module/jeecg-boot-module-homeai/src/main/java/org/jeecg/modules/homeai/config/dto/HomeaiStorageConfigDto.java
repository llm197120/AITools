package org.jeecg.modules.homeai.config.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 资料存储配额配置（Redis 可覆盖，yml 为默认值）
 */
@Data
@Schema(description = "资料存储配额配置")
public class HomeaiStorageConfigDto {

    @Schema(description = "单用户默认空间上限（字节），默认 1GB")
    private Long defaultUserLimitBytes;

    //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R28】家庭默认配额-----------
    @Schema(description = "单家庭默认空间上限（字节），默认 5GB")
    private Long defaultFamilyLimitBytes;
    //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R28】家庭默认配额-----------

    @Schema(description = "用量告警阈值百分比，默认 80")
    private Integer warnPercent;
}
