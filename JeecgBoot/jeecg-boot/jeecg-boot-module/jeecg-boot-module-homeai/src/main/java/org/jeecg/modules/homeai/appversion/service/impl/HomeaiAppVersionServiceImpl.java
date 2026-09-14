package org.jeecg.modules.homeai.appversion.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.homeai.appversion.dto.HomeaiAppVersionPublicDto;
import org.jeecg.modules.homeai.appversion.entity.HomeaiAppVersion;
import org.jeecg.modules.homeai.appversion.mapper.HomeaiAppVersionMapper;
import org.jeecg.modules.homeai.appversion.service.HomeaiAppPackageCache;
import org.jeecg.modules.homeai.appversion.service.IHomeaiAppVersionService;
import org.jeecg.modules.homeai.appversion.util.HomeaiAppPackageDownloadPaths;
import org.jeecg.modules.homeai.config.HomeaiFileMagicUtil;
import org.jeecg.modules.homeai.config.HomeaiFileUrlUtil;
import org.jeecg.modules.homeai.config.service.IHomeaiFileStorageService;
import org.jeecg.modules.homeai.preview.HomeaiPreviewKind;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
public class HomeaiAppVersionServiceImpl extends ServiceImpl<HomeaiAppVersionMapper, HomeaiAppVersion>
        implements IHomeaiAppVersionService {

    private static final long MAX_APK_BYTES = 200L * 1024 * 1024;
    private static final long MAX_ZIP_BYTES = 80L * 1024 * 1024;

    @Autowired
    private IHomeaiFileStorageService fileStorageService;

    @Autowired
    private HomeaiAppPackageCache appPackageCache;

    @Override
    public HomeaiAppVersion requireCurrent() {
        HomeaiAppVersion row = getById(HomeaiAppVersion.CURRENT_ID);
        if (row != null) {
            return row;
        }
        HomeaiAppVersion seed = new HomeaiAppVersion();
        seed.setId(HomeaiAppVersion.CURRENT_ID);
        seed.setVersionName("1.0.0");
        seed.setVersionCode(100);
        seed.setUpdateMode("apk");
        seed.setForceUpdate(0);
        seed.setMinShellCode(100);
        seed.setEnabled(0);
        seed.setChangelog("当前内测版本，未开放自动更新");
        save(seed);
        return seed;
    }

    @Override
    public HomeaiAppVersionPublicDto toPublic(HomeaiAppVersion row) {
        HomeaiAppVersionPublicDto dto = new HomeaiAppVersionPublicDto();
        dto.setVersionName(row.getVersionName());
        dto.setVersionCode(row.getVersionCode() == null ? 0 : row.getVersionCode());
        dto.setUpdateMode(normalizeMode(row.getUpdateMode()));
        dto.setForceUpdate(row.getForceUpdate() != null && row.getForceUpdate() == 1);
        //update-begin---author:cursor---date:2026-09-04---for:【APP热更新】H5 zip 与 APK 一样走后端代理下载---
        dto.setApkUrl(packageDownloadUrl(row.getApkUrl(), false));
        dto.setResourceUrl(packageDownloadUrl(row.getResourceUrl(), true));
        //update-end---author:cursor---date:2026-09-04---for:【APP热更新】H5 zip 与 APK 一样走后端代理下载---
        dto.setApkSha256(blankToNull(row.getApkSha256()));
        dto.setResourceSha256(blankToNull(row.getResourceSha256()));
        dto.setMinShellCode(row.getMinShellCode() == null ? 0 : row.getMinShellCode());
        dto.setChangelog(row.getChangelog());
        dto.setEnabled(row.getEnabled() != null && row.getEnabled() == 1);
        return dto;
    }

    @Override
    public HomeaiAppVersion toAdminView(HomeaiAppVersion row) {
        HomeaiAppVersion view = new HomeaiAppVersion();
        view.setId(row.getId());
        view.setVersionName(row.getVersionName());
        view.setVersionCode(row.getVersionCode());
        view.setUpdateMode(normalizeMode(row.getUpdateMode()));
        view.setForceUpdate(row.getForceUpdate());
        //update-begin---author:cursor---date:2026-09-04---for:【APP热更新】H5 zip 与 APK 一样走后端代理下载---
        view.setApkUrl(packageDownloadUrl(row.getApkUrl(), false));
        view.setResourceUrl(packageDownloadUrl(row.getResourceUrl(), true));
        //update-end---author:cursor---date:2026-09-04---for:【APP热更新】H5 zip 与 APK 一样走后端代理下载---
        view.setApkSha256(row.getApkSha256());
        view.setResourceSha256(row.getResourceSha256());
        view.setMinShellCode(row.getMinShellCode());
        view.setChangelog(row.getChangelog());
        view.setEnabled(row.getEnabled());
        view.setUpdateTime(row.getUpdateTime());
        return view;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveCurrent(HomeaiAppVersion body) {
        if (body == null) {
            throw new JeecgBootException("参数不能为空");
        }
        if (oConvertUtils.isEmpty(body.getVersionName())) {
            throw new JeecgBootException("请填写版本号");
        }
        if (body.getVersionCode() == null || body.getVersionCode() < 1) {
            throw new JeecgBootException("versionCode 必须为正整数");
        }
        String mode = normalizeMode(body.getUpdateMode());
        HomeaiAppVersion current = requireCurrent();
        current.setVersionName(body.getVersionName().trim());
        current.setVersionCode(body.getVersionCode());
        current.setUpdateMode(mode);
        current.setForceUpdate(body.getForceUpdate() != null && body.getForceUpdate() == 1 ? 1 : 0);
        current.setMinShellCode(body.getMinShellCode() == null ? current.getVersionCode() : body.getMinShellCode());
        current.setChangelog(body.getChangelog());
        current.setEnabled(body.getEnabled() != null && body.getEnabled() == 1 ? 1 : 0);
        //update-begin---author:cursor---date:2026-09-03---for:【HomeAI-R126】管理端展示的是代理下载 URL，保存时不得覆盖真实 OSS 引用---
        if (oConvertUtils.isNotEmpty(body.getApkUrl())
                && !HomeaiAppPackageDownloadPaths.isProxyUrl(body.getApkUrl())) {
            current.setApkUrl(fileStorageService.normalizeStoredReference(body.getApkUrl()));
        }
        if (oConvertUtils.isNotEmpty(body.getResourceUrl())
                && !HomeaiAppPackageDownloadPaths.isProxyUrl(body.getResourceUrl())) {
            current.setResourceUrl(fileStorageService.normalizeStoredReference(body.getResourceUrl()));
        }
        //update-end---author:cursor---date:2026-09-03---for:【HomeAI-R126】管理端展示的是代理下载 URL，保存时不得覆盖真实 OSS 引用---
        if (oConvertUtils.isNotEmpty(body.getApkSha256())) {
            current.setApkSha256(body.getApkSha256().trim().toLowerCase(Locale.ROOT));
        }
        if (oConvertUtils.isNotEmpty(body.getResourceSha256())) {
            current.setResourceSha256(body.getResourceSha256().trim().toLowerCase(Locale.ROOT));
        }
        updateById(current);
    }

    @Override
    public Map<String, String> uploadPackage(MultipartFile file, String kind) {
        if (file == null || file.isEmpty()) {
            throw new JeecgBootException("请选择文件");
        }
        String type = kind == null ? "" : kind.trim().toLowerCase(Locale.ROOT);
        if (!"apk".equals(type) && !"resource".equals(type)) {
            throw new JeecgBootException("kind 只能是 apk 或 resource");
        }
        String ext = HomeaiPreviewKind.extensionOfNameOrUrl(file.getOriginalFilename());
        if ("apk".equals(type)) {
            if (!"apk".equals(ext)) {
                throw new JeecgBootException("请上传 .apk 文件");
            }
            if (file.getSize() > MAX_APK_BYTES) {
                throw new JeecgBootException("APK 不能超过 200MB");
            }
        } else {
            if (!"zip".equals(ext)) {
                throw new JeecgBootException("热更新包请上传根目录含 index.html 的 zip");
            }
            if (file.getSize() > MAX_ZIP_BYTES) {
                throw new JeecgBootException("H5 zip 不能超过 80MB");
            }
        }
        try {
            HomeaiFileMagicUtil.validate(file, ext);
        } catch (IOException e) {
            throw new JeecgBootException(e.getMessage());
        }
        Path tmp = null;
        try {
            tmp = Files.createTempFile("homeai-app-", "." + ext);
            Files.deleteIfExists(tmp);
            file.transferTo(tmp.toFile());
            String sha = sha256Hex(tmp);
            //update-begin---author:cursor---date:2026-08-31---for:【APP更新】apk 对象由后端代理接口（SDK 拉流）分发，存储 key 恢复 .apk 后缀（预签名直链仍被 OSS 限制）---
            String objectKey = "homeai/app-version/" + type + "-" + System.currentTimeMillis() + "." + ext;
            //update-end---author:cursor---date:2026-08-31---for:【APP更新】apk 对象由后端代理接口（SDK 拉流）分发---
            String stored = fileStorageService.storeLocalFile(tmp, objectKey);
            //update-begin---author:cursor---date:2026-09-03---for:【HomeAI-R127】上传成功即写入服务器本地缓存---
            if ("apk".equals(type)) {
                appPackageCache.rememberApk(tmp, sha, stored);
            } else {
                appPackageCache.rememberZip(tmp, sha, stored);
            }
            //update-end---author:cursor---date:2026-09-03---for:【HomeAI-R127】上传成功即写入服务器本地缓存---
            Map<String, String> result = new HashMap<>();
            result.put("url", fileStorageService.resolveAccessUrl(stored));
            result.put("stored", stored);
            result.put("sha256", sha);
            result.put("kind", type);
            return result;
        } catch (JeecgBootException e) {
            throw e;
        } catch (Exception e) {
            log.warn("APP 安装包上传失败", e);
            throw new JeecgBootException("上传失败");
        } finally {
            if (tmp != null) {
                try {
                    Files.deleteIfExists(tmp);
                } catch (IOException ignored) {
                    // ignore
                }
            }
        }
    }

    private static String normalizeMode(String mode) {
        if ("resource".equalsIgnoreCase(mode)) {
            return "resource";
        }
        return "apk";
    }

    //update-begin---author:cursor---date:2026-09-04---for:【APP热更新】apk/zip 下载地址统一走后端代理接口---
    /**
     * 安装包下载地址：OSS 默认域名禁止预签名 URL 直链分发部分类型，统一返回后端代理下载接口。
     * 未配置存储引用时返回 null。
     */
    private String packageDownloadUrl(String storedReference, boolean resource) {
        if (oConvertUtils.isEmpty(storedReference)) {
            return null;
        }
        String path = resource ? HomeaiAppPackageDownloadPaths.RESOURCE : HomeaiAppPackageDownloadPaths.APK;
        return HomeaiFileUrlUtil.toAbsoluteUrl(path);
    }
    //update-end---author:cursor---date:2026-09-04---for:【APP热更新】apk/zip 下载地址统一走后端代理接口---

    private static String blankToNull(String value) {
        return oConvertUtils.isEmpty(value) ? null : value;
    }

    private static String sha256Hex(Path file) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (InputStream in = Files.newInputStream(file)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                md.update(buf, 0, n);
            }
        }
        byte[] digest = md.digest();
        StringBuilder sb = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
