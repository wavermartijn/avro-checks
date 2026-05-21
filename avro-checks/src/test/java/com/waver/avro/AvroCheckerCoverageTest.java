package com.waver.avro;

import org.apache.avro.Schema;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Targeted tests to cover enums, arrays, maps, type promotion, all compatibility
 * levels, CompatibilityResult, and remaining MigrationAdvisor branches.
 */
class AvroCheckerCoverageTest {

    // ── Schema fixtures ───────────────────────────────────────────────────────

    private final Schema enumV1          = SchemaLoader.load("myrecord-enum-v1.json");
    private final Schema enumV2Removed   = SchemaLoader.load("myrecord-enum-v2-symbol-removed.json");
    private final Schema arrayInt        = SchemaLoader.load("myrecord-array-int.json");
    private final Schema arrayLong       = SchemaLoader.load("myrecord-array-long.json");
    private final Schema arrayString     = SchemaLoader.load("myrecord-array-string.json");
    private final Schema mapString       = SchemaLoader.load("myrecord-map-string.json");
    private final Schema mapInt          = SchemaLoader.load("myrecord-map-int.json");

    private final Schema v1  = SchemaLoader.load("myrecord-v1.json");
    private final Schema v2  = SchemaLoader.load("myrecord-v2-field-with-default.json");
    private final Schema v3  = SchemaLoader.load("myrecord-v3-field-no-default.json");
    private final Schema v8  = SchemaLoader.load("myrecord-v8-three-fields-defaults.json");

    // ── CompatibilityResult ───────────────────────────────────────────────────

    @Test
    void compatibilityResultOk() {
        CompatibilityResult r = CompatibilityResult.ok();
        assertTrue(r.compatible());
        assertTrue(r.incompatibilities().isEmpty());
    }

    @Test
    void compatibilityResultFail() {
        CompatibilityResult r = CompatibilityResult.fail(List.of("reason"));
        assertFalse(r.compatible());
        assertEquals(1, r.incompatibilities().size());
    }

    // ── NONE level ────────────────────────────────────────────────────────────

    @Test
    void noneLevelAlwaysCompatible() {
        assertTrue(AvroCompatibilityChecker.isCompatible(v3, v1, CompatibilityLevel.NONE));
    }

    // ── FULL level ────────────────────────────────────────────────────────────

    @Test
    void fullCompatibleChange() {
        assertTrue(AvroCompatibilityChecker.isCompatible(v2, v1, CompatibilityLevel.FULL));
    }

    @Test
    void fullIncompatibleChange() {
        assertFalse(AvroCompatibilityChecker.isCompatible(v3, v1, CompatibilityLevel.FULL));
    }

    // ── FULL_TRANSITIVE level ─────────────────────────────────────────────────

    @Test
    void fullTransitiveCompatible() {
        assertTrue(AvroCompatibilityChecker.isCompatible(
            v8, List.of(v1, v2), CompatibilityLevel.FULL_TRANSITIVE));
    }

    @Test
    void fullTransitiveIncompatible() {
        assertFalse(AvroCompatibilityChecker.isCompatible(
            v3, List.of(v2, v1), CompatibilityLevel.FULL_TRANSITIVE));
    }

    // ── Enum ──────────────────────────────────────────────────────────────────

    @Test
    void enumSymbolRemovedIsBackwardIncompatible() {
        // BACKWARD: new=reader=enumV1(A,B,C), prev=writer=enumV2Removed(A,B)
        // reader expects C but writer cannot produce it -> incompatible
        List<String> issues = AvroCompatibilityChecker.check(
            enumV1, enumV2Removed, CompatibilityLevel.BACKWARD);
        assertFalse(issues.isEmpty(), "reader has symbol C not in writer -> incompatible");
        assertTrue(issues.get(0).contains("enum symbol"));
    }

    @Test
    void enumSymbolAddedIsForwardCompatible() {
        // FORWARD: new=writer=enumV1(A,B,C), prev=reader=enumV2Removed(A,B)
        // old reader symbols A,B are all in new writer -> forward compatible
        assertTrue(AvroCompatibilityChecker.isCompatible(
            enumV1, enumV2Removed, CompatibilityLevel.FORWARD),
            "old reader symbols are all present in new writer -> forward compatible");
    }

    // ── Array ─────────────────────────────────────────────────────────────────

