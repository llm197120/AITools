-- =============================================================

-- 【旧库专用】菜单结构调整 v1 迁移脚本

-- 适用：早期 init_homeai_menus 产生的中间结构

-- 新库请勿执行：init-db 已跳过 sql/legacy/ 目录；当前 init_homeai_menus.sql 已是最终结构

-- 手动恢复旧库时：SOURCE legacy/alter_homeai_menus_v1.sql

-- =============================================================



-- 1. 新增 AI管理 父菜单

INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `perms`, `menu_type`, `icon`, `sort_no`, `status`, `del_flag`, `create_time`)

SELECT 'homeai_menu_ai', 'homeai_menu_root', 'AI管理', '/homeai/ai', 'Layout', 1, NULL, 0, 'ant-design:robot-outlined', 3.0, 1, 0, NOW()

FROM DUAL

WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `id` = 'homeai_menu_ai');



-- 2. AI密钥配置：移到 AI管理 下，修复 component 路径

UPDATE `sys_permission`

SET `parent_id` = 'homeai_menu_ai',

    `component` = 'views/homeai/ai/keyConfig',

    `sort_no` = 1.0,

    `status` = 1

WHERE `id` = 'homeai_menu_ai_keys';



-- 3. Token额度配置：移到 AI管理 下，修复 component 路径

UPDATE `sys_permission`

SET `parent_id` = 'homeai_menu_ai',

    `component` = 'views/homeai/ai/quota',

    `sort_no` = 2.0,

    `status` = 1

WHERE `id` = 'homeai_menu_quota';



-- 4～7：仅当计划菜单仍为旧版单页结构时执行（非 Layout 父菜单）

SET @legacy_plan := (

    SELECT COUNT(*) FROM `sys_permission`

    WHERE `id` = 'homeai_menu_plan'

      AND `component` NOT IN ('Layout', 'layouts/RouteView')

);



UPDATE `sys_permission`

SET `url` = '/homeai/plan/planList',

    `component` = 'views/homeai/plan/planList',

    `perms` = 'homeai:plan:list'

WHERE `id` = 'homeai_menu_plan' AND @legacy_plan > 0;



DELETE FROM `sys_permission` WHERE `id` = 'homeai_menu_plan_list' AND @legacy_plan > 0;



DELETE FROM `sys_permission` WHERE `id` = 'homeai_menu_recipe' AND @legacy_plan > 0;



UPDATE `sys_permission`

SET `parent_id` = 'homeai_menu_root',

    `name` = '菜谱列表',

    `url` = '/homeai/recipe/recipeList',

    `sort_no` = 8.0

WHERE `id` = 'homeai_menu_recipe_list' AND @legacy_plan > 0;



DELETE FROM `sys_permission` WHERE `id` = 'homeai_menu_learn' AND @legacy_plan > 0;



UPDATE `sys_permission`

SET `parent_id` = 'homeai_menu_root',

    `name` = '学习资料',

    `url` = '/homeai/learn/learnList',

    `sort_no` = 8.5

WHERE `id` = 'homeai_menu_learn_list' AND @legacy_plan > 0;


