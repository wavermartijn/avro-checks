package com.waver.avro;

import org.apache.avro.Schema;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Compatibility tests ported from the Confluent Schema Registry:
 * https://github.com/confluentinc/schema-registry/blob/master/core/src/test/java/io/confluent/kafka/schemaregistry/avro/AvroCompatibilityTest.java
 *
 * Adapted to use com.waver.avro.AvroCompatibilityChecker and JUnit 5.
 */
class AvroCompatibilityTest {

    // schema1 — single required string field f1
    private final Schema schema1 = AvroChecks.parseSchema(
        "{\"type\":\"record\","
        + "\"name\":\"myrecord\","
        + "\"fields\":"
        + "[{\"type\":\"string\",\"name\":\"f1\"}]}");

    // schema2 — f1 + f2 (string, default "foo")
    private final Schema schema2 = AvroChecks.parseSchema(
        "{\"type\":\"record\","
        + "\"name\":\"myrecord\","
        + "\"fields\":"
        + "[{\"type\":\"string\",\"name\":\"f1\"},"
        + " {\"type\":\"string\",\"name\":\"f2\", \"default\": \"foo\"}]}");

    // schema3 — f1 + f2 (string, NO default)
    private final Schema schema3 = AvroChecks.parseSchema(
        "{\"type\":\"record\","
        + "\"name\":\"myrecord\","
        + "\"fields\":"
        + "[{\"type\":\"string\",\"name\":\"f1\"},"
        + " {\"type\":\"string\",\"name\":\"f2\"}]}");

    // schema4 — f1 renamed to f1_new with alias f1
    private final Schema schema4 = AvroChecks.parseSchema(
        "{\"type\":\"record\","
        + "\"name\":\"myrecord\","
        + "\"fields\":"
        + "[{\"type\":\"string\",\"name\":\"f1_new\", \"aliases\": [\"f1\"]}]}");

    // schema6 — f1 as union [null, string]
    private final Schema schema6 = AvroChecks.parseSchema(
        "{\"type\":\"record\","
        + "\"name\":\"myrecord\","
        + "\"fields\":"
        + "[{\"type\":[\"null\", \"string\"],\"name\":\"f1\","
        + " \"doc\":\"doc of f1\"}]}");

    // schema7 — f1 as union [null, string, int]
    private final Schema schema7 = AvroChecks.parseSchema(
        "{\"type\":\"record\","
        + "\"name\":\"myrecord\","
        + "\"fields\":"
        + "[{\"type\":[\"null\", \"string\", \"int\"],\"name\":\"f1\","
        + " \"doc\":\"doc of f1\"}]}");

    // schema8 — f1 + f2 (default "foo") + f3 (default "bar")
    private final Schema schema8 = AvroChecks.parseSchema(
        "{\"type\":\"record\","
        + "\"name\":\"myrecord\","
        + "\"fields\":"
        + "[{\"type\":\"string\",\"name\":\"f1\"},"
        + " {\"type\":\"string\",\"name\":\"f2\", \"default\": \"foo\"},"
        + " {\"type\":\"string\",\"name\":\"f3\", \"default\": \"bar\"}]}");

    // -------------------------------------------------------------------------
    // BACKWARD
    // -------------------------------------------------------------------------

    @Test
    void testBasicBackwardsCompatibility() {
        assertTrue(
            check(schema2, list(schema1), CompatibilityLevel.BACKWARD).isEmpty(),
            "adding a field with default is a backward compatible change");

        assertFalse(
            check(schema3, list(schema1), CompatibilityLevel.BACKWARD).isEmpty(),
            "adding a field w/o default is not a backward compatible change");

        assertTrue(
            check(schema4, list(schema1), CompatibilityLevel.BACKWARD).isEmpty(),
            "changing field name with alias is a backward compatible change");

        assertTrue(
            check(schema6, list(schema1), CompatibilityLevel.BACKWARD).isEmpty(),
            "evolving a field type to a union is a backward compatible change");

        assertFalse(
            check(schema1, list(schema6), CompatibilityLevel.BACKWARD).isEmpty(),
            "removing a type from a union is not a backward compatible change");

        assertTrue(
            check(schema7, list(schema6), CompatibilityLevel.BACKWARD).isEmpty(),
            "adding a new type in union is a backward compatible change");

        assertFalse(
            check(schema6, list(schema7), CompatibilityLevel.BACKWARD).isEmpty(),
            "removing a type from a union is not a backward compatible change");

        // Only the last (newest) schema is checked for non-transitive backward
        assertTrue(
            check(schema3, list(schema1, schema2), CompatibilityLevel.BACKWARD).isEmpty(),
            "removing a default is not a transitively compatible change (non-transitive: only newest checked)");
    }

    // -------------------------------------------------------------------------
    // BACKWARD_TRANSITIVE
    // -------------------------------------------------------------------------

