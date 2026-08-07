package org.jeecg.modules.homeai.config.entity;



import com.baomidou.mybatisplus.annotation.*;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

import lombok.EqualsAndHashCode;



import java.io.Serializable;

import java.util.Date;



@Data

@TableName("homeai_file_whitelist")

@EqualsAndHashCode(callSuper = false)

@Schema(description = "文件上传白名单")

public class HomeaiFileWhitelist implements Serializable {

    private static final long serialVersionUID = 1L;



    @TableId(type = IdType.ASSIGN_ID)

    private String id;

    /** 扩展名（不含点，小写） */

    private String extension;

    /** 分类：image/doc/video/archive/text/other */

    private String category;

    private Integer sortOrder;

    private Integer isEnabled;

    private String createBy;

    private Date createTime;

    private String updateBy;

    private Date updateTime;

    @TableLogic

    private Integer delFlag;

}

