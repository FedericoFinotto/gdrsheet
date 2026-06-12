package it.fin8.gdrsheet.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class JacksonConfig {

    /**
     * Nei payload in ingresso le stringhe vuote (o di soli spazi) diventano
     * null: un campo lasciato vuoto non deve sporcare il DB con "".
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer emptyStringAsNull() {
        return builder -> builder.deserializerByType(String.class, new StdDeserializer<String>(String.class) {
            @Override
            public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                String v = p.getValueAsString();
                if (v == null || v.trim().isEmpty()) return null;
                return v;
            }
        });
    }
}
