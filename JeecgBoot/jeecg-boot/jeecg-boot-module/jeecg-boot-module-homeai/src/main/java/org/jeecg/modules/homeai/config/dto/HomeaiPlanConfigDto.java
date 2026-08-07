package org.jeecg.modules.homeai.config.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 计划模块运行时配置（Redis 可覆盖，yml 为默认值）
 */
@Data
@Schema(description = "计划模块配置")
public class HomeaiPlanConfigDto {

    @Schema(description = "重复计划预生成/滚动窗口（天）")
    private Integer repeatHorizonDays;

    @Schema(description = "实例清理：保留最近 N 天，更早物理删除")
    private Integer instanceCleanupDays;

    @Schema(description = "是否启用微信计划提醒定时任务")
    private Boolean remindEnabled;

    @Schema(description = "AI 文档生成是否调用大模型润色")
    private Boolean aiDocPolishEnabled;
}
