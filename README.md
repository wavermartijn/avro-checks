# avro-checks

> A self-contained Java 21 library and CLI tool for checking Avro schema compatibility.

**Version:** `0.0.1-RC1` · **Owner:** Martijn van der Pauw · **Repo:** https://github.com/wavermartijn/avro-checks

---

## Features

- Check Avro schema compatibility for all standard levels: `BACKWARD`, `FORWARD`, `FULL` and their `_TRANSITIVE` variants, plus `NONE`
- Returns human-readable incompatibility messages
- Provides step-by-step **migration advice** for every detected incompatibility
- CLI tool with `--help`, `--version`, and file-based schema input (`-f`)
- Zero runtime dependencies beyond `org.apache.avro:avro` (schema parsing only — all compatibility logic is hand-written)
- ≥ 80% test coverage enforced via JaCoCo

---

## Project Structure

```
windsurf-project-2/
├── specs/                                  Project specifications
│   ├── 01-initial-specs.md
│   ├── 02-advice-specs.md
│   ├── 03-test-specs.md
│   └── 04-documentation-specs.md
├── VERSION                                 Current version string (0.0.1-RC1)
├── demo.bat                                Windows demo script
├── settings.gradle.kts                     Gradle multi-project root
├── build.gradle.kts                        Shared Java 21 toolchain
│
├── avro-checks/                            LIBRARY — avro-checks-1.0.0-SNAPSHOT.jar
│   ├── build.gradle.kts                    java-library + JaCoCo ≥80% coverage check
│   └── src/
│       ├── main/java/com/waver/avro/
│       │   ├── schema/
│       │   │   └── AvroChecks.java         Schema parsing facade
│       │   ├── checker/
│       │   │   ├── CompatibilityLevel.java  Enum of all compatibility levels
│       │   │   ├── CompatibilityResult.java Result record (compatible + messages)
│       │   │   └── AvroCompatibilityChecker.java  Main checker (all static methods)
│       │   └── advice/
│       │       ├── MigrationAdvice.java     Incompatibility + migration steps record
│       │       └── MigrationAdvisor.java    Maps issues to step-by-step advice
│       └── test/
│           ├── java/com/waver/avro/
│           │   ├── SchemaLoader.java        Test utility: loads schemas from resources
│           │   ├── AvroChecksTest.java
│           │   ├── AvroCompatibilityTest.java
│           │   ├── MigrationAdviceTest.java
│           │   └── AvroCheckerCoverageTest.java
│           └── resources/schemas/          Avro schema JSON fixtures for tests
│               ├── myrecord-v1.json
│               ├── myrecord-v2-field-with-default.json
│               ├── order-v1.json
│               └── ... (12 schema files total)
│
└── avro-checks-cli/                        APPLICATION — fat-jar (Gradle)
    ├── build.gradle.kts                    application plugin + fat-jar + slf4j-nop
    └── src/main/java/com/waver/avro/cli/
        └── Main.java                       CLI entry point

└── avro-checks-quarkus-cli/                APPLICATION — native binary (Maven + Quarkus)
    ├── pom.xml                             Quarkus + Picocli configuration
    ├── build-native.bat                    Windows native build script
    ├── build-native.sh                     Unix native build script
    └── src/main/java/com/waver/avro/cli/
        └── AvroChecksCommand.java          Picocli command with native support
```

---

## Library API

### Schema parsing

```java
import com.waver.avro.schema.AvroChecks;

Schema schema = AvroChecks.parseSchema(jsonString);
```

### Compatibility check

```java
import com.waver.avro.checker.AvroCompatibilityChecker;
import com.waver.avro.checker.CompatibilityLevel;

// Single previous version — returns empty list when compatible
List<String> issues = AvroCompatibilityChecker.check(newSchema, previousSchema, CompatibilityLevel.BACKWARD);

// Multiple previous versions (transitive)
List<String> issues = AvroCompatibilityChecker.check(newSchema, List.of(v1, v2), CompatibilityLevel.FULL_TRANSITIVE);

// Convenience boolean
boolean ok = AvroCompatibilityChecker.isCompatible(newSchema, previousSchema, CompatibilityLevel.FORWARD);
```

> **Convention:** `previousSchemas` is **oldest-first**. Non-transitive levels use only the last (most recent) entry.

### Migration advice

