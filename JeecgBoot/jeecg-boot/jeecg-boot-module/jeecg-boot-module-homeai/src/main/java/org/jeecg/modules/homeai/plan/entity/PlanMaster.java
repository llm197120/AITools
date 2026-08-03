package org.jeecg.modules.homeai.plan.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;

@Data
@TableName("homeai_plan_master")
@EqualsAndHashCode(callSuper = false)
@Schema(description = "主计划")
public class PlanMaster implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.ASSIGN_ID)
    private String id;
    private String userId;
    private String title;
    private String content;
    private LocalDate planDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer isAllDay;
    private String priority;
    private String category;
    private Integer remindMinutes;
    private Integer isRepeatMaster;
    private String repeatRule;
    @Version
    private Integer version;
    private String createBy;
    private Date createTime;
    private String updateBy;
    private Date updateTime;
    @TableLogic
    private Integer delFlag;
}
