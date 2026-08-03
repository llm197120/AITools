package org.jeecg.modules.homeai.storage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.homeai.storage.entity.ConvertRule;
import org.jeecg.modules.homeai.storage.mapper.ConvertRuleMapper;
import org.jeecg.modules.homeai.storage.service.IConvertRuleService;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class ConvertRuleServiceImpl extends ServiceImpl<ConvertRuleMapper, ConvertRule>
        implements IConvertRuleService {

    @Override
    public List<ConvertRule> getTargetFormats(String sourceFormat) {
        LambdaQueryWrapper<ConvertRule> query = new LambdaQueryWrapper<>();
        query.eq(ConvertRule::getSourceFormat, sourceFormat)
                .eq(ConvertRule::getIsEnabled, "1");
        return list(query);
    }

    @Override
    public List<ConvertRule> getEnabledRules() {
        LambdaQueryWrapper<ConvertRule> query = new LambdaQueryWrapper<>();
        query.eq(ConvertRule::getIsEnabled, "1");
        return list(query);
    }
}
