-- -*- coding: utf-8 -*-
-- 第 69 轮：APP 当前发布版本表 + 白名单补 apk（已有库可重复执行）

CREATE TABLE IF NOT EXISTS `homeai_app_version` (
    `id`               VARCHAR(32)  NOT NULL COMMENT '固定 current',
    `version_name`     VARCHAR(32)  NOT NULL DEFAULT '1.0.0' COMMENT '展示版本号',
    `version_code`     INT          NOT NULL DEFAULT 100 COMMENT '整数版本，仅当本地更小才更新',
    `update_mode`      VARCHAR(16)  NOT NULL DEFAULT 'apk' COMMENT 'resource=热更新 H5 zip / apk=覆盖安装',
    `force_update`     TINYINT      NOT NULL DEFAULT 0 COMMENT '1=强制，不可跳过',
    `apk_url`          VARCHAR(1024)         COMMENT 'APK 持久化引用或 URL',
    `resource_url`     VARCHAR(1024)         COMMENT 'H5 zip 持久化引用或 URL',
    `apk_sha256`       VARCHAR(64)           COMMENT 'APK SHA-256',
    `resource_sha256`  VARCHAR(64)           COMMENT 'zip SHA-256',
    `min_shell_code`   INT          NOT NULL DEFAULT 100 COMMENT '热更新要求的最低原生 versionCode',
    `changelog`        VARCHAR(2000)         COMMENT '更新说明',
    `enabled`          TINYINT      NOT NULL DEFAULT 0 COMMENT '1=对 APP 生效',
    `create_by`        VARCHAR(50)           COMMENT '创建人',
    `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_by`        VARCHAR(50)           COMMENT '更新人',
    `update_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `del_flag`         TINYINT      DEFAULT 0 COMMENT '删除状态(0-正常,1-已删除)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='APP当前发布版本' ROW_FORMAT=DYNAMIC;

INSERT IGNORE INTO `homeai_app_version`
(`id`, `version_name`, `version_code`, `update_mode`, `force_update`, `min_shell_code`, `enabled`, `changelog`)
VALUES
('current', '1.0.0', 100, 'apk', 0, 100, 0, '当前内测版本，未开放自动更新');

INSERT INTO `homeai_file_whitelist` (`id`, `extension`, `category`, `sort_order`, `is_enabled`)
SELECT 'fw_apk', 'apk', 'archive', 33, 1
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM `homeai_file_whitelist` w WHERE w.`extension` = 'apk'
);
