package org.jeecg.modules.homeai.appversion.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * HomeAI 离线同步与缓存配置（单行 current）
 */
@Data
@TableName("homeai_sync_config")
@Schema(description = "HomeAI离线同步配置")
public class HomeaiSyncConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String CURRENT_ID = "current";

    @TableId(type = IdType.INPUT)
    private String id;

    /** 每批同步条数 */
    private Integer batchSize;

    /** 批间隔毫秒 */
    private Integer intervalMs;

    /** 单条 24h 最大尝试次数 */
    private Integer maxRetriesPerDay;

    /** 图片缓存上限 MB */
    private Integer imageCacheLimitMb;

    private Date createTime;

    private Date updateTime;
}