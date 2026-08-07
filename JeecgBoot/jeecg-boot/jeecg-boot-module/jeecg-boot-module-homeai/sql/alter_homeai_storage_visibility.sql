-- 资料存储三级可见性：private / family / public，家庭可见支持多家庭关联

ALTER TABLE `homeai_storage_folder`
    MODIFY COLUMN `visibility` VARCHAR(20) DEFAULT 'private' COMMENT '可见性:private/family/public';

ALTER TABLE `homeai_storage_file`
    MODIFY COLUMN `visibility` VARCHAR(20) DEFAULT 'private' COMMENT '可见性:private/family/public';

CREATE TABLE IF NOT EXISTS `homeai_storage_folder_family` (
    `id`          VARCHAR(32) NOT NULL COMMENT '主键',
    `folder_id`   VARCHAR(32) NOT NULL COMMENT '文件夹ID',
    `family_id`   VARCHAR(32) NOT NULL COMMENT '家庭ID',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uniq_hw_folder_family` (`folder_id`, `family_id`),
    KEY `idx_hw_sf_folder` (`folder_id`),
    KEY `idx_hw_sf_family` (`family_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件夹-家庭可见关联' ROW_FORMAT=DYNAMIC;

CREATE TABLE IF NOT EXISTS `homeai_storage_file_family` (
    `id`          VARCHAR(32) NOT NULL COMMENT '主键',
    `file_id`     VARCHAR(32) NOT NULL COMMENT '文件ID',
    `family_id`   VARCHAR(32) NOT NULL COMMENT '家庭ID',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uniq_hw_file_family` (`file_id`, `family_id`),
    KEY `idx_hw_ff_file` (`file_id`),
    KEY `idx_hw_ff_family` (`family_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件-家庭可见关联' ROW_FORMAT=DYNAMIC;

-- 存量 family 可见记录：将 family_id 同步到关联表（可重复执行，IGNORE 跳过已存在）
INSERT IGNORE INTO `homeai_storage_folder_family` (`id`, `folder_id`, `family_id`, `create_time`)
SELECT REPLACE(UUID(), '-', ''), `id`, `family_id`, NOW()
FROM `homeai_storage_folder`
WHERE `del_flag` = 0 AND `visibility` = 'family' AND `family_id` IS NOT NULL AND `family_id` <> '';

INSERT IGNORE INTO `homeai_storage_file_family` (`id`, `file_id`, `family_id`, `create_time`)
SELECT REPLACE(UUID(), '-', ''), `id`, `family_id`, NOW()
FROM `homeai_storage_file`
WHERE `del_flag` = 0 AND `visibility` = 'family' AND `family_id` IS NOT NULL AND `family_id` <> '';
