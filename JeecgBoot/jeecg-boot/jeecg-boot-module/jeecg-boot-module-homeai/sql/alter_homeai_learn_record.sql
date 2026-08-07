-- 第 12 轮：学习记录表与实体字段对齐（幂等）

SET @has_start := (

    SELECT COUNT(*) FROM information_schema.COLUMNS

    WHERE TABLE_SCHEMA = DATABASE()

      AND TABLE_NAME = 'homeai_learn_record'

      AND COLUMN_NAME = 'start_time'

);

SET @ddl := IF(@has_start = 0,

    'ALTER TABLE `homeai_learn_record`

        ADD COLUMN `start_time` DATETIME NULL COMMENT ''开始时间'' AFTER `material_id`,

        ADD COLUMN `end_time`   DATETIME NULL COMMENT ''结束时间'' AFTER `start_time`,

        ADD COLUMN `duration`   INT          DEFAULT 0 COMMENT ''学习时长(秒)'' AFTER `end_time`,

        ADD COLUMN `notes`      TEXT                  COMMENT ''学习笔记'' AFTER `duration`',

    'SELECT 1');

PREPARE stmt FROM @ddl;

EXECUTE stmt;

DEALLOCATE PREPARE stmt;



-- 历史分钟字段迁移为秒（仅旧库存在 duration_minutes 时执行）

SET @has_minutes := (

    SELECT COUNT(*) FROM information_schema.COLUMNS

    WHERE TABLE_SCHEMA = DATABASE()

      AND TABLE_NAME = 'homeai_learn_record'

      AND COLUMN_NAME = 'duration_minutes'

);

SET @migrate := IF(@has_minutes > 0,

    'UPDATE `homeai_learn_record`

     SET `duration` = IFNULL(`duration_minutes`, 0) * 60

     WHERE (`duration` IS NULL OR `duration` = 0)

       AND `duration_minutes` IS NOT NULL AND `duration_minutes` > 0',

    'SELECT 1');

PREPARE stmt2 FROM @migrate;

EXECUTE stmt2;

DEALLOCATE PREPARE stmt2;



SET @has_note := (

    SELECT COUNT(*) FROM information_schema.COLUMNS

    WHERE TABLE_SCHEMA = DATABASE()

      AND TABLE_NAME = 'homeai_learn_record'

      AND COLUMN_NAME = 'note'

);

SET @note_migrate := IF(@has_note > 0,
    'UPDATE `homeai_learn_record`
     SET `notes` = `note`
     WHERE (`notes` IS NULL OR `notes` = '''')
       AND `note` IS NOT NULL AND `note` != ''''''',
    'SELECT 1');

PREPARE stmt3 FROM @note_migrate;

EXECUTE stmt3;

DEALLOCATE PREPARE stmt3;

