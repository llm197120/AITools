-- 第五轮迭代：学习分类、文件白名单、AI对话按钮权限菜单

-- 已有库执行本脚本（幂等）



-- 学习分类菜单

INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `perms`, `menu_type`, `icon`, `sort_no`, `status`, `del_flag`, `create_time`)

SELECT 'homeai_menu_learn_category', 'homeai_menu_learn', '学习分类', '/homeai/learn/learnCategory', '/views/homeai/learn/learnCategory', 1, 'homeai:learn:category:list', 1, 'ant-design:tags-outlined', 1.5, 1, 0, NOW()

FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `id` = 'homeai_menu_learn_category');



INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `perms`, `menu_type`, `sort_no`, `status`, `del_flag`, `create_time`)

SELECT * FROM (

    SELECT 'homeai_btn_learn_cat_add' AS id, 'homeai_menu_learn_category' AS parent_id, '新增分类' AS name, NULL AS url, NULL AS component, 0 AS is_route, 'homeai:learn:category:add' AS perms, 2 AS menu_type, 1.0 AS sort_no, 1 AS status, 0 AS del_flag, NOW() AS create_time

    UNION ALL SELECT 'homeai_btn_learn_cat_edit', 'homeai_menu_learn_category', '编辑分类', NULL, NULL, 0, 'homeai:learn:category:edit', 2, 2.0, 1, 0, NOW()

    UNION ALL SELECT 'homeai_btn_learn_cat_delete', 'homeai_menu_learn_category', '删除分类', NULL, NULL, 0, 'homeai:learn:category:delete', 2, 3.0, 1, 0, NOW()

) AS tmp WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `id` = 'homeai_btn_learn_cat_add');



-- 文件白名单菜单（挂在资料存储下）

INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `perms`, `menu_type`, `icon`, `sort_no`, `status`, `del_flag`, `create_time`)

SELECT 'homeai_menu_storage_whitelist', 'homeai_menu_storage', '文件白名单', '/homeai/storage/fileWhitelist', '/views/homeai/storage/fileWhitelist', 1, 'homeai:config:whitelist:list', 1, 'ant-design:safety-outlined', 5.0, 1, 0, NOW()

FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `id` = 'homeai_menu_storage_whitelist');



INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `perms`, `menu_type`, `sort_no`, `status`, `del_flag`, `create_time`)

SELECT 'homeai_btn_whitelist_edit', 'homeai_menu_storage_whitelist', '保存配置', NULL, NULL, 0, 'homeai:config:whitelist:edit', 2, 1.0, 1, 0, NOW()

FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `id` = 'homeai_btn_whitelist_edit');



-- AI 对话管理按钮权限

INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `perms`, `menu_type`, `sort_no`, `status`, `del_flag`, `create_time`)

SELECT * FROM (

    SELECT 'homeai_btn_ai_conv_view' AS id, 'homeai_menu_ai_conversation' AS parent_id, '查看消息' AS name, NULL AS url, NULL AS component, 0 AS is_route, 'homeai:ai:conversation:list' AS perms, 2 AS menu_type, 1.0 AS sort_no, 1 AS status, 0 AS del_flag, NOW() AS create_time

    UNION ALL SELECT 'homeai_btn_ai_conv_delete', 'homeai_menu_ai_conversation', '删除对话', NULL, NULL, 0, 'homeai:ai:conversation:delete', 2, 2.0, 1, 0, NOW()

) AS tmp WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `id` = 'homeai_btn_ai_conv_delete');

