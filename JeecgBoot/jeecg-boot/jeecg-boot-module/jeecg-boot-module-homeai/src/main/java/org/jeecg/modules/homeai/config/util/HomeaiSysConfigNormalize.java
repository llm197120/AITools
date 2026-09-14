package org.jeecg.modules.homeai.config.util;

import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.homeai.config.dto.HomeaiSysConfigDto;
import org.jeecg.modules.homeai.storage.util.HomeaiLocalHttpUrl;

/**
 * 系统配置合并与边界校验（yml 默认 + 库中覆盖）。
 */
public final class HomeaiSysConfigNormalize {

    public static final long MIN_UPLOAD = 1024L * 1024L;
    public static final long MAX_UPLOAD = 2L * 1024L * 1024L * 1024L;

    private HomeaiSysConfigNormalize() {
    }

    public static HomeaiSysConfigDto merge(HomeaiSysConfigDto defaults, HomeaiSysConfigDto stored) {
        HomeaiSysConfigDto out = copyDefaults(defaults);
        if (stored == null) {
            return out;
        }
        overlayUpload(out.getUpload(), stored.getUpload());
        overlayLearn(out.getLearn(), stored.getLearn());
        overlayWechat(out.getWechat(), stored.getWechat());
        overlayOffice(out.getOffice(), stored.getOffice());
        overlayOss(out.getOss(), stored.getOss());
        overlayFile(out.getFile(), stored.getFile());
        return out;
    }

