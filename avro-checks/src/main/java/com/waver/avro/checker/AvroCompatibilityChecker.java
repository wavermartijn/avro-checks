package com.waver.avro.checker;

import com.waver.avro.advice.MigrationAdvice;
import com.waver.avro.advice.MigrationAdvisor;
import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.Schema.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Checks Avro schema compatibility between a new schema and one or more
 * previously registered schemas, according to a given {@link CompatibilityLevel}.
 *
 * <p>All compatibility logic is implemented from scratch — no Confluent or
 * third-party compatibility library is used at runtime.
 *
 * <p>Compatibility semantics follow the Avro spec and mirror the behaviour
 * tested by the Confluent Schema Registry test suite:
 * <ul>
 *   <li>BACKWARD  — new schema can read data written by the previous schema</li>
 *   <li>FORWARD   — previous schema can read data written by the new schema</li>
 *   <li>FULL      — both BACKWARD and FORWARD against the previous schema</li>
 *   <li>*_TRANSITIVE variants apply the rule against every schema in the history</li>
 * </ul>
 */
public final class AvroCompatibilityChecker {

    private AvroCompatibilityChecker() {}

    /**
     * Returns an empty list when compatible, or a list of human-readable
     * incompatibility messages when not.
     *
     * @param newSchema       the candidate schema (the new version)
     * @param previousSchemas history ordered oldest-first; only the last entry
     *                        is used for non-transitive levels
     * @param level           the compatibility level to enforce
     */
    public static List<String> check(Schema newSchema,
                                     List<Schema> previousSchemas,
                                     CompatibilityLevel level) {
        if (level == CompatibilityLevel.NONE || previousSchemas.isEmpty()) {
            return List.of();
        }

        // For non-transitive levels the list is treated as oldest-first and only
        // the last entry (most recent previous version) is checked.
        Schema mostRecent = previousSchemas.get(previousSchemas.size() - 1);

        return switch (level) {
            case BACKWARD            -> checkBackward(newSchema, List.of(mostRecent));
            case BACKWARD_TRANSITIVE -> checkBackward(newSchema, previousSchemas);
            case FORWARD             -> checkForward(newSchema, List.of(mostRecent));
            case FORWARD_TRANSITIVE  -> checkForward(newSchema, previousSchemas);
            case FULL                -> merge(
                                          checkBackward(newSchema, List.of(mostRecent)),
                                          checkForward(newSchema, List.of(mostRecent)));
            case FULL_TRANSITIVE     -> merge(
                                          checkBackward(newSchema, previousSchemas),
                                          checkForward(newSchema, previousSchemas));
            case NONE                -> List.of();
        };
    }

    /** Convenience overload for a single previous schema. */
    public static List<String> check(Schema newSchema,
                                     Schema previousSchema,
                                     CompatibilityLevel level) {
        return check(newSchema, List.of(previousSchema), level);
    }

    /** Returns {@code true} when compatible (i.e. the issues list is empty). */
    public static boolean isCompatible(Schema newSchema,
                                       Schema previousSchema,
                                       CompatibilityLevel level) {
        return check(newSchema, previousSchema, level).isEmpty();
    }

    /** Returns {@code true} when compatible (transitive overload). */
    public static boolean isCompatible(Schema newSchema,
                                       List<Schema> previousSchemas,
                                       CompatibilityLevel level) {
        return check(newSchema, previousSchemas, level).isEmpty();
    }

    /**
     * Like {@link #check} but returns each incompatibility paired with
     * step-by-step migration advice.  Returns an empty list when compatible.
     */
    public static List<MigrationAdvice> checkWithAdvice(Schema newSchema,
                                                        Schema previousSchema,
                                                        CompatibilityLevel level) {
        return checkWithAdvice(newSchema, List.of(previousSchema), level);
    }

    /** Transitive overload of {@link #checkWithAdvice}. */
    public static List<MigrationAdvice> checkWithAdvice(Schema newSchema,
                                                        List<Schema> previousSchemas,
                                                        CompatibilityLevel level) {
        return check(newSchema, previousSchemas, level).stream()
            .map(issue -> MigrationAdvisor.advise(issue, level))
            .toList();
    }

    // -------------------------------------------------------------------------
    // Backward: newSchema (reader) must be able to read data written by each
    //           previousSchema (writer).
    // -------------------------------------------------------------------------

    private static List<String> checkBackward(Schema newSchema, List<Schema> previousSchemas) {
        List<String> issues = new ArrayList<>();
        for (Schema prev : previousSchemas) {
            issues.addAll(checkReaderWriter(newSchema, prev));
        }
        return issues;
    }

    // -------------------------------------------------------------------------
    // Forward: each previousSchema (reader) must be able to read data written
    //          by newSchema (writer).
    // -------------------------------------------------------------------------

    private static List<String> checkForward(Schema newSchema, List<Schema> previousSchemas) {
        List<String> issues = new ArrayList<>();
        for (Schema prev : previousSchemas) {
            issues.addAll(checkReaderWriter(prev, newSchema));
        }
        return issues;
    }

    // -------------------------------------------------------------------------
    // Core reader/writer compatibility check (Avro resolution rules §Schema Resolution)
    // -------------------------------------------------------------------------

    private static List<String> checkReaderWriter(Schema reader, Schema writer) {
        List<String> issues = new ArrayList<>();
        checkSchemas(reader, writer, issues, "");
        return issues;
    }

