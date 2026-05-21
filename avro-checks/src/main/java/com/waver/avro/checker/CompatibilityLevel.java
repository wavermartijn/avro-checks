package com.waver.avro.checker;

/**
 * The set of Avro schema compatibility levels supported by
 * {@link AvroCompatibilityChecker}.
 *
 * <p>Non-transitive levels check only the most recent previous schema;
 * {@code *_TRANSITIVE} variants check every schema in the history list.
 */
public enum CompatibilityLevel {
    /** No compatibility check is performed. */
    NONE,
    /** New schema can read data written with the most recent previous schema. */
    BACKWARD,
    /** New schema can read data written with every previous schema. */
    BACKWARD_TRANSITIVE,
    /** Most recent previous schema can read data written with the new schema. */
    FORWARD,
    /** Every previous schema can read data written with the new schema. */
    FORWARD_TRANSITIVE,
    /** Both {@link #BACKWARD} and {@link #FORWARD} against the most recent previous schema. */
    FULL,
    /** Both {@link #BACKWARD} and {@link #FORWARD} against every previous schema. */
    FULL_TRANSITIVE
}
