package org.jeecg.modules.homeai.recipe.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.homeai.config.HomeaiFileUrlUtil;
import org.jeecg.modules.homeai.recipe.entity.*;
import org.jeecg.modules.homeai.recipe.mapper.LearnMaterialMapper;
import org.jeecg.modules.homeai.recipe.mapper.LearnRecordMapper;
import org.jeecg.modules.homeai.user.entity.WxUser;
import org.jeecg.modules.homeai.user.mapper.WxUserMapper;
import org.jeecg.modules.homeai.recipe.service.ILearnService;
import org.jeecg.common.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

@Slf4j
@Service
public class LearnServiceImpl extends ServiceImpl<LearnMaterialMapper, LearnMaterial> implements ILearnService {
    @Autowired private LearnRecordMapper recordMapper;
    @Autowired private WxUserMapper wxUserMapper;
    @Autowired private RedisUtil redisUtil;
    private final Map<String, Date> activeSessions = new HashMap<>();

    /** 学习统计缓存 key + TTL(10分钟) */
    private static final String CACHE_LEARN_STATS = "homeai:cache:learn:stats:%s";
    private static final long CACHE_LEARN_TTL = 600;

    @Value("${jeecg.path.upload:./upload}")
    private String uploadPath;

    @Override
    public void startLearn(String userId, String materialId) {
        activeSessions.put(userId + "_" + materialId, new Date());
    }

    @Override
    public LearnRecord stopLearn(String userId, String materialId) {
        Date start = activeSessions.remove(userId + "_" + materialId);
        Date end = new Date();
        int duration = start != null ? (int)((end.getTime() - start.getTime()) / 1000) : 0;
        LearnRecord rec = new LearnRecord();
        rec.setUserId(userId);
        rec.setMaterialId(materialId);
        rec.setStartTime(start);
        rec.setEndTime(end);
        rec.setDuration(duration);
        rec.setMode("timer");
        rec.setCreateTime(end);
        recordMapper.insert(rec);
        return rec;
    }

    @Override
    public List<LearnRecord> getUserRecords(String userId) {
        LambdaQueryWrapper<LearnRecord> q = new LambdaQueryWrapper<>();
        q.eq(LearnRecord::getUserId, userId).orderByDesc(LearnRecord::getCreateTime);
        return recordMapper.selectList(q);
    }

    @Override
    public Map<String, Object> getUserStatistics(String userId) {
        String cacheKey = String.format(CACHE_LEARN_STATS, userId);
        Object cached = redisUtil.get(cacheKey);
        if (cached instanceof Map) {
            return (Map<String, Object>) cached;
        }
        List<LearnRecord> records = getUserRecords(userId);
        Map<String, Object> result = new HashMap<>();
        int totalSeconds = records.stream()
                .mapToInt(r -> r.getDuration() != null ? r.getDuration() : 0)
                .sum();
        result.put("totalRecords", records.size());
        result.put("totalDuration", totalSeconds / 60);
        redisUtil.set(cacheKey, result, CACHE_LEARN_TTL);
        return result;
    }

    @Override
    public LearnRecord addRecord(String userId, String materialId, int duration, String recordType) {
        LearnRecord rec = new LearnRecord();
        rec.setUserId(userId);
        rec.setMaterialId(materialId);
        rec.setDuration(duration);
        rec.setMode(recordType != null && !recordType.isEmpty() ? recordType : "timer");
        rec.setCreateTime(new Date());
        recordMapper.insert(rec);
        redisUtil.del(String.format(CACHE_LEARN_STATS, userId));
        return rec;
    }

    @Override
    public IPage<Map<String, Object>> adminListRecords(Integer pageNo, Integer pageSize, String userId) {
        Page<LearnRecord> page = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<LearnRecord> q = new LambdaQueryWrapper<>();
        if (userId != null && !userId.isEmpty()) {
            q.eq(LearnRecord::getUserId, userId);
        }
        q.orderByDesc(LearnRecord::getCreateTime);
        IPage<LearnRecord> records = recordMapper.selectPage(page, q);
        IPage<Map<String, Object>> result = new Page<>(pageNo, pageSize, records.getTotal());
        List<Map<String, Object>> rows = new ArrayList<>();
        for (LearnRecord r : records.getRecords()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", r.getId());
            row.put("userId", r.getUserId());
            row.put("materialId", r.getMaterialId());
            row.put("duration", r.getDuration());
            row.put("mode", r.getMode());
            row.put("createTime", r.getCreateTime());
            WxUser u = wxUserMapper.selectById(r.getUserId());
            row.put("nickname", u != null ? u.getNickname() : "未知");
            LearnMaterial m = getById(r.getMaterialId());
            row.put("materialTitle", m != null ? m.getTitle() : "未知资料");
            rows.add(row);
        }
        result.setRecords(rows);
        return result;
    }

    @Override
    public Map<String, Object> adminStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        List<LearnRecord> records = recordMapper.selectList(null);
        int totalDuration = records.stream().mapToInt(r -> r.getDuration() != null ? r.getDuration() : 0).sum();
        java.util.Set<String> users = new java.util.HashSet<>();
        java.util.Set<String> activeDays = new java.util.HashSet<>();
        for (LearnRecord r : records) {
            if (r.getUserId() != null) users.add(r.getUserId());
            if (r.getCreateTime() != null) {
                java.time.LocalDate d = r.getCreateTime().toInstant()
                        .atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                activeDays.add(d.toString());
            }
        }
        stats.put("totalRecords", records.size());
        stats.put("totalDurationMinutes", totalDuration / 60);
        stats.put("activeUserCount", users.size());
        stats.put("activeDayCount", activeDays.size());
        return stats;
    }

    @Override
    public String uploadMaterialFile(String materialId, MultipartFile file) {
        try {
            String dir = uploadPath + "/homeai/learn/" + materialId + "/";
            Files.createDirectories(Path.of(dir));
            String ext = getExtension(file.getOriginalFilename());
            String fileName = UUID.randomUUID().toString().replace("-", "") + (ext != null ? "." + ext : "");
            Path targetPath = Path.of(dir + fileName);
            file.transferTo(targetPath.toFile());
            return HomeaiFileUrlUtil.toAbsoluteUrl("/upload/homeai/learn/" + materialId + "/" + fileName);
        } catch (IOException e) {
            log.error("学习资料文件上传失败", e);
            throw new RuntimeException("文件上传失败", e);
        }
    }

    private String getExtension(String filename) {
        if (filename == null) return null;
        int idx = filename.lastIndexOf('.');
        return idx >= 0 ? filename.substring(idx + 1) : null;
    }
}
