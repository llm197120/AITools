-- =============================================================
-- 按钮权限补充 - 新增/导入/导出/回收站/文件夹 迁移脚本
-- 在已执行过 init_homeai_menus.sql 的数据库上运行
-- =============================================================

-- =============================================================
-- 1. 用户管理 - 补充按钮权限
-- =============================================================
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `perms`, `menu_type`, `sort_no`, `status`, `del_flag`, `create_time`)
VALUES
('homeai_btn_user_add',     'homeai_menu_user', '新增',         NULL, NULL, 0, 'homeai:user:add',               2, 5.0, 1, 0, NOW()),
('homeai_btn_user_export',  'homeai_menu_user', '导出',         NULL, NULL, 0, 'homeai:user:exportXls',         2, 6.0, 1, 0, NOW()),
('homeai_btn_user_import',  'homeai_menu_user', '导入',         NULL, NULL, 0, 'homeai:user:importExcel',       2, 7.0, 1, 0, NOW()),
('homeai_btn_user_recycle', 'homeai_menu_user', '移入回收站',    NULL, NULL, 0, 'homeai:user:moveToRecycleBin',  2, 8.0, 1, 0, NOW()),
('homeai_btn_user_restore', 'homeai_menu_user', '恢复',         NULL, NULL, 0, 'homeai:user:restore',            2, 9.0, 1, 0, NOW()),
('homeai_btn_user_permDel', 'homeai_menu_user', '彻底删除',      NULL, NULL, 0, 'homeai:user:deletePermanently',  2, 10.0, 1, 0, NOW());

-- =============================================================
-- 2. 家庭管理 - 补充按钮权限
-- =============================================================
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `perms`, `menu_type`, `sort_no`, `status`, `del_flag`, `create_time`)
VALUES
('homeai_btn_family_add',     'homeai_menu_family', '新增',         NULL, NULL, 0, 'homeai:family:add',               2, 3.0, 1, 0, NOW()),
('homeai_btn_family_export',  'homeai_menu_family', '导出',         NULL, NULL, 0, 'homeai:family:exportXls',         2, 4.0, 1, 0, NOW()),
('homeai_btn_family_import',  'homeai_menu_family', '导入',         NULL, NULL, 0, 'homeai:family:importExcel',       2, 5.0, 1, 0, NOW()),
('homeai_btn_family_recycle', 'homeai_menu_family', '移入回收站',    NULL, NULL, 0, 'homeai:family:moveToRecycleBin',  2, 6.0, 1, 0, NOW()),
('homeai_btn_family_restore', 'homeai_menu_family', '恢复',         NULL, NULL, 0, 'homeai:family:restore',            2, 7.0, 1, 0, NOW()),
('homeai_btn_family_permDel', 'homeai_menu_family', '彻底删除',      NULL, NULL, 0, 'homeai:family:deletePermanently',  2, 8.0, 1, 0, NOW());

-- =============================================================
-- 3. 账单列表 - 新增按钮权限
-- =============================================================
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `perms`, `menu_type`, `sort_no`, `status`, `del_flag`, `create_time`)
VALUES
('homeai_btn_bill_add',     'homeai_menu_bill_list', '新增',         NULL, NULL, 0, 'homeai:bill:add',               2, 1.0, 1, 0, NOW()),
('homeai_btn_bill_export',  'homeai_menu_bill_list', '导出',         NULL, NULL, 0, 'homeai:bill:exportXls',         2, 2.0, 1, 0, NOW()),
('homeai_btn_bill_import',  'homeai_menu_bill_list', '导入',         NULL, NULL, 0, 'homeai:bill:importExcel',       2, 3.0, 1, 0, NOW()),
('homeai_btn_bill_recycle', 'homeai_menu_bill_list', '移入回收站',    NULL, NULL, 0, 'homeai:bill:moveToRecycleBin',  2, 4.0, 1, 0, NOW()),
('homeai_btn_bill_restore', 'homeai_menu_bill_list', '恢复',         NULL, NULL, 0, 'homeai:bill:restore',            2, 5.0, 1, 0, NOW()),
('homeai_btn_bill_permDel', 'homeai_menu_bill_list', '彻底删除',      NULL, NULL, 0, 'homeai:bill:deletePermanently',  2, 6.0, 1, 0, NOW());

