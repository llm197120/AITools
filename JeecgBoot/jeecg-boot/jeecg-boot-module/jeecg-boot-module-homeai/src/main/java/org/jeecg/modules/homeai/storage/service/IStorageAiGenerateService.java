package org.jeecg.modules.homeai.storage.service;

import java.nio.file.Path;

/**
 * AI 文档生成（poi-tl 模板填充）
 */
public interface IStorageAiGenerateService {

    /**
     * 根据模板与指令生成 docx 文件
     *
     * @param userId         用户 ID
     * @param templateRefId  模板 ID（homeai_office_template）或源文件 ID，可为空则使用默认 Word 模板
     * @param instruction    生成指令/正文
     * @return 生成文件的物理路径
     */
    Path generateDocx(String userId, String templateRefId, String instruction) throws Exception;
}
