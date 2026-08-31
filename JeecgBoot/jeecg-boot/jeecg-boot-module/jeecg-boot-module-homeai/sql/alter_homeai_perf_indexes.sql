-- 第 118 轮：账单按月汇总 / 学习日历与记录查询复合索引（幂等）
-- -*- coding: utf-8 -*-

DROP PROCEDURE IF EXISTS homeai_add_perf_indexes;
DELIMITER //
CREATE PROCEDURE homeai_add_perf_indexes()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'homeai_bill_entry'
          AND index_name = 'idx_bill_user_del_date'
    ) THEN
        ALTER TABLE `homeai_bill_entry`
            ADD INDEX `idx_bill_user_del_date` (`user_id`, `del_flag`, `bill_date`);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'homeai_learn_record'
          AND index_name = 'idx_learn_user_study_date'
    ) THEN
        ALTER TABLE `homeai_learn_record`
            ADD INDEX `idx_learn_user_study_date` (`user_id`, `study_date`);
    END IF;
END //
DELIMITER ;
CALL homeai_add_perf_indexes();
DROP PROCEDURE IF EXISTS homeai_add_perf_indexes;
