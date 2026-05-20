package com.waver.avro;

import org.apache.avro.Schema;

/**
 * Entry-point facade for the avro-checks library.
 */
public final class AvroChecks {

    private AvroChecks() {}

    /** Parses an Avro schema from its JSON string representation. */
    public static Schema parseSchema(String json) {
        return new Schema.Parser().parse(json);
    }
}
