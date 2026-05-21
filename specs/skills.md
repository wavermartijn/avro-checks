# avro-checks — Skills & Project Specification

## Goal
A self-contained Java 21 library (`avro-checks`) that compares two Avro schemas and
determines whether they are compatible under a specified compatibility level.
The library ships as a plain JAR with **no external runtime dependencies** beyond
`org.apache.avro:avro` (used only for schema parsing — the compatibility logic is
reimplemented from scratch).

A companion CLI application (`avro-checks-cli`) wraps the library and exposes it
as a command-line tool.

---

## Compatibility Levels

| Level                | Description |
|----------------------|-------------|
| `BACKWARD`           | New schema can read data written with the previous schema |
| `BACKWARD_TRANSITIVE`| New schema can read data written with **all** previous schemas |
| `FORWARD`            | Previous schema can read data written with the new schema |
| `FORWARD_TRANSITIVE` | All previous schemas can read data written with the new schema |
| `FULL`               | New schema is both BACKWARD and FORWARD compatible with the previous schema |
| `FULL_TRANSITIVE`    | New schema is both BACKWARD and FORWARD compatible with **all** previous schemas |
| `NONE`               | No compatibility check |

---

## Library API (avro-checks)

```java
// Parse
AvroSchema schema = AvroSchema.parse(jsonString);

// Single previous version
List<String> issues = AvroCompatibilityChecker.check(newSchema, previousSchema, CompatibilityLevel.BACKWARD);

// Multiple previous versions (transitive)
List<String> issues = AvroCompatibilityChecker.check(newSchema, List.of(v2, v1), CompatibilityLevel.FULL_TRANSITIVE);

// Convenience
boolean ok = AvroCompatibilityChecker.isCompatible(newSchema, previousSchema, CompatibilityLevel.FORWARD);
```

Returns an **empty list** when compatible, or a list of human-readable incompatibility
messages when not.

---

## Compatibility Rules Implemented

Derived from Apache Avro spec and Confluent Schema Registry tests:

### BACKWARD (new reads old)
- Adding a field **with** a default value → compatible
- Adding a field **without** a default value → **incompatible**
- Removing a field that had a default → compatible
- Removing a field without a default → **incompatible**
- Renaming a field (with alias pointing to old name) → compatible
- Widening a type to a union that includes the original type → compatible
- Removing a type from a union → **incompatible**

### FORWARD (old reads new)
- Adding a field (with or without default) → compatible (old reader ignores unknown fields)
- Removing a field without a default → **incompatible** (old reader expects it)

### FULL
- Intersection of BACKWARD and FORWARD rules

### TRANSITIVE variants
- Apply the non-transitive rule against **every** schema in the history list

---

## Technology Choices

- **Java 21** — records, sealed classes, pattern matching where appropriate
- **`org.apache.avro:avro`** — used only for `Schema` parsing (JSON → Schema object graph)
- **No other runtime dependencies** — all compatibility logic is hand-written
- **JUnit 5** — test framework
- Compatibility test cases ported directly from
  [Confluent Schema Registry `AvroCompatibilityTest`](https://github.com/confluentinc/schema-registry/blob/master/core/src/test/java/io/confluent/kafka/schemaregistry/avro/AvroCompatibilityTest.java)

---

## Project Structure

```
windsurf-project-2/
├── specs/
│   └── skills.md                  ← this file
├── settings.gradle.kts
├── build.gradle.kts               ← shared Java 21 toolchain
├── avro-checks/                   ← LIBRARY (ships as avro-checks-x.y.z.jar)
│   ├── build.gradle.kts
│   └── src/
│       ├── main/java/com/waver/avro/

│       └── test/java/com/waver/avro/
Schema Registry
└── avro-checks-cli/               ← APPLICATION (ships as fat-jar or distZip)
    ├── build.gradle.kts
    └── src/main/java/com/waver/avro/cli/
```
