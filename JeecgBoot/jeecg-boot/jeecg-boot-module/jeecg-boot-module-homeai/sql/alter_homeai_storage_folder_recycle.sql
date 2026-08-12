-- -*- coding: utf-8 -*-
-- HomeAI 第 23 轮：文件夹回收站（deleted_at）
-- 已有库执行本脚本；新库见 init_homeai_tables.sql

ALTER TABLE `homeai_storage_folder`
  ADD COLUMN `deleted_at` DATETIME NULL COMMENT '删除时间' AFTER `del_flag`;

ALTER TABLE `homeai_storage_folder`
  ADD KEY `idx_hw_folder_recycle` (`del_flag`, `deleted_at`);
