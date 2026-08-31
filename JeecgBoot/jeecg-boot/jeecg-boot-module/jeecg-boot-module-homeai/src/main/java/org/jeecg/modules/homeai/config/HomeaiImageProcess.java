package org.jeecg.modules.homeai.config;

/**
 * APP 展示用阿里云图片处理参数。GIF 不处理以免破坏动画。
 */
public final class HomeaiImageProcess {

    /** 列表缩略图：约 480 宽 */
    public static final String THUMB = "image/resize,w_480/quality,q_70";

    /** 详情/预览：约 1080 宽，手机屏足够 */
    public static final String DISPLAY = "image/resize,w_1080/quality,q_75";

    private HomeaiImageProcess() {
    }

    public static boolean isProcessableImage(String nameOrUrl) {
        String ext = extensionOf(nameOrUrl);
        if (ext.isEmpty() && nameOrUrl != null) {
            ext = nameOrUrl.trim().toLowerCase();
        }
        return "jpg".equals(ext) || "jpeg".equals(ext) || "png".equals(ext)
                || "webp".equals(ext) || "bmp".equals(ext);
    }

    public static String extensionOf(String nameOrUrl) {
        if (nameOrUrl == null || nameOrUrl.isEmpty()) {
            return "";
        }
        String path = nameOrUrl.trim();
        int q = path.indexOf('?');
        if (q >= 0) {
            path = path.substring(0, q);
        }
        int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf(':'));
        String name = slash >= 0 ? path.substring(slash + 1) : path;
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return "";
        }
        return name.substring(dot + 1).toLowerCase();
    }
}
