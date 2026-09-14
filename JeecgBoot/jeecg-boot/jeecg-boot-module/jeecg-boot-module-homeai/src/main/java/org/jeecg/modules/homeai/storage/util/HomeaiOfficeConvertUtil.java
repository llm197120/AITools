package org.jeecg.modules.homeai.storage.util;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.homeai.config.dto.HomeaiSysConfigDto;
import org.jeecg.modules.homeai.config.service.IHomeaiSysConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Office 格式转换：Gotenberg（PDF）→ 本机 Microsoft Office → LibreOffice。
 */
@Slf4j
@Component
public class HomeaiOfficeConvertUtil {

    private static final String SCRIPT_CLASSPATH = "/homeai/scripts/office-convert.ps1";
    /** 脚本版本变更时强制重写临时文件，避免旧缓存导致编码错误 */
    private static final String SCRIPT_VERSION = "v3";

    private static final byte[] UTF8_BOM = new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

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

    //update-begin---author:cursor---date:2026-09-07---for:【系统配置】Office 转换走后台覆盖-----------
    @Autowired
    private IHomeaiSysConfigService sysConfigService;
    //update-end---author:cursor---date:2026-09-07---for:【系统配置】Office 转换走后台覆盖-----------

    @Autowired
    private HomeaiGotenbergClient gotenbergClient;

    private volatile Path cachedScriptPath;

    /**
     * 执行格式转换，优先 Microsoft Office，回退 LibreOffice。
     */
    public Path convert(Path sourcePath, Path outDir, String targetFormat) throws Exception {
        String format = targetFormat != null ? targetFormat.toLowerCase() : "pdf";
        //update-begin---author:cursor---date:2026-09-07---for:【资料转换】docx→txt 优先解 OOXML 抽文本---
        String sourceExt = StorageFileNameUtil.extensionOf(sourcePath.getFileName().toString());
        if (HomeaiOfficeTextExtractUtil.supports(sourceExt, format)) {
            try {
                Path extracted = HomeaiOfficeTextExtractUtil.extract(sourcePath, outDir, format);
                if (extracted != null && Files.exists(extracted)) {
                    log.info("已从文档抽出文本: {} -> {}", sourcePath.getFileName(), format);
                    return extracted;
                }
            } catch (Exception e) {
                log.warn("文档抽文本失败，回退 Office: {}", e.getMessage());
            }
        }
        //update-end---author:cursor---date:2026-09-07---for:【资料转换】docx→txt 优先解 OOXML 抽文本---
        //update-begin---author:cursor---date:2026-09-07---for:【资料转换】PDF 优先走 Gotenberg---
        if (HomeaiGotenbergConvert.handlesTarget(format) && HomeaiGotenbergConvert.enabled(gotenbergUrlNow())) {
            try {
                Path converted = gotenbergClient.convertToPdf(sourcePath, outDir, gotenbergUrlNow(), convertTimeoutNow());
                if (converted != null && Files.exists(converted)) {
                    return converted;
                }
            } catch (Exception e) {
                log.warn("Gotenberg 转换失败，回退本机 Office: {}", e.getMessage());
            }
        }
        //update-end---author:cursor---date:2026-09-07---for:【资料转换】PDF 优先走 Gotenberg---
        Exception msOfficeError = null;
        if (shouldTryMsOffice()) {
            try {
                Path converted = convertWithMsOffice(sourcePath, outDir, format);
                if (converted != null && Files.exists(converted)) {
                    log.info("使用 Microsoft Office 完成转换: {} -> {}", sourcePath.getFileName(), format);
                    return converted;
                }
            } catch (Exception e) {
                msOfficeError = e;
                log.warn("Microsoft Office 转换失败，回退 LibreOffice: {}", e.getMessage());
            }
        }
        String libreOfficePath = resolveLibreOfficePath();
        if (libreOfficePath == null) {
            if (msOfficeError != null) {
                throw new RuntimeException("Microsoft Office 转换失败且未找到 LibreOffice: " + msOfficeError.getMessage(), msOfficeError);
            }
            throw new RuntimeException("未找到转换引擎，请启动 Gotenberg 或安装 Microsoft Office / LibreOffice");
        }
        Path converted = convertWithLibreOffice(sourcePath, outDir, format, libreOfficePath);
        if (converted != null && Files.exists(converted)) {
            log.info("使用 LibreOffice 完成转换: {} -> {}", sourcePath.getFileName(), format);
        }
        return converted;
    }

    private boolean shouldTryMsOffice() {
        if (!preferMsOfficeNow()) {
            return false;
        }
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("win");
    }

    private boolean preferMsOfficeNow() {
        HomeaiSysConfigDto.Office office = officeCfg();
        if (office != null && office.getPreferMsOffice() != null) {
            return office.getPreferMsOffice();
        }
        return preferMsOffice;
    }

    private String sofficePathNow() {
        HomeaiSysConfigDto.Office office = officeCfg();
        if (office != null && office.getSofficePath() != null && !office.getSofficePath().isBlank()) {
            return office.getSofficePath();
        }
        return sofficePath;
    }

    private String gotenbergUrlNow() {
        HomeaiSysConfigDto.Office office = officeCfg();
        if (office != null && office.getGotenbergUrl() != null && !office.getGotenbergUrl().isBlank()) {
            return office.getGotenbergUrl();
        }
        return gotenbergUrl;
    }

    private String powershellPathNow() {
        HomeaiSysConfigDto.Office office = officeCfg();
        if (office != null && office.getPowershellPath() != null && !office.getPowershellPath().isBlank()) {
            return office.getPowershellPath();
        }
        return powershellPath;
    }

