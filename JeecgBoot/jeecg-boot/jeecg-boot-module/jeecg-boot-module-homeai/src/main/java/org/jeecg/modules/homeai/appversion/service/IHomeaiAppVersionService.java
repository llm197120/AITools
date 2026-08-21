package org.jeecg.modules.homeai.appversion.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.homeai.appversion.dto.HomeaiAppVersionPublicDto;
import org.jeecg.modules.homeai.appversion.entity.HomeaiAppVersion;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface IHomeaiAppVersionService extends IService<HomeaiAppVersion> {

    HomeaiAppVersion requireCurrent();

    HomeaiAppVersionPublicDto toPublic(HomeaiAppVersion row);

    HomeaiAppVersion toAdminView(HomeaiAppVersion row);

    void saveCurrent(HomeaiAppVersion body);

    Map<String, String> uploadPackage(MultipartFile file, String kind);
}
