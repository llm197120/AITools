-- -*- coding: utf-8 -*-
-- 第 142 轮：系统运行时配置（单行 current，JSON 覆盖 yml）
CREATE TABLE IF NOT EXISTS `homeai_sys_config` (
  `id`          VARCHAR(32) NOT NULL COMMENT '主键（固定 current）',
  `content`     LONGTEXT COMMENT 'JSON：上传上限/学习提醒/微信模板/Office/OSS/文件外链',
  `create_time` DATETIME DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HomeAI 系统运行时配置';

INSERT IGNORE INTO `homeai_sys_config` (`id`, `content`) VALUES ('current', '{}');
