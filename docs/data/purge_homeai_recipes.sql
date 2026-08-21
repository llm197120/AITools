-- =============================================================================
-- 清空现有菜谱业务数据（物理删除），便于重新 Excel 导入
-- =============================================================================
-- 删除：homeai_recipe / ingredient / step / favorite
-- 断开：homeai_plan_master.recipe_id（计划本身保留）
-- 保留：homeai_recipe_category（导入依赖默认分类）
--
-- 执行前请确认已备份。建议用 docs/data/purge_homeai_recipes.ps1 带确认后执行。
-- 管理端 / Navicat 也可直接对本库执行本文件。
-- =============================================================================

-- 执行前数量（便于核对）
SELECT 'homeai_recipe' AS tbl, COUNT(*) AS cnt FROM `homeai_recipe`
UNION ALL SELECT 'homeai_recipe_ingredient', COUNT(*) FROM `homeai_recipe_ingredient`
UNION ALL SELECT 'homeai_recipe_step', COUNT(*) FROM `homeai_recipe_step`
UNION ALL SELECT 'homeai_recipe_favorite', COUNT(*) FROM `homeai_recipe_favorite`
UNION ALL SELECT 'plan_linked_recipe', COUNT(*) FROM `homeai_plan_master`
         WHERE `recipe_id` IS NOT NULL AND `recipe_id` <> ''
UNION ALL SELECT 'homeai_recipe_category(保留)', COUNT(*) FROM `homeai_recipe_category`;

START TRANSACTION;

-- 计划只清空菜谱外键，不删计划
UPDATE `homeai_plan_master`
   SET `recipe_id` = NULL
 WHERE `recipe_id` IS NOT NULL AND `recipe_id` <> '';

DELETE FROM `homeai_recipe_favorite`;
DELETE FROM `homeai_recipe_ingredient`;
DELETE FROM `homeai_recipe_step`;
DELETE FROM `homeai_recipe`;

COMMIT;

-- 执行后数量（菜谱相关应为 0；分类应仍有数据）
SELECT 'homeai_recipe' AS tbl, COUNT(*) AS cnt FROM `homeai_recipe`
UNION ALL SELECT 'homeai_recipe_ingredient', COUNT(*) FROM `homeai_recipe_ingredient`
UNION ALL SELECT 'homeai_recipe_step', COUNT(*) FROM `homeai_recipe_step`
UNION ALL SELECT 'homeai_recipe_favorite', COUNT(*) FROM `homeai_recipe_favorite`
UNION ALL SELECT 'plan_linked_recipe', COUNT(*) FROM `homeai_plan_master`
         WHERE `recipe_id` IS NOT NULL AND `recipe_id` <> ''
UNION ALL SELECT 'homeai_recipe_category(保留)', COUNT(*) FROM `homeai_recipe_category`;

-- -----------------------------------------------------------------------------
-- 可选：只删管理端导入的公开菜谱，保留用户自建（user_id 非空）
-- 若需要，注释掉上面 START TRANSACTION ～ COMMIT，改跑下面这段。
-- -----------------------------------------------------------------------------
-- START TRANSACTION;
-- UPDATE `homeai_plan_master` pm
--    SET pm.`recipe_id` = NULL
--  WHERE pm.`recipe_id` IN (
--        SELECT r.`id` FROM `homeai_recipe` r
--         WHERE r.`user_id` IS NULL OR r.`user_id` = '');
-- DELETE fi FROM `homeai_recipe_favorite` fi
--  INNER JOIN `homeai_recipe` r ON r.`id` = fi.`recipe_id`
--  WHERE r.`user_id` IS NULL OR r.`user_id` = '';
-- DELETE i FROM `homeai_recipe_ingredient` i
--  INNER JOIN `homeai_recipe` r ON r.`id` = i.`recipe_id`
--  WHERE r.`user_id` IS NULL OR r.`user_id` = '';
-- DELETE s FROM `homeai_recipe_step` s
--  INNER JOIN `homeai_recipe` r ON r.`id` = s.`recipe_id`
--  WHERE r.`user_id` IS NULL OR r.`user_id` = '';
-- DELETE FROM `homeai_recipe` WHERE `user_id` IS NULL OR `user_id` = '';
-- COMMIT;
