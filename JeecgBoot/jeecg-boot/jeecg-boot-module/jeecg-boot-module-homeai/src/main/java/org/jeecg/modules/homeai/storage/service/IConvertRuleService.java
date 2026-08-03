package org.jeecg.modules.homeai.storage.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.homeai.storage.entity.ConvertRule;

import java.util.List;

/**
 * 转换规则 Service
 */
public interface IConvertRuleService extends IService<ConvertRule> {

    /** 获取某格式可转换的目标格式列表 */
    List<ConvertRule> getTargetFormats(String sourceFormat);

    /** 获取所有启用的规则 */
    List<ConvertRule> getEnabledRules();
}
