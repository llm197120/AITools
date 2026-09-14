package org.jeecg.modules.homeai.config.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * HomeAI 系统运行时配置（单行 current，JSON）
 */
@Data
@TableName("homeai_sys_config")
@Schema(description = "HomeAI系统配置")
public class HomeaiSysConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String CURRENT_ID = "current";

    @TableId(type = IdType.INPUT)
    private String id;

    private String content;

    private Date createTime;

    private Date updateTime;
}
