-- -*- coding: utf-8 -*-
-- HomeAI 列/表存在性 smoke：缺列或缺表则本脚本失败。
-- 已有库在跑完 alter-order.txt 后执行；新库跑完 init 后也应通过。
-- 不改数据（SELECT ... LIMIT 0）。

SELECT `password`, `salt`, `login_type` FROM `homeai_wx_user` LIMIT 0;
SELECT `deleted_at` FROM `homeai_storage_folder` LIMIT 0;
SELECT `preview_pdf_url`, `del_flag`, `deleted_at` FROM `homeai_storage_file` LIMIT 0;
SELECT `preview_pdf_url`, `category_id` FROM `homeai_learn_material` LIMIT 0;
SELECT `study_date` FROM `homeai_learn_record` LIMIT 0;
SELECT `recipe_id` FROM `homeai_plan_master` LIMIT 0;
SELECT `id` FROM `homeai_recipe_favorite` LIMIT 0;
SELECT `id` FROM `homeai_app_version` LIMIT 0;
SELECT `extension` FROM `homeai_file_whitelist` LIMIT 0;
SELECT `del_flag`, `deleted_at` FROM `homeai_ai_conversation` LIMIT 0;
