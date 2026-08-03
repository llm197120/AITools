package org.jeecg.modules.homeai.recipe.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.homeai.config.HomeaiFileUrlUtil;
import org.jeecg.modules.homeai.recipe.entity.*;
import org.jeecg.modules.homeai.recipe.mapper.LearnMaterialMapper;
import org.jeecg.modules.homeai.recipe.mapper.LearnRecordMapper;
import org.jeecg.modules.homeai.recipe.service.ILearnService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@Slf4j
@Service
public class LearnServiceImpl extends ServiceImpl<LearnMaterialMapper, LearnMaterial> implements ILearnService {
    @Autowired private LearnRecordMapper recordMapper;
    private final Map<String, Date> activeSessions = new HashMap<>();

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
