package com.waver.avro;

import com.waver.avro.schema.AvroChecks;
import org.apache.avro.Schema;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Test utility that loads Avro schema JSON from {@code src/test/resources/schemas/}.
 */
final class SchemaLoader {

    private SchemaLoader() {}

    /**
     * Loads and parses a schema from the test-resources {@code schemas/} folder.
     *
     * @param filename e.g. {@code "myrecord-v1.json"}
     * @return parsed {@link Schema}
     */
    static Schema load(String filename) {
        String path = "schemas/" + filename;
        try (InputStream is = SchemaLoader.class.getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                throw new IllegalArgumentException("Schema resource not found: " + path);
            }
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return AvroChecks.parseSchema(json);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load schema: " + path, e);
        }
    }
}
