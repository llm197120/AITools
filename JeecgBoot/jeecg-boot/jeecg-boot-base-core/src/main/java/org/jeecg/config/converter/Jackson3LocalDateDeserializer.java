package org.jeecg.config.converter;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Jackson 3 自定义 LocalDate 反序列化器，兼容 yyyy-MM-dd 和 yyyy-MM-dd HH:mm:ss 两种格式
 *
 * <p>项目已升级到 Spring Boot 4（Jackson 3，tools.jackson 命名空间），
 * HTTP JSON 消息转换器使用自动配置的 JsonMapper，
 * 必须通过 JsonMapperBuilderCustomizer 注册本反序列化器才能生效。
 */
public class Jackson3LocalDateDeserializer extends ValueDeserializer<LocalDate> {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) {
        String source = p.getString();
        if (source == null || source.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(source, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            try {
                return LocalDate.parse(source, DATE_FORMATTER);
            } catch (DateTimeParseException ex) {
                throw new IllegalArgumentException("Cannot deserialize LocalDate: " + source, ex);
            }
        }
    }
}
