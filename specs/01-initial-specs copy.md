# avro-checks — Initial Specification (v1)

## Status: implemented & tested

---

## Goal

A self-contained Java 21 library (`avro-checks`) that compares two Avro schemas and
determines whether they are compatible under a specified compatibility level.

The library ships as a plain JAR. Its **only external runtime dependency** is
`org.apache.avro:avro:1.11.3`, used solely for JSON → `Schema` object-graph parsing.
All compatibility logic is hand-written — no Confluent or third-party compatibility
library is used at runtime.

A companion CLI application (`avro-checks-cli`) wraps the library and exposes it
as a command-line tool.

---

## Project Layout
Create clear and separated package names so the functionality is clearly separated in the packages of the library
```
windsurf-project-2/                    Gradle multi-project root (Gradle 8.5, Kotlin DSL)
├── specs/
│   └── 01-initial-specs.md            ← this file
├── settings.gradle.kts                includes :avro-checks and :avro-checks-cli
├── build.gradle.kts                   shared Java 21 toolchain + JUnit platform
├── avro-checks/                       LIBRARY  — ships as avro-checks-1.0.0-SNAPSHOT.jar
│   ├── build.gradle.kts               java-library plugin; api("org.apache.avro:avro:1.11.3")
│   └── src/
│       ├── main/java/com/waver/avro/
│       │   ├── sub packages and classes here
│       └── test/java/com/waver/avro/
│           ├── tests matching the classes in main
└── avro-checks-cli/                   APPLICATION — ships as fat-jar or distZip
    ├── build.gradle.kts               application plugin; depends on :avro-checks
    └── src/main/java/com/waver/avro/cli/
        └── Main.java
```

---

## Public API

### `AvroChecks` — schema parsing facade

```java
Schema schema = AvroChecks.parseSchema(jsonString);
```

Thin wrapper around `new org.apache.avro.Schema.Parser().parse(json)`.

---

### `CompatibilityLevel` — enum

```java
public enum CompatibilityLevel {
    NONE,
    BACKWARD,
    BACKWARD_TRANSITIVE,
    FORWARD,
    FORWARD_TRANSITIVE,
    FULL,
    FULL_TRANSITIVE
}
```

---

### `CompatibilityResult` — record

```java
public record CompatibilityResult(boolean compatible, List<String> incompatibilities) {
    public static CompatibilityResult ok();           // compatible, empty list
    public static CompatibilityResult fail(List<String> reasons);
}
```

---

### `AvroCompatibilityChecker` — main entry point

All methods are `static`. The class is non-instantiable.

```java
// Returns empty list when compatible; list of messages when not.
List<String> check(Schema newSchema, Schema previousSchema, CompatibilityLevel level);
List<String> check(Schema newSchema, List<Schema> previousSchemas, CompatibilityLevel level);

// Convenience boolean wrappers.
boolean isCompatible(Schema newSchema, Schema previousSchema, CompatibilityLevel level);
boolean isCompatible(Schema newSchema, List<Schema> previousSchemas, CompatibilityLevel level);
```

**List ordering convention**: `previousSchemas` is **oldest-first**.
- Non-transitive levels (`BACKWARD`, `FORWARD`, `FULL`) check only the **last** element
  (most recent previous version).
- Transitive levels (`*_TRANSITIVE`) check **every** element.

---

## Compatibility Levels — Semantics

| Level                 | Direction          | Scope             |
|-----------------------|--------------------|-------------------|
| `BACKWARD`            | new reads old      | most recent only  |
| `BACKWARD_TRANSITIVE` | new reads old      | all history       |
| `FORWARD`             | old reads new      | most recent only  |
| `FORWARD_TRANSITIVE`  | old reads new      | all history       |
| `FULL`                | both directions    | most recent only  |
| `FULL_TRANSITIVE`     | both directions    | all history       |
| `NONE`                | no check           | —                 |

---

## Compatibility Rules Implemented

All rules follow the Apache Avro Schema Resolution specification.

### Records
- Reader field present in writer → recurse into field schemas.
- Reader field absent from writer, reader has default → compatible (default used).
- Reader field absent from writer, reader has **no** default → **incompatible**.
- Field matching respects **reader field aliases** (alias resolves to writer field name).

### Unions
- **Reader is union, writer is union** — every writer branch must be covered by at least
  one reader union branch.
- **Reader is union, writer is non-union** — writer type must be covered by at least one
  reader branch.
- **Reader is non-union, writer is union** — reader must be compatible with **every**
  writer branch (a `null` written cannot be read by a `string` reader).

### Enums
- Reader symbol absent from writer symbols, reader has no `default` → **incompatible**.

### Arrays & Maps
- Compatibility is checked recursively on element / value types.

### Type Promotion (primitives)
| Writer  | Compatible readers          |
|---------|-----------------------------|
| `int`   | `long`, `float`, `double`   |
| `long`  | `float`, `double`           |
| `float` | `double`                    |
| `string`| `bytes`                     |
| `bytes` | `string`                    |

### Named Types (record / enum / fixed)
- Full names (namespace + name) must match, **or** one schema's aliases contain the
  other's full name.

---

## Test Coverage

`AvroCompatibilityTest` — 6 tests ported from
[Confluent Schema Registry `AvroCompatibilityTest`](https://github.com/confluentinc/schema-registry/blob/master/core/src/test/java/io/confluent/kafka/schemaregistry/avro/AvroCompatibilityTest.java),
adapted to JUnit 5 and the `com.waver.avro` API.

| Test method                              | Level                 | Result |
|------------------------------------------|-----------------------|--------|
| `testBasicBackwardsCompatibility`        | `BACKWARD`            | PASS   |
| `testBasicBackwardsTransitiveCompatibility` | `BACKWARD_TRANSITIVE` | PASS   |
| `testBasicForwardsCompatibility`         | `FORWARD`             | PASS   |
| `testBasicForwardsTransitiveCompatibility`  | `FORWARD_TRANSITIVE`  | PASS   |
| `testBasicFullCompatibility`             | `FULL`                | PASS   |
| `testBasicFullTransitiveCompatibility`   | `FULL_TRANSITIVE`     | PASS   |

Run with: `.\gradlew.bat :avro-checks:test`

---

## Technology Choices

- **Java 21** — records (`CompatibilityResult`), switch expressions, pattern matching
- **`org.apache.avro:avro:1.11.3`** — schema parsing only; declared as `api` so
  consumers get `Schema` on their compile classpath
- **No other runtime dependencies** in the library
- **JUnit 5 (5.10.2)** — test-only dependency
- **Gradle 8.5** (Kotlin DSL) — already cached locally, no network needed

---

## Output Requirements

- **No logging noise on stdout/stderr.** The CLI output must be clean — only the
  compatibility result and any incompatibility messages.
- SLF4J warnings such as the following are **not acceptable**:
  ```
  SLF4J: Failed to load class "org.slf4j.impl.StaticLoggerBinder".
  SLF4J: Defaulting to no-operation (NOP) logger implementation
  ```
- Fix: bind SLF4J to the NOP backend explicitly via `slf4j-nop` in the CLI's
  runtime classpath. The library itself must **not** pull in any logger binding.