    @Test
    void arrayCompatiblePromotion() {
        assertTrue(AvroCompatibilityChecker.isCompatible(
            arrayLong, arrayInt, CompatibilityLevel.BACKWARD),
            "int->long promotion in array is backward compatible");
    }

    @Test
    void arrayIncompatibleTypeMismatch() {
        assertFalse(AvroCompatibilityChecker.isCompatible(
            arrayString, arrayInt, CompatibilityLevel.BACKWARD),
            "string vs int in array is incompatible");
    }

    // ── Map ───────────────────────────────────────────────────────────────────

    @Test
    void mapCompatibleSameType() {
        assertTrue(AvroCompatibilityChecker.isCompatible(
            mapString, mapString, CompatibilityLevel.BACKWARD));
    }

    @Test
    void mapIncompatibleTypeMismatch() {
        assertFalse(AvroCompatibilityChecker.isCompatible(
            mapString, mapInt, CompatibilityLevel.BACKWARD),
            "string vs int map values is incompatible");
    }

    // ── Type promotion ────────────────────────────────────────────────────────

    @Test
    void intToFloatPromotion() {
        Schema intSchema   = Schema.create(Schema.Type.INT);
        Schema floatSchema = Schema.create(Schema.Type.FLOAT);
        Schema longSchema  = Schema.create(Schema.Type.LONG);
        Schema doubleSchema = Schema.create(Schema.Type.DOUBLE);

        Schema writerInt  = wrapPrimitive(intSchema);
        Schema readerFloat = wrapPrimitive(floatSchema);
        Schema readerLong  = wrapPrimitive(longSchema);
        Schema readerDouble = wrapPrimitive(doubleSchema);

        assertTrue(AvroCompatibilityChecker.isCompatible(readerFloat,  writerInt, CompatibilityLevel.BACKWARD));
        assertTrue(AvroCompatibilityChecker.isCompatible(readerLong,   writerInt, CompatibilityLevel.BACKWARD));
        assertTrue(AvroCompatibilityChecker.isCompatible(readerDouble, writerInt, CompatibilityLevel.BACKWARD));
    }

    @Test
    void longToFloatDoublePromotion() {
        Schema writerLong   = wrapPrimitive(Schema.create(Schema.Type.LONG));
        Schema readerFloat  = wrapPrimitive(Schema.create(Schema.Type.FLOAT));
        Schema readerDouble = wrapPrimitive(Schema.create(Schema.Type.DOUBLE));

        assertTrue(AvroCompatibilityChecker.isCompatible(readerFloat,  writerLong, CompatibilityLevel.BACKWARD));
        assertTrue(AvroCompatibilityChecker.isCompatible(readerDouble, writerLong, CompatibilityLevel.BACKWARD));
    }

    @Test
    void floatToDoublePromotion() {
        Schema writerFloat  = wrapPrimitive(Schema.create(Schema.Type.FLOAT));
        Schema readerDouble = wrapPrimitive(Schema.create(Schema.Type.DOUBLE));

        assertTrue(AvroCompatibilityChecker.isCompatible(readerDouble, writerFloat, CompatibilityLevel.BACKWARD));
    }

    @Test
    void stringBytesPromotion() {
        Schema writerString = wrapPrimitive(Schema.create(Schema.Type.STRING));
        Schema readerBytes  = wrapPrimitive(Schema.create(Schema.Type.BYTES));
        Schema writerBytes  = wrapPrimitive(Schema.create(Schema.Type.BYTES));
        Schema readerString = wrapPrimitive(Schema.create(Schema.Type.STRING));

        assertTrue(AvroCompatibilityChecker.isCompatible(readerBytes,  writerString, CompatibilityLevel.BACKWARD));
        assertTrue(AvroCompatibilityChecker.isCompatible(readerString, writerBytes,  CompatibilityLevel.BACKWARD));
    }

    // ── MigrationAdvisor: remaining branches ─────────────────────────────────

    @Test
    void adviceForEnumSymbolRemoved() {
        // reader=enumV1(A,B,C), writer=enumV2Removed(A,B) -> C missing from writer
        List<MigrationAdvice> advice = AvroCompatibilityChecker.checkWithAdvice(
            enumV1, enumV2Removed, CompatibilityLevel.BACKWARD);
        assertFalse(advice.isEmpty());
        String steps = String.join(" ", advice.get(0).steps());
        assertTrue(steps.contains("default") || steps.contains("enum"),
            "Advice should mention enum or default");
    }

