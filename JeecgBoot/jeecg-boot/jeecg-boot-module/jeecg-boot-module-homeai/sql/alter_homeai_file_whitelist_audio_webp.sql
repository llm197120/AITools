-- -*- coding: utf-8 -*-
-- 第 63 轮：白名单补 webp / 音频 / webm（已有库可重复执行）

INSERT INTO `homeai_file_whitelist` (`id`, `extension`, `category`, `sort_order`, `is_enabled`)
SELECT * FROM (
    SELECT 'fw_webp' AS id, 'webp' AS extension, 'image' AS category, 6 AS sort_order, 1 AS is_enabled
    UNION ALL SELECT 'fw_webm', 'webm', 'video', 24, 1
    UNION ALL SELECT 'fw_mp3', 'mp3', 'audio', 50, 1
    UNION ALL SELECT 'fw_wav', 'wav', 'audio', 51, 1
    UNION ALL SELECT 'fw_m4a', 'm4a', 'audio', 52, 1
    UNION ALL SELECT 'fw_aac', 'aac', 'audio', 53, 1
) AS tmp
WHERE NOT EXISTS (
    SELECT 1 FROM `homeai_file_whitelist` w WHERE w.`extension` = tmp.extension
);
