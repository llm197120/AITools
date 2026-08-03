-- =============================================================
-- 修复父菜单 component 值（layouts/RouteView → Layout）
-- 
-- 原因：Vue3 版前端路由解析时，只有 component='Layout'（不区分大小写）
--       会保留子菜单；其他值（如 layouts/RouteView）会导致子菜单
--       被克隆覆盖，路由树中各级子菜单全部丢失。
-- 
-- 在已执行过 init_homeai_menus.sql 或 alter_homeai_menus.sql 的数据库上运行
-- =============================================================

-- 将所有用 layouts/RouteView 的父菜单统一改为 Layout
UPDATE `sys_permission`
SET `component` = 'Layout'
WHERE `id` IN (
    'homeai_menu_root',    -- 家庭AI小工具
    'homeai_menu_ai',      -- AI管理
    'homeai_menu_storage', -- 资料存储管理
    'homeai_menu_bill'     -- 账单管理
)
AND `component` = 'layouts/RouteView';

-- 验证（可选）
-- SELECT id, name, component FROM sys_permission WHERE id IN ('homeai_menu_root','homeai_menu_ai','homeai_menu_storage','homeai_menu_bill');