    private int convertTimeoutNow() {
        HomeaiSysConfigDto.Office office = officeCfg();
        if (office != null && office.getConvertTimeoutSeconds() != null) {
            return office.getConvertTimeoutSeconds();
        }
        return convertTimeoutSeconds;
    }

    private HomeaiSysConfigDto.Office officeCfg() {
        return sysConfigService == null ? null : sysConfigService.getOffice();
    }

    private Path convertWithMsOffice(Path sourcePath, Path outDir, String targetFormat) throws Exception {
        Path scriptPath = resolveScriptPath();
        ProcessBuilder pb = new ProcessBuilder(
                powershellPathNow(),
                "-NoProfile",
                "-NonInteractive",
                "-ExecutionPolicy",
                "Bypass",
                "-File",
                scriptPath.toAbsolutePath().toString(),
                "-SourcePath",
                sourcePath.toAbsolutePath().toString(),
                "-OutDir",
                outDir.toAbsolutePath().toString(),
                "-TargetFormat",
                targetFormat
        );
        pb.redirectErrorStream(true);
        Process process = pb.start();
        String output = readProcessOutput(process);
        boolean finished = process.waitFor(convertTimeoutNow(), TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Microsoft Office 转换超时");
        }
        if (process.exitValue() != 0) {
            throw new RuntimeException(trimErrorOutput(output));
        }
        String resultPath = output.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("Write-Error") && !line.contains("CategoryInfo"))
                .reduce((first, second) -> second)
                .orElse("");
        if (resultPath.isEmpty()) {
            return locateConvertedFile(sourcePath, outDir, targetFormat);
        }
        Path resolved = Path.of(resultPath);
        return Files.exists(resolved) ? resolved : locateConvertedFile(sourcePath, outDir, targetFormat);
    }

    private Path convertWithLibreOffice(Path sourcePath, Path outDir, String targetFormat, String libreOfficePath) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
                libreOfficePath,
                "--headless",
                "--convert-to",
                HomeaiLibreOfficeConvertArg.of(targetFormat),
                "--outdir",
                outDir.toAbsolutePath().toString(),
                sourcePath.toAbsolutePath().toString()
        );
        pb.redirectErrorStream(true);
        Process process = pb.start();
        String output = readProcessOutput(process);
        boolean finished = process.waitFor(convertTimeoutNow(), TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("LibreOffice 转换超时");
        }
        if (process.exitValue() != 0) {
            throw new RuntimeException("LibreOffice 退出码: " + process.exitValue()
                    + (output.isBlank() ? "" : "，" + trimErrorOutput(output)));
        }
        return locateConvertedFile(sourcePath, outDir, targetFormat);
    }

    private String resolveLibreOfficePath() {
        if (isExecutable(sofficePathNow())) {
            return sofficePathNow();
        }
        if (!shouldTryMsOffice()) {
            return null;
        }
        String[] candidates = {
                "C:\\Program Files\\LibreOffice\\program\\soffice.exe",
                "C:\\Program Files (x86)\\LibreOffice\\program\\soffice.exe"
        };
        for (String candidate : candidates) {
            if (isExecutable(candidate)) {
                log.info("自动检测到 LibreOffice: {}", candidate);
                return candidate;
            }
        }
        return null;
    }

    private boolean isExecutable(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        Path file = Path.of(path);
        return Files.isRegularFile(file) || Files.isRegularFile(Path.of(path + ".exe"));
    }

    private Path locateConvertedFile(Path sourcePath, Path outDir, String targetFormat) throws IOException {
        String baseName = sourcePath.getFileName().toString();
        int dot = baseName.lastIndexOf('.');
        String nameWithoutExt = dot > 0 ? baseName.substring(0, dot) : baseName;
        Path expected = outDir.resolve(nameWithoutExt + "." + targetFormat);
        if (Files.exists(expected)) {
            return expected;
        }
        try (var stream = Files.list(outDir)) {
            return stream
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith("." + targetFormat))
                    .max((a, b) -> {
                        try {
                            return Files.getLastModifiedTime(a).compareTo(Files.getLastModifiedTime(b));
                        } catch (IOException e) {
                            return 0;
                        }
                    })
                    .orElse(null);
        }
    }

    private Path resolveScriptPath() throws IOException {
        Path expected = Path.of(System.getProperty("java.io.tmpdir"), "homeai-office-" + SCRIPT_VERSION, "office-convert.ps1");
        if (Files.exists(expected)) {
            cachedScriptPath = expected;
            return expected;
        }
        synchronized (this) {
            if (Files.exists(expected)) {
                cachedScriptPath = expected;
                return expected;
            }
            Files.createDirectories(expected.getParent());
            try (InputStream in = getClass().getResourceAsStream(SCRIPT_CLASSPATH)) {
                if (in == null) {
                    throw new IOException("未找到 Office 转换脚本: " + SCRIPT_CLASSPATH);
                }
                byte[] content = in.readAllBytes();
                byte[] withBom = new byte[UTF8_BOM.length + content.length];
                System.arraycopy(UTF8_BOM, 0, withBom, 0, UTF8_BOM.length);
                System.arraycopy(content, 0, withBom, UTF8_BOM.length, content.length);
                Files.write(expected, withBom);
            }
            cachedScriptPath = expected;
            return expected;
        }
    }

    private String readProcessOutput(Process process) throws IOException {
        Charset charset = shouldTryMsOffice() ? Charset.defaultCharset() : StandardCharsets.UTF_8;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), charset))) {
            return reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }

    private String trimErrorOutput(String output) {
        if (output == null || output.isBlank()) {
            return "转换失败";
        }
        String trimmed = output.trim();
        if (trimmed.length() > 500) {
            return trimmed.substring(trimmed.length() - 500);
        }
        return trimmed;
    }
}
