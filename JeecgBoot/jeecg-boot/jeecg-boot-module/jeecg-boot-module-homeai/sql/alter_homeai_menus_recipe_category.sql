-- 菜谱管理：新增「菜谱分类」菜单
INSERT INTO `sys_permission`
  (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `perms`, `is_leaf`, `icon`, `sort_no`, `del_flag`, `status`, `create_time`)
VALUES
  ('homeai_menu_recipe_category', 'homeai_menu_recipe_list', '菜谱分类',
   '/homeai/recipe/recipeCategory', '/views/homeai/recipe/recipeCategory',
   1, 'homeai:recipe:category:list', 1, 'ant-design:appstore-outlined',
   1.5, 0, 1, NOW());

INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `perms`, `menu_type`, `sort_no`, `status`, `del_flag`, `create_time`) VALUES
('homeai_btn_recipe_cat_add',    'homeai_menu_recipe_category', '新增分类', NULL, NULL, 0, 'homeai:recipe:category:add',    2, 1.0, 1, 0, NOW()),
('homeai_btn_recipe_cat_edit',   'homeai_menu_recipe_category', '编辑分类', NULL, NULL, 0, 'homeai:recipe:category:edit',   2, 2.0, 1, 0, NOW()),
('homeai_btn_recipe_cat_delete', 'homeai_menu_recipe_category', '删除分类', NULL, NULL, 0, 'homeai:recipe:category:delete', 2, 3.0, 1, 0, NOW());
