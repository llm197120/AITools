package org.jeecg.modules.homeai.config.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HomeaiLearnRemindCronTest {

    @Test
    void defaultEveningCronMatches20() {
        assertTrue(HomeaiLearnRemindCron.isValid("0 0 20 * * ?"));
        assertTrue(HomeaiLearnRemindCron.matchesMinute("0 0 20 * * ?", LocalDateTime.of(2026, 9, 7, 20, 0, 30)));
        assertFalse(HomeaiLearnRemindCron.matchesMinute("0 0 20 * * ?", LocalDateTime.of(2026, 9, 7, 19, 59, 0)));
    }

    @Test
    void invalidCronRejected() {
        assertFalse(HomeaiLearnRemindCron.isValid(""));
        assertFalse(HomeaiLearnRemindCron.isValid("not-a-cron"));
    }
}
