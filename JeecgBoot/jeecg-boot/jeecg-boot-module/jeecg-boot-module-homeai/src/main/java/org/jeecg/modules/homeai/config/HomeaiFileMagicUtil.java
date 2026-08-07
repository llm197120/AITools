package org.jeecg.modules.homeai.config;

import org.jeecg.common.util.oConvertUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 文件头魔数校验，防止扩展名伪造
 */
public final class HomeaiFileMagicUtil {

    private static final Set<String> SKIP_MAGIC_EXTENSIONS = new HashSet<>(Arrays.asList(
            "txt", "csv", "md", "rar", "7z", "avi", "mov", "mkv", "bmp"
    ));

    private HomeaiFileMagicUtil() {
    }

    public static void validate(MultipartFile file, String extension) throws IOException {
        if (file == null || oConvertUtils.isEmpty(extension)) {
            throw new IOException("无法校验文件类型");
        }
        String ext = extension.toLowerCase();
        if (SKIP_MAGIC_EXTENSIONS.contains(ext)) {
            validateNotExecutable(file);
            return;
        }
        byte[] header = readHeader(file, 8);
        if (!matchesMagic(header, ext)) {
            throw new IOException("文件内容与扩展名不匹配");
        }
    }

    private static void validateNotExecutable(MultipartFile file) throws IOException {
        byte[] header = readHeader(file, 2);
        if (header.length >= 2 && header[0] == 'M' && header[1] == 'Z') {
            throw new IOException("不允许上传可执行文件");
        }
    }

    private static byte[] readHeader(MultipartFile file, int len) throws IOException {
        byte[] buf = new byte[len];
        try (InputStream in = file.getInputStream()) {
            int read = in.read(buf);
            if (read <= 0) {
                return new byte[0];
            }
            if (read < len) {
                return Arrays.copyOf(buf, read);
            }
        }
        return buf;
    }

    static boolean matchesMagic(byte[] header, String ext) {
        if (header == null || header.length < 2) {
            return false;
        }
        switch (ext) {
            case "jpg":
            case "jpeg":
                return header.length >= 3
                        && (header[0] & 0xFF) == 0xFF
                        && (header[1] & 0xFF) == 0xD8
                        && (header[2] & 0xFF) == 0xFF;
            case "png":
                return header.length >= 4
                        && header[0] == (byte) 0x89
                        && header[1] == 0x50
                        && header[2] == 0x4E
                        && header[3] == 0x47;
            case "gif":
                return header.length >= 4
                        && header[0] == 'G'
                        && header[1] == 'I'
                        && header[2] == 'F'
                        && header[3] == '8';
            case "pdf":
                return header.length >= 4
                        && header[0] == '%'
                        && header[1] == 'P'
                        && header[2] == 'D'
                        && header[3] == 'F';
            case "zip":
            case "docx":
            case "xlsx":
            case "pptx":
                return header[0] == 0x50 && header[1] == 0x4B;
            case "doc":
            case "xls":
            case "ppt":
                return header.length >= 4
                        && header[0] == (byte) 0xD0
                        && header[1] == (byte) 0xCF
                        && header[2] == 0x11
                        && header[3] == (byte) 0xE0;
            case "mp4":
                return containsAscii(header, "ftyp");
            default:
                return true;
        }
    }

    private static boolean containsAscii(byte[] header, String text) {
        String s = new String(header);
        return s.contains(text);
    }
}
