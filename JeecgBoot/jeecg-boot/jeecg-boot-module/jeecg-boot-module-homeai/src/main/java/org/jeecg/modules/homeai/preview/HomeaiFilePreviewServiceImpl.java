package org.jeecg.modules.homeai.preview;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.homeai.config.service.IHomeaiFileStorageService;
import org.jeecg.modules.homeai.recipe.entity.LearnMaterial;
import org.jeecg.modules.homeai.recipe.service.ILearnService;
import org.jeecg.modules.homeai.storage.entity.StorageConvertTask;
import org.jeecg.modules.homeai.storage.entity.StorageFile;
import org.jeecg.modules.homeai.storage.service.IStorageConvertTaskService;
import org.jeecg.modules.homeai.storage.service.IStorageFileService;
import org.jeecg.modules.homeai.storage.service.IStorageOfficeConvertExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 预览描述组装 + Office 转 PDF 任务提交
 */
@Slf4j
@Service
public class HomeaiFilePreviewServiceImpl implements IHomeaiFilePreviewService {

    private static final List<String> IN_FLIGHT = Arrays.asList("PENDING", "PROCESSING");

    @Autowired
    private IHomeaiFileStorageService fileStorageService;

    @Autowired
    private IStorageConvertTaskService convertTaskService;

    @Autowired
    private IStorageFileService storageFileService;

    @Autowired
    private ILearnService learnService;

    @Lazy
    @Autowired
    private IStorageOfficeConvertExecutor convertExecutor;

    @Override
    public HomeaiFilePreviewDto previewStorage(StorageFile file) {
        HomeaiFilePreviewDto dto = new HomeaiFilePreviewDto();
        String ext = HomeaiPreviewKind.normalizeExt(file.getExtension());
        if (ext.isEmpty()) {
            ext = HomeaiPreviewKind.extensionOfNameOrUrl(file.getOriginalName());
        }
        dto.setKind(HomeaiPreviewKind.ofExtension(ext));
        dto.setExtension(ext);
        dto.setFileName(file.getOriginalName());
        dto.setFileUrl(fileStorageService.resolveAccessUrl(file.getFileUrl()));
        if (oConvertUtils.isNotEmpty(file.getPreviewPdfUrl())) {
            dto.setPreviewPdfUrl(fileStorageService.resolveAccessUrl(file.getPreviewPdfUrl()));
            dto.setConvertStatus("COMPLETED");
        } else if (HomeaiPreviewKind.isOffice(dto.getKind())) {
            attachLatest(dto, file.getId(), CONVERT_PREVIEW_PDF);
        }
        return dto;
    }

    @Override
    public HomeaiFilePreviewDto previewLearn(LearnMaterial material) {
        HomeaiFilePreviewDto dto = new HomeaiFilePreviewDto();
        String ext = HomeaiPreviewKind.extensionOfNameOrUrl(material.getFileUrl());
        dto.setKind(HomeaiPreviewKind.ofLearnType(material.getType(), ext));
        dto.setExtension(ext);
        dto.setFileName(material.getTitle());
        dto.setFileUrl(fileStorageService.resolveAccessUrl(material.getFileUrl()));
        if (oConvertUtils.isNotEmpty(material.getPreviewPdfUrl())) {
            dto.setPreviewPdfUrl(fileStorageService.resolveAccessUrl(material.getPreviewPdfUrl()));
            dto.setConvertStatus("COMPLETED");
        } else if (HomeaiPreviewKind.isOffice(dto.getKind())) {
            attachLatest(dto, material.getId(), CONVERT_PREVIEW_PDF_LEARN);
        }
        return dto;
    }

    @Override
    public HomeaiFilePreviewDto ensureStoragePreviewPdf(String userId, StorageFile file) {
        HomeaiFilePreviewDto dto = previewStorage(file);
        if (!HomeaiPreviewKind.isOffice(dto.getKind())) {
            return dto;
        }
        if (oConvertUtils.isNotEmpty(dto.getPreviewPdfUrl())) {
            return dto;
        }
        StorageConvertTask existing = findLatest(file.getId(), CONVERT_PREVIEW_PDF);
        if (existing != null && "COMPLETED".equals(existing.getStatus())
                && oConvertUtils.isNotEmpty(existing.getResultFileUrl())) {
            writeBackStorage(file, existing.getResultFileUrl());
            return previewStorage(storageFileService.getById(file.getId()));
        }
        if (existing != null && IN_FLIGHT.contains(existing.getStatus())) {
            return refreshByTask(dto, existing);
        }
        StorageConvertTask task = submitPreviewTask(userId, file.getId(), CONVERT_PREVIEW_PDF,
                HomeaiPreviewKind.normalizeExt(file.getExtension()));
        return refreshByTask(dto, task);
    }

