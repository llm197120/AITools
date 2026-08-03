package org.jeecg.modules.homeai.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

/**
 * AI 用户 Token 额度配置
 */
@Data
@TableName("homeai_ai_user_quota")
@EqualsAndHashCode(callSuper = false)
@Schema(description = "AI用户Token额度配置")
public class AiUserQuota implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private String id;
    private String userId;
    private Integer dailyLimit;
    private Integer monthlyLimit;
    private Date effectiveStart;
    private Date effectiveEnd;
    private String createBy;
    private Date createTime;
    private String updateBy;
    private Date updateTime;
    @TableLogic
    private Integer delFlag;
}
