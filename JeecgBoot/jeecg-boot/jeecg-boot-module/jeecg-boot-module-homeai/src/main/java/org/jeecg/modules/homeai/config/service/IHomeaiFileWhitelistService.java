package org.jeecg.modules.homeai.config.service;



import com.baomidou.mybatisplus.extension.service.IService;

import org.jeecg.modules.homeai.config.entity.HomeaiFileWhitelist;



import java.util.List;



public interface IHomeaiFileWhitelistService extends IService<HomeaiFileWhitelist> {



    /** 获取启用的扩展名列表（缓存 5 分钟） */

    List<String> getEnabledExtensions();



    /** 校验扩展名是否允许上传 */

    boolean isAllowedExtension(String extension);



    /** 批量更新白名单配置 */

    void replaceAll(List<HomeaiFileWhitelist> items);



    void evictCache();

}

