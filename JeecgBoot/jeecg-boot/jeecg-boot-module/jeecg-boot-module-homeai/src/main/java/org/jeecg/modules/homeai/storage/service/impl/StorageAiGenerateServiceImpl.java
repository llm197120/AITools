package org.jeecg.modules.homeai.storage.service.impl;

import com.deepoove.poi.XWPFTemplate;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.homeai.config.service.IHomeaiFileStorageService;
import org.jeecg.modules.homeai.storage.entity.OfficeTemplate;
import org.jeecg.modules.homeai.storage.entity.StorageFile;
import org.jeecg.modules.homeai.ai.service.IHomeaiLlmService;
import org.jeecg.modules.homeai.config.service.IHomeaiPlanConfigService;
import org.jeecg.modules.homeai.storage.service.IOfficeTemplateService;
import org.jeecg.modules.homeai.storage.service.IStorageAiGenerateService;
import org.jeecg.modules.homeai.storage.service.IStorageFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class StorageAiGenerateServiceImpl implements IStorageAiGenerateService {

    @Value("${jeecg.path.upload:./upload}")
    private String uploadPath;

    @Autowired
    private IOfficeTemplateService templateService;

    @Autowired
    private IStorageFileService fileService;

    @Autowired
    private IHomeaiLlmService llmService;

    @Autowired
    private IHomeaiPlanConfigService planConfigService;

    @Autowired
    private IHomeaiFileStorageService fileStorageService;

    @Override
    public Path generateDocx(String userId, String templateRefId, String instruction) throws Exception {
        String content = resolveDocumentContent(userId, instruction);
        Path templatePath = resolveTemplatePath(templateRefId);
        String outDir = uploadPath + "/homeai/" + userId + "/";
        Files.createDirectories(Paths.get(outDir));
        String fileName = UUID.randomUUID().toString().replace("-", "") + ".docx";
        Path outputPath = Paths.get(outDir + fileName);

        Map<String, Object> data = new HashMap<>();
        data.put("title", "AI 生成文档");
        data.put("content", content);
        data.put("instruction", content);
        data.put("date", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

        if (templatePath != null && Files.exists(templatePath)) {
            try (XWPFTemplate tpl = XWPFTemplate.compile(templatePath.toString()).render(data);
                 OutputStream out = Files.newOutputStream(outputPath)) {
                tpl.write(out);
            }
        } else {
            try (XWPFDocument doc = new XWPFDocument();
                 OutputStream out = Files.newOutputStream(outputPath)) {
                XWPFParagraph title = doc.createParagraph();
                XWPFRun tRun = title.createRun();
                tRun.setBold(true);
                tRun.setFontSize(16);
                tRun.setText("AI 生成文档");
                XWPFParagraph body = doc.createParagraph();
                XWPFRun bRun = body.createRun();
                bRun.setText(content);
                doc.write(out);
            }
        }
        log.info("AI文档生成完成: userId={}, output={}", userId, outputPath);
        return outputPath;
    }

    private String resolveDocumentContent(String userId, String instruction) {
        if (oConvertUtils.isEmpty(instruction)) {
            return "（无生成内容）";
        }
        String trimmed = instruction.trim();
        if (planConfigService.isAiDocPolishEnabled()) {
            try {
                var result = llmService.generateDocumentContent(userId, trimmed);
                if (result != null && oConvertUtils.isNotEmpty(result.getContent())) {
                    return result.getContent();
                }
            } catch (Exception e) {
                log.warn("AI文档润色失败，使用原始指令: {}", e.getMessage());
                if (e instanceof org.jeecg.common.exception.JeecgBootException) {
                    throw e;
                }
            }
        }
        return trimmed;
    }

    private Path resolveTemplatePath(String templateRefId) {
        if (oConvertUtils.isNotEmpty(templateRefId)) {
            OfficeTemplate tpl = templateService.getById(templateRefId);
            if (tpl != null && oConvertUtils.isNotEmpty(tpl.getFileUrl())) {
                Path p = toPhysicalPath(tpl.getFileUrl());
                if (Files.exists(p)) {
                    return p;
                }
            }
            StorageFile sf = fileService.getById(templateRefId);
            if (sf != null) {
                Path p = toPhysicalPath(sf.getFileUrl());
                if (Files.exists(p)) {
                    return p;
                }
            }
        }
        List<OfficeTemplate> defaults = templateService.getEnabledTemplates("word");
        if (defaults != null && !defaults.isEmpty()) {
            for (OfficeTemplate t : defaults) {
                if ("1".equals(t.getIsDefault()) && oConvertUtils.isNotEmpty(t.getFileUrl())) {
                    Path p = toPhysicalPath(t.getFileUrl());
                    if (Files.exists(p)) {
                        return p;
                    }
                }
            }
            OfficeTemplate first = defaults.get(0);
            if (oConvertUtils.isNotEmpty(first.getFileUrl())) {
                return toPhysicalPath(first.getFileUrl());
            }
        }
        return null;
    }

    private Path toPhysicalPath(String fileUrl) {
        return fileStorageService.resolveLocalPath(fileUrl);
    }
}
