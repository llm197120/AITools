package org.jeecg.modules.homeai.storage.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.homeai.storage.entity.OfficeTemplate;

/**
 * 文档模板 Service
 */
public interface IOfficeTemplateService extends IService<OfficeTemplate> {

    /** 获取启用的模板列表 */
    java.util.List<OfficeTemplate> getEnabledTemplates(String type);

    /** 设为默认模板 */
    void setDefault(String id);
}
