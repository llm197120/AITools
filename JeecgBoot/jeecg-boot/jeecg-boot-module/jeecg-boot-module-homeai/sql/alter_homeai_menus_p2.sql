-- P2 批次补充权限码
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `perms`, `menu_type`, `sort_no`, `status`, `del_flag`, `create_time`) VALUES
('homeai_btn_history_delete', 'homeai_menu_storage_history', '删除记录', NULL, NULL, 0, 'homeai:storage:history:delete', 2, 1.0, 1, 0, NOW());
