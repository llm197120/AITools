package org.jeecg.modules.homeai.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
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
 * 微信用户
 */
@Data
@TableName("homeai_wx_user")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(description = "微信用户")
public class WxUser implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键")
    private String id;

    /** 微信openid（唯一） */
    @Excel(name = "微信openid", width = 25)
    @Schema(description = "微信openid")
    private String openid;

    /** 微信昵称 */
    @Excel(name = "微信昵称", width = 15)
    @Schema(description = "微信昵称")
    private String nickname;

    /** 头像URL */
    @Schema(description = "头像URL")
    private String avatarUrl;

    /** 手机号 */
    @Excel(name = "手机号", width = 15)
    @Schema(description = "手机号")
    private String phone;

    //update-begin---author:admin ---date:2026-08-17 for:【Android迁移】手机号密码登录字段---
    /** 密码(PBE加密) */
    @Schema(description = "密码(PBE加密)")
    private String password;

    /** 密码盐 */
    @Schema(description = "密码盐")
    private String salt;

    /** 登录方式:wechat/phone */
    @Schema(description = "登录方式:wechat/phone")
    private String loginType;
    //update-end---author:admin ---date:2026-08-17 for:【Android迁移】手机号密码登录字段---

    /** 家庭角色:爸爸/妈妈/孩子/其他 */
    @Excel(name = "家庭角色", width = 10)
    @Schema(description = "家庭角色:爸爸/妈妈/孩子/其他")
    private String familyRole;

    /** 所属家庭ID（NULL=无家庭） */
    @Schema(description = "所属家庭ID")
    private String familyId;

    /** 所属家庭名称（非表字段，用于列表展示） */
    @TableField(exist = false)
    @Schema(description = "所属家庭名称")
    private String familyName;

    /** 家庭成员权限:admin/member/restricted */
    @Excel(name = "角色类型", width = 15)
    @Schema(description = "家庭成员权限:admin/member/restricted")
    private String familyRoleType;

    /** 最后登录时间 */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "最后登录时间")
    private Date lastLoginTime;

    /** 状态:1=正常 0=禁用 */
    @Excel(name = "状态", width = 15)
    @Schema(description = "状态:1=正常 0=禁用")
    private String status;

    /** 创建人 */
    @Schema(description = "创建人")
    private String createBy;

    /** 注册时间 */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "注册时间")
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
    @TableLogic
    @Schema(description = "删除状态(0-正常,1-已删除)")
    private Integer delFlag;

    /** 租户ID */
    @Schema(description = "租户ID")
    private String tenantId;

    /** 所属部门 */
    @Schema(description = "所属部门")
    private String sysOrgCode;
}
