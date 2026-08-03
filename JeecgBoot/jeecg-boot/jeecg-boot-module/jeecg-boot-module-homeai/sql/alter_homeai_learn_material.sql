-- 为 homeai_learn_material 表补齐缺失列，使表结构与 LearnMaterial.java 实体一致
-- LearnMaterial 包含 coverUrl/description/totalDuration 字段，
-- 但数据库表缺少对应列，导致 SELECT 报 Unknown column 'cover_url'

ALTER TABLE `homeai_learn_material`
    ADD COLUMN `cover_url`       VARCHAR(512) NULL COMMENT '封面图URL' AFTER `file_url`,
    ADD COLUMN `description`     TEXT         NULL COMMENT '资料描述' AFTER `category`,
    ADD COLUMN `total_duration`  INT          DEFAULT 0 COMMENT '总时长(分钟)' AFTER `description`;
