-- 管理端/导入新增账单时可能不传 category_id，而该列 NOT NULL 无默认值导致插入失败。
-- 改为可空，未分类账单由后端代码自动填充默认分类。

ALTER TABLE `homeai_bill_entry`
    MODIFY COLUMN `category_id` VARCHAR(32) NULL COMMENT '分类ID（NULL=未分类）';
