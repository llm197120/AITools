package org.jeecg.modules.homeai.config.service.impl;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.homeai.config.dto.HomeaiSysConfigDto;
import org.jeecg.modules.homeai.config.entity.HomeaiSysConfig;
import org.jeecg.modules.homeai.config.mapper.HomeaiSysConfigMapper;
import org.jeecg.modules.homeai.config.service.IHomeaiPlanConfigService;
import org.jeecg.modules.homeai.config.service.IHomeaiStorageConfigService;
import org.jeecg.modules.homeai.config.service.IHomeaiSysConfigService;
import org.jeecg.modules.homeai.config.util.HomeaiSysConfigNormalize;
import org.jeecg.modules.homeai.preview.HomeaiPreviewKind;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Slf4j
@Service
public class HomeaiSysConfigServiceImpl implements IHomeaiSysConfigService {

    private static final String REDIS_KEY = "homeai:config:sys";

    @Value("${homeai.upload.limits.video:209715200}")
    private long videoLimit;
    @Value("${homeai.upload.limits.audio:52428800}")
    private long audioLimit;
    @Value("${homeai.upload.limits.image:20971520}")
    private long imageLimit;
    @Value("${homeai.upload.limits.document:52428800}")
    private long documentLimit;
    @Value("${homeai.upload.limits.archive:104857600}")
    private long archiveLimit;
    @Value("${homeai.upload.limits.text:10485760}")
    private long textLimit;

    @Value("${homeai.learn.remind-enabled:true}")
    private boolean learnRemindEnabled;
    @Value("${homeai.learn.remind-cron:0 0 20 * * ?}")
    private String learnRemindCron;

    @Value("${homeai.wechat.plan-remind-template-id:}")
    private String planRemindTemplateId;
    @Value("${homeai.wechat.learn-remind-template-id:}")
    private String learnRemindTemplateId;
    @Value("${homeai.wechat.learn-remind-title-field:thing1}")
    private String learnRemindTitleField;
    @Value("${homeai.wechat.learn-remind-progress-field:number2}")
    private String learnRemindProgressField;
    @Value("${homeai.wechat.learn-remind-goal-field:number3}")
    private String learnRemindGoalField;
    @Value("${homeai.wechat.learn-remind-date-field:time4}")
    private String learnRemindDateField;
    @Value("${homeai.wechat.learn-remind-title-text:每日学习目标}")
    private String learnRemindTitleText;

    @Value("${homeai.office.prefer-ms-office:true}")
    private boolean preferMsOffice;
    @Value("${homeai.office.soffice-path:soffice}")
    private String sofficePath;
    @Value("${homeai.office.powershell-path:powershell}")
    private String powershellPath;
    @Value("${homeai.office.convert-timeout-seconds:120}")
    private int convertTimeoutSeconds;
    @Value("${homeai.office.gotenberg-url:}")
    private String gotenbergUrl;
    @Value("${homeai.office.kkfileview-url:}")
    private String kkFileViewUrl;

    @Value("${homeai.oss.private-bucket:true}")
    private boolean privateOssBucket;
    @Value("${homeai.oss.presign-expire-seconds:7200}")
    private long presignExpireSeconds;

    @Value("${homeai.file.base-url:}")
    private String fileBaseUrl;
    @Value("${homeai.file.scheme:http}")
    private String fileScheme;
    @Value("${homeai.file.host:127.0.0.1}")
    private String fileHost;

    @Autowired
    private HomeaiSysConfigMapper mapper;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private IHomeaiPlanConfigService planConfigService;

    @Autowired
    private IHomeaiStorageConfigService storageConfigService;

