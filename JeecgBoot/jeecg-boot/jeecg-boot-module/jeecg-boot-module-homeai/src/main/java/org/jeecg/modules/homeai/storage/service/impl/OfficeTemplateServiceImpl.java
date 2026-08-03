package org.jeecg.modules.homeai.storage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.homeai.storage.entity.OfficeTemplate;
import org.jeecg.modules.homeai.storage.mapper.OfficeTemplateMapper;
import org.jeecg.modules.homeai.storage.service.IOfficeTemplateService;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class OfficeTemplateServiceImpl extends ServiceImpl<OfficeTemplateMapper, OfficeTemplate>
        implements IOfficeTemplateService {

    @Override
    public List<OfficeTemplate> getEnabledTemplates(String type) {
        LambdaQueryWrapper<OfficeTemplate> query = new LambdaQueryWrapper<>();
        query.eq(OfficeTemplate::getType, type)
                .orderByDesc(OfficeTemplate::getIsDefault)
                .orderByAsc(OfficeTemplate::getCreateTime);
        return list(query);
    }

    @Override
    public void setDefault(String id) {
        OfficeTemplate template = getById(id);
        if (template == null) return;
        // 清除同类型的默认
        LambdaQueryWrapper<OfficeTemplate> query = new LambdaQueryWrapper<>();
        query.eq(OfficeTemplate::getType, template.getType())
                .eq(OfficeTemplate::getIsDefault, "1");
        List<OfficeTemplate> defaults = list(query);
        defaults.forEach(d -> { d.setIsDefault("0"); updateById(d); });
        // 设为默认
        template.setIsDefault("1");
        updateById(template);
    }
}