```java
import com.waver.avro.advice.MigrationAdvice;

List<MigrationAdvice> advice = AvroCompatibilityChecker.checkWithAdvice(newSchema, previousSchema, CompatibilityLevel.BACKWARD);

for (MigrationAdvice a : advice) {
    System.out.println(a); // prints Issue + numbered steps
}
```

### Builder API (Fluent)

```java
// Single schema check
List<String> issues = AvroCompatibilityChecker.check()
    .forCandidate(newSchema)
    .withCompatibility(CompatibilityLevel.FULL)
    .withOlderSchema(oldSchema)
    .check();

// Multiple schemas with varargs
boolean ok = AvroCompatibilityChecker.check()
    .forCandidate(newSchema)
    .withCompatibility(CompatibilityLevel.BACKWARD)
    .withHistory(v1, v2, v3)
    .isCompatible();

// With List
List<MigrationAdvice> advice = AvroCompatibilityChecker.check()
    .forCandidate(newSchema)
    .withCompatibility(CompatibilityLevel.FULL_TRANSITIVE)
    .withHistory(List.of(v1, v2))
    .checkWithAdvice();
```

---

## Compatibility Levels

| Level                  | Direction            | Scope              |
|------------------------|----------------------|--------------------|
| `BACKWARD`             | new reads old        | most recent only   |
| `BACKWARD_TRANSITIVE`  | new reads old        | all history        |
| `FORWARD`              | old reads new        | most recent only   |
| `FORWARD_TRANSITIVE`   | old reads new        | all history        |
| `FULL`                 | both directions      | most recent only   |
| `FULL_TRANSITIVE`      | both directions      | all history        |
| `NONE`                 | no check             | —                  |

---

## CLI Usage

```
avro-checks-cli [--help]
avro-checks-cli <new-schema-json> <previous-schema-json> [LEVEL]
avro-checks-cli -f <new-schema.json> <previous-schema.json> [LEVEL]
```

**Options:**

| Flag     | Description                                    |
|----------|------------------------------------------------|
| `--help` | Print help and exit                            |
| `-f`     | Read schemas from files instead of inline JSON |

**Exit codes:** `0` = compatible · `1` = incompatible · `2` = usage/parse error

**Default level:** `BACKWARD`

### Example — BACKWARD incompatible change

```bash
java -jar avro-checks-cli/build/libs/avro-checks-cli-1.0.0-SNAPSHOT.jar \
  -f new-schema.json old-schema.json BACKWARD
```

Output:
```
avro-checks-cli v0.0.1-RC1
Compatibility level : BACKWARD
New schema          : com.waver.avro.Order
Previous schema     : com.waver.avro.Order
Result              : INCOMPATIBLE

--- Incompatibility 1 ---
Issue   : reader field 'currency' has no default and is missing from writer schema
Advice  :
  1. Do NOT add 'currency' without a default in a single step.
  2. Step 1 -> In the new schema, add 'currency' WITH a default value ...
  ...
```

### Native CLI (GraalVM — No Java Required)

Build a standalone native executable with GraalVM:

**Windows:**
```bash
cd avro-checks-quarkus-cli
build-native.bat
```

**Mac/Linux:**
```bash
cd avro-checks-quarkus-cli
chmod +x build-native.sh
./build-native.sh
```

Run without any Java installation:
```bash
./build/avro-checks-quarkus-cli-0.0.1-RC1-runner -f new.json old.json --level FULL
```

---

## Building

```bash
.\gradlew.bat build          # compile, test, coverage check
.\gradlew.bat :avro-checks:test   # library tests only
```

The build enforces **≥ 80% instruction and branch coverage** via JaCoCo. The build fails if coverage drops below this threshold.

---

## Running the Demo

```bash
.\demo.bat
```

Runs four scenarios: compatible BACKWARD change, incompatible BACKWARD change, compatible FORWARD change.

---

## Technology Stack

| Concern        | Choice                                    |
|----------------|-------------------------------------------|
| Language       | Java 21                                   |
| Schema parsing | `org.apache.avro:avro:1.11.3`             |
| Build          | Gradle 8.5, Kotlin DSL                    |
| Testing        | JUnit 5 (5.10.2)                          |
| Coverage       | JaCoCo 0.8.11, ≥ 80% enforced            |
| Logging        | SLF4J NOP (silent CLI output)             |
