package org.jeecg.modules.homeai.config.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * HomeAI 协议/隐私富文本配置（id: agreement/privacy）
 */
@Data
@TableName("homeai_doc_config")
@Schema(description = "HomeAI协议与隐私配置")
public class HomeaiDocConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    private String id;

    /** 富文本 HTML 内容 */
    private String content;

    private Date createTime;

    private Date updateTime;
}