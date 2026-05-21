package com.waver.avro.checker;

import org.apache.avro.Schema;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder for constructing compatibility check requests.
 *
 * <p>Provides a fluent API for configuring and executing schema compatibility checks:
 * <pre>{@code
 * AvroCompatibilityChecker.check()
 *     .forCandidate(newSchema)
 *     .withCompatibility(CompatibilityLevel.FULL)
 *     .withOlderSchema(oldSchema)
 *     .withOlderSchema(olderSchema)
 *     .check();
 * }</pre>
 *
 * @see AvroCompatibilityChecker#check()
 */
public final class CompatibilityCheckBuilder {

    private Schema candidate;
    private CompatibilityLevel level = CompatibilityLevel.BACKWARD;
    private final List<Schema> olderSchemas = new ArrayList<>();

    CompatibilityCheckBuilder() {
        // package-private constructor - use AvroCompatibilityChecker.check()
    }

    /**
     * Sets the candidate (new) schema to be checked for compatibility.
     *
     * @param schema the new schema version
     * @return this builder
     */
    public CompatibilityCheckBuilder forCandidate(Schema schema) {
        this.candidate = schema;
        return this;
    }

    /**
     * Sets the compatibility level for the check.
     *
     * @param level the compatibility level (default: BACKWARD)
     * @return this builder
     */
    public CompatibilityCheckBuilder withCompatibility(CompatibilityLevel level) {
        this.level = level;
        return this;
    }

    /**
     * Adds a single older schema to the history.
     * Multiple calls add schemas in order (oldest first).
     *
     * @param schema an older schema version
     * @return this builder
     */
    public CompatibilityCheckBuilder withOlderSchema(Schema schema) {
        this.olderSchemas.add(schema);
        return this;
    }

    /**
     * Adds multiple older schemas to the history.
     *
     * @param schemas varargs of older schema versions
     * @return this builder
     */
    public CompatibilityCheckBuilder withHistory(Schema... schemas) {
        for (Schema schema : schemas) {
            this.olderSchemas.add(schema);
        }
        return this;
    }

    /**
     * Adds a list of older schemas to the history.
     *
     * @param schemas list of older schema versions
     * @return this builder
     */
    public CompatibilityCheckBuilder withHistory(List<Schema> schemas) {
        this.olderSchemas.addAll(schemas);
        return this;
    }

    /**
     * Executes the compatibility check with the configured parameters.
     *
     * @return list of incompatibility messages (empty if compatible)
     * @throws IllegalStateException if candidate schema is not set
     */
    public List<String> check() {
        if (candidate == null) {
            throw new IllegalStateException("Candidate schema must be set via forCandidate()");
        }
        if (olderSchemas.isEmpty()) {
            return List.of();
        }
        return AvroCompatibilityChecker.check(candidate, olderSchemas, level);
    }

    /**
     * Executes the compatibility check and returns migration advice for any incompatibilities.
     *
     * @return list of migration advice (empty if compatible)
     * @throws IllegalStateException if candidate schema is not set
     */
    public List<com.waver.avro.advice.MigrationAdvice> checkWithAdvice() {
        if (candidate == null) {
            throw new IllegalStateException("Candidate schema must be set via forCandidate()");
        }
        if (olderSchemas.isEmpty()) {
            return List.of();
        }
        return AvroCompatibilityChecker.checkWithAdvice(candidate, olderSchemas, level);
    }

    /**
     * Returns true if the candidate schema is compatible with the older schemas.
     *
     * @return true if compatible, false otherwise
     * @throws IllegalStateException if candidate schema is not set
     */
    public boolean isCompatible() {
        return check().isEmpty();
    }
}
