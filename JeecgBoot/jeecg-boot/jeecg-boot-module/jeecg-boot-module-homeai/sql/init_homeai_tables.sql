-- =============================================================
-- 家庭AI小工具 - 数据库初始化脚本
-- 基于 design/database-flows.md DDL 生成
-- 版本: v1
-- 表数量: 24
-- =============================================================

-- 确保使用正确的数据库
-- CREATE DATABASE IF NOT EXISTS `jeecg-boot` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- USE `jeecg-boot`;

-- =============================================================
-- 1. 微信用户表
-- =============================================================
CREATE TABLE IF NOT EXISTS `homeai_wx_user` (
    `id`              VARCHAR(32)  NOT NULL COMMENT '主键',
    `openid`          VARCHAR(64)  NULL COMMENT '微信openid（唯一，微信登录后填入）',
    `nickname`        VARCHAR(64)           COMMENT '微信昵称',
    `avatar_url`      VARCHAR(512)          COMMENT '头像URL',
    `phone`           VARCHAR(20)           COMMENT '手机号',
    `family_role`     VARCHAR(20)  DEFAULT '其他' COMMENT '家庭角色:爸爸/妈妈/孩子/其他',
    `family_id`       VARCHAR(32)           COMMENT '所属家庭ID（NULL=无家庭）',
    `family_role_type` VARCHAR(10) DEFAULT 'member' COMMENT '家庭成员权限:admin/member/restricted',
    `last_login_time` DATETIME              COMMENT '最后登录时间',
    `status`          VARCHAR(2)   DEFAULT '1' COMMENT '状态:1=正常 0=禁用',
    `create_by`       VARCHAR(50)           COMMENT '创建人',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    `update_by`       VARCHAR(50)           COMMENT '更新人',
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `del_flag`        TINYINT      DEFAULT 0 COMMENT '删除状态(0-正常,1-已删除)',
    `tenant_id`       VARCHAR(10)  DEFAULT '0' COMMENT '租户ID',
    `sys_org_code`    VARCHAR(64)           COMMENT '所属部门',
    PRIMARY KEY (`id`),
    KEY `idx_hw_user_family_id` (`family_id`),
    KEY `idx_hw_user_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='微信用户' ROW_FORMAT=DYNAMIC;

-- =============================================================
-- 2. 家庭表
-- =============================================================
CREATE TABLE IF NOT EXISTS `homeai_family` (
    `id`              VARCHAR(32)  NOT NULL COMMENT '主键',
    `name`            VARCHAR(100) NOT NULL COMMENT '家庭名称',
    `creator_id`      VARCHAR(32)  NULL COMMENT '创建者用户ID（APP端填写，管理端可为空）',
    `member_count`    INT          DEFAULT 1 COMMENT '成员数量',
    `status`          VARCHAR(20)  DEFAULT 'normal' COMMENT '状态: normal-正常 disbanded-已解散',
    `create_by`       VARCHAR(50)           COMMENT '创建人',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_by`       VARCHAR(50)           COMMENT '更新人',
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `del_flag`        TINYINT      DEFAULT 0 COMMENT '删除状态(0-正常,1-已删除)',
    `deleted_at`      DATETIME              COMMENT '解散时间(进入保留期)',
    `tenant_id`       VARCHAR(10)  DEFAULT '0' COMMENT '租户ID',
    `sys_org_code`    VARCHAR(64)           COMMENT '所属部门',
    PRIMARY KEY (`id`),
    KEY `idx_hw_family_creator` (`creator_id`),
    KEY `idx_hw_family_deleted` (`del_flag`, `deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='家庭' ROW_FORMAT=DYNAMIC;

-- =============================================================
-- 3. 家庭成员表
-- =============================================================
CREATE TABLE IF NOT EXISTS `homeai_family_member` (
    `id`              VARCHAR(32)  NOT NULL COMMENT '主键',
    `family_id`       VARCHAR(32)  NOT NULL COMMENT '家庭ID',
    `user_id`         VARCHAR(32)  NOT NULL COMMENT '用户ID',
    `role`            VARCHAR(20)  DEFAULT 'member' COMMENT '角色:admin/member/restricted',
    `joined_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    `create_by`       VARCHAR(50)           COMMENT '创建人',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_by`       VARCHAR(50)           COMMENT '更新人',
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uniq_hw_family_member_user_id` (`user_id`),
    KEY `idx_hw_family_member_family` (`family_id`),
    KEY `idx_hw_family_member_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='家庭成员' ROW_FORMAT=DYNAMIC;

-- =============================================================
-- 4. 邀请码表
-- =============================================================
CREATE TABLE IF NOT EXISTS `homeai_family_invite_code` (
    `id`              VARCHAR(32)  NOT NULL COMMENT '主键',
    `family_id`       VARCHAR(32)  NOT NULL COMMENT '家庭ID',
    `invite_code`     VARCHAR(10)  NOT NULL COMMENT '6位字母数字邀请码',
    `expire_at`       DATETIME     NOT NULL COMMENT '过期时间(生成后24h)',
    `used_by`         VARCHAR(32)           COMMENT '被谁使用（NULL=未使用）',
    `used_at`         DATETIME              COMMENT '使用时间',
    `create_by`       VARCHAR(50)           COMMENT '创建人',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_by`       VARCHAR(50)           COMMENT '更新人',
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uniq_hw_invite_code_code` (`invite_code`),
    KEY `idx_hw_invite_code_family` (`family_id`),
    KEY `idx_hw_invite_code_expire` (`expire_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='邀请码' ROW_FORMAT=DYNAMIC;

-- =============================================================
-- 5. AI密钥配置表
-- =============================================================
CREATE TABLE IF NOT EXISTS `homeai_ai_key_config` (
    `id`                VARCHAR(32)  NOT NULL COMMENT '主键',
    `provider`          VARCHAR(50)  NOT NULL COMMENT '提供商:DeepSeek/Qwen/OpenAI/Anthropic/Ollama',
    `model_name`        VARCHAR(100) NOT NULL COMMENT '模型名',
    `api_key_encrypted` VARCHAR(512) NOT NULL COMMENT 'AES加密后的API Key',
    `api_base_url`      VARCHAR(256)          COMMENT 'API地址（NULL=默认官方地址）',
    `remark`            VARCHAR(200)          COMMENT '备注说明',
    `is_enabled`        VARCHAR(2)   DEFAULT '1' COMMENT '是否启用:1=启用 0=停用',
    `is_default`        VARCHAR(2)   DEFAULT '0' COMMENT '是否为默认模型:1=默认 0=否',
    `sort_order`        INT          DEFAULT 0 COMMENT '排序号',
    `create_by`         VARCHAR(50)           COMMENT '创建人',
    `create_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_by`         VARCHAR(50)           COMMENT '更新人',
    `update_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_hw_ai_key_enabled` (`is_enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI密钥配置' ROW_FORMAT=DYNAMIC;

-- =============================================================
-- 6. AI对话主表
-- =============================================================
CREATE TABLE IF NOT EXISTS `homeai_ai_conversation` (
    `id`              VARCHAR(32)  NOT NULL COMMENT '主键',
    `user_id`         VARCHAR(32)  NOT NULL COMMENT '用户ID',
    `title`           VARCHAR(200) DEFAULT '新对话' COMMENT '对话标题',
    `model_name`      VARCHAR(100)          COMMENT '使用的模型名',
    `message_count`   INT          DEFAULT 0 COMMENT '消息数量',
    `create_by`       VARCHAR(50)           COMMENT '创建人',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_by`       VARCHAR(50)           COMMENT '更新人',
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `del_flag`        TINYINT      DEFAULT 0 COMMENT '删除状态(0-正常,1-已删除)',
    `deleted_at`      DATETIME              COMMENT '删除时间',
    PRIMARY KEY (`id`),
    KEY `idx_hw_conversation_user` (`user_id`),
    KEY `idx_hw_conversation_update` (`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI对话主表' ROW_FORMAT=DYNAMIC;

-- =============================================================
-- 7. AI对话消息表
-- =============================================================
CREATE TABLE IF NOT EXISTS `homeai_ai_message` (
    `id`              VARCHAR(32)  NOT NULL COMMENT '主键',
    `conversation_id` VARCHAR(32)  NOT NULL COMMENT '对话ID',
    `role`            VARCHAR(20)  NOT NULL COMMENT '角色:user/assistant/system',
    `content`         TEXT         NOT NULL COMMENT '消息内容（AES-256-GCM加密存储）',
    `content_type`    VARCHAR(20)  DEFAULT 'text' COMMENT '内容类型:text/image/file',
    `file_url`        VARCHAR(512)          COMMENT '附件文件URL',
    `token_count`     INT                   COMMENT '本条消息消耗的Token数',
    `create_by`       VARCHAR(50)           COMMENT '创建人',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `tenant_id`       VARCHAR(10)  DEFAULT '0' COMMENT '租户ID',
    `sys_org_code`    VARCHAR(64)           COMMENT '所属部门',
    PRIMARY KEY (`id`),
    KEY `idx_hw_message_conversation` (`conversation_id`),
    KEY `idx_hw_message_create` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI对话消息' ROW_FORMAT=DYNAMIC;

-- =============================================================
-- 8. Token额度消耗日志
-- =============================================================
CREATE TABLE IF NOT EXISTS `homeai_ai_quota_log` (
    `id`              VARCHAR(32)  NOT NULL COMMENT '主键',
    `user_id`         VARCHAR(32)  NOT NULL COMMENT '用户ID',
    `conversation_id` VARCHAR(32)  NOT NULL COMMENT '对话ID',
    `model_name`      VARCHAR(100) NOT NULL COMMENT '使用的模型',
    `input_tokens`    INT          DEFAULT 0 COMMENT '输入Token数',
    `output_tokens`   INT          DEFAULT 0 COMMENT '输出Token数',
    `total_tokens`    INT          DEFAULT 0 COMMENT '总Token数',
    `cost_type`       VARCHAR(20)  DEFAULT 'daily' COMMENT '扣费类型:daily=日额度 monthly=月额度',
    `create_by`       VARCHAR(50)           COMMENT '创建人',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '消耗时间',
    PRIMARY KEY (`id`),
    KEY `idx_hw_quota_user` (`user_id`),
    KEY `idx_hw_quota_create` (`create_time`),
    KEY `idx_hw_quota_user_date` (`user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Token额度消耗日志' ROW_FORMAT=DYNAMIC;

-- =============================================================
-- 9. 文件夹表
-- =============================================================
CREATE TABLE IF NOT EXISTS `homeai_storage_folder` (
    `id`              VARCHAR(32)  NOT NULL COMMENT '主键',
    `family_id`       VARCHAR(32)           COMMENT '家庭ID（NULL=个人）',
    `user_id`         VARCHAR(32)  NOT NULL COMMENT '创建者用户ID',
    `parent_id`       VARCHAR(32)           COMMENT '父文件夹ID（NULL=根目录）',
    `name`            VARCHAR(200) NOT NULL COMMENT '文件夹名称',
    `visibility`      VARCHAR(20)  DEFAULT 'private' COMMENT '可见性:private/family/public',
    `level`           INT          DEFAULT 0 COMMENT '嵌套层级',
    `create_by`       VARCHAR(50)           COMMENT '创建人',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_by`       VARCHAR(50)           COMMENT '更新人',
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `del_flag`        TINYINT      DEFAULT 0 COMMENT '删除状态(0-正常,1-已删除)',
    `deleted_at`      DATETIME              COMMENT '删除时间',
    PRIMARY KEY (`id`),
    KEY `idx_hw_folder_family` (`family_id`),
    KEY `idx_hw_folder_user` (`user_id`),
    KEY `idx_hw_folder_parent` (`parent_id`),
    KEY `idx_hw_folder_recycle` (`del_flag`, `deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件夹' ROW_FORMAT=DYNAMIC;

-- =============================================================
-- 10. 文件记录表
-- =============================================================
CREATE TABLE IF NOT EXISTS `homeai_storage_file` (
    `id`              VARCHAR(32)  NOT NULL COMMENT '主键',
    `family_id`       VARCHAR(32)           COMMENT '家庭ID（NULL=个人）',
    `user_id`         VARCHAR(32)  NOT NULL COMMENT '上传者用户ID',
    `folder_id`       VARCHAR(32)           COMMENT '所属文件夹ID（NULL=根目录）',
    `original_name`   VARCHAR(300) NOT NULL COMMENT '原始文件名',
    `stored_name`     VARCHAR(200) NOT NULL COMMENT '存储文件名(UUID+ext)',
    `extension`       VARCHAR(20)  NOT NULL COMMENT '文件扩展名',
    `mime_type`       VARCHAR(100)          COMMENT 'MIME类型',
    `file_size`       BIGINT       DEFAULT 0 COMMENT '文件大小(字节)',
    `file_url`        VARCHAR(512) NOT NULL COMMENT '文件访问URL',
    `thumbnail_url`   VARCHAR(512)          COMMENT '缩略图URL（图片/视频）',
    `visibility`      VARCHAR(20)  DEFAULT 'private' COMMENT '可见性:private/family/public',
    `is_favorite`     VARCHAR(2)   DEFAULT '0' COMMENT '是否收藏:1=是 0=否',
    `download_count`  INT          DEFAULT 0 COMMENT '下载次数',
    `create_by`       VARCHAR(50)           COMMENT '创建人',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_by`       VARCHAR(50)           COMMENT '更新人',
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `del_flag`        TINYINT      DEFAULT 0 COMMENT '删除状态(0-正常,1-已删除)',
    `deleted_at`      DATETIME              COMMENT '删除时间',
    PRIMARY KEY (`id`),
    KEY `idx_hw_file_family_folder` (`family_id`, `folder_id`),
    KEY `idx_hw_file_user` (`user_id`),
    KEY `idx_hw_file_visibility` (`visibility`),
    KEY `idx_hw_file_create` (`create_time` DESC),
    KEY `idx_hw_file_extension` (`extension`),
    FULLTEXT KEY `idx_hw_file_name_fulltext` (`original_name`) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件记录' ROW_FORMAT=DYNAMIC;

-- =============================================================
-- 11. Office转换历史
-- =============================================================
CREATE TABLE IF NOT EXISTS `homeai_office_convert_history` (
    `id`              VARCHAR(32)  NOT NULL COMMENT '主键',
    `file_id`         VARCHAR(32)  NOT NULL COMMENT '源文件ID',
    `user_id`         VARCHAR(32)  NOT NULL COMMENT '操作人',
    `convert_type`    VARCHAR(20)  NOT NULL COMMENT '转换类型:format_convert=格式转换 ai_generate=AI生成',
    `source_format`   VARCHAR(20)           COMMENT '源格式',
    `target_format`   VARCHAR(20)           COMMENT '目标格式（格式转换时）',
    `instruction`     VARCHAR(1000)         COMMENT 'AI生成指令',
    `status`          VARCHAR(20)  DEFAULT 'PENDING' COMMENT '状态:PENDING/PROCESSING/COMPLETED/FAILED',
    `result_file_url` VARCHAR(512)          COMMENT '结果文件URL',
    `result_file_size` BIGINT               COMMENT '结果文件大小',
    `error_message`   VARCHAR(500)          COMMENT '失败原因',
    `task_duration`   INT                   COMMENT '处理耗时（秒）',
    `create_by`       VARCHAR(50)           COMMENT '创建人',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_by`       VARCHAR(50)           COMMENT '更新人',
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `completed_at`    DATETIME              COMMENT '完成时间',
    `tenant_id`       VARCHAR(10)  DEFAULT '0' COMMENT '租户ID',
    `sys_org_code`    VARCHAR(64)           COMMENT '所属部门',
    PRIMARY KEY (`id`),
    KEY `idx_hw_convert_file` (`file_id`),
    KEY `idx_hw_convert_user` (`user_id`),
    KEY `idx_hw_convert_status` (`status`),
    KEY `idx_hw_convert_create` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Office转换历史' ROW_FORMAT=DYNAMIC;

-- =============================================================
-- 12. 文档模板表
-- =============================================================
CREATE TABLE IF NOT EXISTS `homeai_office_template` (
    `id`              VARCHAR(32)  NOT NULL COMMENT '主键',
    `name`            VARCHAR(200) NOT NULL COMMENT '模板名称',
    `type`            VARCHAR(20)  NOT NULL COMMENT '模板类型:word/excel/ppt',
    `file_url`        VARCHAR(512) NOT NULL COMMENT '模板文件URL',
    `preview_url`     VARCHAR(512)          COMMENT '预览图URL',
    `is_default`      VARCHAR(2)   DEFAULT '0' COMMENT '是否默认模板:1=是 0=否',
    `remark`          VARCHAR(500)          COMMENT '备注',
    `create_by`       VARCHAR(50)           COMMENT '创建人',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_by`       VARCHAR(50)           COMMENT '更新人',
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_hw_template_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档模板' ROW_FORMAT=DYNAMIC;

-- =============================================================
-- 13. 格式转换规则表
-- =============================================================
CREATE TABLE IF NOT EXISTS `homeai_convert_rule` (
    `id`                VARCHAR(32)  NOT NULL COMMENT '主键',
    `source_format`     VARCHAR(20)  NOT NULL COMMENT '源格式(如docx)',
    `target_format`     VARCHAR(20)  NOT NULL COMMENT '目标格式(如pdf)',
    `is_enabled`        VARCHAR(2)   DEFAULT '1' COMMENT '是否启用:1=启用 0=停用',
    `create_by`         VARCHAR(50)           COMMENT '创建人',
    `create_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_by`         VARCHAR(50)           COMMENT '更新人',
    `update_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_hw_convert_rule_source` (`source_format`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='格式转换规则' ROW_FORMAT=DYNAMIC;

-- =============================================================
-- 14. 账单记录表
-- =============================================================
CREATE TABLE IF NOT EXISTS `homeai_bill_entry` (
    `id`              VARCHAR(32)  NOT NULL COMMENT '主键',
    `family_id`       VARCHAR(32)           COMMENT '家庭ID（NULL=个人）',
    `user_id`         VARCHAR(32)  NULL COMMENT '录入人（NULL=管理端录入）',
    `bill_date`       DATE         NOT NULL COMMENT '账单日期',
    `type`            VARCHAR(10)  NOT NULL COMMENT '类型:income=收入 expense=支出',
    `amount`          DECIMAL(12,2) NOT NULL COMMENT '金额(精确到分)',
    `category_id`     VARCHAR(32)  NULL COMMENT '分类ID（NULL=未分类）',
    `payment_method`  VARCHAR(20)  DEFAULT '微信' COMMENT '支付方式:微信/支付宝/现金/银行卡/其他',
    `remark`          VARCHAR(500)          COMMENT '备注',
    `voucher_url`     VARCHAR(512)          COMMENT '凭证图片URL',
    `source`          VARCHAR(20)  DEFAULT 'manual' COMMENT '来源:manual=手动录入 import_csv=CSV导入 import_excel=Excel导入 ai_import=AI识别',
    `version`         INT          DEFAULT 0 COMMENT '乐观锁版本号',
    `create_by`       VARCHAR(50)           COMMENT '创建人',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_by`       VARCHAR(50)           COMMENT '更新人',
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `del_flag`        TINYINT      DEFAULT 0 COMMENT '删除状态(0-正常,1-已删除)',
    `deleted_at`      DATETIME              COMMENT '删除时间',
    `deleted_by`      VARCHAR(32)           COMMENT '删除人',
    PRIMARY KEY (`id`),
    KEY `idx_hw_bill_family_date` (`family_id`, `bill_date`),
    KEY `idx_hw_bill_user` (`user_id`),
    KEY `idx_hw_bill_category` (`category_id`),
    KEY `idx_hw_bill_type_date` (`type`, `bill_date`),
    KEY `idx_hw_bill_deleted` (`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='账单记录' ROW_FORMAT=DYNAMIC;

-- =============================================================
-- 15. 账单分类表
-- =============================================================
CREATE TABLE IF NOT EXISTS `homeai_bill_category` (
    `id`              VARCHAR(32)  NOT NULL COMMENT '主键',
    `name`            VARCHAR(50)  NOT NULL COMMENT '分类名称',
    `icon`            VARCHAR(10)  DEFAULT '📦' COMMENT '分类图标(emoji)',
    `color`           VARCHAR(10)  DEFAULT '#999' COMMENT '分类颜色(十六进制)',
    `type`            VARCHAR(10)  DEFAULT 'expense' COMMENT '类型:income=收入 expense=支出',
    `sort_order`      INT          DEFAULT 0 COMMENT '排序号',
    `is_default`      VARCHAR(2)   DEFAULT '0' COMMENT '是否系统默认:1=默认(不可删) 0=自定义',
    `is_enabled`      VARCHAR(2)   DEFAULT '1' COMMENT '是否启用:1=启用 0=停用',
    `version`         INT          DEFAULT 0 COMMENT '乐观锁版本号',
    `create_by`       VARCHAR(50)           COMMENT '创建人',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_by`       VARCHAR(50)           COMMENT '更新人',
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `del_flag`        TINYINT      DEFAULT 0 COMMENT '删除状态(0-正常,1-已删除)',
    PRIMARY KEY (`id`),
    KEY `idx_hw_bill_cat_type` (`type`),
    KEY `idx_hw_bill_cat_enabled` (`is_enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='账单分类' ROW_FORMAT=DYNAMIC;

-- =============================================================
-- 16. 账单导入记录表
-- =============================================================
CREATE TABLE IF NOT EXISTS `homeai_bill_import_record` (
    `id`              VARCHAR(32)  NOT NULL COMMENT '主键',
    `family_id`       VARCHAR(32)           COMMENT '家庭ID',
    `user_id`         VARCHAR(32)  NOT NULL COMMENT '导入人',
    `import_type`     VARCHAR(20)  NOT NULL COMMENT '导入类型:wechat_csv/excel/bank_statement/ai_import',
    `file_name`       VARCHAR(300) NOT NULL COMMENT '导入文件名',
    `file_url`        VARCHAR(512)          COMMENT '文件存储URL',
    `total_count`     INT          DEFAULT 0 COMMENT '解析总条数',
    `success_count`   INT          DEFAULT 0 COMMENT '成功导入条数',
    `fail_count`      INT          DEFAULT 0 COMMENT '失败条数',
    `status`          VARCHAR(20)  DEFAULT 'preview' COMMENT '状态:preview=预览 confirmed=已确认部分 failed=全部失败',
    `ai_used`         VARCHAR(2)   DEFAULT '0' COMMENT '是否使用AI识别:1=是 0=否',
    `create_by`       VARCHAR(50)           COMMENT '创建人',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_by`       VARCHAR(50)           COMMENT '更新人',
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `tenant_id`       VARCHAR(10)  DEFAULT '0' COMMENT '租户ID',
    `sys_org_code`    VARCHAR(64)           COMMENT '所属部门',
    PRIMARY KEY (`id`),
    KEY `idx_hw_import_family` (`family_id`),
    KEY `idx_hw_import_user` (`user_id`),
    KEY `idx_hw_import_create` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='账单导入记录' ROW_FORMAT=DYNAMIC;

-- =============================================================
-- 17. 计划主表（重复计划）
-- =============================================================
CREATE TABLE IF NOT EXISTS `homeai_plan_master` (
    `id`              VARCHAR(32)  NOT NULL COMMENT '主键',
    `user_id`         VARCHAR(32)  NULL COMMENT '创建者用户ID（NULL=管理端录入）',
    `title`           VARCHAR(100) NOT NULL COMMENT '计划标题',
    `content`         TEXT                   COMMENT '计划内容',
    `category`        VARCHAR(20)  DEFAULT '生活' COMMENT '分类:工作/学习/生活/运动/家庭/其他',
    `recipe_id`       VARCHAR(32)           COMMENT '关联菜谱ID',
    `priority`        VARCHAR(10)  DEFAULT 'normal' COMMENT '优先级:normal/important/urgent',
    `is_all_day`      VARCHAR(2)   DEFAULT '0' COMMENT '是否全天:1=全天 0=定时',
    `start_time`      TIME                   COMMENT '开始时间',
    `end_time`        TIME                   COMMENT '结束时间',
    `remind_before`   INT          DEFAULT 0 COMMENT '提前提醒分钟数:0=不提醒',
    `repeat_type`     VARCHAR(20)  DEFAULT 'none' COMMENT '重复类型:none/daily/weekly/monthly/custom',
    `repeat_end_date` DATE                   COMMENT '重复结束日期（NULL=永久）',
    `is_repeat_master` VARCHAR(2)  DEFAULT '0' COMMENT '是否为重复计划主记录:1=是 0=否',
    `create_by`       VARCHAR(50)           COMMENT '创建人',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_by`       VARCHAR(50)           COMMENT '更新人',
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `del_flag`        TINYINT      DEFAULT 0 COMMENT '删除状态(0-正常,1-已删除)',
    PRIMARY KEY (`id`),
    KEY `idx_hw_plan_master_user` (`user_id`),
    KEY `idx_hw_plan_master_repeat` (`is_repeat_master`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='计划主表' ROW_FORMAT=DYNAMIC;

-- =============================================================
-- 17.1 计划分类表
-- =============================================================
CREATE TABLE IF NOT EXISTS `homeai_plan_category` (
    `id`              VARCHAR(32)  NOT NULL COMMENT '主键',
    `name`            VARCHAR(50)  NOT NULL COMMENT '分类名称',
    `icon`            VARCHAR(10)  DEFAULT '📋' COMMENT '分类图标(emoji)',
    `color`           VARCHAR(10)  DEFAULT '#999' COMMENT '分类颜色(十六进制)',
    `sort_order`      INT          DEFAULT 0 COMMENT '排序号',
    `is_default`      TINYINT      DEFAULT 0 COMMENT '是否系统默认:1=默认 0=自定义',
    `is_enabled`      TINYINT      DEFAULT 1 COMMENT '是否启用:1=启用 0=停用',
    `version`         INT          DEFAULT 0 COMMENT '乐观锁版本号',
    `create_by`       VARCHAR(50)           COMMENT '创建人',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_by`       VARCHAR(50)           COMMENT '更新人',
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `del_flag`        TINYINT      DEFAULT 0 COMMENT '删除状态(0-正常,1-已删除)',
    PRIMARY KEY (`id`),
    KEY `idx_hw_plan_cat_enabled` (`is_enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='计划分类' ROW_FORMAT=DYNAMIC;

-- =============================================================
-- 18. 计划实例表（每日记录）
-- =============================================================
CREATE TABLE IF NOT EXISTS `homeai_plan_instance` (
    `id`              VARCHAR(32)  NOT NULL COMMENT '主键',
    `master_id`       VARCHAR(32)           COMMENT '主计划ID（NULL=一次性计划）',
    `user_id`         VARCHAR(32)  NOT NULL COMMENT '用户ID',
    `plan_date`       DATE         NOT NULL COMMENT '计划日期',
    `title`           VARCHAR(100) NOT NULL COMMENT '计划标题',
    `content`         TEXT                   COMMENT '计划内容',
    `category`        VARCHAR(20)  DEFAULT '生活' COMMENT '分类',
    `priority`        VARCHAR(10)  DEFAULT 'normal' COMMENT '优先级',
    `start_time`      TIME                   COMMENT '开始时间',
    `end_time`        TIME                   COMMENT '结束时间',
    `remind_at`       DATETIME              COMMENT '提醒时间',
    `reminded`        VARCHAR(2)   DEFAULT '0' COMMENT '是否已提醒:1=是 0=否',
    `status`          VARCHAR(20)  DEFAULT 'pending' COMMENT '状态:pending/completed/cancelled/expired',
    `completed_at`    DATETIME              COMMENT '完成时间',
    `create_by`       VARCHAR(50)           COMMENT '创建人',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_by`       VARCHAR(50)           COMMENT '更新人',
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `del_flag`        TINYINT      DEFAULT 0 COMMENT '删除状态(0-正常,1-已删除)',
    `deleted_at`      DATETIME              COMMENT '删除时间',
    PRIMARY KEY (`id`),
    KEY `idx_hw_plan_inst_master` (`master_id`),
    KEY `idx_hw_plan_inst_user_date` (`user_id`, `plan_date`),
    KEY `idx_hw_plan_inst_date` (`plan_date`),
    KEY `idx_hw_plan_inst_status` (`status`),
    KEY `idx_hw_plan_inst_remind` (`reminded`, `remind_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='计划实例' ROW_FORMAT=DYNAMIC;

-- =============================================================
-- 19. 菜谱表
-- =============================================================
CREATE TABLE IF NOT EXISTS `homeai_recipe` (
    `id`              VARCHAR(32)  NOT NULL COMMENT '主键',
    `user_id`         VARCHAR(32)  NULL COMMENT '创建者用户ID（NULL=管理端录入）',
    `family_id`       VARCHAR(32)           COMMENT '家庭ID',
    `name`            VARCHAR(200) NOT NULL COMMENT '菜名',
    `category`        VARCHAR(50)  NOT NULL COMMENT '分类',
    `cover_image`     VARCHAR(512)          COMMENT '封面图URL',
    `video_url`       VARCHAR(512)          COMMENT '做菜视频URL',
    `difficulty`      INT          DEFAULT 1 COMMENT '难度:1-5星',
    `cook_time`       INT                   COMMENT '烹饪时间(分钟)',
    `servings`        INT          DEFAULT 1 COMMENT '份数',
    `tips`            TEXT                   COMMENT '小贴士',
    `view_count`      INT          DEFAULT 0 COMMENT '浏览次数',
    `favorite_count`  INT          DEFAULT 0 COMMENT '收藏次数',
    `visibility`      VARCHAR(20)  DEFAULT 'private' COMMENT '可见性:private/family/public',
    `audit_status`    VARCHAR(20)  DEFAULT 'approved' COMMENT '审核状态:approved/rejected/pending',
    `create_by`       VARCHAR(50)           COMMENT '创建人',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_by`       VARCHAR(50)           COMMENT '更新人',
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `del_flag`        TINYINT      DEFAULT 0 COMMENT '删除状态(0-正常,1-已删除)',
    `deleted_at`      DATETIME              COMMENT '删除时间',
    `deleted_by`      VARCHAR(32)           COMMENT '删除人',
    PRIMARY KEY (`id`),
    KEY `idx_hw_recipe_user` (`user_id`),
    KEY `idx_hw_recipe_family` (`family_id`),
    KEY `idx_hw_recipe_category` (`category`),
    KEY `idx_hw_recipe_visibility` (`visibility`),
    KEY `idx_hw_recipe_create` (`create_time` DESC),
    KEY `idx_hw_recipe_view` (`view_count`),
    FULLTEXT KEY `idx_hw_recipe_name_fulltext` (`name`) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜谱' ROW_FORMAT=DYNAMIC;

-- =============================================================
-- 20. 菜谱食材表
-- =============================================================
CREATE TABLE IF NOT EXISTS `homeai_recipe_ingredient` (
    `id`              VARCHAR(32)  NOT NULL COMMENT '主键',
    `recipe_id`       VARCHAR(32)  NOT NULL COMMENT '菜谱ID',
    `name`            VARCHAR(100) NOT NULL COMMENT '食材名',
    `quantity`        DECIMAL(10,2)         COMMENT '数量',
    `unit`            VARCHAR(20)           COMMENT '单位:克/毫升/个/根/块/勺等',
    `sort_order`      INT          DEFAULT 0 COMMENT '排序号',
    `create_by`       VARCHAR(50)           COMMENT '创建人',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_by`       VARCHAR(50)           COMMENT '更新人',
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_hw_ingredient_recipe` (`recipe_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜谱食材' ROW_FORMAT=DYNAMIC;

-- =============================================================
-- 21. 菜谱步骤表
-- =============================================================
CREATE TABLE IF NOT EXISTS `homeai_recipe_step` (
    `id`              VARCHAR(32)  NOT NULL COMMENT '主键',
    `recipe_id`       VARCHAR(32)  NOT NULL COMMENT '菜谱ID',
    `step_number`     INT          NOT NULL COMMENT '步骤序号',
    `description`     TEXT         NOT NULL COMMENT '步骤说明',
    `image_url`       VARCHAR(512)          COMMENT '步骤图片URL',
    `sort_order`      INT          DEFAULT 0 COMMENT '排序号（支持拖拽排序）',
    `create_by`       VARCHAR(50)           COMMENT '创建人',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_by`       VARCHAR(50)           COMMENT '更新人',
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_hw_step_recipe` (`recipe_id`),
    KEY `idx_hw_step_order` (`step_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜谱步骤' ROW_FORMAT=DYNAMIC;

CREATE TABLE IF NOT EXISTS `homeai_recipe_favorite` (
    `id`          VARCHAR(32) NOT NULL COMMENT '主键',
    `user_id`     VARCHAR(32) NOT NULL COMMENT '用户ID',
    `recipe_id`   VARCHAR(32) NOT NULL COMMENT '菜谱ID',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_hw_recipe_fav_user_recipe` (`user_id`, `recipe_id`),
    KEY `idx_hw_recipe_fav_recipe` (`recipe_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜谱收藏' ROW_FORMAT=DYNAMIC;

-- =============================================================
-- 22. 学习资料表
-- =============================================================
CREATE TABLE IF NOT EXISTS `homeai_learn_material` (
    `id`              VARCHAR(32)  NOT NULL COMMENT '主键',
    `user_id`         VARCHAR(32)  NULL COMMENT '上传者用户ID（NULL=管理端录入）',
    `family_id`       VARCHAR(32)           COMMENT '家庭ID',
    `title`           VARCHAR(200) NOT NULL COMMENT '资料标题',
    `type`            VARCHAR(20)  NOT NULL COMMENT '类型:video/image/pdf/doc/xls/ppt/link/note',
    `file_url`        VARCHAR(512)          COMMENT '文件URL',
    `thumbnail_url`   VARCHAR(512)          COMMENT '缩略图URL',
    `category`        VARCHAR(50)           COMMENT '分类名称(冗余)',
    `category_id`     VARCHAR(32)           COMMENT '分类ID',
    `tags`            VARCHAR(500)          COMMENT '标签(JSON数组)',
    `visibility`      VARCHAR(20)  DEFAULT 'private' COMMENT '可见性:private/family/public',
    `study_count`     INT          DEFAULT 0 COMMENT '学习次数',
    `favorite_count`  INT          DEFAULT 0 COMMENT '收藏次数',
    `create_by`       VARCHAR(50)           COMMENT '创建人',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_by`       VARCHAR(50)           COMMENT '更新人',
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `del_flag`        TINYINT      DEFAULT 0 COMMENT '删除状态(0-正常,1-已删除)',
    `tenant_id`       VARCHAR(10)  DEFAULT '0' COMMENT '租户ID',
    `sys_org_code`    VARCHAR(64)           COMMENT '所属部门',
    PRIMARY KEY (`id`),
    KEY `idx_hw_material_user` (`user_id`),
    KEY `idx_hw_material_family` (`family_id`),
    KEY `idx_hw_material_type` (`type`),
    KEY `idx_hw_material_category` (`category`),
    KEY `idx_hw_material_visibility` (`visibility`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习资料' ROW_FORMAT=DYNAMIC;

-- =============================================================
-- 22.1 学习分类表
-- =============================================================
CREATE TABLE IF NOT EXISTS `homeai_learn_category` (
    `id`              VARCHAR(32)  NOT NULL COMMENT '主键',
    `name`            VARCHAR(50)  NOT NULL COMMENT '分类名称',
    `icon`            VARCHAR(10)  DEFAULT '📖' COMMENT '分类图标(emoji)',
    `color`           VARCHAR(10)  DEFAULT '#999' COMMENT '分类颜色(十六进制)',
    `sort_order`      INT          DEFAULT 0 COMMENT '排序号',
    `is_default`      TINYINT      DEFAULT 0 COMMENT '是否系统默认:1=默认 0=自定义',
    `is_enabled`      TINYINT      DEFAULT 1 COMMENT '是否启用:1=启用 0=停用',
    `version`         INT          DEFAULT 0 COMMENT '乐观锁版本号',
    `create_by`       VARCHAR(50)           COMMENT '创建人',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_by`       VARCHAR(50)           COMMENT '更新人',
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `del_flag`        TINYINT      DEFAULT 0 COMMENT '删除状态(0-正常,1-已删除)',
    PRIMARY KEY (`id`),
    KEY `idx_hw_learn_cat_enabled` (`is_enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习分类' ROW_FORMAT=DYNAMIC;

-- =============================================================
-- 22.2 文件上传白名单表
-- =============================================================
CREATE TABLE IF NOT EXISTS `homeai_file_whitelist` (
    `id`              VARCHAR(32)  NOT NULL COMMENT '主键',
    `extension`       VARCHAR(20)  NOT NULL COMMENT '扩展名(不含点,小写)',
    `category`        VARCHAR(20)  DEFAULT 'other' COMMENT '分类:image/doc/video/archive/text/other',
    `sort_order`      INT          DEFAULT 0 COMMENT '排序号',
    `is_enabled`      TINYINT      DEFAULT 1 COMMENT '是否启用:1=启用 0=停用',
    `create_by`       VARCHAR(50)           COMMENT '创建人',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_by`       VARCHAR(50)           COMMENT '更新人',
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `del_flag`        TINYINT      DEFAULT 0 COMMENT '删除状态(0-正常,1-已删除)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_hw_file_whitelist_ext` (`extension`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件上传白名单' ROW_FORMAT=DYNAMIC;

-- =============================================================
-- 23. 学习记录表
-- =============================================================
CREATE TABLE IF NOT EXISTS `homeai_learn_record` (
    `id`              VARCHAR(32)  NOT NULL COMMENT '主键',
    `user_id`         VARCHAR(32)  NOT NULL COMMENT '用户ID',
    `material_id`     VARCHAR(32)  NOT NULL COMMENT '学习资料ID',
    `start_time`      DATETIME              COMMENT '开始时间',
    `end_time`        DATETIME              COMMENT '结束时间',
    `mode`            VARCHAR(20)  DEFAULT 'timer' COMMENT '学习模式:timer=计时 manual=手动',
    `duration`        INT          DEFAULT 0 COMMENT '学习时长(秒)',
    `notes`           TEXT                   COMMENT '学习笔记',
    `create_by`       VARCHAR(50)           COMMENT '创建人',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_hw_record_user` (`user_id`),
    KEY `idx_hw_record_material` (`material_id`),
    KEY `idx_hw_record_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习记录' ROW_FORMAT=DYNAMIC;

-- =============================================================
-- 24. 操作审计日志表
-- =============================================================
CREATE TABLE IF NOT EXISTS `homeai_audit_log` (
    `id`              VARCHAR(32)  NOT NULL COMMENT '主键',
    `user_id`         VARCHAR(32)  NOT NULL COMMENT '操作人ID',
    `action_type`     VARCHAR(50)  NOT NULL COMMENT '操作类型:file_upload/file_delete/bill_add/family_create等',
    `module`          VARCHAR(50)  NOT NULL COMMENT '所属模块:storage/bill/family/ai/plan/recipe/learn',
    `target_id`       VARCHAR(32)           COMMENT '操作对象ID',
    `target_summary`  VARCHAR(500)          COMMENT '操作对象摘要(如文件名/账单摘要)',
    `detail`          JSON                   COMMENT '操作详情JSON',
    `result`          VARCHAR(10)  DEFAULT 'success' COMMENT '结果:success/fail',
    `ip_address`      VARCHAR(50)           COMMENT '操作IP',
    `create_by`       VARCHAR(50)           COMMENT '创建人',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY `idx_hw_audit_user` (`user_id`),
    KEY `idx_hw_audit_action` (`action_type`),
    KEY `idx_hw_audit_module` (`module`),
    KEY `idx_hw_audit_create` (`create_time`),
    KEY `idx_hw_audit_module_time` (`module`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作审计日志' ROW_FORMAT=DYNAMIC;

-- =============================================================
-- 初始化数据：默认账单分类
-- =============================================================
INSERT INTO `homeai_bill_category` (`id`, `name`, `icon`, `color`, `type`, `sort_order`, `is_default`, `is_enabled`)
SELECT * FROM (
    SELECT 'bill_cat_exp_food'   AS id, '餐饮' AS name, '🍜' AS icon, '#FF6B6B' AS color, 'expense' AS type, 1 AS sort_order, '1' AS is_default, '1' AS is_enabled
    UNION ALL SELECT 'bill_cat_exp_traffic', '交通', '🚗', '#4ECDC4', 'expense', 2, '1', '1'
    UNION ALL SELECT 'bill_cat_exp_shop',    '购物', '🛒', '#45B7D1', 'expense', 3, '1', '1'
    UNION ALL SELECT 'bill_cat_exp_home',    '居住', '🏠', '#96CEB4', 'expense', 4, '1', '1'
    UNION ALL SELECT 'bill_cat_exp_fun',     '娱乐', '🎮', '#FFEAA7', 'expense', 5, '1', '1'
    UNION ALL SELECT 'bill_cat_exp_edu',     '教育', '📚', '#DDA0DD', 'expense', 6, '1', '1'
    UNION ALL SELECT 'bill_cat_exp_med',     '医疗', '🏥', '#FF6B6B', 'expense', 7, '1', '1'
    UNION ALL SELECT 'bill_cat_exp_other',   '其他支出', '📦', '#999999', 'expense', 99, '1', '1'
    UNION ALL SELECT 'bill_cat_inc_salary',  '工资', '💰', '#2ECC71', 'income', 1, '1', '1'
    UNION ALL SELECT 'bill_cat_inc_bonus',   '奖金', '🎯', '#F39C12', 'income', 2, '1', '1'
    UNION ALL SELECT 'bill_cat_inc_finance', '理财', '📈', '#3498DB', 'income', 3, '1', '1'
    UNION ALL SELECT 'bill_cat_inc_other',   '其他收入', '📦', '#999999', 'income', 99, '1', '1'
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM `homeai_bill_category` WHERE `is_default` = '1' LIMIT 1);

-- =============================================================
-- 初始化数据：默认计划分类
-- =============================================================
INSERT IGNORE INTO `homeai_plan_category` (`id`, `name`, `icon`, `color`, `sort_order`, `is_default`, `is_enabled`)
VALUES
('plan_cat_work',   '工作', '💼', '#1890ff', 1, 1, 1),
('plan_cat_study',  '学习', '📚', '#52c41a', 2, 1, 1),
('plan_cat_life',   '生活', '🏠', '#faad14', 3, 1, 1),
('plan_cat_sport',  '运动', '🏃', '#eb2f96', 4, 1, 1),
('plan_cat_family', '家庭', '👨‍👩‍👧', '#722ed1', 5, 1, 1),
('plan_cat_other',  '其他', '📦', '#999999', 99, 1, 1);

-- =============================================================
-- 初始化数据：默认学习分类
-- =============================================================
INSERT IGNORE INTO `homeai_learn_category` (`id`, `name`, `icon`, `color`, `sort_order`, `is_default`, `is_enabled`)
VALUES
('learn_cat_course',  '课程', '🎓', '#1890ff', 1, 1, 1),
('learn_cat_book',    '书籍', '📚', '#52c41a', 2, 1, 1),
('learn_cat_skill',   '技能', '🛠', '#faad14', 3, 1, 1),
('learn_cat_exam',    '考试', '📝', '#eb2f96', 4, 1, 1),
('learn_cat_other',   '其他', '📦', '#999999', 99, 1, 1);

-- =============================================================
-- 初始化数据：默认文件白名单
-- =============================================================
INSERT IGNORE INTO `homeai_file_whitelist` (`id`, `extension`, `category`, `sort_order`, `is_enabled`)
VALUES
('fw_jpg',  'jpg',  'image',   1, 1),
('fw_jpeg', 'jpeg', 'image',   2, 1),
('fw_png',  'png',  'image',   3, 1),
('fw_gif',  'gif',  'image',   4, 1),
('fw_bmp',  'bmp',  'image',   5, 1),
('fw_pdf',  'pdf',  'doc',     10, 1),
('fw_doc',  'doc',  'doc',     11, 1),
('fw_docx', 'docx', 'doc',     12, 1),
('fw_xls',  'xls',  'doc',     13, 1),
('fw_xlsx', 'xlsx', 'doc',     14, 1),
('fw_ppt',  'ppt',  'doc',     15, 1),
('fw_pptx', 'pptx', 'doc',     16, 1),
('fw_mp4',  'mp4',  'video',   20, 1),
('fw_avi',  'avi',  'video',   21, 1),
('fw_mov',  'mov',  'video',   22, 1),
('fw_mkv',  'mkv',  'video',   23, 1),
('fw_zip',  'zip',  'archive', 30, 1),
('fw_rar',  'rar',  'archive', 31, 1),
('fw_7z',   '7z',   'archive', 32, 1),
('fw_txt',  'txt',  'text',    40, 1),
('fw_csv',  'csv',  'text',    41, 1),
('fw_md',   'md',   'text',    42, 1);
