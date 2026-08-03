package org.jeecg.modules.homeai.storage.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.homeai.storage.entity.StorageConvertTask;

import java.util.List;

/**
 * Office转换任务 Service
 */
public interface IStorageConvertTaskService extends IService<StorageConvertTask> {

    /** 提交转换任务 */
    StorageConvertTask submitConvertTask(String userId, String fileId,
                                         String sourceFormat, String targetFormat);

    /** 提交AI生成任务 */
    StorageConvertTask submitGenerateTask(String userId, String fileId, String instruction);

    /** 查询任务状态 */
    StorageConvertTask getTaskStatus(String id);

    /** 获取用户的转换历史 */
    List<StorageConvertTask> getUserHistory(String userId);
}
