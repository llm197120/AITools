package org.jeecg.modules.homeai.config.util;

import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.homeai.config.dto.HomeaiSysConfigDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HomeaiSysConfigNormalizeTest {

    @Test
    void overlayKeepsYmlWhenStoredEmpty() {
        HomeaiSysConfigDto defaults = defaults();
        HomeaiSysConfigDto merged = HomeaiSysConfigNormalize.merge(defaults, new HomeaiSysConfigDto());
        assertEquals(209715200L, merged.getUpload().getVideo());
        assertEquals("0 0 20 * * ?", merged.getLearn().getRemindCron());
    }

    @Test
    void overlayUploadAndWechat() {
        HomeaiSysConfigDto stored = new HomeaiSysConfigDto();
        HomeaiSysConfigDto.Upload upload = new HomeaiSysConfigDto.Upload();
        upload.setVideo(100L * 1024 * 1024);
        stored.setUpload(upload);
        HomeaiSysConfigDto.Wechat wechat = new HomeaiSysConfigDto.Wechat();
        wechat.setPlanRemindTemplateId("tpl-plan");
        stored.setWechat(wechat);
        HomeaiSysConfigDto merged = HomeaiSysConfigNormalize.merge(defaults(), stored);
        assertEquals(100L * 1024 * 1024, merged.getUpload().getVideo());
        assertEquals("tpl-plan", merged.getWechat().getPlanRemindTemplateId());
        assertEquals("thing1", merged.getWechat().getLearnRemindTitleField());
    }

    @Test
    void overlayGotenbergUrl() {
        HomeaiSysConfigDto stored = new HomeaiSysConfigDto();
        HomeaiSysConfigDto.Office office = new HomeaiSysConfigDto.Office();
        office.setGotenbergUrl("http://127.0.0.1:3000");
        stored.setOffice(office);
        HomeaiSysConfigDto merged = HomeaiSysConfigNormalize.merge(defaults(), stored);
        assertEquals("http://127.0.0.1:3000", merged.getOffice().getGotenbergUrl());
    }

    @Test
    void rejectBadCronOnSave() {
        HomeaiSysConfigDto dto = HomeaiSysConfigNormalize.merge(defaults(), null);
        dto.getLearn().setRemindCron("bad");
        assertThrows(JeecgBootException.class, () -> HomeaiSysConfigNormalize.normalizeForSave(dto, defaults()));
    }

    @Test
    void acceptValidCronOnSave() {
        HomeaiSysConfigDto dto = HomeaiSysConfigNormalize.merge(defaults(), null);
        dto.getLearn().setRemindCron("0 30 8 * * ?");
        HomeaiSysConfigNormalize.normalizeForSave(dto, defaults());
        assertTrue(HomeaiLearnRemindCron.isValid(dto.getLearn().getRemindCron()));
    }

    @Test
    void rejectPublicGotenbergOnSave() {
        HomeaiSysConfigDto dto = HomeaiSysConfigNormalize.merge(defaults(), null);
        dto.getOffice().setGotenbergUrl("https://gotenberg.example.com");
        assertThrows(JeecgBootException.class, () -> HomeaiSysConfigNormalize.normalizeForSave(dto, defaults()));
    }

    private static HomeaiSysConfigDto defaults() {
        HomeaiSysConfigDto dto = new HomeaiSysConfigDto();
        HomeaiSysConfigDto.Upload upload = new HomeaiSysConfigDto.Upload();
        upload.setVideo(209715200L);
        upload.setAudio(52428800L);
        upload.setImage(20971520L);
        upload.setDocument(52428800L);
        upload.setArchive(104857600L);
        upload.setText(10485760L);
        dto.setUpload(upload);
        HomeaiSysConfigDto.Learn learn = new HomeaiSysConfigDto.Learn();
        learn.setRemindEnabled(true);
        learn.setRemindCron("0 0 20 * * ?");
        dto.setLearn(learn);
        HomeaiSysConfigDto.Wechat wechat = new HomeaiSysConfigDto.Wechat();
        wechat.setPlanRemindTemplateId("");
        wechat.setLearnRemindTemplateId("");
        wechat.setLearnRemindTitleField("thing1");
        wechat.setLearnRemindProgressField("number2");
        wechat.setLearnRemindGoalField("number3");
        wechat.setLearnRemindDateField("time4");
        wechat.setLearnRemindTitleText("每日学习目标");
        dto.setWechat(wechat);
        HomeaiSysConfigDto.Office office = new HomeaiSysConfigDto.Office();
        office.setPreferMsOffice(true);
        office.setSofficePath("soffice");
        office.setPowershellPath("powershell");
        office.setConvertTimeoutSeconds(120);
        dto.setOffice(office);
        HomeaiSysConfigDto.Oss oss = new HomeaiSysConfigDto.Oss();
        oss.setPrivateBucket(true);
        oss.setPresignExpireSeconds(7200L);
        dto.setOss(oss);
        HomeaiSysConfigDto.FileUrl file = new HomeaiSysConfigDto.FileUrl();
        file.setBaseUrl("");
        file.setScheme("http");
        file.setHost("127.0.0.1");
        dto.setFile(file);
        return dto;
    }
}
