package org.jeecg.modules.homeai.config.service;

import org.jeecg.modules.homeai.config.dto.HomeaiPlanConfigDto;

public interface IHomeaiPlanConfigService {

    HomeaiPlanConfigDto getConfig();

    void saveConfig(HomeaiPlanConfigDto config);

    int getRepeatHorizonDays();

    int getInstanceCleanupDays();

    boolean isRemindEnabled();

    boolean isAiDocPolishEnabled();
}