-- =============================================================
-- 4. 计划管理 - 新增按钮权限
-- =============================================================
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `perms`, `menu_type`, `sort_no`, `status`, `del_flag`, `create_time`)
VALUES
('homeai_btn_plan_add',     'homeai_menu_plan', '新增',         NULL, NULL, 0, 'homeai:plan:add',               2, 1.0, 1, 0, NOW()),
('homeai_btn_plan_edit',    'homeai_menu_plan', '编辑',         NULL, NULL, 0, 'homeai:plan:edit',              2, 2.0, 1, 0, NOW()),
('homeai_btn_plan_export',  'homeai_menu_plan', '导出',         NULL, NULL, 0, 'homeai:plan:exportXls',         2, 3.0, 1, 0, NOW()),
('homeai_btn_plan_import',  'homeai_menu_plan', '导入',         NULL, NULL, 0, 'homeai:plan:importExcel',       2, 4.0, 1, 0, NOW()),
('homeai_btn_plan_recycle', 'homeai_menu_plan', '移入回收站',    NULL, NULL, 0, 'homeai:plan:moveToRecycleBin',  2, 5.0, 1, 0, NOW()),
('homeai_btn_plan_restore', 'homeai_menu_plan', '恢复',         NULL, NULL, 0, 'homeai:plan:restore',            2, 6.0, 1, 0, NOW()),
('homeai_btn_plan_permDel', 'homeai_menu_plan', '彻底删除',      NULL, NULL, 0, 'homeai:plan:deletePermanently',  2, 7.0, 1, 0, NOW());

-- =============================================================
-- 5. 菜谱列表 - 新增按钮权限
-- =============================================================
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `perms`, `menu_type`, `sort_no`, `status`, `del_flag`, `create_time`)
VALUES
('homeai_btn_recipe_add',     'homeai_menu_recipe_list', '新增',         NULL, NULL, 0, 'homeai:recipe:add',               2, 1.0, 1, 0, NOW()),
('homeai_btn_recipe_export',  'homeai_menu_recipe_list', '导出',         NULL, NULL, 0, 'homeai:recipe:exportXls',         2, 2.0, 1, 0, NOW()),
('homeai_btn_recipe_import',  'homeai_menu_recipe_list', '导入',         NULL, NULL, 0, 'homeai:recipe:importExcel',       2, 3.0, 1, 0, NOW()),
('homeai_btn_recipe_recycle', 'homeai_menu_recipe_list', '移入回收站',    NULL, NULL, 0, 'homeai:recipe:moveToRecycleBin',  2, 4.0, 1, 0, NOW()),
('homeai_btn_recipe_restore', 'homeai_menu_recipe_list', '恢复',         NULL, NULL, 0, 'homeai:recipe:restore',            2, 5.0, 1, 0, NOW()),
('homeai_btn_recipe_permDel', 'homeai_menu_recipe_list', '彻底删除',      NULL, NULL, 0, 'homeai:recipe:deletePermanently',  2, 6.0, 1, 0, NOW());

-- =============================================================
-- 6. 学习资料 - 新增按钮权限
-- =============================================================
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `perms`, `menu_type`, `sort_no`, `status`, `del_flag`, `create_time`)
VALUES
('homeai_btn_learn_add',     'homeai_menu_learn_list', '新增',         NULL, NULL, 0, 'homeai:learn:addMaterial',        2, 1.0, 1, 0, NOW()),
('homeai_btn_learn_export',  'homeai_menu_learn_list', '导出',         NULL, NULL, 0, 'homeai:learn:exportXls',          2, 2.0, 1, 0, NOW()),
('homeai_btn_learn_import',  'homeai_menu_learn_list', '导入',         NULL, NULL, 0, 'homeai:learn:importExcel',        2, 3.0, 1, 0, NOW()),
('homeai_btn_learn_recycle', 'homeai_menu_learn_list', '移入回收站',    NULL, NULL, 0, 'homeai:learn:moveToRecycleBin',   2, 4.0, 1, 0, NOW()),
('homeai_btn_learn_restore', 'homeai_menu_learn_list', '恢复',         NULL, NULL, 0, 'homeai:learn:restore',             2, 5.0, 1, 0, NOW()),
('homeai_btn_learn_permDel', 'homeai_menu_learn_list', '彻底删除',      NULL, NULL, 0, 'homeai:learn:deletePermanently',   2, 6.0, 1, 0, NOW());

-- =============================================================
-- 7. 文件管理 - 新增文件夹按钮权限
-- =============================================================
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `perms`, `menu_type`, `sort_no`, `status`, `del_flag`, `create_time`)
VALUES
('homeai_btn_storage_folder', 'homeai_menu_storage_files', '新增文件夹', NULL, NULL, 0, 'homeai:storage:folder:add', 2, 1.0, 1, 0, NOW());
