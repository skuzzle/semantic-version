package de.skuzzle.semantic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Type;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class CustomGsonSerialization {

    private static class SemanticVersionSerializer implements JsonSerializer<Version>, JsonDeserializer<Version> {

        @Override
        public Version deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {

            final String versionString = json.getAsString();
            return Version.parseVersion(versionString);
        }

        @Override
        public JsonElement serialize(Version src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.toString());
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
    }

    @Test
    public void testCustomGsonSerialization() throws Exception {
        final Gson gson = new GsonBuilder()
                .registerTypeAdapter(Version.class, new SemanticVersionSerializer())
                .create();

        final ObjectWithVersionField object = new ObjectWithVersionField(Version.create(1, 2, 3, "pre-release"),
                "someString");
        final String json = gson.toJson(object);
        final ObjectWithVersionField object2 = gson.fromJson(json, ObjectWithVersionField.class);
        assertEquals(object.getVersion(), object2.getVersion());
    }
}
