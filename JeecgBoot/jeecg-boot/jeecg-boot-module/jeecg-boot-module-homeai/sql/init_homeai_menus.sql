-- =============================================================
-- 家庭AI小工具 - 管理端菜单权限初始化 SQL
-- 运行此脚本前请先执行 init_homeai_tables.sql
-- =============================================================

-- 注意：以下 ID 为固定 UUID，实际执行时应使用系统中的 UUID 生成方式
-- 菜单层级：家庭AI小工具 > 用户管理 / 家庭管理
-- 权限编码格式：homeai:module:operation

-- =============================================================
-- 1. 一级菜单：家庭AI小工具
-- =============================================================
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `perms`, `menu_type`, `icon`, `sort_no`, `status`, `del_flag`, `create_time`)
VALUES ('homeai_menu_root', NULL, '家庭AI小工具', '/homeai', 'Layout', 1, NULL, 0, 'ant-design:home-outlined', 30.0, 1, 0, NOW());

-- =============================================================
-- 2. 二级菜单：用户管理
-- =============================================================
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `perms`, `menu_type`, `icon`, `sort_no`, `status`, `del_flag`, `create_time`)
VALUES ('homeai_menu_user', 'homeai_menu_root', '用户管理', '/homeai/user', '/views/homeai/user/index', 1, 'homeai:user:list', 1, 'ant-design:user-outlined', 1.0, 1, 0, NOW());

-- 用户管理 - 按钮权限
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `perms`, `menu_type`, `sort_no`, `status`, `del_flag`, `create_time`)
VALUES
('homeai_btn_user_view', 'homeai_menu_user', '查看', NULL, NULL, 0, 'homeai:user:getById', 2, 1.0, 1, 0, NOW()),
('homeai_btn_user_edit', 'homeai_menu_user', '编辑', NULL, NULL, 0, 'homeai:user:edit', 2, 2.0, 1, 0, NOW()),
('homeai_btn_user_delete', 'homeai_menu_user', '注销', NULL, NULL, 0, 'homeai:user:delete', 2, 3.0, 1, 0, NOW()),
('homeai_btn_user_status', 'homeai_menu_user', '启用/禁用', NULL, NULL, 0, 'homeai:user:updateStatus', 2, 4.0, 1, 0, NOW());

-- =============================================================
-- 3. 二级菜单：家庭管理
-- =============================================================
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `perms`, `menu_type`, `icon`, `sort_no`, `status`, `del_flag`, `create_time`)
VALUES ('homeai_menu_family', 'homeai_menu_root', '家庭管理', '/homeai/family', '/views/homeai/family/index', 1, 'homeai:family:list', 1, 'ant-design:team-outlined', 2.0, 1, 0, NOW());

-- 家庭管理 - 按钮权限
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `perms`, `menu_type`, `sort_no`, `status`, `del_flag`, `create_time`)
VALUES
('homeai_btn_family_view', 'homeai_menu_family', '查看', NULL, NULL, 0, 'homeai:family:getById', 2, 1.0, 1, 0, NOW()),
('homeai_btn_family_disband', 'homeai_menu_family', '解散', NULL, NULL, 0, 'homeai:family:disband', 2, 2.0, 1, 0, NOW());

-- =============================================================
-- 4. AI 管理父菜单
-- =============================================================
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `perms`, `menu_type`, `icon`, `sort_no`, `status`, `del_flag`, `create_time`)
VALUES ('homeai_menu_ai', 'homeai_menu_root', 'AI管理', '/homeai/ai', 'Layout', 1, NULL, 0, 'ant-design:robot-outlined', 3.0, 1, 0, NOW());

-- AI密钥配置（子菜单）
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `perms`, `menu_type`, `icon`, `sort_no`, `status`, `del_flag`, `create_time`)
VALUES ('homeai_menu_ai_keys', 'homeai_menu_ai', 'AI密钥配置', '/homeai/ai/keyConfig', '/views/homeai/ai/keyConfig', 1, 'homeai:config:key:list', 1, 'ant-design:key-outlined', 1.0, 1, 0, NOW());

-- Token额度配置（子菜单）
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `perms`, `menu_type`, `icon`, `sort_no`, `status`, `del_flag`, `create_time`)
VALUES ('homeai_menu_quota', 'homeai_menu_ai', 'Token额度配置', '/homeai/ai/quota', '/views/homeai/ai/quota', 1, 'homeai:quota:list', 1, 'ant-design:dashboard-outlined', 2.0, 1, 0, NOW());

