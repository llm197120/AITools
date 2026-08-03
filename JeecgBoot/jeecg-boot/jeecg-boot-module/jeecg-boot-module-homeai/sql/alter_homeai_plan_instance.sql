-- 为 homeai_plan_instance 表补齐缺失列，使表结构与 PlanInstance.java 实体一致
-- PlanInstance 实体没有 user_id / title 字段，createInstance() 插入时不会携带这两列，
-- 表中 NOT NULL 且无默认值会报 "Field 'user_id' doesn't have a default value"，改为可空。
-- PlanInstance 使用 @Version 乐观锁，补 version 列。

ALTER TABLE `homeai_plan_instance`
    MODIFY COLUMN `user_id` VARCHAR(32) NULL COMMENT '用户ID（NULL=管理端录入）',
    MODIFY COLUMN `title`   VARCHAR(100) NULL COMMENT '计划标题（NULL=由主计划展开）',
    ADD COLUMN `version`    INT DEFAULT 0 COMMENT '乐观锁版本号' AFTER `reminded`;
