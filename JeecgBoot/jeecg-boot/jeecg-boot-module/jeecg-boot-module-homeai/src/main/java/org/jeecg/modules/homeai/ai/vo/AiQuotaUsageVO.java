package org.jeecg.modules.homeai.ai.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户Token消耗统计（管理端）
 */
@Data
@Schema(description = "用户Token消耗统计")
public class AiQuotaUsageVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 用户ID */
    @Schema(description = "用户ID")
    private String userId;

    /** 用户昵称 */
    @Schema(description = "用户昵称")
    private String nickname;

    /** 手机号 */
    @Schema(description = "手机号")
    private String phone;

    /** 今日已消耗Token */
    @Schema(description = "今日已消耗Token")
    private Integer dailyUsage;

    /** 本月已消耗Token */
    @Schema(description = "本月已消耗Token")
    private Integer monthlyUsage;

    /** 最后活跃时间 */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "最后活跃时间")
    private Date lastActiveTime;
}
