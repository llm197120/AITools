package org.jeecg.modules.homeai.storage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.homeai.config.service.IHomeaiFileStorageService;
import org.jeecg.modules.homeai.storage.entity.StorageConvertTask;
import org.jeecg.modules.homeai.storage.entity.StorageFile;
import org.jeecg.modules.homeai.storage.service.IStorageAiGenerateService;
import org.jeecg.modules.homeai.storage.service.IStorageConvertTaskService;
import org.jeecg.modules.homeai.storage.service.IStorageFileService;
import org.jeecg.modules.homeai.storage.service.IStorageOfficeConvertExecutor;
import org.jeecg.modules.homeai.storage.util.HomeaiOfficeConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class StorageOfficeConvertExecutorImpl implements IStorageOfficeConvertExecutor {

    @Autowired
    private IStorageConvertTaskService taskService;

    @Autowired
    private IStorageFileService fileService;

    @Autowired
    private IStorageAiGenerateService aiGenerateService;

    @Autowired
    private HomeaiOfficeConvertUtil officeConvertUtil;

    @Autowired
    private IHomeaiFileStorageService fileStorageService;

    @Override
    @Async
    public void executeAsync(String taskId) {
        processTask(taskId);
    }

    @Override
    @Scheduled(fixedDelay = 30000)
    public void processPendingTasks() {
        LambdaQueryWrapper<StorageConvertTask> q = new LambdaQueryWrapper<>();
        q.eq(StorageConvertTask::getStatus, "PENDING").orderByAsc(StorageConvertTask::getCreateTime).last("LIMIT 5");
        for (StorageConvertTask task : taskService.list(q)) {
            processTask(task.getId());
        }
    }

    private void processTask(String taskId) {
        StorageConvertTask task = taskService.getById(taskId);
        if (task == null || !"PENDING".equals(task.getStatus())) {
            return;
        }
        long startMs = System.currentTimeMillis();
        task.setStatus("PROCESSING");
        task.setUpdateTime(new Date());
        taskService.updateById(task);

        try {
            if ("ai_generate".equals(task.getConvertType())) {
                processAiGenerate(task, startMs);
                return;
            }
            processFormatConvert(task, startMs);
        } catch (Exception e) {
            log.error("Office转换任务失败: taskId={}", taskId, e);
            failTask(task, e.getMessage() != null ? e.getMessage() : "转换异常", startMs);
        }
    }

    private void processAiGenerate(StorageConvertTask task, long startMs) throws Exception {
        Path output = aiGenerateService.generateDocx(
                task.getUserId(),
                task.getFileId(),
                task.getInstruction());
        String storedName = output.getFileName().toString();
        String objectKey = "homeai/" + task.getUserId() + "/" + storedName;
        task.setStatus("COMPLETED");
        task.setResultFileUrl(fileStorageService.storeLocalFile(output, objectKey));
        task.setResultFileSize(Files.size(output));
        task.setErrorMessage(null);
        finishTask(task, startMs);
    }

    private void processFormatConvert(StorageConvertTask task, long startMs) throws Exception {
        StorageFile sourceFile = fileService.getById(task.getFileId());
        if (sourceFile == null) {
            failTask(task, "源文件不存在", startMs);
            return;
        }
        Path sourcePath = resolvePhysicalPath(sourceFile);
        if (!Files.exists(sourcePath)) {
            failTask(task, "源文件物理路径不存在: " + sourcePath, startMs);
            return;
        }
        String targetFormat = task.getTargetFormat() != null ? task.getTargetFormat().toLowerCase() : "pdf";
        Path outDir = sourcePath.getParent();
        //update-begin---author:admin ---date:2026-08-04  for：优先调用本机 Microsoft Office，失败回退 LibreOffice-----------
        Path converted = officeConvertUtil.convert(sourcePath, outDir, targetFormat);
        //update-end---author:admin ---date:2026-08-04  for：优先调用本机 Microsoft Office，失败回退 LibreOffice-----------
        if (converted == null || !Files.exists(converted)) {
            failTask(task, "格式转换失败，请确认本机已安装 Microsoft Office 或 LibreOffice（soffice）", startMs);
            return;
        }
        String storedName = converted.getFileName().toString();
        String objectKey = "homeai/" + sourceFile.getUserId() + "/" + storedName;
        task.setStatus("COMPLETED");
        task.setResultFileUrl(fileStorageService.storeLocalFile(converted, objectKey));
        task.setResultFileSize(Files.size(converted));
        task.setErrorMessage(null);
        finishTask(task, startMs);
    }

    private void finishTask(StorageConvertTask task, long startMs) {
        task.setTaskDuration((int) TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startMs));
        task.setCompletedAt(new Date());
        task.setUpdateTime(new Date());
        taskService.updateById(task);
        log.info("Office任务完成: taskId={}, type={}", task.getId(), task.getConvertType());
    }

    private Path resolvePhysicalPath(StorageFile file) {
        return fileStorageService.resolveLocalPath(file.getFileUrl());
    }

    private void failTask(StorageConvertTask task, String message, long startMs) {
        task.setStatus("FAILED");
        task.setErrorMessage(message);
        task.setTaskDuration((int) TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startMs));
        task.setCompletedAt(new Date());
        task.setUpdateTime(new Date());
        taskService.updateById(task);
        log.warn("Office任务失败: taskId={}, reason={}", task.getId(), message);
    }
}
