package org.jeecg.modules.homeai.recipe.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.homeai.config.HomeaiFileMagicUtil;
import org.jeecg.modules.homeai.config.HomeaiUploadLimitService;
import org.jeecg.modules.homeai.config.service.IHomeaiFileStorageService;
import org.jeecg.modules.homeai.config.service.IHomeaiFileWhitelistService;
import org.jeecg.modules.homeai.learn.entity.LearnCategory;
import org.jeecg.modules.homeai.learn.service.ILearnCategoryService;
import org.jeecg.modules.homeai.recipe.entity.*;
import org.jeecg.modules.homeai.recipe.mapper.LearnMaterialMapper;
import org.jeecg.modules.homeai.recipe.mapper.LearnRecordMapper;
import org.jeecg.modules.homeai.user.entity.WxUser;
import org.jeecg.modules.homeai.user.mapper.WxUserMapper;
import org.jeecg.modules.homeai.recipe.service.ILearnService;
import org.jeecg.modules.homeai.learn.util.LearnMaterialAccess;
import org.jeecg.modules.homeai.learn.util.LearnRecordAssembler;
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
    //update-begin---author:cursor---date:2026-08-21---for:【HomeAI-R63】学习上传白名单与单文件上限-----------
    @Autowired private IHomeaiFileWhitelistService whitelistService;
    @Autowired private HomeaiUploadLimitService uploadLimitService;
    //update-end---author:cursor---date:2026-08-21---for:【HomeAI-R63】学习上传白名单与单文件上限-----------
    //update-begin---author:copilot ---date:2026-08-12 for：【第15轮】学习按分类统计-----------
    @Autowired private ILearnCategoryService learnCategoryService;
    //update-end---author:copilot ---date:2026-08-12 for：【第15轮】学习按分类统计-----------
    private static final String CACHE_LEARN_STATS = "homeai:cache:learn:stats:%s";
    private static final long CACHE_LEARN_TTL = 600;
    /** 学习计时会话 key，TTL 24h 防泄漏 */
    private static final String LEARN_SESSION_KEY = "homeai:learn:session:%s:%s";
    private static final String LEARN_ACTIVE_USER_KEY = "homeai:learn:active:%s";
    private static final long LEARN_SESSION_TTL = 86400;

    private String sessionKey(String userId, String materialId) {
        return String.format(LEARN_SESSION_KEY, userId, materialId);
    }

    //update-begin---author:cursor---date:2026-08-21---for:【HomeAI-R64】计时/记录校验资料归属-----------
    private void assertCanUseMaterial(String userId, String materialId) {
        LearnMaterialAccess.assertCanUse(getById(materialId), userId);
    }
    //update-end---author:cursor---date:2026-08-21---for:【HomeAI-R64】计时/记录校验资料归属-----------

    @Override
    public void startLearn(String userId, String materialId) {
        //update-begin---author:cursor---date:2026-08-21---for:【HomeAI-R64】计时/记录校验资料归属-----------
        assertCanUseMaterial(userId, materialId);
        //update-end---author:cursor---date:2026-08-21---for:【HomeAI-R64】计时/记录校验资料归属-----------
        redisUtil.set(sessionKey(userId, materialId), System.currentTimeMillis(), LEARN_SESSION_TTL);
        redisUtil.set(String.format(LEARN_ACTIVE_USER_KEY, userId), materialId, LEARN_SESSION_TTL);
    }

    @Override
    public LearnRecord stopLearn(String userId, String materialId) {
        //update-begin---author:cursor---date:2026-08-21---for:【HomeAI-R64】计时/记录校验资料归属-----------
        assertCanUseMaterial(userId, materialId);
        //update-end---author:cursor---date:2026-08-21---for:【HomeAI-R64】计时/记录校验资料归属-----------
        Object raw = redisUtil.get(sessionKey(userId, materialId));
        redisUtil.del(sessionKey(userId, materialId));
        redisUtil.del(String.format(LEARN_ACTIVE_USER_KEY, userId));
        Long startMs = raw != null ? Long.parseLong(raw.toString()) : null;
        //update-begin---author:cursor---date:2026-08-20---for:【Android体验】结束学习统一写入 study_date---
        LearnRecord rec = LearnRecordAssembler.fromTimerSession(userId, materialId, startMs, new Date());
        //update-end---author:cursor---date:2026-08-20---for:【Android体验】结束学习统一写入 study_date---
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
        //update-begin---author:cursor---date:2026-08-21---for:【HomeAI-R64】计时/记录校验资料归属-----------
        assertCanUseMaterial(userId, materialId);
        //update-end---author:cursor---date:2026-08-21---for:【HomeAI-R64】计时/记录校验资料归属-----------
        LearnRecord rec = new LearnRecord();
        rec.setUserId(userId);
        rec.setMaterialId(materialId);
        rec.setDuration(duration);
        rec.setMode(recordType != null && !recordType.isEmpty() ? recordType : "timer");
        rec.setCreateTime(new Date());
        //update-begin---author:cursor---date:2026-08-20---for:【Android体验】写入 study_date 避免旧库无默认值插入失败---
        rec.setStudyDate(java.sql.Date.valueOf(LocalDate.now()));
        //update-end---author:cursor---date:2026-08-20---for:【Android体验】写入 study_date 避免旧库无默认值插入失败---
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
        return adminStats(0, null);
    }

    //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R29】多维统计-----------
    @Override
    public Map<String, Object> adminStats(int days, String userId) {
        List<LearnRecord> records = listRecordsInRange(days, userId);
        Map<String, Object> stats = new LinkedHashMap<>();
        int totalDuration = records.stream().mapToInt(r -> r.getDuration() != null ? r.getDuration() : 0).sum();
        java.util.Set<String> users = new java.util.HashSet<>();
        java.util.Set<String> activeDays = new java.util.HashSet<>();
        for (LearnRecord r : records) {
            if (r.getUserId() != null) users.add(r.getUserId());
            if (r.getCreateTime() != null) {
                LocalDate d = r.getCreateTime().toInstant()
                        .atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                activeDays.add(d.toString());
            }
        }
        stats.put("totalRecords", records.size());
        stats.put("totalDurationMinutes", totalDuration / 60);
        stats.put("activeUserCount", users.size());
        stats.put("activeDayCount", activeDays.size());
        stats.put("days", days <= 0 ? 0 : Math.min(days, 90));
        return stats;
    }

    @Override
    public List<Map<String, Object>> adminStatsByUser(int days) {
        List<LearnRecord> records = listRecordsInRange(days, null);
        Map<String, int[]> buckets = new LinkedHashMap<>();
        Map<String, java.util.Set<String>> activeDays = new HashMap<>();
        for (LearnRecord r : records) {
            if (oConvertUtils.isEmpty(r.getUserId())) continue;
            int[] b = buckets.computeIfAbsent(r.getUserId(), k -> new int[]{0, 0});
            b[0]++;
            b[1] += r.getDuration() != null ? r.getDuration() : 0;
            if (r.getCreateTime() != null) {
                LocalDate d = r.getCreateTime().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                activeDays.computeIfAbsent(r.getUserId(), k -> new java.util.HashSet<>()).add(d.toString());
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, int[]> e : buckets.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("userId", e.getKey());
            WxUser u = wxUserMapper.selectById(e.getKey());
            row.put("nickname", u != null && oConvertUtils.isNotEmpty(u.getNickname()) ? u.getNickname() : e.getKey());
            row.put("recordCount", e.getValue()[0]);
            row.put("durationMinutes", e.getValue()[1] / 60);
            row.put("activeDays", activeDays.getOrDefault(e.getKey(), Collections.emptySet()).size());
            result.add(row);
        }
        result.sort((a, b) -> Integer.compare(
                ((Number) b.get("durationMinutes")).intValue(),
                ((Number) a.get("durationMinutes")).intValue()));
        return result;
    }

    private List<LearnRecord> listRecordsInRange(int days, String userId) {
        LambdaQueryWrapper<LearnRecord> q = new LambdaQueryWrapper<>();
        if (oConvertUtils.isNotEmpty(userId)) {
            q.eq(LearnRecord::getUserId, userId);
        }
        if (days > 0) {
            int range = Math.min(days, 90);
            LocalDate start = LocalDate.now().minusDays(range - 1L);
            q.ge(LearnRecord::getCreateTime, java.sql.Date.valueOf(start));
        }
        return recordMapper.selectList(q);
    }
    //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R29】多维统计-----------

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

    //update-begin---author:copilot ---date:2026-08-12 for：【第15轮】学习按分类统计-----------
    @Override
    public List<Map<String, Object>> getAdminStatsByCategory() {
        return getAdminStatsByCategory(0, null);
    }

    //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R29】分类统计支持日期/用户-----------
    @Override
    public List<Map<String, Object>> getAdminStatsByCategory(int days, String userId) {
        final String uncategorizedKey = "__uncategorized__";
        List<LearnMaterial> materials = list();
        Map<String, LearnMaterial> materialById = new HashMap<>();
        Map<String, int[]> buckets = new LinkedHashMap<>();
        for (LearnMaterial m : materials) {
            if (m.getId() != null) {
                materialById.put(m.getId(), m);
            }
            String key = normalizeCategoryKey(m.getCategoryId(), uncategorizedKey);
            int[] bucket = buckets.computeIfAbsent(key, k -> new int[]{0, 0, 0});
            bucket[0]++;
        }
        List<LearnRecord> records = listRecordsInRange(days, userId);
        for (LearnRecord r : records) {
            LearnMaterial m = r.getMaterialId() != null ? materialById.get(r.getMaterialId()) : null;
            if (m == null && r.getMaterialId() != null) {
                m = getById(r.getMaterialId());
                if (m != null) {
                    materialById.put(m.getId(), m);
                }
            }
            String key = m != null
                    ? normalizeCategoryKey(m.getCategoryId(), uncategorizedKey)
                    : uncategorizedKey;
            int[] bucket = buckets.computeIfAbsent(key, k -> new int[]{0, 0, 0});
            bucket[1]++;
            bucket[2] += r.getDuration() != null ? r.getDuration() : 0;
        }

        Map<String, String> categoryNames = new HashMap<>();
        List<LearnCategory> categories = learnCategoryService.list();
        for (LearnCategory c : categories) {
            if (c.getId() != null) {
                categoryNames.put(c.getId(), c.getName());
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, int[]> e : buckets.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            String key = e.getKey();
            boolean uncategorized = uncategorizedKey.equals(key);
            row.put("categoryId", uncategorized ? null : key);
            row.put("categoryName", uncategorized ? "未分类" : categoryNames.getOrDefault(key, "未知分类"));
            row.put("materialCount", e.getValue()[0]);
            row.put("recordCount", e.getValue()[1]);
            row.put("totalDuration", e.getValue()[2] / 60);
            result.add(row);
        }
        result.sort((a, b) -> {
            int cmp = Integer.compare(
                    ((Number) b.get("recordCount")).intValue(),
                    ((Number) a.get("recordCount")).intValue());
            if (cmp != 0) return cmp;
            return Integer.compare(
                    ((Number) b.get("materialCount")).intValue(),
                    ((Number) a.get("materialCount")).intValue());
        });
        return result;
    }
    //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R29】分类统计支持日期/用户-----------

    private String normalizeCategoryKey(String categoryId, String uncategorizedKey) {
        return oConvertUtils.isEmpty(categoryId) ? uncategorizedKey : categoryId.trim();
    }
    //update-end---author:copilot ---date:2026-08-12 for：【第15轮】学习按分类统计-----------

    //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R29】每日目标-----------
    private static final String REDIS_LEARN_GOAL = "homeai:learn:goal:";
    private static final int DEFAULT_DAILY_GOAL_MINUTES = 30;

    @Override
    public int getDailyGoalMinutes(String userId) {
        if (oConvertUtils.isEmpty(userId)) {
            return DEFAULT_DAILY_GOAL_MINUTES;
        }
        Object v = redisUtil.get(REDIS_LEARN_GOAL + userId);
        if (v == null) {
            return DEFAULT_DAILY_GOAL_MINUTES;
        }
        try {
            int m = Integer.parseInt(String.valueOf(v));
            return Math.max(5, Math.min(m, 480));
        } catch (Exception e) {
            return DEFAULT_DAILY_GOAL_MINUTES;
        }
    }

    @Override
    public void setDailyGoalMinutes(String userId, int minutes) {
        if (oConvertUtils.isEmpty(userId)) {
            throw new JeecgBootException("未登录");
        }
        int m = Math.max(5, Math.min(minutes, 480));
        redisUtil.set(REDIS_LEARN_GOAL + userId, String.valueOf(m));
    }

    @Override
    public Map<String, Object> getTodayProgress(String userId) {
        int goal = getDailyGoalMinutes(userId);
        LocalDate today = LocalDate.now();
        List<LearnRecord> records = recordMapper.selectList(new LambdaQueryWrapper<LearnRecord>()
                .eq(LearnRecord::getUserId, userId)
                .ge(LearnRecord::getCreateTime, java.sql.Date.valueOf(today)));
        int seconds = records.stream().mapToInt(r -> r.getDuration() != null ? r.getDuration() : 0).sum();
        int todayMinutes = seconds / 60;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("goalMinutes", goal);
        result.put("todayMinutes", todayMinutes);
        result.put("progressPercent", goal > 0 ? Math.min(100, todayMinutes * 100.0 / goal) : 0);
        result.put("reached", todayMinutes >= goal);
        return result;
    }
    //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R29】每日目标-----------

    @Override
    public List<String> getLearnCalendarDates(String userId, String yearMonth) {
        LocalDate start = LocalDate.parse(yearMonth + "-01");
        LocalDate end = start.plusMonths(1);
        LambdaQueryWrapper<LearnRecord> q = new LambdaQueryWrapper<>();
        //update-begin---author:cursor---date:2026-08-20---for:【审查修复】学习日历按月用 study_date 查库---
        q.eq(LearnRecord::getUserId, userId)
                .ge(LearnRecord::getStudyDate, java.sql.Date.valueOf(start))
                .lt(LearnRecord::getStudyDate, java.sql.Date.valueOf(end))
                .orderByAsc(LearnRecord::getStudyDate);
        List<LearnRecord> records = recordMapper.selectList(q);
        java.util.TreeSet<String> dates = new java.util.TreeSet<>();
        for (LearnRecord r : records) {
            if (r.getStudyDate() == null) continue;
            dates.add(new java.sql.Date(r.getStudyDate().getTime()).toLocalDate().toString());
        }
        //update-end---author:cursor---date:2026-08-20---for:【审查修复】学习日历按月用 study_date 查库---
        return new ArrayList<>(dates);
    }

    /** 资料类型与允许的文件扩展名 */
    private static final Map<String, Set<String>> TYPE_EXTENSIONS = Map.of(
            "video", Set.of("mp4", "avi", "mov", "webm", "mkv"),
            "audio", Set.of("mp3", "wav", "m4a", "aac"),
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
        //update-begin---author:cursor---date:2026-08-21---for:【HomeAI-R63】学习上传对齐白名单与单文件上限-----------
        if (!whitelistService.isAllowedExtension(ext)) {
            throw new JeecgBootException("不支持上传该文件类型");
        }
        uploadLimitService.assertAllowed(ext, file.getSize());
        //update-end---author:cursor---date:2026-08-21---for:【HomeAI-R63】学习上传对齐白名单与单文件上限-----------
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
