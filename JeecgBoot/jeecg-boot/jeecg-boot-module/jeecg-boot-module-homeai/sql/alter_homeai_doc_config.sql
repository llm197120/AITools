-- HomeAI 协议/隐私富文本配置（id: agreement=用户协议, privacy=隐私政策）
CREATE TABLE IF NOT EXISTS `homeai_doc_config` (
  `id` varchar(32) NOT NULL COMMENT '主键（agreement/privacy）',
  `content` longtext COMMENT '富文本 HTML 内容',
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='HomeAI 协议与隐私政策配置';

INSERT INTO `homeai_doc_config` (`id`, `content`) VALUES ('agreement', ''), ('privacy', '')
ON DUPLICATE KEY UPDATE `id` = VALUES(`id`);