package org.jeecg.modules.homeai.learn.util;

import org.jeecg.modules.homeai.recipe.entity.LearnRecord;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/**
 * 结束学习时组装记录（纯逻辑，便于单测 study_date / duration）。
 */
public final class LearnRecordAssembler {

    private LearnRecordAssembler() {
    }

    public static LearnRecord fromTimerSession(String userId, String materialId, Long startMs, Date endTime) {
        Date end = endTime != null ? endTime : new Date();
        Date start = startMs == null ? null : new Date(startMs);
        int duration = 0;
        if (start != null) {
            duration = (int) Math.max(0, (end.getTime() - start.getTime()) / 1000);
        }
        LocalDate studyDay = end.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LearnRecord rec = new LearnRecord();
        rec.setUserId(userId);
        rec.setMaterialId(materialId);
        rec.setStartTime(start);
        rec.setEndTime(end);
        rec.setDuration(duration);
        rec.setMode("timer");
        rec.setCreateTime(end);
        rec.setStudyDate(java.sql.Date.valueOf(studyDay));
        return rec;
    }
}
