-- Office 转换任务表增加 AI 生成指令字段（幂等）

SET @col_exists := (

    SELECT COUNT(*) FROM information_schema.COLUMNS

    WHERE TABLE_SCHEMA = DATABASE()

      AND TABLE_NAME = 'homeai_office_convert_history'

      AND COLUMN_NAME = 'instruction'

);

SET @ddl := IF(@col_exists = 0,

    'ALTER TABLE `homeai_office_convert_history` ADD COLUMN `instruction` VARCHAR(1000) NULL COMMENT ''AI生成指令'' AFTER `target_format`',

    'SELECT 1');

PREPARE stmt FROM @ddl;

EXECUTE stmt;

DEALLOCATE PREPARE stmt;