    @Override
    public HomeaiFilePreviewDto ensureLearnPreviewPdf(String userId, LearnMaterial material) {
        HomeaiFilePreviewDto dto = previewLearn(material);
        if (!HomeaiPreviewKind.isOffice(dto.getKind())) {
            return dto;
        }
        if (oConvertUtils.isNotEmpty(dto.getPreviewPdfUrl())) {
            return dto;
        }
        if (oConvertUtils.isEmpty(material.getFileUrl())) {
            throw new JeecgBootException("该资料没有可转换的文件");
        }
        StorageConvertTask existing = findLatest(material.getId(), CONVERT_PREVIEW_PDF_LEARN);
        if (existing != null && "COMPLETED".equals(existing.getStatus())
                && oConvertUtils.isNotEmpty(existing.getResultFileUrl())) {
            writeBackLearn(material, existing.getResultFileUrl());
            return previewLearn(learnService.getById(material.getId()));
        }
        if (existing != null && IN_FLIGHT.contains(existing.getStatus())) {
            return refreshByTask(dto, existing);
        }
        String ext = dto.getExtension();
        if (oConvertUtils.isEmpty(ext)) {
            ext = HomeaiPreviewKind.extensionOfNameOrUrl(material.getFileUrl());
        }
        StorageConvertTask task = submitPreviewTask(userId, material.getId(), CONVERT_PREVIEW_PDF_LEARN, ext);
        return refreshByTask(dto, task);
    }

    @Override
    public HomeaiFilePreviewDto refreshByTask(HomeaiFilePreviewDto preview, StorageConvertTask task) {
        if (preview == null) {
            preview = new HomeaiFilePreviewDto();
        }
        if (task == null) {
            return preview;
        }
        preview.setConvertTaskId(task.getId());
        preview.setConvertStatus(task.getStatus());
        preview.setErrorMessage(task.getErrorMessage());
        if ("COMPLETED".equals(task.getStatus()) && oConvertUtils.isNotEmpty(task.getResultFileUrl())) {
            preview.setPreviewPdfUrl(fileStorageService.resolveAccessUrl(task.getResultFileUrl()));
        }
        return preview;
    }

    private void attachLatest(HomeaiFilePreviewDto dto, String sourceId, String convertType) {
        StorageConvertTask task = findLatest(sourceId, convertType);
        if (task != null) {
            refreshByTask(dto, task);
        }
    }

    private StorageConvertTask findLatest(String sourceId, String convertType) {
        LambdaQueryWrapper<StorageConvertTask> q = new LambdaQueryWrapper<>();
        q.eq(StorageConvertTask::getFileId, sourceId)
                .eq(StorageConvertTask::getConvertType, convertType)
                .orderByDesc(StorageConvertTask::getCreateTime)
                .last("LIMIT 1");
        return convertTaskService.getOne(q, false);
    }

    private StorageConvertTask submitPreviewTask(String userId, String sourceId, String convertType, String sourceExt) {
        StorageConvertTask task = new StorageConvertTask();
        task.setUserId(userId);
        task.setFileId(sourceId);
        task.setConvertType(convertType);
        task.setSourceFormat(sourceExt);
        task.setTargetFormat("pdf");
        task.setStatus("PENDING");
        task.setCreateTime(new Date());
        task.setUpdateTime(new Date());
        convertTaskService.save(task);
        convertExecutor.executeAsync(task.getId());
        log.info("提交预览转 PDF: sourceId={}, type={}, taskId={}", sourceId, convertType, task.getId());
        return task;
    }

    private void writeBackStorage(StorageFile file, String storedPdfRef) {
        file.setPreviewPdfUrl(storedPdfRef);
        file.setUpdateTime(new Date());
        storageFileService.updateById(file);
    }

    private void writeBackLearn(LearnMaterial material, String storedPdfRef) {
        material.setPreviewPdfUrl(storedPdfRef);
        material.setUpdateTime(new Date());
        learnService.updateById(material);
    }
}
