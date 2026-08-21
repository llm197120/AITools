package org.jeecg.modules.homeai.recipe.util;

import org.jeecg.common.util.oConvertUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 菜谱封面批量导入：按路径匹配菜谱名（纯逻辑，无 IO）。
 *
 * <p>匹配优先级：
 * 1. 文件名去扩展名等于菜谱名（如 {@code 红烧肉.jpg}）
 * 2. 父目录名等于菜谱名（如 {@code 红烧肉/cover.jpg}）
 */
public final class RecipeCoverMatch {

    public static final Set<String> IMAGE_EXTENSIONS = new HashSet<>(Arrays.asList(
            "jpg", "jpeg", "png", "gif", "webp", "bmp"
    ));

    private static final Set<String> GENERIC_PARENTS = new HashSet<>(Arrays.asList(
            "images", "img", "imgs", "image", "pictures", "pics", "pic", "photo", "photos",
            "covers", "cover", "assets", "static", "upload", "uploads", "dishes", "dish-images",
            "图片", "封面", "菜谱", "菜谱图", "菜谱图片", "图库"
    ));

    private static final Set<String> GENERIC_BASENAMES = new HashSet<>(Arrays.asList(
            "cover", "封面", "主图", "index", "poster", "thumb", "thumbnail",
            "default", "image", "img", "photo", "pic", "0", "1", "01"
    ));

    private RecipeCoverMatch() {
    }

    public static String normalize(String name) {
        if (name == null) {
            return "";
        }
        return name.replace('\u3000', ' ').trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    /** 统一为正斜杠路径，去掉首尾斜杠 */
    public static String originalPath(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }
        String path = originalFilename.replace('\\', '/').trim();
        while (path.startsWith("./")) {
            path = path.substring(2);
        }
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        while (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    public static boolean isIgnoredPath(String originalFilename) {
        String path = originalPath(originalFilename);
        if (oConvertUtils.isEmpty(path)) {
            return true;
        }
        String[] parts = path.split("/");
        for (String part : parts) {
            if (part.startsWith(".") || "__macosx".equalsIgnoreCase(part)) {
                return true;
            }
        }
        return path.contains("..");
    }

    public static String extension(String originalFilename) {
        String path = originalPath(originalFilename);
        int slash = path.lastIndexOf('/');
        String file = slash < 0 ? path : path.substring(slash + 1);
        int dot = file.lastIndexOf('.');
        if (dot < 0 || dot == file.length() - 1) {
            return "";
        }
        return file.substring(dot + 1).replaceAll("[^a-zA-Z0-9]", "").toLowerCase(Locale.ROOT);
    }

    public static boolean isImage(String originalFilename) {
        return IMAGE_EXTENSIONS.contains(extension(originalFilename));
    }

    public static boolean isZip(String originalFilename) {
        return "zip".equals(extension(originalFilename));
    }

    public static String basenameNoExt(String originalFilename) {
        String path = originalPath(originalFilename);
        int slash = path.lastIndexOf('/');
        String file = slash < 0 ? path : path.substring(slash + 1);
        int dot = file.lastIndexOf('.');
        return dot < 0 ? file : file.substring(0, dot);
    }

    public static String parentName(String originalFilename) {
        String path = originalPath(originalFilename);
        int slash = path.lastIndexOf('/');
        if (slash <= 0) {
            return "";
        }
        String parentPath = path.substring(0, slash);
        int prev = parentPath.lastIndexOf('/');
        return prev < 0 ? parentPath : parentPath.substring(prev + 1);
    }

    public static boolean isGenericParent(String parent) {
        return GENERIC_PARENTS.contains(normalize(parent));
    }

    public static boolean isGenericBasename(String base) {
        return GENERIC_BASENAMES.contains(normalize(base));
    }

    /**
     * @param nameIndex normalize(菜谱名) → 原始菜谱名
     * @return 匹配到的原始菜谱名；未匹配返回 null
     */
    public static String matchRecipeName(String originalFilename, Map<String, String> nameIndex) {
        if (nameIndex == null || nameIndex.isEmpty() || isIgnoredPath(originalFilename) || !isImage(originalFilename)) {
            return null;
        }
        String base = normalize(basenameNoExt(originalFilename));
        String parent = normalize(parentName(originalFilename));
        if (!isGenericBasename(base) && nameIndex.containsKey(base)) {
            return nameIndex.get(base);
        }
        if (!oConvertUtils.isEmpty(parent) && !isGenericParent(parent) && nameIndex.containsKey(parent)) {
            return nameIndex.get(parent);
        }
        if (nameIndex.containsKey(base)) {
            return nameIndex.get(base);
        }
        return null;
    }

    /** 同一菜谱多张图时选封面：文件名等于菜名 > 封面类文件名 > 路径更浅 */
    public static int coverScore(String originalFilename, String recipeName) {
        String path = originalPath(originalFilename);
        int depth = 0;
        for (int i = 0; i < path.length(); i++) {
            if (path.charAt(i) == '/') {
                depth++;
            }
        }
        int score = 10 - depth;
        String base = normalize(basenameNoExt(originalFilename));
        String recipe = normalize(recipeName);
        if (base.equals(recipe)) {
            score += 100;
        } else if (isGenericBasename(base)) {
            score += 40;
        }
        return score;
    }
}