    public static void normalizeForSave(HomeaiSysConfigDto dto, HomeaiSysConfigDto defaults) {
        HomeaiSysConfigDto merged = merge(defaults, dto);
        dto.setUpload(merged.getUpload());
        dto.setLearn(merged.getLearn());
        dto.setWechat(merged.getWechat());
        dto.setOffice(merged.getOffice());
        dto.setOss(merged.getOss());
        dto.setFile(merged.getFile());
        if (!HomeaiLearnRemindCron.isValid(dto.getLearn().getRemindCron())) {
            throw new JeecgBootException("学习提醒 cron 无效，需为 Spring 6 段表达式，例如 0 0 20 * * ?");
        }
        assertPath(dto.getOffice().getSofficePath(), "LibreOffice 路径");
        assertPath(dto.getOffice().getPowershellPath(), "PowerShell 路径");
        assertOptionalHttpUrl(dto.getOffice().getGotenbergUrl(), "Gotenberg 地址");
        HomeaiLocalHttpUrl.requireLoopbackOrPrivate(dto.getOffice().getGotenbergUrl(), "Gotenberg 地址");
        assertOptionalHttpUrl(dto.getOffice().getKkFileViewUrl(), "kkFileView 地址");
        HomeaiLocalHttpUrl.requireLoopbackOrPrivate(dto.getOffice().getKkFileViewUrl(), "kkFileView 地址");
        assertBaseUrl(dto.getFile().getBaseUrl());
        assertHost(dto.getFile().getHost());
        String scheme = oConvertUtils.getString(dto.getFile().getScheme(), "http").trim().toLowerCase();
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            throw new JeecgBootException("文件外链协议只能是 http 或 https");
        }
        dto.getFile().setScheme(scheme);
    }

    public static long clampUpload(Long value, long fallback) {
        long v = value != null && value > 0 ? value : fallback;
        if (v < MIN_UPLOAD) {
            return MIN_UPLOAD;
        }
        if (v > MAX_UPLOAD) {
            return MAX_UPLOAD;
        }
        return v;
    }

    public static int clampTimeout(Integer value, int fallback) {
        int v = value != null ? value : fallback;
        return Math.max(30, Math.min(v, 600));
    }

    public static long clampPresign(Long value, long fallback) {
        long v = value != null && value > 0 ? value : fallback;
        return Math.max(300L, Math.min(v, 86400L));
    }

    private static HomeaiSysConfigDto copyDefaults(HomeaiSysConfigDto defaults) {
        HomeaiSysConfigDto out = new HomeaiSysConfigDto();
        out.setUpload(copyUpload(defaults == null ? null : defaults.getUpload()));
        out.setLearn(copyLearn(defaults == null ? null : defaults.getLearn()));
        out.setWechat(copyWechat(defaults == null ? null : defaults.getWechat()));
        out.setOffice(copyOffice(defaults == null ? null : defaults.getOffice()));
        out.setOss(copyOss(defaults == null ? null : defaults.getOss()));
        out.setFile(copyFile(defaults == null ? null : defaults.getFile()));
        return out;
    }

    private static void overlayUpload(HomeaiSysConfigDto.Upload target, HomeaiSysConfigDto.Upload src) {
        if (src == null) {
            return;
        }
        if (src.getVideo() != null) target.setVideo(clampUpload(src.getVideo(), target.getVideo()));
        if (src.getAudio() != null) target.setAudio(clampUpload(src.getAudio(), target.getAudio()));
        if (src.getImage() != null) target.setImage(clampUpload(src.getImage(), target.getImage()));
        if (src.getDocument() != null) target.setDocument(clampUpload(src.getDocument(), target.getDocument()));
        if (src.getArchive() != null) target.setArchive(clampUpload(src.getArchive(), target.getArchive()));
        if (src.getText() != null) target.setText(clampUpload(src.getText(), target.getText()));
    }

    private static void overlayLearn(HomeaiSysConfigDto.Learn target, HomeaiSysConfigDto.Learn src) {
        if (src == null) {
            return;
        }
        if (src.getRemindEnabled() != null) {
            target.setRemindEnabled(src.getRemindEnabled());
        }
        if (src.getRemindCron() != null) {
            target.setRemindCron(src.getRemindCron().trim());
        }
    }

    private static void overlayWechat(HomeaiSysConfigDto.Wechat target, HomeaiSysConfigDto.Wechat src) {
        if (src == null) {
            return;
        }
        if (src.getPlanRemindTemplateId() != null) target.setPlanRemindTemplateId(src.getPlanRemindTemplateId().trim());
        if (src.getLearnRemindTemplateId() != null) target.setLearnRemindTemplateId(src.getLearnRemindTemplateId().trim());
        if (src.getLearnRemindTitleField() != null) target.setLearnRemindTitleField(src.getLearnRemindTitleField().trim());
        if (src.getLearnRemindProgressField() != null) target.setLearnRemindProgressField(src.getLearnRemindProgressField().trim());
        if (src.getLearnRemindGoalField() != null) target.setLearnRemindGoalField(src.getLearnRemindGoalField().trim());
        if (src.getLearnRemindDateField() != null) target.setLearnRemindDateField(src.getLearnRemindDateField().trim());
        if (src.getLearnRemindTitleText() != null) target.setLearnRemindTitleText(src.getLearnRemindTitleText().trim());
    }

    private static void overlayOffice(HomeaiSysConfigDto.Office target, HomeaiSysConfigDto.Office src) {
        if (src == null) {
            return;
        }
        if (src.getPreferMsOffice() != null) target.setPreferMsOffice(src.getPreferMsOffice());
        if (src.getSofficePath() != null) target.setSofficePath(src.getSofficePath().trim());
        if (src.getPowershellPath() != null) target.setPowershellPath(src.getPowershellPath().trim());
        if (src.getConvertTimeoutSeconds() != null) {
            target.setConvertTimeoutSeconds(clampTimeout(src.getConvertTimeoutSeconds(), target.getConvertTimeoutSeconds()));
        }
        if (src.getGotenbergUrl() != null) {
            target.setGotenbergUrl(src.getGotenbergUrl().trim());
        }
        if (src.getKkFileViewUrl() != null) {
            target.setKkFileViewUrl(src.getKkFileViewUrl().trim());
        }
    }

    private static void overlayOss(HomeaiSysConfigDto.Oss target, HomeaiSysConfigDto.Oss src) {
        if (src == null) {
            return;
        }
        if (src.getPrivateBucket() != null) target.setPrivateBucket(src.getPrivateBucket());
        if (src.getPresignExpireSeconds() != null) {
            target.setPresignExpireSeconds(clampPresign(src.getPresignExpireSeconds(), target.getPresignExpireSeconds()));
        }
    }

    private static void overlayFile(HomeaiSysConfigDto.FileUrl target, HomeaiSysConfigDto.FileUrl src) {
        if (src == null) {
            return;
        }
        if (src.getBaseUrl() != null) target.setBaseUrl(src.getBaseUrl().trim());
        if (src.getScheme() != null) target.setScheme(src.getScheme().trim());
        if (src.getHost() != null) target.setHost(src.getHost().trim());
    }

    private static HomeaiSysConfigDto.Upload copyUpload(HomeaiSysConfigDto.Upload src) {
        HomeaiSysConfigDto.Upload t = new HomeaiSysConfigDto.Upload();
        if (src == null) {
            return t;
        }
        t.setVideo(src.getVideo());
        t.setAudio(src.getAudio());
        t.setImage(src.getImage());
        t.setDocument(src.getDocument());
        t.setArchive(src.getArchive());
        t.setText(src.getText());
        return t;
    }

    private static HomeaiSysConfigDto.Learn copyLearn(HomeaiSysConfigDto.Learn src) {
        HomeaiSysConfigDto.Learn t = new HomeaiSysConfigDto.Learn();
        if (src == null) {
            return t;
        }
        t.setRemindEnabled(src.getRemindEnabled());
        t.setRemindCron(src.getRemindCron());
        return t;
    }

    private static HomeaiSysConfigDto.Wechat copyWechat(HomeaiSysConfigDto.Wechat src) {
        HomeaiSysConfigDto.Wechat t = new HomeaiSysConfigDto.Wechat();
        if (src == null) {
            return t;
        }
        t.setPlanRemindTemplateId(src.getPlanRemindTemplateId());
        t.setLearnRemindTemplateId(src.getLearnRemindTemplateId());
        t.setLearnRemindTitleField(src.getLearnRemindTitleField());
        t.setLearnRemindProgressField(src.getLearnRemindProgressField());
        t.setLearnRemindGoalField(src.getLearnRemindGoalField());
        t.setLearnRemindDateField(src.getLearnRemindDateField());
        t.setLearnRemindTitleText(src.getLearnRemindTitleText());
        return t;
    }

    private static HomeaiSysConfigDto.Office copyOffice(HomeaiSysConfigDto.Office src) {
        HomeaiSysConfigDto.Office t = new HomeaiSysConfigDto.Office();
        if (src == null) {
            return t;
        }
        t.setPreferMsOffice(src.getPreferMsOffice());
        t.setSofficePath(src.getSofficePath());
        t.setPowershellPath(src.getPowershellPath());
        t.setConvertTimeoutSeconds(src.getConvertTimeoutSeconds());
        t.setGotenbergUrl(src.getGotenbergUrl());
        t.setKkFileViewUrl(src.getKkFileViewUrl());
        return t;
    }

    private static HomeaiSysConfigDto.Oss copyOss(HomeaiSysConfigDto.Oss src) {
        HomeaiSysConfigDto.Oss t = new HomeaiSysConfigDto.Oss();
        if (src == null) {
            return t;
        }
        t.setPrivateBucket(src.getPrivateBucket());
        t.setPresignExpireSeconds(src.getPresignExpireSeconds());
        return t;
    }

    private static HomeaiSysConfigDto.FileUrl copyFile(HomeaiSysConfigDto.FileUrl src) {
        HomeaiSysConfigDto.FileUrl t = new HomeaiSysConfigDto.FileUrl();
        if (src == null) {
            return t;
        }
        t.setBaseUrl(src.getBaseUrl());
        t.setScheme(src.getScheme());
        t.setHost(src.getHost());
        return t;
    }

    private static void assertOptionalHttpUrl(String url, String label) {
        if (oConvertUtils.isEmpty(url)) {
            return;
        }
        String v = url.trim();
        if (!v.startsWith("http://") && !v.startsWith("https://")) {
            throw new JeecgBootException(label + "须以 http:// 或 https:// 开头，或留空");
        }
        if (v.length() > 512) {
            throw new JeecgBootException(label + "过长");
        }
        if (v.indexOf('\n') >= 0 || v.indexOf('\r') >= 0
                || v.contains(";") || v.contains("|") || v.contains("`")) {
            throw new JeecgBootException(label + "含有非法字符");
        }
    }

    private static void assertPath(String path, String label) {
        if (oConvertUtils.isEmpty(path)) {
            throw new JeecgBootException(label + "不能为空");
        }
        if (path.indexOf('\n') >= 0 || path.indexOf('\r') >= 0
                || path.contains(";") || path.contains("|") || path.contains("&") || path.contains("`")) {
            throw new JeecgBootException(label + "含有非法字符");
        }
        if (path.length() > 260) {
            throw new JeecgBootException(label + "过长");
        }
    }

    private static void assertBaseUrl(String baseUrl) {
        if (oConvertUtils.isEmpty(baseUrl)) {
            return;
        }
        String v = baseUrl.trim();
        if (!v.startsWith("http://") && !v.startsWith("https://")) {
            throw new JeecgBootException("文件根地址须以 http:// 或 https:// 开头，或留空");
        }
        if (v.length() > 512) {
            throw new JeecgBootException("文件根地址过长");
        }
    }

    private static void assertHost(String host) {
        if (oConvertUtils.isEmpty(host)) {
            throw new JeecgBootException("文件外链主机不能为空");
        }
        if (host.contains("/") || host.contains(" ") || host.length() > 253) {
            throw new JeecgBootException("文件外链主机格式无效");
        }
    }
}
