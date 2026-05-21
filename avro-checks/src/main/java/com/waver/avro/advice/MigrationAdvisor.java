package com.waver.avro.advice;

import com.waver.avro.checker.CompatibilityLevel;

import java.util.List;

/**
 * Produces human-readable, step-by-step migration advice for a given
 * incompatibility message and the compatibility level that was violated.
 *
 * <p>The advisor pattern-matches on the incompatibility message text
 * produced by {@link com.waver.avro.checker.AvroCompatibilityChecker} to recognise
 * the root cause and generate targeted steps.
 */
public final class MigrationAdvisor {

    private MigrationAdvisor() {}

    /**
     * Returns a {@link MigrationAdvice} for the given incompatibility message.
     *
     * @param incompatibility the raw message from {@link com.waver.avro.checker.AvroCompatibilityChecker#check}
     * @param level           the compatibility level that was being checked
     * @return advice with concrete migration steps
     */
    public static MigrationAdvice advise(String incompatibility, CompatibilityLevel level) {
        List<String> steps = buildSteps(incompatibility, level);
        return new MigrationAdvice(incompatibility, steps);
    }

    private static List<String> buildSteps(String msg, CompatibilityLevel level) {

        // ── Missing field without default (reader field not in writer) ────────
        if (msg.contains("has no default and is missing from writer schema")) {
            String field = extractQuoted(msg);
            return switch (level) {
                case BACKWARD, BACKWARD_TRANSITIVE, FULL, FULL_TRANSITIVE -> List.of(
                    "Do NOT add '" + field + "' without a default in a single step.",
                    "Step 1 -> In the new schema, add '" + field + "' WITH a default value "
                        + "(e.g. null, \"\", or a sensible domain value). "
                        + "This makes the change backward compatible.",
                    "Step 2 -> Deploy producers and consumers that understand the new field.",
                    "Step 3 -> Once all consumers are updated, you may optionally make the "
                        + "field mandatory by removing the default in a future schema version "
                        + "(only safe if no old data without the field will ever be read)."
                );
                case FORWARD, FORWARD_TRANSITIVE -> List.of(
                    "The old schema (reader) expects field '" + field + "' but the new schema "
                        + "(writer) does not produce it.",
                    "Step 1 -> Keep field '" + field + "' in the new schema, or add a default "
                        + "to it in the OLD schema so the old reader can handle its absence.",
                    "Step 2 -> Update all consumers (old readers) before removing the field "
                        + "from the writer schema.",
                    "Step 3 -> Only remove the field from the writer once no old readers rely on it."
                );
                default -> genericSteps(msg);
            };
        }

        // ── Union branch removed / writer union branch not covered ────────────
        if (msg.contains("writer union branch") && msg.contains("has no compatible reader")) {
            String branch = extractAfterLast(msg, "writer union branch ");
            return switch (level) {
                case BACKWARD, BACKWARD_TRANSITIVE, FULL, FULL_TRANSITIVE -> List.of(
                    "The reader union no longer covers the '" + branch + "' type that the "
                        + "writer may produce.",
                    "Step 1 -> Add '" + branch + "' back to the reader (new schema) union, "
                        + "e.g. change [\"null\",\"string\"] to [\"null\",\"string\",\"" + branch + "\"].",
                    "Step 2 -> If the intent is to narrow the union, first ensure ALL existing "
                        + "data has been migrated away from the '" + branch + "' type.",
                    "Step 3 -> Remove the type from the union only after migration is complete."
                );
                default -> genericSteps(msg);
            };
        }

        // ── Non-union reader vs union writer ──────────────────────────────────
        if (msg.contains("is not compatible with writer union branch")) {
            return switch (level) {
                case BACKWARD, BACKWARD_TRANSITIVE, FULL, FULL_TRANSITIVE -> List.of(
                    "The new schema uses a plain type but the old schema wrote a union "
                        + "(which may include null or other types).",
                    "Step 1 -> Change the new schema field to a union that includes at least "
                        + "all types present in the old schema's union.",
                    "Step 2 -> Handle all union branches in your application code.",
                    "Step 3 -> After all data has been re-written with the narrowed type, "
                        + "a further schema evolution can simplify the union."
                );
                default -> genericSteps(msg);
            };
        }

        // ── Writer union branch not covered by reader union ───────────────────
        if (msg.contains("is not covered by any reader union branch")) {
            String writerType = extractAfterPrefix(msg, "writer type ");
            return List.of(
                "The writer can produce type '" + writerType + "' but the reader union "
                    + "has no branch that can consume it.",
                "Step 1 -> Add '" + writerType + "' as a branch to the reader (new schema) union.",
                "Step 2 -> Update application code to handle the new branch.",
                "Step 3 -> Re-deploy readers before deploying the writer change."
            );
        }

        // ── Enum symbol missing ───────────────────────────────────────────────
        if (msg.contains("enum symbol") && msg.contains("is in the reader but not in the writer")) {
            String symbol = extractQuoted(msg);
            return List.of(
                "Enum symbol '" + symbol + "' exists in the new schema but not in the old.",
                "Step 1 -> Add a default to the enum in the new schema "
                    + "(Avro supports an 'default' on enum fields) to handle unknown symbols.",
                "Step 2 -> Alternatively, keep '" + symbol + "' in the writer schema "
                    + "until all readers are updated.",
                "Step 3 -> Remove the symbol from the writer only after all readers can handle "
                    + "its absence via the enum default."
            );
        }

        // ── Type mismatch ─────────────────────────────────────────────────────
        if (msg.contains("reader type") && msg.contains("is not compatible with writer type")) {
            return List.of(
                "The field type has changed in an incompatible way.",
                "Step 1 -> Introduce a union in the new schema that accepts both the old and "
                    + "new types (e.g. [\"string\", \"int\"]). This is safe for both "
                    + "BACKWARD and FORWARD compatibility.",
                "Step 2 -> Migrate all data from the old type to the new type.",
                "Step 3 -> Once migration is complete, narrow the union back to the target type "
                    + "in a subsequent schema version."
            );
        }

        return genericSteps(msg);
    }

    private static List<String> genericSteps(String msg) {
        return List.of(
            "Investigate the incompatibility: " + msg,
            "Make the change in multiple backwards-compatible steps rather than one "
                + "breaking change.",
            "Consult the Avro Schema Resolution specification for promotion and "
                + "compatibility rules: https://avro.apache.org/docs/current/specification/"
        );
    }

    private static String extractQuoted(String msg) {
        int start = msg.indexOf('\'');
        int end   = msg.indexOf('\'', start + 1);
        if (start >= 0 && end > start) {
            return msg.substring(start + 1, end);
        }
        return "<field>";
    }

    private static String extractAfterLast(String msg, String prefix) {
        int idx = msg.lastIndexOf(prefix);
        if (idx >= 0) {
            String rest = msg.substring(idx + prefix.length());
            int space = rest.indexOf(' ');
            return space >= 0 ? rest.substring(0, space) : rest;
        }
        return "<type>";
    }

    private static String extractAfterPrefix(String msg, String prefix) {
        int idx = msg.indexOf(prefix);
        if (idx >= 0) {
            String rest = msg.substring(idx + prefix.length());
            int space = rest.indexOf(' ');
            return space >= 0 ? rest.substring(0, space) : rest;
        }
        return "<type>";
    }
}
