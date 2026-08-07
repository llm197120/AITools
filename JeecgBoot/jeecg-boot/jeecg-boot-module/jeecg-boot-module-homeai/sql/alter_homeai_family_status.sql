-- 家庭表补充状态字段：normal-正常 / disbanded-已解散（解散保留数据，删除进回收站）
-- 幂等：新库 init_homeai_tables 已含该字段则跳过
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'homeai_family'
      AND COLUMN_NAME = 'status'
);
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `homeai_family` ADD COLUMN `status` VARCHAR(20) DEFAULT ''normal'' COMMENT ''状态: normal-正常 disbanded-已解散'' AFTER `member_count`',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
