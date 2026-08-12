package org.jeecg.modules.homeai.config.service;

import org.jeecg.modules.homeai.config.dto.HomeaiStorageConfigDto;

public interface IHomeaiStorageConfigService {

    HomeaiStorageConfigDto getConfig();

    void saveConfig(HomeaiStorageConfigDto config);

    long getDefaultUserLimitBytes();

    //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R28】家庭默认配额-----------
    long getDefaultFamilyLimitBytes();
    //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R28】家庭默认配额-----------

    //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R30】家庭级配额覆盖-----------
    /** 解析家庭配额：有 Redis 覆盖则用覆盖值，否则默认家庭配额 */
    long getFamilyLimitBytes(String familyId);

    /** 是否存在家庭级覆盖 */
    boolean hasFamilyLimitOverride(String familyId);

    /** 设置家庭级覆盖（字节）；传 null 或 <=0 则清除 */
    void setFamilyLimitBytes(String familyId, Long limitBytes);

    /** 清除家庭级覆盖 */
    void clearFamilyLimitBytes(String familyId);
    //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R30】家庭级配额覆盖-----------

    int getWarnPercent();
}
