package org.jeecg.modules.homeai.config.service;

import org.jeecg.modules.homeai.config.dto.HomeaiSysConfigDto;

public interface IHomeaiSysConfigService {

    HomeaiSysConfigDto getConfig();

    void saveConfig(HomeaiSysConfigDto config);

    long uploadLimitOf(String category);

    boolean isLearnRemindEnabled();

    String getLearnRemindCron();

    HomeaiSysConfigDto.Wechat getWechat();

    HomeaiSysConfigDto.Office getOffice();

    boolean isPrivateOssBucket();

    long getPresignExpireSeconds();

    String getFileBaseUrl();

    String getFileScheme();

    String getFileHost();
}
