package org.jeecg.modules.homeai.learn.util;

import org.jeecg.modules.homeai.recipe.entity.LearnRecord;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 结束学习写入 study_date，避免旧库无默认值插入失败。
 */
class LearnRecordAssemblerTest {

    @Test
    void writesStudyDateEvenWhenNoStart() {
        Date end = Date.from(LocalDate.of(2026, 8, 20)
                .atStartOfDay(ZoneId.systemDefault()).toInstant());
        LearnRecord rec = LearnRecordAssembler.fromTimerSession("u1", "m1", null, end);
        assertNull(rec.getStartTime());
        assertEquals(0, rec.getDuration());
        assertEquals("timer", rec.getMode());
        LocalDate study = new java.sql.Date(rec.getStudyDate().getTime()).toLocalDate();
        assertEquals(LocalDate.of(2026, 8, 20), study);
    }

    @Test
    void durationUsesElapsedSeconds() {
        Date end = new Date();
        long startMs = end.getTime() - 15_000;
        LearnRecord rec = LearnRecordAssembler.fromTimerSession("u1", "m1", startMs, end);
        assertEquals(15, rec.getDuration());
        assertNotNull(rec.getStudyDate());
        assertEquals(end, rec.getEndTime());
    }
}
