-- 学习资料补充 tags 标签列（管理端「标签」字段）
ALTER TABLE `homeai_learn_material`
    ADD COLUMN `tags` VARCHAR(500) NULL COMMENT '标签(逗号分隔)' AFTER `category`;
