-- 计划分类表（独立维护分类数据）
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

-- 默认计划分类
INSERT INTO `homeai_plan_category` (`id`, `name`, `icon`, `color`, `sort_order`, `is_default`, `is_enabled`)
SELECT * FROM (
    SELECT 'plan_cat_work'   AS id, '工作' AS name, '💼' AS icon, '#1890ff' AS color, 1 AS sort_order, 1 AS is_default, 1 AS is_enabled
    UNION ALL SELECT 'plan_cat_study',  '学习', '📚', '#52c41a', 2, 1, 1
    UNION ALL SELECT 'plan_cat_life',   '生活', '🏠', '#faad14', 3, 1, 1
    UNION ALL SELECT 'plan_cat_sport',  '运动', '🏃', '#eb2f96', 4, 1, 1
    UNION ALL SELECT 'plan_cat_family', '家庭', '👨‍👩‍👧', '#722ed1', 5, 1, 1
    UNION ALL SELECT 'plan_cat_other',  '其他', '📦', '#999999', 99, 1, 1
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM `homeai_plan_category` LIMIT 1);

-- 计划分类管理菜单
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `perms`, `menu_type`, `icon`, `sort_no`, `status`, `del_flag`, `create_time`)
SELECT 'homeai_menu_plan_category', 'homeai_menu_root', '计划分类', '/homeai/plan/planCategory', '/views/homeai/plan/planCategory', 1, 'homeai:plan:category:list', 1, 'ant-design:tags-outlined', 7.1, 1, 0, NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `id` = 'homeai_menu_plan_category');
