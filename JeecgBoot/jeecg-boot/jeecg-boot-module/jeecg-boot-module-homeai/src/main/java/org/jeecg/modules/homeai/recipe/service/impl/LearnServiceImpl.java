package org.jeecg.modules.homeai.recipe.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.homeai.config.HomeaiFileMagicUtil;
import org.jeecg.modules.homeai.config.service.IHomeaiFileStorageService;
import org.jeecg.modules.homeai.recipe.entity.*;
import org.jeecg.modules.homeai.recipe.mapper.LearnMaterialMapper;
import org.jeecg.modules.homeai.recipe.mapper.LearnRecordMapper;
import org.jeecg.modules.homeai.user.entity.WxUser;
import org.jeecg.modules.homeai.user.mapper.WxUserMapper;
import org.jeecg.modules.homeai.recipe.service.ILearnService;
import org.jeecg.common.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

@Slf4j
@Service
public class LearnServiceImpl extends ServiceImpl<LearnMaterialMapper, LearnMaterial> implements ILearnService {
    @Autowired private LearnRecordMapper recordMapper;
    @Autowired private WxUserMapper wxUserMapper;
    @Autowired private RedisUtil redisUtil;
    @Autowired private IHomeaiFileStorageService fileStorageService;
    private static final String CACHE_LEARN_STATS = "homeai:cache:learn:stats:%s";
    private static final long CACHE_LEARN_TTL = 600;
    /** 学习计时会话 key，TTL 24h 防泄漏 */
    private static final String LEARN_SESSION_KEY = "homeai:learn:session:%s:%s";
    private static final String LEARN_ACTIVE_USER_KEY = "homeai:learn:active:%s";
    private static final long LEARN_SESSION_TTL = 86400;

    private String sessionKey(String userId, String materialId) {
        return String.format(LEARN_SESSION_KEY, userId, materialId);
    }

    @Override
    public void startLearn(String userId, String materialId) {
        redisUtil.set(sessionKey(userId, materialId), System.currentTimeMillis(), LEARN_SESSION_TTL);
        redisUtil.set(String.format(LEARN_ACTIVE_USER_KEY, userId), materialId, LEARN_SESSION_TTL);
    }

    @Override
    public LearnRecord stopLearn(String userId, String materialId) {
        Object raw = redisUtil.get(sessionKey(userId, materialId));
        redisUtil.del(sessionKey(userId, materialId));
        redisUtil.del(String.format(LEARN_ACTIVE_USER_KEY, userId));
        Date start = raw != null ? new Date(Long.parseLong(raw.toString())) : null;
        Date end = new Date();
        int duration = start != null ? (int) ((end.getTime() - start.getTime()) / 1000) : 0;
        LearnRecord rec = new LearnRecord();
        rec.setUserId(userId);
        rec.setMaterialId(materialId);
        rec.setStartTime(start);
        rec.setEndTime(end);
        rec.setDuration(duration);
        rec.setMode("timer");
        rec.setCreateTime(end);
        recordMapper.insert(rec);
        redisUtil.del(String.format(CACHE_LEARN_STATS, userId));
        return rec;
    }

