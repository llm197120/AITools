-- -*- coding: utf-8 -*-
-- 第 63 轮：Office 预览 PDF 缓存字段（已有库可重复执行）

SET @sql := (
    SELECT IF(
        EXISTS(SELECT 1 FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'homeai_storage_file'
                 AND COLUMN_NAME = 'preview_pdf_url'),
        'SELECT 1',
        'ALTER TABLE `homeai_storage_file` ADD COLUMN `preview_pdf_url` VARCHAR(512) NULL COMMENT ''Office 预览用 PDF 存储引用'' AFTER `thumbnail_url`'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(
        EXISTS(SELECT 1 FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'homeai_learn_material'
                 AND COLUMN_NAME = 'preview_pdf_url'),
        'SELECT 1',
        'ALTER TABLE `homeai_learn_material` ADD COLUMN `preview_pdf_url` VARCHAR(512) NULL COMMENT ''Office 预览用 PDF 存储引用'' AFTER `thumbnail_url`'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(
        (SELECT CHARACTER_MAXIMUM_LENGTH FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'homeai_office_convert_history' AND COLUMN_NAME = 'convert_type') >= 32,
        'SELECT 1',
        'ALTER TABLE `homeai_office_convert_history` MODIFY COLUMN `convert_type` VARCHAR(32) NOT NULL COMMENT ''转换类型:format_convert/ai_generate/preview_pdf/preview_pdf_learn'''
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
