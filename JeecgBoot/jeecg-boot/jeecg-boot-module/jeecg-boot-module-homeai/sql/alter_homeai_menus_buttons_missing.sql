-- =============================================================
-- HomeAI: 补充缺失的按钮权限码（与后端 @RequiresPermissions 对齐）
-- 幂等：已存在的主键会因重复主键报错，仅需执行一次
-- =============================================================

-- 用户管理：编辑/注销
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `perms`, `menu_type`, `sort_no`, `status`, `del_flag`, `create_time`) VALUES
('homeai_btn_user_edit',   'homeai_menu_user', '编辑', NULL, NULL, 0, 'homeai:user:edit',   2, 2.0, 1, 0, NOW()),
('homeai_btn_user_delete', 'homeai_menu_user', '注销', NULL, NULL, 0, 'homeai:user:delete', 2, 3.0, 1, 0, NOW());

-- 家庭管理：编辑/移除成员
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `perms`, `menu_type`, `sort_no`, `status`, `del_flag`, `create_time`) VALUES
('homeai_btn_family_edit',   'homeai_menu_family', '编辑', NULL, NULL, 0, 'homeai:family:edit',   2, 1.5, 1, 0, NOW()),
('homeai_btn_family_delete', 'homeai_menu_family', '移除成员', NULL, NULL, 0, 'homeai:family:delete', 2, 2.5, 1, 0, NOW());

-- 账单：编辑 + 消费分类增删改
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `perms`, `menu_type`, `sort_no`, `status`, `del_flag`, `create_time`) VALUES
('homeai_btn_bill_edit',        'homeai_menu_bill_list',    '编辑',         NULL, NULL, 0, 'homeai:bill:edit',            2, 1.5, 1, 0, NOW()),
('homeai_btn_bill_cat_add',     'homeai_menu_bill_category','新增分类',     NULL, NULL, 0, 'homeai:bill:category:add',    2, 1.0, 1, 0, NOW()),
('homeai_btn_bill_cat_edit',    'homeai_menu_bill_category','编辑分类',     NULL, NULL, 0, 'homeai:bill:category:edit',   2, 2.0, 1, 0, NOW()),
('homeai_btn_bill_cat_delete',  'homeai_menu_bill_category','删除分类',     NULL, NULL, 0, 'homeai:bill:category:delete', 2, 3.0, 1, 0, NOW());

-- 菜谱：编辑
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `perms`, `menu_type`, `sort_no`, `status`, `del_flag`, `create_time`) VALUES
('homeai_btn_recipe_edit', 'homeai_menu_recipe_list', '编辑', NULL, NULL, 0, 'homeai:recipe:edit', 2, 1.5, 1, 0, NOW());

-- 学习：编辑
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `perms`, `menu_type`, `sort_no`, `status`, `del_flag`, `create_time`) VALUES
('homeai_btn_learn_edit', 'homeai_menu_learn_list', '编辑', NULL, NULL, 0, 'homeai:learn:edit', 2, 1.5, 1, 0, NOW());

-- AI 密钥：新增/编辑/删除
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `perms`, `menu_type`, `sort_no`, `status`, `del_flag`, `create_time`) VALUES
('homeai_btn_ai_key_add',    'homeai_menu_ai_keys', '新增', NULL, NULL, 0, 'homeai:config:key:add',    2, 1.0, 1, 0, NOW()),
('homeai_btn_ai_key_edit',   'homeai_menu_ai_keys', '编辑', NULL, NULL, 0, 'homeai:config:key:edit',   2, 2.0, 1, 0, NOW()),
('homeai_btn_ai_key_delete', 'homeai_menu_ai_keys', '删除', NULL, NULL, 0, 'homeai:config:key:delete', 2, 3.0, 1, 0, NOW());

-- Token 额度：编辑
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `perms`, `menu_type`, `sort_no`, `status`, `del_flag`, `create_time`) VALUES
('homeai_btn_quota_edit', 'homeai_menu_quota', '编辑额度', NULL, NULL, 0, 'homeai:quota:edit', 2, 1.0, 1, 0, NOW());

-- 转换规则：新增/编辑/删除
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `perms`, `menu_type`, `sort_no`, `status`, `del_flag`, `create_time`) VALUES
('homeai_btn_rule_add',    'homeai_menu_storage_rule', '新增', NULL, NULL, 0, 'homeai:storage:rule:add',    2, 1.0, 1, 0, NOW()),
('homeai_btn_rule_edit',   'homeai_menu_storage_rule', '编辑', NULL, NULL, 0, 'homeai:storage:rule:edit',   2, 2.0, 1, 0, NOW()),
('homeai_btn_rule_delete', 'homeai_menu_storage_rule', '删除', NULL, NULL, 0, 'homeai:storage:rule:delete', 2, 3.0, 1, 0, NOW());

-- 文档模板：新增/编辑/删除
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `perms`, `menu_type`, `sort_no`, `status`, `del_flag`, `create_time`) VALUES
('homeai_btn_tpl_add',    'homeai_menu_storage_template', '新增', NULL, NULL, 0, 'homeai:storage:template:add',    2, 1.0, 1, 0, NOW()),
('homeai_btn_tpl_edit',   'homeai_menu_storage_template', '编辑', NULL, NULL, 0, 'homeai:storage:template:edit',   2, 2.0, 1, 0, NOW()),
('homeai_btn_tpl_delete', 'homeai_menu_storage_template', '删除', NULL, NULL, 0, 'homeai:storage:template:delete', 2, 3.0, 1, 0, NOW());
