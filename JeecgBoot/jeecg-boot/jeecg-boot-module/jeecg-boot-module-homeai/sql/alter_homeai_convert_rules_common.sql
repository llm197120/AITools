-- -*- coding: utf-8 -*-
-- 第 130 轮：补齐常用 Office 转换规则（与本机 Word/Excel/PowerPoint 及 LibreOffice 能力对齐）
-- 已有 csv→xlsx 行保留；按 (source,target) 去重，可重复执行。

INSERT INTO `homeai_convert_rule` (`id`, `source_format`, `target_format`, `is_enabled`, `create_by`)
SELECT tmp.id, tmp.source_format, tmp.target_format, '1', 'system'
FROM (
    -- Word
    SELECT 'cvrule_docx_pdf' AS id, 'docx' AS source_format, 'pdf' AS target_format
    UNION ALL SELECT 'cvrule_docx_doc', 'docx', 'doc'
    UNION ALL SELECT 'cvrule_docx_txt', 'docx', 'txt'
    UNION ALL SELECT 'cvrule_doc_pdf', 'doc', 'pdf'
    UNION ALL SELECT 'cvrule_doc_docx', 'doc', 'docx'
    UNION ALL SELECT 'cvrule_doc_txt', 'doc', 'txt'
    UNION ALL SELECT 'cvrule_txt_pdf', 'txt', 'pdf'
    UNION ALL SELECT 'cvrule_txt_docx', 'txt', 'docx'
    -- Excel
    UNION ALL SELECT 'cvrule_xlsx_pdf', 'xlsx', 'pdf'
    UNION ALL SELECT 'cvrule_xlsx_xls', 'xlsx', 'xls'
    UNION ALL SELECT 'cvrule_xlsx_csv', 'xlsx', 'csv'
    UNION ALL SELECT 'cvrule_xls_pdf', 'xls', 'pdf'
    UNION ALL SELECT 'cvrule_xls_xlsx', 'xls', 'xlsx'
    UNION ALL SELECT 'cvrule_xls_csv', 'xls', 'csv'
    UNION ALL SELECT 'cvrule_csv_xlsx', 'csv', 'xlsx'
    UNION ALL SELECT 'cvrule_csv_pdf', 'csv', 'pdf'
    -- PowerPoint
    UNION ALL SELECT 'cvrule_pptx_pdf', 'pptx', 'pdf'
    UNION ALL SELECT 'cvrule_pptx_ppt', 'pptx', 'ppt'
    UNION ALL SELECT 'cvrule_ppt_pdf', 'ppt', 'pdf'
    UNION ALL SELECT 'cvrule_ppt_pptx', 'ppt', 'pptx'
) AS tmp
WHERE NOT EXISTS (
    SELECT 1 FROM `homeai_convert_rule` r
    WHERE r.`source_format` = tmp.source_format
      AND r.`target_format` = tmp.target_format
);

DROP PROCEDURE IF EXISTS homeai_add_convert_rule_uk;
DELIMITER //
CREATE PROCEDURE homeai_add_convert_rule_uk()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'homeai_convert_rule'
          AND index_name = 'uk_hw_convert_rule_pair'
    ) THEN
        ALTER TABLE `homeai_convert_rule`
            ADD UNIQUE KEY `uk_hw_convert_rule_pair` (`source_format`, `target_format`);
    END IF;
END //
DELIMITER ;
CALL homeai_add_convert_rule_uk();
DROP PROCEDURE IF EXISTS homeai_add_convert_rule_uk;