-- AI密钥配置 - 按钮权限
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `perms`, `menu_type`, `sort_no`, `status`, `del_flag`, `create_time`)
VALUES
('homeai_btn_key_add',    'homeai_menu_ai_keys', '新增',       NULL, NULL, 0, 'homeai:config:key:add',         2, 1.0, 1, 0, NOW()),
('homeai_btn_key_edit',   'homeai_menu_ai_keys', '编辑',       NULL, NULL, 0, 'homeai:config:key:edit',        2, 2.0, 1, 0, NOW()),
('homeai_btn_key_delete', 'homeai_menu_ai_keys', '删除',       NULL, NULL, 0, 'homeai:config:key:delete',      2, 3.0, 1, 0, NOW()),
('homeai_btn_key_default','homeai_menu_ai_keys', '设为默认',    NULL, NULL, 0, 'homeai:config:key:setDefault',  2, 4.0, 1, 0, NOW()),
('homeai_btn_key_status', 'homeai_menu_ai_keys', '启用/停用',   NULL, NULL, 0, 'homeai:config:key:toggleStatus',2, 5.0, 1, 0, NOW());

-- =============================================================
-- 5. 资料存储管理（Phase 4）
-- =============================================================
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `perms`, `menu_type`, `icon`, `sort_no`, `status`, `del_flag`, `create_time`)
VALUES
('homeai_menu_storage',         'homeai_menu_root',    '资料存储管理',  '/homeai/storage',                'Layout',                  1, NULL,                            0, 'ant-design:folder-outlined',      5.0, 1, 0, NOW()),
('homeai_menu_storage_files',    'homeai_menu_storage', '文件管理',     '/homeai/storage/fileList',       '/views/homeai/storage/fileList',        1, 'homeai:storage:file:list',       1, 'ant-design:file-outlined',        1.0, 1, 0, NOW()),
('homeai_menu_storage_template', 'homeai_menu_storage', '文档模板',     '/homeai/storage/officeTemplate',  '/views/homeai/storage/officeTemplate',  1, 'homeai:storage:template:list',   1, 'ant-design:file-word-outlined',   2.0, 1, 0, NOW()),
('homeai_menu_storage_rule',     'homeai_menu_storage', '转换规则',     '/homeai/storage/convertRule',     '/views/homeai/storage/convertRule',     1, 'homeai:storage:rule:list',       1, 'ant-design:swap-outlined',        3.0, 1, 0, NOW()),
('homeai_menu_storage_history',  'homeai_menu_storage', '处理记录',     '/homeai/storage/officeHistory',   '/views/homeai/storage/officeHistory',   1, 'homeai:storage:history:list',    1, 'ant-design:history-outlined',     4.0, 1, 0, NOW());

-- =============================================================
-- 6. 账单管理（Phase 5）
-- =============================================================
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `perms`, `menu_type`, `icon`, `sort_no`, `status`, `del_flag`, `create_time`)
VALUES
('homeai_menu_bill',         'homeai_menu_root', '账单管理', '/homeai/bill',              'Layout',                1, NULL,                          0, 'ant-design:account-book-outlined',   6.0, 1, 0, NOW()),
('homeai_menu_bill_list',     'homeai_menu_bill', '账单列表', '/homeai/bill/billList',      '/views/homeai/bill/billList',        1, 'homeai:bill:list',             1, 'ant-design:unordered-list-outlined', 1.0, 1, 0, NOW()),
('homeai_menu_bill_category', 'homeai_menu_bill', '消费分类', '/homeai/bill/billCategory',  '/views/homeai/bill/billCategory',    1, 'homeai:bill:category:list',    1, 'ant-design:tags-outlined',           2.0, 1, 0, NOW());

-- =============================================================
-- 7. 计划管理（Phase 6）- 直接菜单，不再嵌套子菜单
-- =============================================================
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `perms`, `menu_type`, `icon`, `sort_no`, `status`, `del_flag`, `create_time`)
VALUES ('homeai_menu_plan', 'homeai_menu_root', '计划管理', '/homeai/plan/planList', '/views/homeai/plan/planList', 1, 'homeai:plan:list', 1, 'ant-design:calendar-outlined', 7.0, 1, 0, NOW());

-- =============================================================
-- 8. 菜谱列表（Phase 7）- 直接菜单，不再嵌套子菜单
-- =============================================================
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `perms`, `menu_type`, `icon`, `sort_no`, `status`, `del_flag`, `create_time`)
VALUES ('homeai_menu_recipe_list', 'homeai_menu_root', '菜谱列表', '/homeai/recipe/recipeList', '/views/homeai/recipe/recipeList', 1, 'homeai:recipe:list', 1, 'ant-design:coffee-outlined', 8.0, 1, 0, NOW());

-- =============================================================
-- 9. 学习资料（Phase 7）- 直接菜单，不再嵌套子菜单
-- =============================================================
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `perms`, `menu_type`, `icon`, `sort_no`, `status`, `del_flag`, `create_time`)
VALUES ('homeai_menu_learn_list', 'homeai_menu_root', '学习资料', '/homeai/learn/learnList', '/views/homeai/learn/learnList', 1, 'homeai:learn:material:list', 1, 'ant-design:book-outlined', 8.5, 1, 0, NOW());
