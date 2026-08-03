package org.jeecg.config.converter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 自定义 LocalDate JSON 反序列化器，兼容 yyyy-MM-dd 和 yyyy-MM-dd HH:mm:ss 两种格式
 */
public class MultiFormatLocalDateDeserializer extends JsonDeserializer<LocalDate> {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String source = p.getText();
        if (source == null || source.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(source, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            try {
                return LocalDate.parse(source, DATE_FORMATTER);
            } catch (DateTimeParseException ex) {
                throw new IOException("Cannot deserialize LocalDate: " + source, ex);
            }
        }
    }
}
