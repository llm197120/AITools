-- 为 homeai_bill_category 表添加缺失的 version 和 del_flag 列
-- BillCategory.java 使用了 @Version 和 @TableLogic 注解，但数据库表缺少对应列

ALTER TABLE `homeai_bill_category`
    ADD COLUMN `version` INT DEFAULT 0 COMMENT '乐观锁版本号' AFTER `is_enabled`,
    ADD COLUMN `del_flag` TINYINT DEFAULT 0 COMMENT '删除状态(0-正常,1-已删除)' AFTER `update_time`;
