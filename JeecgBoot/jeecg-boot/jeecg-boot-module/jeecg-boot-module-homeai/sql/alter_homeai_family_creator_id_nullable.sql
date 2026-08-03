-- 将 homeai_family 表的 creator_id 字段改为可空
-- 管理端管理员创建家庭时 creator_id 允许为空
ALTER TABLE `homeai_family`
    MODIFY COLUMN `creator_id` VARCHAR(32) NULL COMMENT '创建者用户ID（APP端填写，管理端可为空）';
