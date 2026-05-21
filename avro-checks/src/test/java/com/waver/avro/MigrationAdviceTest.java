package com.waver.avro;

import com.waver.avro.advice.MigrationAdvice;
import com.waver.avro.checker.AvroCompatibilityChecker;
import com.waver.avro.checker.CompatibilityLevel;
import org.apache.avro.Schema;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for migration advice output on incompatibility detection.
 * Schema files live in src/test/resources/schemas/.
 */
class MigrationAdviceTest {

    private final Schema schemaV1             = SchemaLoader.load("order-v1.json");
    private final Schema schemaV2MandatoryField = SchemaLoader.load("order-v2-mandatory-field.json");
    private final Schema schemaV2OptionalField  = SchemaLoader.load("order-v2-optional-field.json");
    private final Schema schemaUnionV1        = SchemaLoader.load("item-v1-union-null-string.json");
    private final Schema schemaPlainString    = SchemaLoader.load("item-v2-plain-string.json");

    // ── BACKWARD: adding mandatory field ─────────────────────────────────────

    @Test
    void backwardMandatoryFieldAdviceIsProvided() {
        List<MigrationAdvice> advice = AvroCompatibilityChecker.checkWithAdvice(
            schemaV2MandatoryField, schemaV1, CompatibilityLevel.BACKWARD);

        assertFalse(advice.isEmpty(), "Should detect incompatibility");
        MigrationAdvice first = advice.get(0);
        assertNotNull(first.incompatibility());
        assertFalse(first.steps().isEmpty(), "Should provide migration steps");
    }

    @Test
    void backwardMandatoryFieldAdviceMentionsMigrationPath() {
        List<MigrationAdvice> advice = AvroCompatibilityChecker.checkWithAdvice(
            schemaV2MandatoryField, schemaV1, CompatibilityLevel.BACKWARD);

        String allSteps = String.join(" ", advice.get(0).steps());
        assertTrue(allSteps.contains("default"),
            "Advice should suggest adding a default value");
        assertTrue(allSteps.contains("currency"),
            "Advice should mention the offending field name");
    }

    @Test
    void backwardCompatibleChangeProducesNoAdvice() {
        List<MigrationAdvice> advice = AvroCompatibilityChecker.checkWithAdvice(
            schemaV2OptionalField, schemaV1, CompatibilityLevel.BACKWARD);

        assertTrue(advice.isEmpty(), "No advice for compatible change");
    }

    // ── FORWARD: removing required field ─────────────────────────────────────

    @Test
    void forwardRemovingFieldAdviceIsProvided() {
        List<MigrationAdvice> advice = AvroCompatibilityChecker.checkWithAdvice(
            schemaV1, schemaV2MandatoryField, CompatibilityLevel.FORWARD);

        assertFalse(advice.isEmpty(), "Should detect FORWARD incompatibility");
        assertFalse(advice.get(0).steps().isEmpty());
    }

    // ── Union narrowing ───────────────────────────────────────────────────────

    @Test
    void backwardUnionNarrowingAdviceIsProvided() {
        List<MigrationAdvice> advice = AvroCompatibilityChecker.checkWithAdvice(
            schemaPlainString, schemaUnionV1, CompatibilityLevel.BACKWARD);

        assertFalse(advice.isEmpty(), "Narrowing union is incompatible");
        String allSteps = String.join(" ", advice.get(0).steps());
        assertTrue(allSteps.contains("union"),
            "Advice should refer to union handling");
    }

    // ── toString ─────────────────────────────────────────────────────────────

    @Test
    void migrationAdviceToStringIsReadable() {
        List<MigrationAdvice> advice = AvroCompatibilityChecker.checkWithAdvice(
            schemaV2MandatoryField, schemaV1, CompatibilityLevel.BACKWARD);

        String rendered = advice.get(0).toString();
        assertTrue(rendered.contains("Issue"), "Should start with Issue label");
        assertTrue(rendered.contains("Advice"), "Should include Advice section");
        assertTrue(rendered.contains("1."), "Should include numbered steps");
    }
}
