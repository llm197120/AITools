package org.jeecg.modules.homeai.recipe.util;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 菜谱封面路径匹配纯逻辑测试（无 Spring/DB）。
 */
class RecipeCoverMatchTest {

    private Map<String, String> index(String... names) {
        Map<String, String> map = new LinkedHashMap<>();
        for (String n : names) {
            map.put(RecipeCoverMatch.normalize(n), n);
        }
        return map;
    }

    @Test
    void matchByFilename() {
        Map<String, String> idx = index("红烧肉", "番茄炒蛋");
        assertEquals("红烧肉", RecipeCoverMatch.matchRecipeName("红烧肉.jpg", idx));
        assertEquals("番茄炒蛋", RecipeCoverMatch.matchRecipeName("菜谱图/番茄炒蛋.png", idx));
    }

    @Test
    void matchByParentFolder() {
        Map<String, String> idx = index("红烧肉");
        assertEquals("红烧肉", RecipeCoverMatch.matchRecipeName("红烧肉/cover.jpg", idx));
        assertEquals("红烧肉", RecipeCoverMatch.matchRecipeName("home/红烧肉/封面.webp", idx));
    }

    @Test
    void genericParentDoesNotMatch() {
        Map<String, String> idx = index("images");
        // 父目录 images 是通用名，不能把 images/foo.jpg 当成菜谱 images
        assertNull(RecipeCoverMatch.matchRecipeName("images/foo.jpg", idx));
    }

    @Test
    void unmatchedAndIgnored() {
        Map<String, String> idx = index("红烧肉");
        assertNull(RecipeCoverMatch.matchRecipeName("宫保鸡丁.jpg", idx));
        assertNull(RecipeCoverMatch.matchRecipeName("__MACOSX/红烧肉.jpg", idx));
        assertNull(RecipeCoverMatch.matchRecipeName(".DS_Store", idx));
        assertNull(RecipeCoverMatch.matchRecipeName("红烧肉.txt", idx));
    }

    @Test
    void filenameBeatsParent() {
        Map<String, String> idx = index("红烧肉", "番茄炒蛋");
        assertEquals("番茄炒蛋", RecipeCoverMatch.matchRecipeName("红烧肉/番茄炒蛋.jpg", idx));
    }

    @Test
    void coverScorePrefersNamedFile() {
        int named = RecipeCoverMatch.coverScore("红烧肉.jpg", "红烧肉");
        int cover = RecipeCoverMatch.coverScore("红烧肉/cover.jpg", "红烧肉");
        int other = RecipeCoverMatch.coverScore("红烧肉/2.jpg", "红烧肉");
        assertTrue(named > cover);
        assertTrue(cover > other);
    }

    @Test
    void pathHelpers() {
        assertTrue(RecipeCoverMatch.isImage("a.JPEG"));
        assertTrue(RecipeCoverMatch.isZip("pack.ZIP"));
        assertFalse(RecipeCoverMatch.isImage("a.pdf"));
        assertEquals("红烧肉", RecipeCoverMatch.basenameNoExt("dir\\红烧肉.jpg"));
        assertEquals("红烧肉", RecipeCoverMatch.parentName("root/红烧肉/cover.jpg"));
        assertTrue(RecipeCoverMatch.isIgnoredPath("../x.jpg"));
    }
}