    @Test
    void adviceForTypeMismatch() {
        List<MigrationAdvice> advice = AvroCompatibilityChecker.checkWithAdvice(
            arrayString, arrayInt, CompatibilityLevel.BACKWARD);
        assertFalse(advice.isEmpty());
        String steps = String.join(" ", advice.get(0).steps());
        assertTrue(steps.contains("union") || steps.contains("type"),
            "Advice should mention type change");
    }

    @Test
    void adviceForWriterUnionBranchNotCoveredByReaderUnion() {
        Schema writerUnion = AvroChecks.parseSchema(
            "{\"type\":\"record\",\"name\":\"R\",\"fields\":"
            + "[{\"name\":\"v\",\"type\":[\"null\",\"string\",\"int\"]}]}");
        Schema readerUnion = AvroChecks.parseSchema(
            "{\"type\":\"record\",\"name\":\"R\",\"fields\":"
            + "[{\"name\":\"v\",\"type\":[\"null\",\"string\"]}]}");

        List<MigrationAdvice> advice = AvroCompatibilityChecker.checkWithAdvice(
            readerUnion, writerUnion, CompatibilityLevel.BACKWARD);
        assertFalse(advice.isEmpty());
    }

    @Test
    void adviceForForwardMandatoryFieldRemoved() {
        List<MigrationAdvice> advice = AvroCompatibilityChecker.checkWithAdvice(
            v1, v3, CompatibilityLevel.FORWARD);
        assertFalse(advice.isEmpty());
        String steps = String.join(" ", advice.get(0).steps());
        assertTrue(steps.contains("f2") || steps.contains("writer") || steps.contains("reader"));
    }

    @Test
    void genericAdviceForUnknownMessage() {
        MigrationAdvice advice = MigrationAdvisor.advise("some unknown issue", CompatibilityLevel.NONE);
        assertFalse(advice.steps().isEmpty());
        assertTrue(advice.steps().get(0).contains("some unknown issue"));
    }

    // ── Record name alias matching ────────────────────────────────────────────

    @Test
    void recordWriterAliasMatchesReaderName() {
        Schema writer = AvroChecks.parseSchema(
            "{\"type\":\"record\",\"name\":\"OldName\",\"fields\":["
            + "{\"name\":\"id\",\"type\":\"string\"}]}");
        Schema reader = AvroChecks.parseSchema(
            "{\"type\":\"record\",\"name\":\"NewName\",\"aliases\":[\"OldName\"],\"fields\":["
            + "{\"name\":\"id\",\"type\":\"string\"}]}");
        assertTrue(AvroCompatibilityChecker.isCompatible(reader, writer, CompatibilityLevel.BACKWARD),
            "reader alias covering writer name is compatible");
    }

    @Test
    void recordNameMismatchIsIncompatible() {
        Schema a = AvroChecks.parseSchema(
            "{\"type\":\"record\",\"name\":\"Foo\",\"fields\":["
            + "{\"name\":\"id\",\"type\":\"string\"}]}");
        Schema b = AvroChecks.parseSchema(
            "{\"type\":\"record\",\"name\":\"Bar\",\"fields\":["
            + "{\"name\":\"id\",\"type\":\"string\"}]}");
        assertFalse(AvroCompatibilityChecker.isCompatible(a, b, CompatibilityLevel.BACKWARD),
            "different record names without aliases are incompatible");
    }

    @Test
    void fullWithMultiplePreviousSchemasHitsMerge() {
        // Generates issues from both BACKWARD and FORWARD checks, exercises merge()
        List<String> issues = AvroCompatibilityChecker.check(
            v3, List.of(v1), CompatibilityLevel.FULL);
        assertFalse(issues.isEmpty());
    }

    // ── isCompatible transitive overloads ────────────────────────────────────

    @Test
    void isCompatibleTransitiveOverload() {
        assertTrue(AvroCompatibilityChecker.isCompatible(
            v2, List.of(v1), CompatibilityLevel.BACKWARD_TRANSITIVE));
        assertFalse(AvroCompatibilityChecker.isCompatible(
            v3, List.of(v2, v1), CompatibilityLevel.BACKWARD_TRANSITIVE));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static Schema wrapPrimitive(Schema primitive) {
        return AvroChecks.parseSchema(
            "{\"type\":\"record\",\"name\":\"R\",\"fields\":"
            + "[{\"name\":\"v\",\"type\":\"" + primitive.getType().getName() + "\"}]}");
    }
}
