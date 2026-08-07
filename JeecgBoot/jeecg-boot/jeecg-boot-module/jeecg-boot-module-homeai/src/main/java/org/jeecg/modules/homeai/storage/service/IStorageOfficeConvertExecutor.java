package org.jeecg.modules.homeai.storage.service;



/**

 * Office 格式转换异步执行器

 */

public interface IStorageOfficeConvertExecutor {



    /** 异步执行指定任务 */

    void executeAsync(String taskId);



    /** 处理所有待执行任务（定时兜底） */

    void processPendingTasks();

}

