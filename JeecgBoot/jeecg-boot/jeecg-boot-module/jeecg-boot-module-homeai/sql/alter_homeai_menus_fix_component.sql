-- =============================================================
-- 修复子菜单 component 路径（补 /views/ 前缀）
--
-- 原因：Vue3 版前端 dynamicImport 函数在解析后端 component 字段时，
--       会将 /views/ 前缀去掉后匹配 glob 中的组件路径。
--       如果 component 写的是 views/homeai/user/index（无前导斜杠），
--       replace(/^\/views/, '') 无法匹配，导致组件加载失败。
--
--       正确格式：/views/homeai/user/index
--
-- 在已执行过 init_homeai_menus.sql 的数据库上运行
-- =============================================================

-- 用户管理子菜单（不含 Layout 类型的父菜单）
UPDATE `sys_permission`
SET `component` = '/views/homeai/user/index'
WHERE `id` = 'homeai_menu_user'
  AND `component` = 'views/homeai/user/index';

-- 家庭管理子菜单
UPDATE `sys_permission`
SET `component` = '/views/homeai/family/index'
WHERE `id` = 'homeai_menu_family'
  AND `component` = 'views/homeai/family/index';

-- AI密钥配置
UPDATE `sys_permission`
SET `component` = '/views/homeai/ai/keyConfig'
WHERE `id` = 'homeai_menu_ai_keys'
  AND `component` = 'views/homeai/ai/keyConfig';

-- Token额度配置
UPDATE `sys_permission`
SET `component` = '/views/homeai/ai/quota'
WHERE `id` = 'homeai_menu_quota'
  AND `component` = 'views/homeai/ai/quota';

-- 资料存储管理子菜单
UPDATE `sys_permission`
SET `component` = '/views/homeai/storage/fileList'
WHERE `id` = 'homeai_menu_storage_files'
  AND `component` = 'views/homeai/storage/fileList';

UPDATE `sys_permission`
SET `component` = '/views/homeai/storage/officeTemplate'
WHERE `id` = 'homeai_menu_storage_template'
  AND `component` = 'views/homeai/storage/officeTemplate';

UPDATE `sys_permission`
SET `component` = '/views/homeai/storage/convertRule'
WHERE `id` = 'homeai_menu_storage_rule'
  AND `component` = 'views/homeai/storage/convertRule';

UPDATE `sys_permission`
SET `component` = '/views/homeai/storage/officeHistory'
WHERE `id` = 'homeai_menu_storage_history'
  AND `component` = 'views/homeai/storage/officeHistory';

-- 账单管理子菜单
UPDATE `sys_permission`
SET `component` = '/views/homeai/bill/billList'
WHERE `id` = 'homeai_menu_bill_list'
  AND `component` = 'views/homeai/bill/billList';

UPDATE `sys_permission`
SET `component` = '/views/homeai/bill/billCategory'
WHERE `id` = 'homeai_menu_bill_category'
  AND `component` = 'views/homeai/bill/billCategory';

-- 计划管理
UPDATE `sys_permission`
SET `component` = '/views/homeai/plan/planList'
WHERE `id` = 'homeai_menu_plan'
  AND `component` = 'views/homeai/plan/planList';

-- 菜谱列表
UPDATE `sys_permission`
SET `component` = '/views/homeai/recipe/recipeList'
WHERE `id` = 'homeai_menu_recipe_list'
  AND `component` = 'views/homeai/recipe/recipeList';

-- 学习资料
UPDATE `sys_permission`
SET `component` = '/views/homeai/learn/learnList'
WHERE `id` = 'homeai_menu_learn_list'
  AND `component` = 'views/homeai/learn/learnList';

-- 验证（可选）
-- SELECT id, name, component FROM sys_permission WHERE del_flag = 0 ORDER BY parent_id, sort_no;
