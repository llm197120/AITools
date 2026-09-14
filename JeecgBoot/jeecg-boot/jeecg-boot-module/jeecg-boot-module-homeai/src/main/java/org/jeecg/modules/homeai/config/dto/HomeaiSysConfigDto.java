package org.jeecg.modules.homeai.config.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 系统运行时配置（第二节 yml 项进后台；计划/配额复用已有 Redis 配置）
 */
@Data
@Schema(description = "HomeAI 系统配置")
public class HomeaiSysConfigDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private Upload upload;
    private Learn learn;
    private Wechat wechat;
    private Office office;
    private Oss oss;
    private FileUrl file;
    private HomeaiPlanConfigDto plan;
    private HomeaiStorageConfigDto storage;

    @Data
    public static class Upload implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long video;
        private Long audio;
        private Long image;
        private Long document;
        private Long archive;
        private Long text;
    }

    @Data
    public static class Learn implements Serializable {
        private static final long serialVersionUID = 1L;
        private Boolean remindEnabled;
        private String remindCron;
    }

    @Data
    public static class Wechat implements Serializable {
        private static final long serialVersionUID = 1L;
        private String planRemindTemplateId;
        private String learnRemindTemplateId;
        private String learnRemindTitleField;
        private String learnRemindProgressField;
        private String learnRemindGoalField;
        private String learnRemindDateField;
        private String learnRemindTitleText;
    }

    @Data
    public static class Office implements Serializable {
        private static final long serialVersionUID = 1L;
        private Boolean preferMsOffice;
        private String sofficePath;
        private String powershellPath;
        private Integer convertTimeoutSeconds;
        private String gotenbergUrl;
        private String kkFileViewUrl;
    }

    @Data
    public static class Oss implements Serializable {
        private static final long serialVersionUID = 1L;
        private Boolean privateBucket;
        private Long presignExpireSeconds;
    }

    @Data
    public static class FileUrl implements Serializable {
        private static final long serialVersionUID = 1L;
        private String baseUrl;
        private String scheme;
        private String host;
    }
}
