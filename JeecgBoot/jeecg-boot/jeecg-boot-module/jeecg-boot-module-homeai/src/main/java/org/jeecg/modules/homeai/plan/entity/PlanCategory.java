package org.jeecg.modules.homeai.plan.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("homeai_plan_category")
@EqualsAndHashCode(callSuper = false)
@Schema(description = "计划分类")
public class PlanCategory implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private String id;
    private String name;
    private String icon;
    private String color;
    private Integer sortOrder;
    private Integer isDefault;
    private Integer isEnabled;
    @Version
    private Integer version;
    private String createBy;
    private Date createTime;
    private String updateBy;
    private Date updateTime;
    @TableLogic
    private Integer delFlag;
}
