# Changelog

All notable changes to this project are documented in this file.

---

## [0.0.1-RC1] — 2026-05-21

### refactor: introduce sub-packages for clear separation of concerns
- `com.waver.avro.schema` — `AvroChecks` (schema parsing facade)
- `com.waver.avro.checker` — `AvroCompatibilityChecker`, `CompatibilityLevel`, `CompatibilityResult`
- `com.waver.avro.advice` — `MigrationAdvice`, `MigrationAdvisor`
- `com.waver.avro.cli` — `Main` (unchanged)
- Removed flat `com.waver.avro` classes; all tests and CLI updated accordingly

### test: schema resources, coverage boost, JaCoCo 80% enforcement
- 12 Avro schema JSON files added to `src/test/resources/schemas/`
- `SchemaLoader` test utility to load schemas from resources by filename
- `AvroCompatibilityTest` and `MigrationAdviceTest` refactored to use `SchemaLoader` (no inline JSON)
- `AvroCheckerCoverageTest`: 26 new tests covering enums, arrays, maps, type promotion, record aliases, all compatibility levels, `CompatibilityResult`, and all `MigrationAdvisor` advice branches
- JaCoCo 0.8.11 added with ≥ 80% instruction and branch coverage enforcement wired into the `check` task
- 46 total tests, 0 failures

### chore: specs and demo.bat
- Added `specs/` directory with initial and advice specifications
- `demo.bat` Windows script for running four schema compatibility scenarios

### feat: avro-checks-cli fat-jar with VERSION, --help, and migration advice output
- CLI reads `VERSION` resource and displays it in output and `--help`
- `--help` prints usage, compatibility level descriptions, exit codes, repo URL, and owner
- `-f` flag reads schemas from files instead of inline JSON arguments
- CLI uses `checkWithAdvice()` to print step-by-step migration advice for incompatibilities
- SLF4J bound to NOP (`slf4j-nop`) for clean, noise-free CLI output

### feat: avro-checks library with compatibility checker and ported tests
- `AvroChecks.parseSchema()` — schema parsing facade
- `CompatibilityLevel` enum — all 7 levels including transitive variants
- `CompatibilityResult` record — compatible flag + list of messages
- `AvroCompatibilityChecker` — full implementation of Avro schema resolution rules:
  records, unions, enums, arrays, maps, type promotion, named-type aliases
- `MigrationAdvice` record — incompatibility message + migration steps
- `MigrationAdvisor` — pattern-matches issue messages to targeted advice
- `checkWithAdvice()` overloads on `AvroCompatibilityChecker`
- 6 compatibility tests ported from Confluent Schema Registry `AvroCompatibilityTest`

### chore: Gradle 8.5 multi-project scaffold with Java 21 toolchain
- Root `settings.gradle.kts` with `:avro-checks` and `:avro-checks-cli` subprojects
- Shared Java 21 toolchain and JUnit platform config in root `build.gradle.kts`
- `avro-checks` subproject: `java-library` plugin, `api` dependency on Avro 1.11.3
- `avro-checks-cli` subproject: `application` plugin, fat-jar with `Main` as main class
- `VERSION` file at project root: `0.0.1-RC1`
