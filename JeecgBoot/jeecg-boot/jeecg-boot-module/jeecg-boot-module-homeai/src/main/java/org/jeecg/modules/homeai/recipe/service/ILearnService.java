package org.jeecg.modules.homeai.recipe.service;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.homeai.recipe.entity.LearnMaterial;
import org.jeecg.modules.homeai.recipe.entity.LearnRecord;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface ILearnService extends IService<LearnMaterial> {
    void startLearn(String userId, String materialId);
    LearnRecord stopLearn(String userId, String materialId);
    List<LearnRecord> getUserRecords(String userId);
    String uploadMaterialFile(String materialId, MultipartFile file);
}
