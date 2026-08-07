package org.jeecg.modules.homeai.storage.util;

import lombok.extern.slf4j.Slf4j;
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
 * Office 格式转换：Windows 下优先调用本机 Microsoft Office，失败或未安装时回退 LibreOffice。
 */
@Slf4j
@Component
public class HomeaiOfficeConvertUtil {

    private static final String SCRIPT_CLASSPATH = "/homeai/scripts/office-convert.ps1";
    /** 脚本版本变更时强制重写临时文件，避免旧缓存导致编码错误 */
    private static final String SCRIPT_VERSION = "v2";

    private static final byte[] UTF8_BOM = new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    @Value("${homeai.office.prefer-ms-office:true}")
    private boolean preferMsOffice;

    @Value("${homeai.office.soffice-path:soffice}")
    private String sofficePath;

    @Value("${homeai.office.powershell-path:powershell}")
    private String powershellPath;

    @Value("${homeai.office.convert-timeout-seconds:120}")
    private int convertTimeoutSeconds;

    private volatile Path cachedScriptPath;

    /**
     * 执行格式转换，优先 Microsoft Office，回退 LibreOffice。
     */
    public Path convert(Path sourcePath, Path outDir, String targetFormat) throws Exception {
        String format = targetFormat != null ? targetFormat.toLowerCase() : "pdf";
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
            throw new RuntimeException("未找到 LibreOffice，请安装 LibreOffice 或配置 homeai.office.soffice-path");
        }
        Path converted = convertWithLibreOffice(sourcePath, outDir, format, libreOfficePath);
        if (converted != null && Files.exists(converted)) {
            log.info("使用 LibreOffice 完成转换: {} -> {}", sourcePath.getFileName(), format);
        }
        return converted;
    }

    private boolean shouldTryMsOffice() {
        if (!preferMsOffice) {
            return false;
        }
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("win");
    }

    private Path convertWithMsOffice(Path sourcePath, Path outDir, String targetFormat) throws Exception {
        Path scriptPath = resolveScriptPath();
        ProcessBuilder pb = new ProcessBuilder(
                powershellPath,
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
        boolean finished = process.waitFor(convertTimeoutSeconds, TimeUnit.SECONDS);
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
                targetFormat,
                "--outdir",
                outDir.toAbsolutePath().toString(),
                sourcePath.toAbsolutePath().toString()
        );
        pb.redirectErrorStream(true);
        Process process = pb.start();
        String output = readProcessOutput(process);
        boolean finished = process.waitFor(convertTimeoutSeconds, TimeUnit.SECONDS);
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
        if (isExecutable(sofficePath)) {
            return sofficePath;
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
