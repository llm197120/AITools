-- 为 homeai_plan_master 表补齐缺失列，使表结构与 PlanMaster.java 实体一致
-- PlanMaster 使用 @Version 乐观锁，且包含 planDate/remindMinutes/repeatRule 字段，
-- 但数据库表缺少对应列，导致 INSERT 报 Unknown column 'plan_date'

ALTER TABLE `homeai_plan_master`
    ADD COLUMN `plan_date`      DATE         NULL COMMENT '计划日期' AFTER `content`,
    ADD COLUMN `remind_minutes` INT          DEFAULT 0 COMMENT '提前提醒分钟数:0=不提醒' AFTER `is_all_day`,
    ADD COLUMN `repeat_rule`    VARCHAR(200) NULL COMMENT '重复规则' AFTER `is_repeat_master`,
    ADD COLUMN `version`        INT          DEFAULT 0 COMMENT '乐观锁版本号' AFTER `repeat_rule`;
