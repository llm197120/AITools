package org.jeecg.modules.homeai.recipe.service;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.homeai.recipe.entity.LearnMaterial;
import org.jeecg.modules.homeai.recipe.entity.LearnRecord;
import org.springframework.web.multipart.MultipartFile;
import com.baomidou.mybatisplus.core.metadata.IPage;
import java.util.List;
import java.util.Map;

public interface ILearnService extends IService<LearnMaterial> {
    void startLearn(String userId, String materialId);
    LearnRecord stopLearn(String userId, String materialId);
    List<LearnRecord> getUserRecords(String userId);
    /** 用户学习统计（记录数、总时长-分钟） */
    Map<String, Object> getUserStatistics(String userId);
    /** 手动记录一次学习 */
    LearnRecord addRecord(String userId, String materialId, int duration, String recordType);
    String uploadMaterialFile(String materialId, MultipartFile file);
    /** 新增资料前预上传文件，返回可访问 URL */
    String uploadTempFile(MultipartFile file, String type);
    /** 校验上传文件扩展名与资料类型是否匹配 */
    void validateFileFormat(String type, MultipartFile file);

    /** 管理端：分页查询学习记录（含用户昵称、资料标题） */
    IPage<Map<String, Object>> adminListRecords(Integer pageNo, Integer pageSize, String userId);

    /** 管理端：学习统计（总记录数、总时长、活跃天数、活跃用户数） */
    Map<String, Object> adminStats();

    //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R29】多维统计-----------
    /** 管理端：按近 N 日 / 用户筛选的汇总（days<=0 表示全量） */
    Map<String, Object> adminStats(int days, String userId);

    /** 管理端：按用户排行 */
    List<Map<String, Object>> adminStatsByUser(int days);
    //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R29】多维统计-----------

    /** 管理端：近 N 日学习趋势 */
    List<Map<String, Object>> adminStatsTrend(int days);

    //update-begin---author:copilot ---date:2026-08-12 for：【第15轮】学习按分类统计-----------
    /**
     * 管理端：按分类学习统计。
     * 返回字段：categoryId、categoryName、materialCount、recordCount、totalDuration。
     * totalDuration 单位为分钟（与 {@link #adminStats()} 的 totalDurationMinutes、
     * {@link #adminStatsTrend(int)} 的 durationMinutes 一致；库内 duration 存秒，此处汇总后 /60）。
     */
    List<Map<String, Object>> getAdminStatsByCategory();

    //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R29】分类统计支持日期/用户-----------
    List<Map<String, Object>> getAdminStatsByCategory(int days, String userId);
    //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R29】分类统计支持日期/用户-----------
    //update-end---author:copilot ---date:2026-08-12 for：【第15轮】学习按分类统计-----------

    /** 学习日历：某月有学习记录的日期 */
    List<String> getLearnCalendarDates(String userId, String yearMonth);

    /** 当前进行中的学习会话（Redis 持久化，服务重启不丢失） */
    Map<String, Object> getActiveSession(String userId);

    //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R29】每日目标-----------
    /** 获取用户每日学习目标（分钟） */
    int getDailyGoalMinutes(String userId);

    /** 设置用户每日学习目标（分钟） */
    void setDailyGoalMinutes(String userId, int minutes);

    /** 今日进度：goalMinutes / todayMinutes / progressPercent */
    Map<String, Object> getTodayProgress(String userId);
    //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R29】每日目标-----------
}
