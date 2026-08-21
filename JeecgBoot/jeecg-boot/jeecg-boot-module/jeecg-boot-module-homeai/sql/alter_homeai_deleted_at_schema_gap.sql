-- 已有库补齐实体已映射但表上缺失的 deleted_at（CREATE IF NOT EXISTS 不会给旧表加列）
-- 可重复执行：列已存在则跳过

DROP PROCEDURE IF EXISTS homeai_add_col_if_missing;
DELIMITER $$
CREATE PROCEDURE homeai_add_col_if_missing(
    IN p_table VARCHAR(64),
    IN p_column VARCHAR(64),
    IN p_ddl TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = p_table
          AND COLUMN_NAME = p_column
    ) THEN
        SET @sql = p_ddl;
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL homeai_add_col_if_missing('homeai_storage_folder', 'deleted_at',
    'ALTER TABLE `homeai_storage_folder` ADD COLUMN `deleted_at` DATETIME NULL COMMENT ''删除时间'' AFTER `del_flag`');
CALL homeai_add_col_if_missing('homeai_storage_file', 'deleted_at',
    'ALTER TABLE `homeai_storage_file` ADD COLUMN `deleted_at` DATETIME NULL COMMENT ''删除时间'' AFTER `del_flag`');
CALL homeai_add_col_if_missing('homeai_family', 'deleted_at',
    'ALTER TABLE `homeai_family` ADD COLUMN `deleted_at` DATETIME NULL COMMENT ''解散时间'' AFTER `del_flag`');
CALL homeai_add_col_if_missing('homeai_ai_conversation', 'deleted_at',
    'ALTER TABLE `homeai_ai_conversation` ADD COLUMN `deleted_at` DATETIME NULL COMMENT ''删除时间'' AFTER `del_flag`');
CALL homeai_add_col_if_missing('homeai_bill_entry', 'deleted_at',
    'ALTER TABLE `homeai_bill_entry` ADD COLUMN `deleted_at` DATETIME NULL COMMENT ''删除时间'' AFTER `del_flag`');
CALL homeai_add_col_if_missing('homeai_recipe', 'deleted_at',
    'ALTER TABLE `homeai_recipe` ADD COLUMN `deleted_at` DATETIME NULL COMMENT ''删除时间'' AFTER `del_flag`');

DROP PROCEDURE IF EXISTS homeai_add_col_if_missing;