    @Override
    public Map<String, Object> getActiveSession(String userId) {
        Map<String, Object> result = new LinkedHashMap<>();
        Object activeMaterialId = redisUtil.get(String.format(LEARN_ACTIVE_USER_KEY, userId));
        if (activeMaterialId == null) {
            return result;
        }
        String materialId = activeMaterialId.toString();
        Object raw = redisUtil.get(sessionKey(userId, materialId));
        if (raw == null) {
            redisUtil.del(String.format(LEARN_ACTIVE_USER_KEY, userId));
            return result;
        }
        long startMs = Long.parseLong(raw.toString());
        LearnMaterial m = getById(materialId);
        result.put("materialId", materialId);
        result.put("materialTitle", m != null ? m.getTitle() : "未知资料");
        result.put("startTime", new Date(startMs));
        result.put("elapsedSeconds", (int) ((System.currentTimeMillis() - startMs) / 1000));
        return result;
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
    public List<Map<String, Object>> adminStatsTrend(int days) {
        int range = days <= 0 ? 30 : Math.min(days, 90);
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(range - 1L);
        List<LearnRecord> records = recordMapper.selectList(new LambdaQueryWrapper<LearnRecord>()
                .ge(LearnRecord::getCreateTime, java.sql.Date.valueOf(start))
                .orderByAsc(LearnRecord::getCreateTime));
        Map<String, int[]> daily = new LinkedHashMap<>();
        for (int i = 0; i < range; i++) {
            daily.put(start.plusDays(i).toString(), new int[]{0, 0});
        }
        for (LearnRecord r : records) {
            if (r.getCreateTime() == null) continue;
            LocalDate d = r.getCreateTime().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
            if (d.isBefore(start) || d.isAfter(end)) continue;
            int[] bucket = daily.get(d.toString());
            if (bucket == null) continue;
            bucket[0]++;
            bucket[1] += r.getDuration() != null ? r.getDuration() : 0;
        }
        List<Map<String, Object>> trend = new ArrayList<>();
        for (Map.Entry<String, int[]> e : daily.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", e.getKey());
            row.put("recordCount", e.getValue()[0]);
            row.put("durationMinutes", e.getValue()[1] / 60);
            trend.add(row);
        }
        return trend;
    }

    @Override
    public List<String> getLearnCalendarDates(String userId, String yearMonth) {
        LocalDate start = LocalDate.parse(yearMonth + "-01");
        LocalDate end = start.plusMonths(1);
        LambdaQueryWrapper<LearnRecord> q = new LambdaQueryWrapper<>();
        q.eq(LearnRecord::getUserId, userId).orderByAsc(LearnRecord::getCreateTime);
        List<LearnRecord> records = recordMapper.selectList(q);
        java.util.TreeSet<String> dates = new java.util.TreeSet<>();
        for (LearnRecord r : records) {
            if (r.getCreateTime() == null) continue;
            LocalDate d = r.getCreateTime().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
            if (!d.isBefore(start) && d.isBefore(end)) {
                dates.add(d.toString());
            }
        }
        return new ArrayList<>(dates);
    }

    /** 资料类型与允许的文件扩展名 */
    private static final Map<String, Set<String>> TYPE_EXTENSIONS = Map.of(
            "video", Set.of("mp4", "avi", "mov", "webm", "mkv", "flv", "wmv"),
            "image", Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp"),
            "pdf", Set.of("pdf"),
            "doc", Set.of("doc", "docx"),
            "xls", Set.of("xls", "xlsx"),
            "ppt", Set.of("ppt", "pptx"),
            "note", Set.of("txt", "md")
    );

    @Override
    public String uploadMaterialFile(String materialId, MultipartFile file) {
        LearnMaterial material = getById(materialId);
        if (material == null) {
            throw new JeecgBootException("学习资料不存在");
        }
        validateFileFormat(material.getType(), file);
        return saveLearnFile("homeai/learn/" + materialId + "/", file);
    }

    @Override
    public String uploadTempFile(MultipartFile file, String type) {
        validateFileFormat(type, file);
        return saveLearnFile("homeai/learn/temp/", file);
    }

    @Override
    public void validateFileFormat(String type, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new JeecgBootException("请选择要上传的文件");
        }
        if (oConvertUtils.isEmpty(type)) {
            throw new JeecgBootException("请先选择资料类型");
        }
        String normalizedType = type.trim().toLowerCase();
        if ("link".equals(normalizedType)) {
            throw new JeecgBootException("链接类型无需上传文件，请直接填写链接地址");
        }
        String ext = getExtension(file.getOriginalFilename());
        if (oConvertUtils.isEmpty(ext)) {
            throw new JeecgBootException("无法识别文件扩展名");
        }
        ext = ext.toLowerCase();
        Set<String> allowed = TYPE_EXTENSIONS.get(normalizedType);
        if (allowed == null) {
            throw new JeecgBootException("不支持的资料类型: " + type);
        }
        if (!allowed.contains(ext)) {
            throw new JeecgBootException("文件格式与资料类型不匹配，" + type + " 类型允许: "
                    + allowed.stream().sorted().collect(Collectors.joining(", ")));
        }
        try {
            HomeaiFileMagicUtil.validate(file, ext);
        } catch (IOException e) {
            throw new JeecgBootException(e.getMessage());
        }
    }

    private String saveLearnFile(String relativeDir, MultipartFile file) {
        try {
            String ext = getExtension(file.getOriginalFilename());
            String fileName = UUID.randomUUID().toString().replace("-", "") + (ext != null ? "." + ext : "");
            String objectKey = relativeDir + fileName;
            return fileStorageService.storeMultipart(file, objectKey);
        } catch (Exception e) {
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
