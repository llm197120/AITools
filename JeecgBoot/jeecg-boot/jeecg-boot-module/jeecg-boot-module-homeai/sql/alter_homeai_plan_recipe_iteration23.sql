-- -*- coding: utf-8 -*-
-- HomeAI 第 23 轮：计划关联菜谱
-- 已有库执行本脚本；新库见 init_homeai_tables.sql

ALTER TABLE `homeai_plan_master`
  ADD COLUMN `recipe_id` VARCHAR(32) NULL COMMENT '关联菜谱ID' AFTER `category`;

ALTER TABLE `homeai_plan_master`
  ADD KEY `idx_hw_plan_master_recipe` (`recipe_id`);
