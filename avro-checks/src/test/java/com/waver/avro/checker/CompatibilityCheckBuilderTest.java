package com.waver.avro.checker;

import com.waver.avro.advice.MigrationAdvice;
import com.waver.avro.schema.AvroChecks;
import org.apache.avro.Schema;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the CompatibilityCheckBuilder fluent API.
 */
class CompatibilityCheckBuilderTest {

    private final Schema v1 = AvroChecks.parseSchema(
        "{\"type\":\"record\",\"name\":\"Order\",\"namespace\":\"com.waver.avro\","
        + "\"fields\":[{\"name\":\"id\",\"type\":\"string\"},"
        + "{\"name\":\"amount\",\"type\":\"double\"}]}");

    private final Schema v2WithDefault = AvroChecks.parseSchema(
        "{\"type\":\"record\",\"name\":\"Order\",\"namespace\":\"com.waver.avro\","
        + "\"fields\":[{\"name\":\"id\",\"type\":\"string\"},"
        + "{\"name\":\"amount\",\"type\":\"double\"},"
        + "{\"name\":\"currency\",\"type\":\"string\",\"default\":\"USD\"}]}");

    private final Schema v2Mandatory = AvroChecks.parseSchema(
        "{\"type\":\"record\",\"name\":\"Order\",\"namespace\":\"com.waver.avro\","
        + "\"fields\":[{\"name\":\"id\",\"type\":\"string\"},"
        + "{\"name\":\"amount\",\"type\":\"double\"},"
        + "{\"name\":\"currency\",\"type\":\"string\"}]}");

    @Test
    void builderWithSingleOlderSchema() {
        List<String> issues = AvroCompatibilityChecker.check()
            .forCandidate(v2WithDefault)
            .withCompatibility(CompatibilityLevel.BACKWARD)
            .withOlderSchema(v1)
            .check();

        assertTrue(issues.isEmpty(), "Adding field with default is backward compatible");
    }

    @Test
    void builderWithMultipleOlderSchemas() {
        List<String> issues = AvroCompatibilityChecker.check()
            .forCandidate(v2Mandatory)
            .withCompatibility(CompatibilityLevel.BACKWARD)
            .withOlderSchema(v1)
            .withOlderSchema(v1)  // duplicate for test
            .check();

        assertFalse(issues.isEmpty(), "Adding mandatory field is backward incompatible");
    }

    @Test
    void builderWithHistoryVarargs() {
        List<String> issues = AvroCompatibilityChecker.check()
            .forCandidate(v2WithDefault)
            .withCompatibility(CompatibilityLevel.FULL)
            .withHistory(v1)
            .check();

        assertTrue(issues.isEmpty(), "Adding field with default is FULL compatible");
    }

    @Test
    void builderWithHistoryList() {
        List<String> issues = AvroCompatibilityChecker.check()
            .forCandidate(v2Mandatory)
            .withCompatibility(CompatibilityLevel.BACKWARD)
            .withHistory(List.of(v1))
            .check();

        assertFalse(issues.isEmpty(), "Adding mandatory field is backward incompatible");
    }

    @Test
    void builderIsCompatibleReturnsTrue() {
        boolean compatible = AvroCompatibilityChecker.check()
            .forCandidate(v2WithDefault)
            .withCompatibility(CompatibilityLevel.BACKWARD)
            .withOlderSchema(v1)
            .isCompatible();

        assertTrue(compatible);
    }

    @Test
    void builderIsCompatibleReturnsFalse() {
        boolean compatible = AvroCompatibilityChecker.check()
            .forCandidate(v2Mandatory)
            .withCompatibility(CompatibilityLevel.BACKWARD)
            .withOlderSchema(v1)
            .isCompatible();

        assertFalse(compatible);
    }

    @Test
    void builderCheckWithAdvice() {
        List<MigrationAdvice> advice = AvroCompatibilityChecker.check()
            .forCandidate(v2Mandatory)
            .withCompatibility(CompatibilityLevel.BACKWARD)
            .withOlderSchema(v1)
            .checkWithAdvice();

        assertFalse(advice.isEmpty());
        assertTrue(advice.get(0).incompatibility().contains("currency"));
    }

    @Test
    void builderWithoutCandidateThrows() {
        assertThrows(IllegalStateException.class, () -> {
            AvroCompatibilityChecker.check()
                .withCompatibility(CompatibilityLevel.BACKWARD)
                .withOlderSchema(v1)
                .check();
        });
    }

    @Test
    void builderWithoutOlderSchemasReturnsEmpty() {
        List<String> issues = AvroCompatibilityChecker.check()
            .forCandidate(v2Mandatory)
            .withCompatibility(CompatibilityLevel.BACKWARD)
            .check();

        assertTrue(issues.isEmpty(), "No older schemas means nothing to check");
    }

    @Test
    void builderDefaultCompatibilityLevelIsBackward() {
        // Don't specify level - should default to BACKWARD
        List<String> issues = AvroCompatibilityChecker.check()
            .forCandidate(v2Mandatory)
            .withOlderSchema(v1)
            .check();

        assertFalse(issues.isEmpty(), "Default level is BACKWARD, so mandatory field is incompatible");
    }
}
