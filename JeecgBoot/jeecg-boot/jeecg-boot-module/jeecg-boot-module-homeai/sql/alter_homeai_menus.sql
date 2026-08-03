-- =============================================================
-- 菜单结构调整 - 数据库迁移脚本
-- 在已执行过 init_homeai_menus.sql 的数据库上运行
-- =============================================================

-- 1. 新增 AI管理 父菜单
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `perms`, `menu_type`, `icon`, `sort_no`, `status`, `del_flag`, `create_time`)
VALUES ('homeai_menu_ai', 'homeai_menu_root', 'AI管理', '/homeai/ai', 'Layout', 1, NULL, 0, 'ant-design:robot-outlined', 3.0, 1, 0, NOW());

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

-- 4. 计划管理：从空壳父菜单改为直接页面
UPDATE `sys_permission`
SET `url` = '/homeai/plan/planList',
    `component` = 'views/homeai/plan/planList',
    `perms` = 'homeai:plan:list'
WHERE `id` = 'homeai_menu_plan';

-- 5. 删除被合并的"计划列表"子菜单
DELETE FROM `sys_permission` WHERE `id` = 'homeai_menu_plan_list';

-- 6. 菜谱列表：从菜谱管理子菜单提升为根菜单直接项
DELETE FROM `sys_permission` WHERE `id` = 'homeai_menu_recipe';

UPDATE `sys_permission`
SET `parent_id` = 'homeai_menu_root',
    `name` = '菜谱列表',
    `url` = '/homeai/recipe/recipeList',
    `sort_no` = 8.0
WHERE `id` = 'homeai_menu_recipe_list';

-- 7. 学习资料：从学习管理子菜单提升为根菜单直接项
DELETE FROM `sys_permission` WHERE `id` = 'homeai_menu_learn';

UPDATE `sys_permission`
SET `parent_id` = 'homeai_menu_root',
    `name` = '学习资料',
    `url` = '/homeai/learn/learnList',
    `sort_no` = 8.5
WHERE `id` = 'homeai_menu_learn_list';
