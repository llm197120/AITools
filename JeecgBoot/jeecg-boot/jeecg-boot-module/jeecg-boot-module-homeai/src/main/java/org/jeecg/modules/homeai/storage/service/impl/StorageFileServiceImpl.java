package org.jeecg.modules.homeai.storage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.homeai.config.HomeaiFileMagicUtil;
import org.jeecg.modules.homeai.config.service.IHomeaiFileStorageService;
import org.jeecg.modules.homeai.config.service.IHomeaiFileWhitelistService;
import org.jeecg.modules.homeai.config.service.IHomeaiStorageConfigService;
import org.jeecg.modules.homeai.family.entity.Family;
import org.jeecg.modules.homeai.family.entity.FamilyMember;
import org.jeecg.modules.homeai.family.service.IFamilyMemberService;
import org.jeecg.modules.homeai.family.service.IFamilyService;
import org.jeecg.modules.homeai.storage.constant.StorageVisibility;
import org.jeecg.modules.homeai.storage.util.StorageFileNameUtil;
import org.jeecg.modules.homeai.storage.util.StorageVisibilityQueryUtil;
import org.jeecg.modules.homeai.storage.entity.StorageFile;
import org.jeecg.modules.homeai.storage.mapper.StorageFileMapper;
import org.jeecg.modules.homeai.storage.service.IStorageFileService;
import org.jeecg.modules.homeai.storage.service.IStorageResourceFamilyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class StorageFileServiceImpl extends ServiceImpl<StorageFileMapper, StorageFile>
        implements IStorageFileService {

    /** 第 21 轮缩略图 POC：仅常见位图；webp 等无 ImageIO 插件时自动跳过 */
    private static final Set<String> THUMB_IMAGE_EXTS = Set.of("jpg", "jpeg", "png", "gif", "bmp");
    private static final int THUMB_MAX_EDGE = 200;

    @Autowired
    private IHomeaiFileWhitelistService whitelistService;

    @Autowired
    private IHomeaiFileStorageService fileStorageService;

    @Autowired
    private IStorageResourceFamilyService resourceFamilyService;

    @Autowired
    private IHomeaiStorageConfigService storageConfigService;

    @Autowired
    private IFamilyMemberService familyMemberService;

    //update-begin---author:admin ---date:2026-08-13 for：【HomeAI-R32】家庭配额看板-----------
    @Autowired
    private IFamilyService familyService;
    //update-end---author:admin ---date:2026-08-13 for：【HomeAI-R32】家庭配额看板-----------

    @Override
    public List<StorageFile> getFilesByFolder(String folderId) {
        return list(folderFilesQuery(folderId));
    }

    //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R21】资料列表分页-----------
    @Override
    public IPage<StorageFile> pageFilesByFolder(Page<StorageFile> page, String folderId) {
        return page(page, folderFilesQuery(folderId));
    }
    //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R21】资料列表分页-----------

    @Override
    public List<StorageFile> getRootFiles(String userId, String familyId) {
        return list(rootFilesQuery(userId, familyId));
    }

    //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R21】资料列表分页-----------
    @Override
    public IPage<StorageFile> pageRootFiles(Page<StorageFile> page, String userId, String familyId) {
        return page(page, rootFilesQuery(userId, familyId));
    }
    //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R21】资料列表分页-----------

    @Override
    public List<StorageFile> getAllRootFiles() {
        return list(allRootFilesQuery());
    }

    //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R21】资料列表分页-----------
    @Override
    public IPage<StorageFile> pageAllRootFiles(Page<StorageFile> page) {
        return page(page, allRootFilesQuery());
    }
    //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R21】资料列表分页-----------

    @Override
    public StorageFile uploadFile(String userId, String familyId, String folderId,
                                   MultipartFile file, String visibility, String fileName) {
        String originalName = resolveOriginalName(file, fileName);
        String ext = StorageFileNameUtil.extensionOf(originalName);
        if (!whitelistService.isAllowedExtension(ext)) {
            throw new RuntimeException("不支持上传该文件类型");
        }
        try {
            HomeaiFileMagicUtil.validate(file, ext);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R23】上传前用户空间配额校验-----------
        long incoming = file.getSize() > 0 ? file.getSize() : 0L;
        long used = sumUsedBytesByUser(userId);
        long limit = storageConfigService.getDefaultUserLimitBytes();
        if (used + incoming > limit) {
            throw new JeecgBootException("存储空间不足：已用 "
                    + formatBytes(used) + " / 上限 " + formatBytes(limit)
                    + "，本次还需 " + formatBytes(incoming));
        }
        //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R23】上传前用户空间配额校验-----------
        //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R28】上传前家庭空间配额校验-----------
        String checkFamilyId = familyId;
        if (oConvertUtils.isEmpty(checkFamilyId)) {
            FamilyMember member = familyMemberService.getByUserId(userId);
            checkFamilyId = member != null ? member.getFamilyId() : null;
        }
        if (oConvertUtils.isNotEmpty(checkFamilyId)) {
            long familyUsed = sumUsedBytesByFamily(checkFamilyId);
            //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R30】家庭级配额覆盖-----------
            long familyLimit = storageConfigService.getFamilyLimitBytes(checkFamilyId);
            //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R30】家庭级配额覆盖-----------
            if (familyUsed + incoming > familyLimit) {
                throw new JeecgBootException("家庭存储空间不足：已用 "
                        + formatBytes(familyUsed) + " / 上限 " + formatBytes(familyLimit)
                        + "，本次还需 " + formatBytes(incoming));
            }
        }
        //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R28】上传前家庭空间配额校验-----------
        // storedName：OSS/磁盘实际文件名（UUID），与 originalName 分字段存储
        String storedName = UUID.randomUUID().toString().replace("-", "") + (ext.isEmpty() ? "" : "." + ext);
        String objectKey = StorageFileNameUtil.buildObjectKey(userId, storedName);
        String fileUrl = fileStorageService.storeMultipart(file, objectKey);

        StorageFile sf = new StorageFile();
        sf.setUserId(userId);
        sf.setFamilyId(familyId);
        sf.setFolderId(folderId);
        sf.setOriginalName(originalName);
        sf.setStoredName(storedName);
        sf.setExtension(ext);
        sf.setMimeType(file.getContentType());
        sf.setFileSize(file.getSize());
        sf.setFileUrl(fileUrl);
        sf.setVisibility(visibility != null ? visibility : StorageVisibility.PRIVATE);
        sf.setIsFavorite("0");
        sf.setDownloadCount(0);
        sf.setCreateTime(new Date());
        sf.setUpdateTime(new Date());
        //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R21/R22】上传生成缩略图（图片/PDF）-----------
        tryGenerateImageThumbnail(sf, file, userId);
        tryGeneratePdfThumbnail(sf, file, userId);
        //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R21/R22】上传生成缩略图（图片/PDF）-----------
        save(sf);
        return sf;
    }

    private LambdaQueryWrapper<StorageFile> folderFilesQuery(String folderId) {
        LambdaQueryWrapper<StorageFile> query = new LambdaQueryWrapper<>();
        query.eq(StorageFile::getFolderId, folderId)
                .eq(StorageFile::getDelFlag, 0)
                .orderByDesc(StorageFile::getCreateTime);
        return query;
    }

    private LambdaQueryWrapper<StorageFile> rootFilesQuery(String userId, String familyId) {
        LambdaQueryWrapper<StorageFile> query = new LambdaQueryWrapper<>();
        query.eq(StorageFile::getDelFlag, 0)
                .and(w -> w.isNull(StorageFile::getFolderId).or().eq(StorageFile::getFolderId, ""));
        StorageVisibilityQueryUtil.applyReadableFileFilter(query, userId, familyId);
        query.orderByDesc(StorageFile::getCreateTime);
        return query;
    }

    private LambdaQueryWrapper<StorageFile> allRootFilesQuery() {
        LambdaQueryWrapper<StorageFile> query = new LambdaQueryWrapper<>();
        query.eq(StorageFile::getDelFlag, 0)
                .and(w -> w.isNull(StorageFile::getFolderId).or().eq(StorageFile::getFolderId, ""))
                .orderByDesc(StorageFile::getCreateTime);
        return query;
    }

    //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R21】图片上传生成缩略图-----------
    private void tryGenerateImageThumbnail(StorageFile sf, MultipartFile file, String userId) {
        String ext = sf.getExtension() == null ? "" : sf.getExtension().toLowerCase(Locale.ROOT);
        if (!THUMB_IMAGE_EXTS.contains(ext)) {
            return;
        }
        Path temp = null;
        try {
            BufferedImage src = ImageIO.read(file.getInputStream());
            if (src == null) {
                return;
            }
            int w = src.getWidth();
            int h = src.getHeight();
            if (w <= 0 || h <= 0) {
                return;
            }
            double scale = Math.min(1.0, (double) THUMB_MAX_EDGE / Math.max(w, h));
            int tw = Math.max(1, (int) Math.round(w * scale));
            int th = Math.max(1, (int) Math.round(h * scale));
            BufferedImage thumb = new BufferedImage(tw, th, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = thumb.createGraphics();
            try {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, tw, th);
                g.drawImage(src, 0, 0, tw, th, null);
            } finally {
                g.dispose();
            }
            temp = Files.createTempFile("homeai-thumb-", ".jpg");
            if (!ImageIO.write(thumb, "jpg", temp.toFile())) {
                return;
            }
            String thumbStored = "thumb_" + UUID.randomUUID().toString().replace("-", "") + ".jpg";
            String thumbKey = StorageFileNameUtil.buildObjectKey(userId, thumbStored);
            sf.setThumbnailUrl(fileStorageService.storeLocalFile(temp, thumbKey));
        } catch (Exception e) {
            log.warn("图片缩略图生成失败，已跳过: name={}, err={}", sf.getOriginalName(), e.getMessage());
        } finally {
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException ignored) {
                    // ignore
                }
            }
        }
    }

    //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R22】PDF 首帧缩略图-----------
    private void tryGeneratePdfThumbnail(StorageFile sf, MultipartFile file, String userId) {
        String ext = sf.getExtension() == null ? "" : sf.getExtension().toLowerCase(Locale.ROOT);
        if (!"pdf".equals(ext) || oConvertUtils.isNotEmpty(sf.getThumbnailUrl())) {
            return;
        }
        Path temp = null;
        try (InputStream in = file.getInputStream(); PDDocument doc = PDDocument.load(in)) {
            if (doc.getNumberOfPages() < 1) {
                return;
            }
            PDFRenderer renderer = new PDFRenderer(doc);
            BufferedImage src = renderer.renderImageWithDPI(0, 72, ImageType.RGB);
            int w = src.getWidth();
            int h = src.getHeight();
            double scale = Math.min(1.0, (double) THUMB_MAX_EDGE / Math.max(w, h));
            int tw = Math.max(1, (int) Math.round(w * scale));
            int th = Math.max(1, (int) Math.round(h * scale));
            BufferedImage thumb = new BufferedImage(tw, th, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = thumb.createGraphics();
            try {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, tw, th);
                g.drawImage(src, 0, 0, tw, th, null);
            } finally {
                g.dispose();
            }
            temp = Files.createTempFile("homeai-pdf-thumb-", ".jpg");
            if (!ImageIO.write(thumb, "jpg", temp.toFile())) {
                return;
            }
            String thumbStored = "thumb_" + UUID.randomUUID().toString().replace("-", "") + ".jpg";
            String thumbKey = StorageFileNameUtil.buildObjectKey(userId, thumbStored);
            sf.setThumbnailUrl(fileStorageService.storeLocalFile(temp, thumbKey));
        } catch (Exception e) {
            log.warn("PDF 缩略图生成失败，已跳过: name={}, err={}", sf.getOriginalName(), e.getMessage());
        } finally {
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException ignored) {
                    // ignore
                }
            }
        }
    }
    //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R22】PDF 首帧缩略图-----------

    /** 优先使用客户端传入的原始文件名；storedName 单独生成，二者分字段落库 */
    private String resolveOriginalName(MultipartFile file, String fileName) {
        if (oConvertUtils.isNotEmpty(fileName)) {
            return StorageFileNameUtil.sanitizeOriginalName(fileName);
        }
        String fromMultipart = file.getOriginalFilename();
        if (StorageFileNameUtil.isTempUploadName(fromMultipart)) {
            String ext = StorageFileNameUtil.extensionOf(fromMultipart);
            if (oConvertUtils.isEmpty(ext)) {
                ext = "dat";
            }
            return "FILE_" + System.currentTimeMillis() + "." + ext;
        }
        return StorageFileNameUtil.sanitizeOriginalName(fromMultipart);
    }

    @Override
    public void softDelete(String id) {
        //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R22】软删保留家庭关联以便恢复-----------
        update(new LambdaUpdateWrapper<StorageFile>()
                .eq(StorageFile::getId, id)
                .set(StorageFile::getDelFlag, 1)
                .set(StorageFile::getDeletedAt, new Date()));
        //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R22】软删保留家庭关联以便恢复-----------
    }

    @Override
    public void softDeleteByFolderId(String folderId) {
        if (oConvertUtils.isEmpty(folderId)) {
            return;
        }
        //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R22】软删保留家庭关联以便恢复-----------
        update(new LambdaUpdateWrapper<StorageFile>()
                .eq(StorageFile::getFolderId, folderId)
                .eq(StorageFile::getDelFlag, 0)
                .set(StorageFile::getDelFlag, 1)
                .set(StorageFile::getDeletedAt, new Date()));
        //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R22】软删保留家庭关联以便恢复-----------
    }

    //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R22】资料文件回收站-----------
    @Override
    public IPage<StorageFile> pageRecycleBin(Page<StorageFile> page, String keyword) {
        return baseMapper.selectRecycleBinPage(page, keyword);
    }

    //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R24】用户侧回收站-----------
    @Override
    public IPage<StorageFile> pageMyRecycleBin(Page<StorageFile> page, String userId, String keyword) {
        return baseMapper.selectMyRecycleBinPage(page, userId, keyword);
    }
    //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R24】用户侧回收站-----------

    @Override
    public void restoreFiles(Collection<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (String id : ids) {
            if (oConvertUtils.isEmpty(id)) {
                continue;
            }
            baseMapper.restoreById(id);
        }
    }

    @Override
    public void deletePermanently(Collection<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (String id : ids) {
            if (oConvertUtils.isEmpty(id)) {
                continue;
            }
            StorageFile sf = getById(id);
            if (sf == null) {
                // 无 TableLogic 时 getById 可读到已软删行；若仍为空则用原生条件再查一次
                sf = getOne(new LambdaQueryWrapper<StorageFile>().eq(StorageFile::getId, id).last("LIMIT 1"), false);
            }
            if (sf != null) {
                try {
                    fileStorageService.deleteIfExists(sf.getFileUrl());
                } catch (Exception e) {
                    log.warn("彻底删除存储对象失败: id={}, err={}", id, e.getMessage());
                }
                try {
                    fileStorageService.deleteIfExists(sf.getThumbnailUrl());
                } catch (Exception e) {
                    log.warn("彻底删除缩略图失败: id={}, err={}", id, e.getMessage());
                }
            }
            resourceFamilyService.deleteByFileId(id);
        }
        baseMapper.deletePermanentlyByIds(ids);
    }
    //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R22】资料文件回收站-----------

    //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R23】用户存储用量-----------
    @Override
    public long sumUsedBytesByUser(String userId) {
        if (oConvertUtils.isEmpty(userId)) {
            return 0L;
        }
        //update-begin---author:cursor ---date:2026-08-13 for：【性能优化】改为 SQL SUM，避免全量加载到内存累加-----------
        QueryWrapper<StorageFile> qw = new QueryWrapper<>();
        qw.select("COALESCE(SUM(file_size), 0) AS total")
                .eq("user_id", userId)
                .eq("del_flag", 0);
        return sumBytesFromQuery(qw);
        //update-end---author:cursor ---date:2026-08-13 for：【性能优化】改为 SQL SUM-----------
    }

    //update-begin---author:admin ---date:2026-08-12 for：【HomeAI-R28】家庭存储用量-----------
    @Override
    public long sumUsedBytesByFamily(String familyId) {
        if (oConvertUtils.isEmpty(familyId)) {
            return 0L;
        }
        List<FamilyMember> members = familyMemberService.getByFamilyId(familyId);
        if (members == null || members.isEmpty()) {
            return 0L;
        }
        List<String> userIds = members.stream()
                .map(FamilyMember::getUserId)
                .filter(oConvertUtils::isNotEmpty)
                .distinct()
                .collect(Collectors.toList());
        if (userIds.isEmpty()) {
            return 0L;
        }
        //update-begin---author:cursor ---date:2026-08-13 for：【性能优化】改为 SQL SUM，避免全量加载到内存累加-----------
        QueryWrapper<StorageFile> qw = new QueryWrapper<>();
        qw.select("COALESCE(SUM(file_size), 0) AS total")
                .in("user_id", userIds)
                .eq("del_flag", 0);
        return sumBytesFromQuery(qw);
        //update-end---author:cursor ---date:2026-08-13 for：【性能优化】改为 SQL SUM-----------
    }

    /** 执行 SQL 聚合取 SUM(file_size)，兼容多数据库（COALESCE） */
    private long sumBytesFromQuery(QueryWrapper<StorageFile> qw) {
        List<Map<String, Object>> rows = listMaps(qw);
        if (rows == null || rows.isEmpty()) {
            return 0L;
        }
        Map<String, Object> row = rows.get(0);
        Object val = row.get("total");
        if (val == null && !row.isEmpty()) {
            val = row.values().iterator().next();
        }
        return val == null ? 0L : new BigDecimal(val.toString()).longValue();
    }
    //update-end---author:cursor ---date:2026-08-13 for：【性能优化】存储用量 SQL SUM-----------
    //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R28】家庭存储用量-----------

    //update-begin---author:admin ---date:2026-08-13 for：【HomeAI-R32】家庭配额运营看板-----------
    @Override
    public List<Map<String, Object>> listFamilyQuotaBoard(String keyword, Boolean onlyWarn, Boolean onlyCustom) {
        int warnPercent = storageConfigService.getWarnPercent();
        List<Family> families = familyService.list(new LambdaQueryWrapper<Family>()
                .eq(Family::getDelFlag, 0)
                .ne(Family::getStatus, "disbanded"));
        String kw = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Family fam : families) {
            if (fam == null || oConvertUtils.isEmpty(fam.getId())) {
                continue;
            }
            if (!kw.isEmpty()) {
                String name = fam.getName() == null ? "" : fam.getName().toLowerCase(Locale.ROOT);
                if (!name.contains(kw) && !fam.getId().toLowerCase(Locale.ROOT).contains(kw)) {
                    continue;
                }
            }
            long used = sumUsedBytesByFamily(fam.getId());
            long thisFamilyLimit = storageConfigService.getFamilyLimitBytes(fam.getId());
            boolean customLimit = storageConfigService.hasFamilyLimitOverride(fam.getId());
            if (Boolean.TRUE.equals(onlyCustom) && !customLimit) {
                continue;
            }
            List<FamilyMember> memberList = familyMemberService.getByFamilyId(fam.getId());
            int memberCount = memberList == null ? 0 : memberList.size();
            long fileCount = 0L;
            if (memberList != null && !memberList.isEmpty()) {
                Set<String> memberIds = new HashSet<>();
                for (FamilyMember m : memberList) {
                    if (m != null && oConvertUtils.isNotEmpty(m.getUserId())) {
                        memberIds.add(m.getUserId());
                    }
                }
                if (!memberIds.isEmpty()) {
                    fileCount = count(new LambdaQueryWrapper<StorageFile>()
                            .in(StorageFile::getUserId, memberIds)
                            .eq(StorageFile::getDelFlag, 0));
                }
            }
            double usedPercent = thisFamilyLimit > 0 ? Math.min(100, used * 100.0 / thisFamilyLimit) : 0;
            boolean overWarn = thisFamilyLimit > 0 && used * 100.0 / thisFamilyLimit >= warnPercent;
            if (Boolean.TRUE.equals(onlyWarn) && !overWarn) {
                continue;
            }
            Map<String, Object> row = new java.util.LinkedHashMap<>();
            row.put("familyId", fam.getId());
            row.put("familyName", fam.getName());
            row.put("memberCount", memberCount);
            row.put("fileCount", fileCount);
            row.put("totalSize", used);
            row.put("limitBytes", thisFamilyLimit);
            row.put("customLimit", customLimit);
            row.put("usedPercent", usedPercent);
            row.put("overWarn", overWarn);
            rows.add(row);
        }
        rows.sort((a, b) -> Double.compare(
                ((Number) b.getOrDefault("usedPercent", 0)).doubleValue(),
                ((Number) a.getOrDefault("usedPercent", 0)).doubleValue()));
        return rows;
    }
    //update-end---author:admin ---date:2026-08-13 for：【HomeAI-R32】家庭配额运营看板-----------

    private static String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        double kb = bytes / 1024.0;
        if (kb < 1024) {
            return String.format(Locale.ROOT, "%.1f KB", kb);
        }
        double mb = kb / 1024.0;
        if (mb < 1024) {
            return String.format(Locale.ROOT, "%.1f MB", mb);
        }
        return String.format(Locale.ROOT, "%.2f GB", mb / 1024.0);
    }
    //update-end---author:admin ---date:2026-08-12 for：【HomeAI-R23】用户存储用量-----------

    @Override
    public void toggleFavorite(String id) {
        StorageFile sf = getById(id);
        if (sf != null) {
            sf.setIsFavorite("1".equals(sf.getIsFavorite()) ? "0" : "1");
            updateById(sf);
        }
    }

    @Override
    public List<StorageFile> searchFiles(String keyword, String userId, String familyId) {
        LambdaQueryWrapper<StorageFile> query = new LambdaQueryWrapper<>();
        query.eq(StorageFile::getDelFlag, 0)
                .like(StorageFile::getOriginalName, keyword);
        StorageVisibilityQueryUtil.applyReadableFileFilter(query, userId, familyId);
        query.orderByDesc(StorageFile::getCreateTime);
        return list(query);
    }

    @Override
    public List<StorageFile> searchAllFiles(String keyword) {
        LambdaQueryWrapper<StorageFile> query = new LambdaQueryWrapper<>();
        query.eq(StorageFile::getDelFlag, 0)
                .like(StorageFile::getOriginalName, keyword)
                .orderByDesc(StorageFile::getCreateTime);
        return list(query);
    }

    @Override
    public String getFileIcon(String extension) {
        Map<String, String> iconMap = new HashMap<>();
        iconMap.put("jpg", "image");
        iconMap.put("jpeg", "image");
        iconMap.put("png", "image");
        iconMap.put("gif", "image");
        iconMap.put("bmp", "image");
        iconMap.put("webp", "image");
        iconMap.put("pdf", "pdf");
        iconMap.put("doc", "word");
        iconMap.put("docx", "word");
        iconMap.put("xls", "excel");
        iconMap.put("xlsx", "excel");
        iconMap.put("ppt", "ppt");
        iconMap.put("pptx", "ppt");
        iconMap.put("mp4", "video");
        iconMap.put("avi", "video");
        iconMap.put("mov", "video");
        iconMap.put("zip", "archive");
        iconMap.put("rar", "archive");
        iconMap.put("7z", "archive");
        iconMap.put("txt", "text");
        iconMap.put("md", "text");
        return iconMap.getOrDefault(extension, "file");
    }
}
