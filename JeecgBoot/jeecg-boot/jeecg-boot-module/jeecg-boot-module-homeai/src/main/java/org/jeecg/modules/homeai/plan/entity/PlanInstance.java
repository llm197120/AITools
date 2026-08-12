package org.jeecg.modules.homeai.plan.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Date;

@Data
@TableName("homeai_plan_instance")
@EqualsAndHashCode(callSuper = false)
@Schema(description = "计划实例")
public class PlanInstance implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.ASSIGN_ID)
    private String id;
    private String masterId;
    private LocalDate planDate;
    private String status;
    private Integer reminded;
    @Version
    private Integer version;
    private String createBy;
    private Date createTime;
    private String updateBy;
    private Date updateTime;

    /** 冗余展示字段：主计划标题 */
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String title;
    /** 冗余展示字段：主计划分类 */
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String category;
    /** 冗余展示字段：主计划优先级 */
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String priority;
    /** 冗余展示字段：是否全天 */
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private Integer isAllDay;
    /** 冗余展示字段：所属用户 */
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String userId;
    /** 冗余展示字段：重复规则 */
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String repeatRule;
    //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R23】计划关联菜谱-----------
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String recipeId;
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String recipeName;
    //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R23】计划关联菜谱-----------
}
