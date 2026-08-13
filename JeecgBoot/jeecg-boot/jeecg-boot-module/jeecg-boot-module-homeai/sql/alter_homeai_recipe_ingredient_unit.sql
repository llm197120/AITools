-- 菜谱食材单位加宽：openpyxl 导入的菜谱数据中，单位列存在最多 43 字符的描述文本，
-- 原 VARCHAR(20) 会触发 Data truncation: Data too long for column 'unit'，导致导入失败。
ALTER TABLE `homeai_recipe_ingredient`
    MODIFY COLUMN `unit` VARCHAR(100) NULL COMMENT '单位:克/毫升/个/根/块/勺等';
