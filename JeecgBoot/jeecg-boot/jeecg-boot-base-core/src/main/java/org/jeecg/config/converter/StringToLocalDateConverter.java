package org.jeecg.config.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 字符串转 LocalDate 转换器，兼容 yyyy-MM-dd 和 yyyy-MM-dd HH:mm:ss 两种格式
 */
@Component
public class StringToLocalDateConverter implements Converter<String, LocalDate> {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public LocalDate convert(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(source, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            return LocalDate.parse(source, DATE_FORMATTER);
        }
    }
}
