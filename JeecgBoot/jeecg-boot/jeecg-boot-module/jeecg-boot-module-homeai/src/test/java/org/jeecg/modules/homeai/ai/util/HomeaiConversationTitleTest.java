package org.jeecg.modules.homeai.ai.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HomeaiConversationTitleTest {

    @Test
    void placeholderWhenEmptyOrMedia() {
        assertTrue(HomeaiConversationTitle.isPlaceholder(null));
        assertTrue(HomeaiConversationTitle.isPlaceholder("  新对话  "));
        assertEquals("新对话", HomeaiConversationTitle.fromUserMessage(null));
        assertEquals("新对话", HomeaiConversationTitle.fromUserMessage("[图片]"));
        assertEquals("新对话", HomeaiConversationTitle.fromUserMessage("  "));
    }

    @Test
    void usesQuestionAsTitleAndTruncates() {
        assertFalse(HomeaiConversationTitle.isPlaceholder("帮我写一份食谱"));
        assertEquals("帮我写一份食谱", HomeaiConversationTitle.fromUserMessage("帮我写一份食谱"));
        String longQ = "今天晚餐想吃点清淡的，再根据家里冰箱里的菜帮我推荐三道菜并写出步骤";
        String title = HomeaiConversationTitle.fromUserMessage(longQ);
        assertTrue(title.endsWith("…"));
        assertEquals(25, title.length());
    }
}
