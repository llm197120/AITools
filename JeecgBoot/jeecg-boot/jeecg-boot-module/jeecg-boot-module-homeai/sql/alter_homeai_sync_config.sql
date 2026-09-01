-- HomeAI 离线同步配置（单行 current）
CREATE TABLE IF NOT EXISTS `homeai_sync_config` (
  `id` varchar(32) NOT NULL COMMENT '主键（固定 current）',
  `batch_size` int DEFAULT 1 COMMENT '每批同步条数',
  `interval_ms` int DEFAULT 5000 COMMENT '批间隔毫秒',
  `max_retries_per_day` int DEFAULT 20 COMMENT '单条 24h 最大尝试次数',
  `image_cache_limit_mb` int DEFAULT 4096 COMMENT '图片缓存上限 MB',
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='HomeAI 离线同步与缓存配置';

INSERT INTO `homeai_sync_config` (`id`, `batch_size`, `interval_ms`, `max_retries_per_day`, `image_cache_limit_mb`)
VALUES ('current', 1, 5000, 20, 4096)
ON DUPLICATE KEY UPDATE `id` = VALUES(`id`);