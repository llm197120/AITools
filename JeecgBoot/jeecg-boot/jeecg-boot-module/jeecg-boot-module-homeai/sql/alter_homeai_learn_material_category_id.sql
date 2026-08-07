-- 第 12 轮：学习资料分类 ID 外键化
ALTER TABLE `homeai_learn_material`
    ADD COLUMN `category_id` VARCHAR(32) NULL COMMENT '分类ID' AFTER `category`;

UPDATE `homeai_learn_material` m
    INNER JOIN `homeai_learn_category` c ON m.`category` = c.`name` AND c.`del_flag` = 0
SET m.`category_id` = c.`id`
WHERE m.`category_id` IS NULL AND m.`del_flag` = 0;
