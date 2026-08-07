-- 学习分类表 + 默认数据

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



INSERT INTO `homeai_learn_category` (`id`, `name`, `icon`, `color`, `sort_order`, `is_default`, `is_enabled`)

SELECT * FROM (

    SELECT 'learn_cat_course' AS id, '课程' AS name, '🎓' AS icon, '#1890ff' AS color, 1 AS sort_order, 1 AS is_default, 1 AS is_enabled

    UNION ALL SELECT 'learn_cat_book',  '书籍', '📚', '#52c41a', 2, 1, 1

    UNION ALL SELECT 'learn_cat_skill', '技能', '🛠', '#faad14', 3, 1, 1

    UNION ALL SELECT 'learn_cat_exam',  '考试', '📝', '#eb2f96', 4, 1, 1

    UNION ALL SELECT 'learn_cat_other', '其他', '📦', '#999999', 99, 1, 1

) AS tmp

WHERE NOT EXISTS (SELECT 1 FROM `homeai_learn_category` LIMIT 1);

