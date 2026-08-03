-- 账单管理：新增「账单导入」菜单
INSERT INTO `sys_permission`
  (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `perms`, `is_leaf`, `icon`, `sort_no`, `del_flag`, `status`, `create_time`)
VALUES
  ('homeai_menu_bill_import', 'homeai_menu_bill', '账单导入',
   '/homeai/bill/billImport', '/views/homeai/bill/billImport',
   1, 'homeai:bill:importExcel', 1, 'ant-design:upload-outlined',
   4.0, 0, 1, NOW());