    @Test
    void testBasicBackwardsTransitiveCompatibility() {
        assertTrue(
            check(schema8, list(schema1, schema2), CompatibilityLevel.BACKWARD_TRANSITIVE).isEmpty(),
            "iteratively adding fields with defaults is a compatible change");

        assertTrue(
            check(schema2, list(schema1), CompatibilityLevel.BACKWARD_TRANSITIVE).isEmpty(),
            "adding a field with default is a backward compatible change");

        assertTrue(
            check(schema3, list(schema2), CompatibilityLevel.BACKWARD_TRANSITIVE).isEmpty(),
            "removing a default is a compatible change, but not transitively");

        assertFalse(
            check(schema3, list(schema2, schema1), CompatibilityLevel.BACKWARD_TRANSITIVE).isEmpty(),
            "removing a default is not a transitively compatible change");
    }

    // -------------------------------------------------------------------------
    // FORWARD
    // -------------------------------------------------------------------------

    @Test
    void testBasicForwardsCompatibility() {
        assertTrue(
            check(schema2, list(schema1), CompatibilityLevel.FORWARD).isEmpty(),
            "adding a field is a forward compatible change");

        assertTrue(
            check(schema3, list(schema1), CompatibilityLevel.FORWARD).isEmpty(),
            "adding a field is a forward compatible change");

        assertTrue(
            check(schema3, list(schema2), CompatibilityLevel.FORWARD).isEmpty(),
            "adding a field is a forward compatible change");

        assertTrue(
            check(schema2, list(schema3), CompatibilityLevel.FORWARD).isEmpty(),
            "adding a field is a forward compatible change");

        // Only the last (newest) schema is checked for non-transitive forward
        assertTrue(
            check(schema1, list(schema3, schema2), CompatibilityLevel.FORWARD).isEmpty(),
            "removing a default is not a transitively compatible change (non-transitive: only newest checked)");
    }

    // -------------------------------------------------------------------------
    // FORWARD_TRANSITIVE
    // -------------------------------------------------------------------------

    @Test
    void testBasicForwardsTransitiveCompatibility() {
        assertTrue(
            check(schema1, list(schema8, schema2), CompatibilityLevel.FORWARD_TRANSITIVE).isEmpty(),
            "iteratively removing fields with defaults is a compatible change");

        assertTrue(
            check(schema2, list(schema3), CompatibilityLevel.FORWARD_TRANSITIVE).isEmpty(),
            "adding default to a field is a compatible change");

        assertTrue(
            check(schema1, list(schema2), CompatibilityLevel.FORWARD_TRANSITIVE).isEmpty(),
            "removing a field with a default is a compatible change");

        assertFalse(
            check(schema1, list(schema2, schema3), CompatibilityLevel.FORWARD_TRANSITIVE).isEmpty(),
            "removing a default is not a transitively compatible change");
    }

    // -------------------------------------------------------------------------
    // FULL
    // -------------------------------------------------------------------------

    @Test
    void testBasicFullCompatibility() {
        assertTrue(
            check(schema2, list(schema1), CompatibilityLevel.FULL).isEmpty(),
            "adding a field with default is a backward and a forward compatible change");

        // Only the newest schema is checked (non-transitive)
        assertTrue(
            check(schema3, list(schema1, schema2), CompatibilityLevel.FULL).isEmpty(),
            "transitively adding a field without a default is not a compatible change (non-transitive: only newest checked)");

        assertTrue(
            check(schema1, list(schema3, schema2), CompatibilityLevel.FULL).isEmpty(),
            "transitively removing a field without a default is not a compatible change (non-transitive: only newest checked)");
    }

    // -------------------------------------------------------------------------
    // FULL_TRANSITIVE
    // -------------------------------------------------------------------------

    @Test
    void testBasicFullTransitiveCompatibility() {
        assertTrue(
            check(schema8, list(schema1, schema2), CompatibilityLevel.FULL_TRANSITIVE).isEmpty(),
            "iteratively adding fields with defaults is a compatible change");

        assertTrue(
            check(schema1, list(schema8, schema2), CompatibilityLevel.FULL_TRANSITIVE).isEmpty(),
            "iteratively removing fields with defaults is a compatible change");

        assertTrue(
            check(schema2, list(schema3), CompatibilityLevel.FULL_TRANSITIVE).isEmpty(),
            "adding default to a field is a compatible change");

        assertTrue(
            check(schema1, list(schema2), CompatibilityLevel.FULL_TRANSITIVE).isEmpty(),
            "removing a field with a default is a compatible change");

        assertTrue(
            check(schema2, list(schema1), CompatibilityLevel.FULL_TRANSITIVE).isEmpty(),
            "adding a field with default is a compatible change");

        assertTrue(
            check(schema3, list(schema2), CompatibilityLevel.FULL_TRANSITIVE).isEmpty(),
            "removing a default from a field is a compatible change");

        assertFalse(
            check(schema3, list(schema2, schema1), CompatibilityLevel.FULL_TRANSITIVE).isEmpty(),
            "transitively adding a field without a default is not a compatible change");

        assertFalse(
            check(schema1, list(schema2, schema3), CompatibilityLevel.FULL_TRANSITIVE).isEmpty(),
            "transitively removing a field without a default is not a compatible change");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static List<String> check(Schema newSchema, List<Schema> previous, CompatibilityLevel level) {
        return AvroCompatibilityChecker.check(newSchema, previous, level);
    }

    @SafeVarargs
    private static <T> List<T> list(T... items) {
        return Arrays.asList(items);
    }
}
