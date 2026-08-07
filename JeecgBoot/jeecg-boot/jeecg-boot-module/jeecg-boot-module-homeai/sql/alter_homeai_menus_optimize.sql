-- 管理端菜单结构优化（与 init_homeai_menus.sql / 前端路由对齐）
-- 适用于已初始化过的数据库，可重复执行（幂等）

-- AI 对话管理菜单
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `perms`, `menu_type`, `icon`, `sort_no`, `status`, `del_flag`, `create_time`)
SELECT 'homeai_menu_ai_conversation', 'homeai_menu_ai', 'AI对话管理', '/homeai/ai/conversationList', '/views/homeai/ai/conversationList', 1, 'homeai:ai:conversation:list', 1, 'ant-design:message-outlined', 0.5, 1, 0, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `id` = 'homeai_menu_ai_conversation');

-- 账单：统计报表、导入
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `perms`, `menu_type`, `icon`, `sort_no`, `status`, `del_flag`, `create_time`)
SELECT 'homeai_menu_bill_statistics','homeai_menu_bill', '统计报表', '/homeai/bill/billStatistics','/views/homeai/bill/billStatistics', 1, 'homeai:bill:statistics:list', 1, 'ant-design:bar-chart-outlined', 3.0, 1, 0, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `id` = 'homeai_menu_bill_statistics');

INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `perms`, `menu_type`, `icon`, `sort_no`, `status`, `del_flag`, `create_time`)
SELECT 'homeai_menu_bill_import', 'homeai_menu_bill', '账单导入', '/homeai/bill/billImport', '/views/homeai/bill/billImport', 1, 'homeai:bill:import:list', 1, 'ant-design:import-outlined', 4.0, 1, 0, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `id` = 'homeai_menu_bill_import');

-- 计划：调整为 Layout 父菜单 + 子菜单
UPDATE `sys_permission` SET `parent_id` = 'homeai_menu_plan', `url` = '/homeai/plan/planList', `component` = '/views/homeai/plan/planList', `menu_type` = 1, `sort_no` = 1.0
WHERE `id` = 'homeai_menu_plan' AND `url` = '/homeai/plan/planList';

INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `perms`, `menu_type`, `icon`, `sort_no`, `status`, `del_flag`, `create_time`)
SELECT 'homeai_menu_plan', 'homeai_menu_root', '计划管理', '/homeai/plan', 'Layout', 1, NULL, 0, 'ant-design:calendar-outlined', 7.0, 1, 0, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `id` = 'homeai_menu_plan' AND `menu_type` = 0);

INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `perms`, `menu_type`, `icon`, `sort_no`, `status`, `del_flag`, `create_time`)
SELECT 'homeai_menu_plan_list', 'homeai_menu_plan', '计划列表', '/homeai/plan/planList', '/views/homeai/plan/planList', 1, 'homeai:plan:list', 1, 'ant-design:unordered-list-outlined', 1.0, 1, 0, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `id` = 'homeai_menu_plan_list');

UPDATE `sys_permission` SET `parent_id` = 'homeai_menu_plan', `url` = '/homeai/plan/planCategory', `sort_no` = 2.0
WHERE `id` = 'homeai_menu_plan_category';

-- 菜谱：Layout 父菜单
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `perms`, `menu_type`, `icon`, `sort_no`, `status`, `del_flag`, `create_time`)
SELECT 'homeai_menu_recipe', 'homeai_menu_root', '菜谱管理', '/homeai/recipe', 'Layout', 1, NULL, 0, 'ant-design:coffee-outlined', 8.0, 1, 0, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `id` = 'homeai_menu_recipe');

UPDATE `sys_permission` SET `parent_id` = 'homeai_menu_recipe', `url` = '/homeai/recipe/recipeList', `name` = '菜谱列表', `sort_no` = 1.0, `menu_type` = 1
WHERE `id` = 'homeai_menu_recipe_list';

UPDATE `sys_permission` SET `parent_id` = 'homeai_menu_recipe', `url` = '/homeai/recipe/recipeCategory', `sort_no` = 2.0
WHERE `id` = 'homeai_menu_recipe_category';

-- 学习：Layout 父菜单 + 学习记录
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `perms`, `menu_type`, `icon`, `sort_no`, `status`, `del_flag`, `create_time`)
SELECT 'homeai_menu_learn', 'homeai_menu_root', '学习管理', '/homeai/learn', 'Layout', 1, NULL, 0, 'ant-design:book-outlined', 8.5, 1, 0, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `id` = 'homeai_menu_learn');

UPDATE `sys_permission` SET `parent_id` = 'homeai_menu_learn', `url` = '/homeai/learn/learnList', `name` = '学习资料', `sort_no` = 1.0, `menu_type` = 1
WHERE `id` = 'homeai_menu_learn_list';

INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `perms`, `menu_type`, `icon`, `sort_no`, `status`, `del_flag`, `create_time`)
SELECT 'homeai_menu_learn_record', 'homeai_menu_learn', '学习记录', '/homeai/learn/learnRecord', '/views/homeai/learn/learnRecord', 1, 'homeai:learn:material:list', 1, 'ant-design:history-outlined', 2.0, 1, 0, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `id` = 'homeai_menu_learn_record');
