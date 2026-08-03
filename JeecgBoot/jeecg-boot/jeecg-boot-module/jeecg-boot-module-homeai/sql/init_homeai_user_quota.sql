-- AI 用户 Token 额度配置表
CREATE TABLE IF NOT EXISTS `homeai_ai_user_quota` (
  `id`              VARCHAR(32)  NOT NULL COMMENT '主键',
  `user_id`         VARCHAR(32)  NOT NULL COMMENT '微信用户ID',
  `daily_limit`     INT          DEFAULT 10000  COMMENT '每日Token限额',
  `monthly_limit`   INT          DEFAULT 200000 COMMENT '每月Token限额',
  `effective_start` DATETIME     NULL COMMENT '有效期开始',
  `effective_end`   DATETIME     NULL COMMENT '有效期结束',
  `create_by`       VARCHAR(32)  NULL,
  `create_time`     DATETIME     NULL,
  `update_by`       VARCHAR(32)  NULL,
  `update_time`     DATETIME     NULL,
  `del_flag`        INT          DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_hw_quota_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI用户Token额度配置';