    private static void checkSchemas(Schema reader, Schema writer,
                                     List<String> issues, String context) {
        if (reader.getType() == Type.UNION) {
            checkUnionReader(reader, writer, issues, context);
            return;
        }
        if (writer.getType() == Type.UNION) {
            checkUnionWriter(reader, writer, issues, context);
            return;
        }

        if (!typesMatch(reader, writer)) {
            issues.add(context + "reader type " + reader.getType()
                       + " is not compatible with writer type " + writer.getType());
            return;
        }

        switch (reader.getType()) {
            case RECORD -> checkRecord(reader, writer, issues, context);
            case ARRAY  -> checkSchemas(reader.getElementType(), writer.getElementType(),
                                        issues, context + "[].");
            case MAP    -> checkSchemas(reader.getValueType(), writer.getValueType(),
                                        issues, context + "{}.");
            case ENUM   -> checkEnum(reader, writer, issues, context);
            default     -> { /* primitives: type match is sufficient */ }
        }
    }

    private static void checkRecord(Schema reader, Schema writer,
                                    List<String> issues, String context) {
        for (Field readerField : reader.getFields()) {
            Field writerField = findWriterField(readerField, writer);

            if (writerField == null) {
                if (readerField.defaultVal() == null) {
                    issues.add(context + "reader field '" + readerField.name()
                               + "' has no default and is missing from writer schema");
                }
                // else: missing in writer but reader has default → ok
            } else {
                checkSchemas(readerField.schema(), writerField.schema(),
                             issues, context + readerField.name() + ".");
            }
        }
    }

    /**
     * Finds the matching writer field for a reader field, respecting aliases.
     */
    private static Field findWriterField(Field readerField, Schema writer) {
        Field direct = writer.getField(readerField.name());
        if (direct != null) {
            return direct;
        }
        // Check reader field aliases against writer field names
        for (String alias : readerField.aliases()) {
            Field aliased = writer.getField(alias);
            if (aliased != null) {
                return aliased;
            }
        }
        return null;
    }

    private static void checkEnum(Schema reader, Schema writer,
                                  List<String> issues, String context) {
        Set<String> writerSymbols = Set.copyOf(writer.getEnumSymbols());
        for (String symbol : reader.getEnumSymbols()) {
            if (!writerSymbols.contains(symbol) && reader.getEnumDefault() == null) {
                issues.add(context + "enum symbol '" + symbol
                           + "' is in the reader but not in the writer and reader has no default");
            }
        }
    }

    private static void checkUnionReader(Schema reader, Schema writer,
                                         List<String> issues, String context) {
        // writer must be resolvable by at least one branch of the reader union
        if (writer.getType() == Type.UNION) {
            // Every writer branch must be covered by the reader union
            for (Schema writerBranch : writer.getTypes()) {
                if (!unionCovers(reader, writerBranch)) {
                    issues.add(context + "writer union branch " + writerBranch.getType()
                               + " has no compatible reader union branch");
                }
            }
        } else {
            if (!unionCovers(reader, writer)) {
                issues.add(context + "writer type " + writer.getType()
                           + " is not covered by any reader union branch");
            }
        }
    }

    private static void checkUnionWriter(Schema reader, Schema writer,
                                         List<String> issues, String context) {
        // A non-union reader can only read data written by a union writer if
        // every branch of the writer union is compatible with the reader.
        // (A null written by the writer union cannot be read by a string reader.)
        for (Schema branch : writer.getTypes()) {
            if (!isCompatiblePair(reader, branch)) {
                issues.add(context + "reader type " + reader.getType()
                           + " is not compatible with writer union branch " + branch.getType());
            }
        }
    }

    private static boolean unionCovers(Schema union, Schema candidate) {
        return union.getTypes().stream().anyMatch(branch -> isCompatiblePair(branch, candidate));
    }

    /**
     * Quick yes/no compatibility check without accumulating messages.
     */
    private static boolean isCompatiblePair(Schema reader, Schema writer) {
        List<String> probe = new ArrayList<>();
        checkSchemas(reader, writer, probe, "");
        return probe.isEmpty();
    }

    /**
     * Returns true when reader and writer types are promotable/matching per
     * the Avro spec (INT->LONG->FLOAT->DOUBLE promotion chain, etc.).
     */
    private static boolean typesMatch(Schema reader, Schema writer) {
        if (reader.getType() == writer.getType()) {
            if (reader.getType() == Type.RECORD || reader.getType() == Type.ENUM
                    || reader.getType() == Type.FIXED) {
                return reader.getFullName().equals(writer.getFullName())
                    || writer.getAliases().contains(reader.getFullName())
                    || reader.getAliases().contains(writer.getFullName());
            }
            return true;
        }
        // Numeric promotion
        return switch (writer.getType()) {
            case INT    -> reader.getType() == Type.LONG
                           || reader.getType() == Type.FLOAT
                           || reader.getType() == Type.DOUBLE;
            case LONG   -> reader.getType() == Type.FLOAT
                           || reader.getType() == Type.DOUBLE;
            case FLOAT  -> reader.getType() == Type.DOUBLE;
            case STRING -> reader.getType() == Type.BYTES;
            case BYTES  -> reader.getType() == Type.STRING;
            default     -> false;
        };
    }

    private static List<String> merge(List<String> a, List<String> b) {
        if (a.isEmpty()) return b;
        if (b.isEmpty()) return a;
        List<String> merged = new ArrayList<>(a);
        merged.addAll(b);
        return merged;
    }
}
