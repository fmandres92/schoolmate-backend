package com.schoolmate.api.support;

import java.time.LocalDate;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdSerializer;

public final class TestJsonMapperFactory {

    private TestJsonMapperFactory() {
    }

    public static JsonMapper create() {
        return JsonMapper.builder()
            .addModule(localDateModule())
            .build();
    }

    private static SimpleModule localDateModule() {
        SimpleModule module = new SimpleModule();
        module.addSerializer(LocalDate.class, new StdSerializer<>(LocalDate.class) {
            @Override
            public void serialize(LocalDate value, tools.jackson.core.JsonGenerator gen,
                                  SerializationContext provider) throws JacksonException {
                gen.writeString(value != null ? value.toString() : null);
            }
        });
        module.addDeserializer(LocalDate.class, new StdDeserializer<>(LocalDate.class) {
            @Override
            public LocalDate deserialize(tools.jackson.core.JsonParser p,
                                         DeserializationContext ctxt) throws JacksonException {
                String raw = p.getValueAsString();
                return (raw == null || raw.isBlank()) ? null : LocalDate.parse(raw);
            }
        });
        return module;
    }
}
