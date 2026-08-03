package org.jeecg.modules.homeai.storage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.homeai.storage.entity.StorageConvertTask;
import org.jeecg.modules.homeai.storage.mapper.StorageConvertTaskMapper;
import org.jeecg.modules.homeai.storage.service.IStorageConvertTaskService;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class StorageConvertTaskServiceImpl extends ServiceImpl<StorageConvertTaskMapper, StorageConvertTask>
        implements IStorageConvertTaskService {

    @Override
    public StorageConvertTask submitConvertTask(String userId, String fileId,
                                                 String sourceFormat, String targetFormat) {
        StorageConvertTask task = new StorageConvertTask();
        task.setUserId(userId);
        task.setFileId(fileId);
        task.setConvertType("format_convert");
        task.setSourceFormat(sourceFormat);
        task.setTargetFormat(targetFormat);
        task.setStatus("PENDING");
        task.setCreateTime(new Date());
        task.setUpdateTime(new Date());
        save(task);
        log.info("提交格式转换任务: fileId={}, {} -> {}, taskId={}", fileId, sourceFormat, targetFormat, task.getId());
        return task;
    }

    @Override
    public StorageConvertTask submitGenerateTask(String userId, String fileId, String instruction) {
        StorageConvertTask task = new StorageConvertTask();
        task.setUserId(userId);
        task.setFileId(fileId);
        task.setConvertType("ai_generate");
        task.setStatus("PENDING");
        task.setCreateTime(new Date());
        task.setUpdateTime(new Date());
        save(task);
        log.info("提交AI生成任务: fileId={}, instruction={}, taskId={}", fileId, instruction, task.getId());
        return task;
    }

    @Override
    public StorageConvertTask getTaskStatus(String id) {
        return getById(id);
    }

    @Override
    public List<StorageConvertTask> getUserHistory(String userId) {
        LambdaQueryWrapper<StorageConvertTask> query = new LambdaQueryWrapper<>();
        query.eq(StorageConvertTask::getUserId, userId)
                .orderByDesc(StorageConvertTask::getCreateTime);
        return list(query);
    }
}
