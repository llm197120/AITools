package org.jeecg.modules.homeai.appversion.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("homeai_app_version")
@EqualsAndHashCode(callSuper = false)
@Schema(description = "APP当前发布版本")
public class HomeaiAppVersion implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String CURRENT_ID = "current";

    @TableId(type = IdType.INPUT)
    private String id;

    private String versionName;

    private Integer versionCode;

    /** resource / apk */
    private String updateMode;

    private Integer forceUpdate;

    private String apkUrl;

    private String resourceUrl;

    private String apkSha256;

    private String resourceSha256;

    private Integer minShellCode;

    private String changelog;

    private Integer enabled;

    private String createBy;

    private Date createTime;

    private String updateBy;

    private Date updateTime;

    @TableLogic
    private Integer delFlag;
}
