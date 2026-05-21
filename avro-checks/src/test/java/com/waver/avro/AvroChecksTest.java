package com.waver.avro;

import com.waver.avro.schema.AvroChecks;
import org.apache.avro.Schema;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AvroChecksTest {

    @Test
    void parseSchemaParsesValidJson() {
        Schema schema = AvroChecks.parseSchema(
            "{\"type\":\"record\",\"name\":\"Test\",\"fields\":[{\"name\":\"id\",\"type\":\"int\"}]}");
        assertNotNull(schema);
        assertEquals("Test", schema.getName());
    }
}
