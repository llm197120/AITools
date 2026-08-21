package org.jeecg.modules.homeai.family.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.jeecgframework.poi.excel.annotation.Excel;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * 家庭
 */
@Data
@TableName("homeai_family")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(description = "家庭")
public class Family implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键")
    private String id;

    /** 家庭名称 */
    @Excel(name = "家庭名称", width = 20)
    @Schema(description = "家庭名称")
    private String name;

    /** 创建者用户ID */
    @Schema(description = "创建者用户ID")
    private String creatorId;

    /** 成员数量 */
    @Excel(name = "成员数量", width = 10)
    @Schema(description = "成员数量")
    private Integer memberCount;

    /** 状态: normal-正常 disbanded-已解散 */
    @Excel(name = "状态", width = 10)
    @Schema(description = "状态: normal-正常 disbanded-已解散")
    private String status;

    /** 创建人 */
    @Schema(description = "创建人")
    private String createBy;

    /** 创建时间 */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private Date createTime;

    /** 更新人 */
    @Schema(description = "更新人")
    private String updateBy;

    /** 更新时间 */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private Date updateTime;

    /** 删除状态(0-正常,1-已删除) */
    //update-begin---author:cursor---date:2026-08-20---for:【家庭管理】补逻辑删除，后台移入回收站后不再出现在列表-----------
    @TableLogic
    //update-end---author:cursor---date:2026-08-20---for:【家庭管理】补逻辑删除，后台移入回收站后不再出现在列表-----------
    @Schema(description = "删除状态(0-正常,1-已删除)")
    private Integer delFlag;

    /** 解散时间(进入保留期) */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "解散时间")
    private Date deletedAt;

    /** 租户ID */
    @Schema(description = "租户ID")
    private String tenantId;

    /** 所属部门 */
    @Schema(description = "所属部门")
    private String sysOrgCode;
}
