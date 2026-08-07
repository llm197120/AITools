-- =============================================================
-- 修复菜单 menu_type 错误 - 数据库迁移脚本
--
-- 问题：
--   1. 子菜单（有 parent_id 和实际页面 component）的 menu_type=0
--      应为 1
--   2. 按钮权限的 menu_type 被错误地写成递增序号 1,2,3,4,5
--      应统一为 2
--
-- JeecgBoot sys_permission.menu_type 标准值：
--   0 = 一级菜单/目录菜单（component=Layout，parent_id 为空或非空）
--   1 = 子菜单/页面菜单（有实际页面的 component）
--   2 = 按钮/权限
--
-- 在已执行过 init_homeai_menus.sql 的数据库上运行
-- =============================================================

-- =============================================================
-- 一、修复子菜单：menu_type = 0 → 1
-- =============================================================
UPDATE `sys_permission`
SET `menu_type` = 1
WHERE `id` IN (
    'homeai_menu_user',             -- 用户管理
    'homeai_menu_family',           -- 家庭管理
    'homeai_menu_ai_keys',          -- AI密钥配置
    'homeai_menu_quota',            -- Token额度配置
    'homeai_menu_storage_files',    -- 文件管理
    'homeai_menu_storage_template', -- 文档模板
    'homeai_menu_storage_rule',     -- 转换规则
    'homeai_menu_storage_history',  -- 处理记录
    'homeai_menu_bill_list',        -- 账单列表
    'homeai_menu_bill_category',    -- 消费分类
    'homeai_menu_recipe_list',      -- 菜谱列表
    'homeai_menu_learn_list'        -- 学习资料
)
AND `menu_type` = 0
AND `component` NOT IN ('Layout', 'layouts/RouteView');

-- =============================================================
-- 二、修复按钮权限：menu_type ≠ 2 → 2
-- =============================================================
UPDATE `sys_permission`
SET `menu_type` = 2
WHERE `id` IN (
    'homeai_btn_user_view',     -- 查看（用户管理）
    'homeai_btn_user_delete',   -- 注销
    'homeai_btn_user_status',   -- 启用/禁用
    'homeai_btn_family_view',   -- 查看（家庭管理）
    'homeai_btn_key_add',       -- 新增
    'homeai_btn_key_delete',    -- 删除
    'homeai_btn_key_default',   -- 设为默认
    'homeai_btn_key_status'     -- 启用/停用
)
AND `menu_type` != 2;

-- =============================================================
-- 验证（可选）
-- =============================================================
-- -- 检查是否有遗留的 menu_type 异常值
-- SELECT id, name, menu_type, parent_id
-- FROM sys_permission
-- WHERE id LIKE 'homeai_%' AND del_flag = 0
-- ORDER BY parent_id, sort_no;
