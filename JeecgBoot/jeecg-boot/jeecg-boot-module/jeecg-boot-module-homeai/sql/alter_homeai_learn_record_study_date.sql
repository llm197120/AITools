-- 第 48 轮：学习记录 study_date 与实体对齐（幂等）
-- 旧库该列 NOT NULL 且无默认值，结束计时 insert 会失败；新库 init 已含可空列。

DROP PROCEDURE IF EXISTS homeai_fix_learn_study_date;
DELIMITER $$
CREATE PROCEDURE homeai_fix_learn_study_date()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'homeai_learn_record'
          AND COLUMN_NAME = 'study_date'
    ) THEN
        ALTER TABLE `homeai_learn_record`
            ADD COLUMN `study_date` DATE NULL COMMENT '学习日期' AFTER `material_id`;
    ELSE
        ALTER TABLE `homeai_learn_record`
            MODIFY COLUMN `study_date` DATE NULL COMMENT '学习日期';
    END IF;

    UPDATE `homeai_learn_record`
       SET `study_date` = DATE(IFNULL(`create_time`, NOW()))
     WHERE `study_date` IS NULL;
END$$
DELIMITER ;

CALL homeai_fix_learn_study_date();
DROP PROCEDURE IF EXISTS homeai_fix_learn_study_date;