    @Override
    public HomeaiSysConfigDto getConfig() {
        HomeaiSysConfigDto merged = runtime();
        merged.setPlan(planConfigService.getConfig());
        merged.setStorage(storageConfigService.getConfig());
        return merged;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveConfig(HomeaiSysConfigDto config) {
        HomeaiSysConfigDto defaults = buildDefaults();
        HomeaiSysConfigDto toSave = config != null ? config : new HomeaiSysConfigDto();
        HomeaiSysConfigNormalize.normalizeForSave(toSave, defaults);

        HomeaiSysConfigDto persist = new HomeaiSysConfigDto();
        persist.setUpload(toSave.getUpload());
        persist.setLearn(toSave.getLearn());
        persist.setWechat(toSave.getWechat());
        persist.setOffice(toSave.getOffice());
        persist.setOss(toSave.getOss());
        persist.setFile(toSave.getFile());

        Date now = new Date();
        HomeaiSysConfig row = mapper.selectById(HomeaiSysConfig.CURRENT_ID);
        if (row == null) {
            row = new HomeaiSysConfig();
            row.setId(HomeaiSysConfig.CURRENT_ID);
            row.setCreateTime(now);
            row.setContent(JSON.toJSONString(persist));
            row.setUpdateTime(now);
            mapper.insert(row);
        } else {
            row.setContent(JSON.toJSONString(persist));
            row.setUpdateTime(now);
            mapper.updateById(row);
        }
        redisUtil.del(REDIS_KEY);

        if (toSave.getPlan() != null) {
            planConfigService.saveConfig(toSave.getPlan());
        }
        if (toSave.getStorage() != null) {
            storageConfigService.saveConfig(toSave.getStorage());
        }
    }

    private HomeaiSysConfigDto runtime() {
        return HomeaiSysConfigNormalize.merge(buildDefaults(), loadStored());
    }

    @Override
    public long uploadLimitOf(String category) {
        HomeaiSysConfigDto.Upload u = runtime().getUpload();
        if (u == null) {
            return documentLimit;
        }
        if (oConvertUtils.isEmpty(category)) {
            return nvl(u.getDocument(), documentLimit);
        }
        switch (category) {
            case HomeaiPreviewKind.VIDEO:
                return nvl(u.getVideo(), videoLimit);
            case HomeaiPreviewKind.AUDIO:
                return nvl(u.getAudio(), audioLimit);
            case HomeaiPreviewKind.IMAGE:
                return nvl(u.getImage(), imageLimit);
            case HomeaiPreviewKind.ARCHIVE:
                return nvl(u.getArchive(), archiveLimit);
            case HomeaiPreviewKind.TEXT:
                return nvl(u.getText(), textLimit);
            default:
                return nvl(u.getDocument(), documentLimit);
        }
    }

    @Override
    public boolean isLearnRemindEnabled() {
        HomeaiSysConfigDto.Learn learn = runtime().getLearn();
        Boolean v = learn == null ? null : learn.getRemindEnabled();
        return v == null ? learnRemindEnabled : v;
    }

    @Override
    public String getLearnRemindCron() {
        HomeaiSysConfigDto.Learn learn = runtime().getLearn();
        String cron = learn == null ? null : learn.getRemindCron();
        return oConvertUtils.isEmpty(cron) ? learnRemindCron : cron;
    }

    @Override
    public HomeaiSysConfigDto.Wechat getWechat() {
        return runtime().getWechat();
    }

    @Override
    public HomeaiSysConfigDto.Office getOffice() {
        return runtime().getOffice();
    }

    @Override
    public boolean isPrivateOssBucket() {
        HomeaiSysConfigDto.Oss oss = runtime().getOss();
        Boolean v = oss == null ? null : oss.getPrivateBucket();
        return v == null ? privateOssBucket : v;
    }

    @Override
    public long getPresignExpireSeconds() {
        HomeaiSysConfigDto.Oss oss = runtime().getOss();
        Long v = oss == null ? null : oss.getPresignExpireSeconds();
        return HomeaiSysConfigNormalize.clampPresign(v, presignExpireSeconds);
    }

    @Override
    public String getFileBaseUrl() {
        HomeaiSysConfigDto.FileUrl file = runtime().getFile();
        return file == null ? fileBaseUrl : oConvertUtils.getString(file.getBaseUrl(), "");
    }

    @Override
    public String getFileScheme() {
        HomeaiSysConfigDto.FileUrl file = runtime().getFile();
        return file == null ? fileScheme : oConvertUtils.getString(file.getScheme(), fileScheme);
    }

    @Override
    public String getFileHost() {
        HomeaiSysConfigDto.FileUrl file = runtime().getFile();
        return file == null ? fileHost : oConvertUtils.getString(file.getHost(), fileHost);
    }

    private HomeaiSysConfigDto loadStored() {
        Object cached = redisUtil.get(REDIS_KEY);
        if (cached != null) {
            try {
                return JSON.parseObject(String.valueOf(cached), HomeaiSysConfigDto.class);
            } catch (Exception e) {
                log.warn("解析系统配置缓存失败", e);
            }
        }
        HomeaiSysConfig row = mapper.selectById(HomeaiSysConfig.CURRENT_ID);
        if (row == null || oConvertUtils.isEmpty(row.getContent())) {
            return null;
        }
        try {
            HomeaiSysConfigDto stored = JSON.parseObject(row.getContent(), HomeaiSysConfigDto.class);
            redisUtil.set(REDIS_KEY, row.getContent());
            return stored;
        } catch (Exception e) {
            log.warn("解析系统配置失败，使用 yml 默认值", e);
            return null;
        }
    }

    private HomeaiSysConfigDto buildDefaults() {
        HomeaiSysConfigDto dto = new HomeaiSysConfigDto();
        HomeaiSysConfigDto.Upload upload = new HomeaiSysConfigDto.Upload();
        upload.setVideo(videoLimit);
        upload.setAudio(audioLimit);
        upload.setImage(imageLimit);
        upload.setDocument(documentLimit);
        upload.setArchive(archiveLimit);
        upload.setText(textLimit);
        dto.setUpload(upload);

        HomeaiSysConfigDto.Learn learn = new HomeaiSysConfigDto.Learn();
        learn.setRemindEnabled(learnRemindEnabled);
        learn.setRemindCron(learnRemindCron);
        dto.setLearn(learn);

        HomeaiSysConfigDto.Wechat wechat = new HomeaiSysConfigDto.Wechat();
        wechat.setPlanRemindTemplateId(nullToEmpty(planRemindTemplateId));
        wechat.setLearnRemindTemplateId(nullToEmpty(learnRemindTemplateId));
        wechat.setLearnRemindTitleField(learnRemindTitleField);
        wechat.setLearnRemindProgressField(learnRemindProgressField);
        wechat.setLearnRemindGoalField(learnRemindGoalField);
        wechat.setLearnRemindDateField(learnRemindDateField);
        wechat.setLearnRemindTitleText(learnRemindTitleText);
        dto.setWechat(wechat);

        HomeaiSysConfigDto.Office office = new HomeaiSysConfigDto.Office();
        office.setPreferMsOffice(preferMsOffice);
        office.setSofficePath(sofficePath);
        office.setPowershellPath(powershellPath);
        office.setConvertTimeoutSeconds(convertTimeoutSeconds);
        office.setGotenbergUrl(nullToEmpty(gotenbergUrl));
        office.setKkFileViewUrl(nullToEmpty(kkFileViewUrl));
        dto.setOffice(office);

        HomeaiSysConfigDto.Oss oss = new HomeaiSysConfigDto.Oss();
        oss.setPrivateBucket(privateOssBucket);
        oss.setPresignExpireSeconds(presignExpireSeconds);
        dto.setOss(oss);

        HomeaiSysConfigDto.FileUrl file = new HomeaiSysConfigDto.FileUrl();
        file.setBaseUrl(nullToEmpty(fileBaseUrl));
        file.setScheme(fileScheme);
        file.setHost(fileHost);
        dto.setFile(file);
        return dto;
    }

    private static long nvl(Long v, long fallback) {
        return v != null && v > 0 ? v : fallback;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
