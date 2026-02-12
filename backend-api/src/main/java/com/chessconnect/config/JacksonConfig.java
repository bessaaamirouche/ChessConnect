package com.chessconnect.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Jackson configuration to serialize LocalDateTime with Europe/Paris timezone offset.
 * This ensures clients in any timezone correctly parse dates.
 *
 * Before: "2026-02-10T20:00:00"       → parsed as local time (wrong for non-French users)
 * After:  "2026-02-10T20:00:00+01:00" → parsed as UTC 19:00 everywhere (correct)
 */
@Configuration
public class JacksonConfig {

    private static final ZoneId PARIS_ZONE = ZoneId.of("Europe/Paris");

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer localDateTimeSerializerCustomizer() {
        return builder -> {
            SimpleModule module = new SimpleModule("LocalDateTimeTimezoneModule");
            module.addSerializer(LocalDateTime.class, new LocalDateTimeWithOffsetSerializer());
            module.addDeserializer(LocalDateTime.class, new FlexibleLocalDateTimeDeserializer());
            module.addSerializer(LocalTime.class, new LocalTimeSerializer());
            module.addDeserializer(LocalTime.class, new LocalTimeDeserializer());
            module.addSerializer(LocalDate.class, new LocalDateSerializer());
            module.addDeserializer(LocalDate.class, new LocalDateDeserializer());
            builder.modules(module);
        };
    }

    /**
     * Serializes LocalDateTime with the Europe/Paris timezone offset.
     * Handles both winter (CET, +01:00) and summer (CEST, +02:00) offsets automatically.
     */
    static class LocalDateTimeWithOffsetSerializer extends JsonSerializer<LocalDateTime> {

        @Override
        public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            ZonedDateTime zdt = value.atZone(PARIS_ZONE);
            gen.writeString(zdt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        }
    }

    /**
     * Deserializes LocalDateTime from both formats:
     * - "2026-02-10T20:00:00"        → parsed directly as LocalDateTime
     * - "2026-02-10T20:00:00+01:00"  → offset stripped, converted to Paris time
     */
    static class FlexibleLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

        @Override
        public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String text = p.getText().trim();

            // Try parsing with offset first (e.g., "2026-02-10T20:00:00+01:00")
            try {
                OffsetDateTime odt = OffsetDateTime.parse(text);
                return odt.atZoneSameInstant(PARIS_ZONE).toLocalDateTime();
            } catch (DateTimeParseException ignored) {
                // Not an offset date, try without offset
            }

            // Parse without offset (e.g., "2026-02-10T20:00:00")
            return LocalDateTime.parse(text);
        }
    }

    static class LocalTimeSerializer extends JsonSerializer<LocalTime> {
        @Override
        public void serialize(LocalTime value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString(value.format(DateTimeFormatter.ISO_LOCAL_TIME));
        }
    }

    static class LocalTimeDeserializer extends JsonDeserializer<LocalTime> {
        @Override
        public LocalTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            return LocalTime.parse(p.getText().trim());
        }
    }

    static class LocalDateSerializer extends JsonSerializer<LocalDate> {
        @Override
        public void serialize(LocalDate value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString(value.format(DateTimeFormatter.ISO_LOCAL_DATE));
        }
    }

    static class LocalDateDeserializer extends JsonDeserializer<LocalDate> {
        @Override
        public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            return LocalDate.parse(p.getText().trim());
        }
    }
}
