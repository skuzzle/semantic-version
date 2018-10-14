package de.skuzzle.semantic;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class CustomJacksonSerialization {

    private static class SemanticVersionDeserializer extends JsonDeserializer<Version> {

        @Override
        public Version deserialize(JsonParser p, DeserializationContext ctxt)
                throws IOException, JsonProcessingException {

            final String versionString = p.readValueAs(String.class);
            return Version.parseVersion(versionString);
        }
    }

    private static final class SemanticVersionSerializer extends JsonSerializer<Version> {

        @Override
        public void serialize(Version value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString(value.toString());
        }

    }

    private static final class ObjectWithVersionField {
        private Version version;
        private String differentField;

        public ObjectWithVersionField() {
        }

        public ObjectWithVersionField(Version version, String differentField) {
            this.version = version;
            this.differentField = differentField;
        }

        public Version getVersion() {
            return this.version;
        }

        public String getDifferentField() {
            return this.differentField;
        }

    }

    @Test
    public void testCustomGsonSerialization() throws Exception {
        final SimpleModule module = new SimpleModule();
        module.addDeserializer(Version.class, new SemanticVersionDeserializer());
        module.addSerializer(Version.class, new SemanticVersionSerializer());
        final ObjectMapper objectMapper = new ObjectMapper().registerModule(module);

        final ObjectWithVersionField object = new ObjectWithVersionField(Version.create(1, 2, 3, "pre-release"),
                "someString");
        final String json = objectMapper.writeValueAsString(object);
        final ObjectWithVersionField object2 = objectMapper.readValue(json, ObjectWithVersionField.class);
        assertThat(object.getVersion(), is(object2.getVersion()));
    }
}
