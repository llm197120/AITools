package org.jeecg.modules.homeai.appversion.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * APP 启动页公开探测字段（不含管理端内部信息）
 */
@Data
public class HomeaiAppVersionPublicDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private String versionName;
    private Integer versionCode;
    private String updateMode;
    private Boolean forceUpdate;
    private String apkUrl;
    private String resourceUrl;
    private String apkSha256;
    private String resourceSha256;
    private Integer minShellCode;
    private String changelog;
    private Boolean enabled;
}
