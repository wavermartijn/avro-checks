package com.waver.avro.cli;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class MainTest {

    private static final String SCHEMA_V1 =
        "{\"type\":\"record\",\"name\":\"Order\",\"namespace\":\"com.waver.avro\","
        + "\"fields\":[{\"name\":\"id\",\"type\":\"string\"}]}";

    private static final String SCHEMA_V2 =
        "{\"type\":\"record\",\"name\":\"Order\",\"namespace\":\"com.waver.avro\","
        + "\"fields\":[{\"name\":\"id\",\"type\":\"string\"},"
        + "{\"name\":\"amount\",\"type\":\"double\",\"default\":0.0}]}";

    private static final String SCHEMA_V3 =
        "{\"type\":\"record\",\"name\":\"Order\",\"namespace\":\"com.waver.avro\","
        + "\"fields\":[{\"name\":\"id\",\"type\":\"string\"},"
        + "{\"name\":\"amount\",\"type\":\"double\"}]}";

    @Test
    void compatibleChangeReturnsExitCode0() {
        assertEquals(0, Main.run(new String[]{SCHEMA_V2, SCHEMA_V1, "BACKWARD"}));
    }

    @Test
    void incompatibleChangeReturnsExitCode1() {
        assertEquals(1, Main.run(new String[]{SCHEMA_V3, SCHEMA_V1, "BACKWARD"}));
    }

    @Test
    void missingArgsReturnsExitCode2() {
        assertEquals(2, Main.run(new String[]{}));
    }

    @Test
    void unknownLevelReturnsExitCode2() {
        assertEquals(2, Main.run(new String[]{SCHEMA_V2, SCHEMA_V1, "BOGUS"}));
    }

    @Test
    void defaultLevelIsBackward() {
        assertEquals(0, Main.run(new String[]{SCHEMA_V2, SCHEMA_V1}));
    }

    @Test
    void helpFlagReturnsExitCode0() {
        assertEquals(0, Main.run(new String[]{"--help"}));
    }

    @Test
    void versionIsNotUnknown() {
        assertNotEquals("unknown", Main.VERSION,
            "VERSION resource must be present in the jar resources");
    }
}
