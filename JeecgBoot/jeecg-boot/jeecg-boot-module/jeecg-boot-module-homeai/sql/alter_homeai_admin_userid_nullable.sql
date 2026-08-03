-- =============================================================
-- 管理端录入适配：将管理端可操作表的 user_id 改为可空
-- 管理后台（sys_user）新增记录时不会有 wx_user_id，允许为空
-- =============================================================

-- 账单表
ALTER TABLE `homeai_bill_entry`
    MODIFY COLUMN `user_id` VARCHAR(32) NULL COMMENT '录入人（NULL=管理端录入）';

-- 计划主表
ALTER TABLE `homeai_plan_master`
    MODIFY COLUMN `user_id` VARCHAR(32) NULL COMMENT '创建者用户ID（NULL=管理端录入）';

-- 菜谱表
ALTER TABLE `homeai_recipe`
    MODIFY COLUMN `user_id` VARCHAR(32) NULL COMMENT '创建者用户ID（NULL=管理端录入）';

-- 学习资料表
ALTER TABLE `homeai_learn_material`
    MODIFY COLUMN `user_id` VARCHAR(32) NULL COMMENT '上传者用户ID（NULL=管理端录入）';
