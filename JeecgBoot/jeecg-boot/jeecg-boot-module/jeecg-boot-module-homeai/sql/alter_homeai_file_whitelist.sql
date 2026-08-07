-- 文件上传白名单表 + 默认数据

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



INSERT INTO `homeai_file_whitelist` (`id`, `extension`, `category`, `sort_order`, `is_enabled`)

SELECT * FROM (

    SELECT 'fw_jpg' AS id, 'jpg' AS extension, 'image' AS category, 1 AS sort_order, 1 AS is_enabled

    UNION ALL SELECT 'fw_jpeg', 'jpeg', 'image', 2, 1

    UNION ALL SELECT 'fw_png', 'png', 'image', 3, 1

    UNION ALL SELECT 'fw_gif', 'gif', 'image', 4, 1

    UNION ALL SELECT 'fw_bmp', 'bmp', 'image', 5, 1

    UNION ALL SELECT 'fw_pdf', 'pdf', 'doc', 10, 1

    UNION ALL SELECT 'fw_doc', 'doc', 'doc', 11, 1

    UNION ALL SELECT 'fw_docx', 'docx', 'doc', 12, 1

    UNION ALL SELECT 'fw_xls', 'xls', 'doc', 13, 1

    UNION ALL SELECT 'fw_xlsx', 'xlsx', 'doc', 14, 1

    UNION ALL SELECT 'fw_ppt', 'ppt', 'doc', 15, 1

    UNION ALL SELECT 'fw_pptx', 'pptx', 'doc', 16, 1

    UNION ALL SELECT 'fw_mp4', 'mp4', 'video', 20, 1

    UNION ALL SELECT 'fw_avi', 'avi', 'video', 21, 1

    UNION ALL SELECT 'fw_mov', 'mov', 'video', 22, 1

    UNION ALL SELECT 'fw_mkv', 'mkv', 'video', 23, 1

    UNION ALL SELECT 'fw_zip', 'zip', 'archive', 30, 1

    UNION ALL SELECT 'fw_rar', 'rar', 'archive', 31, 1

    UNION ALL SELECT 'fw_7z', '7z', 'archive', 32, 1

    UNION ALL SELECT 'fw_txt', 'txt', 'text', 40, 1

    UNION ALL SELECT 'fw_csv', 'csv', 'text', 41, 1

    UNION ALL SELECT 'fw_md', 'md', 'text', 42, 1

) AS tmp

WHERE NOT EXISTS (SELECT 1 FROM `homeai_file_whitelist` LIMIT 1);

