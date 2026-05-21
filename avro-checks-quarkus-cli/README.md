# avro-checks-quarkus-cli

> Quarkus-based CLI for avro-checks with GraalVM native compilation support.

This module provides a native-compiled CLI that runs without requiring Java installation.

## Prerequisites

- [GraalVM](https://www.graalvm.org/downloads/) JDK 21+ with `native-image` tool
- Gradle (uses project wrapper)

## Quick Start

### Recommended: Tracing Agent Build

Uses GraalVM's tracing agent to **automatically** generate all reflection configuration.
This is the easiest and most reliable way to build the native image.

**Mac/Linux:**
```bash
chmod +x build-native-with-agent.sh
./build-native-with-agent.sh
```

**What it does:**
1. Builds the JAR
2. Runs the app with the GraalVM tracing agent to observe all reflection/resource usage
3. Generates `reflect-config.json`, `resource-config.json`, etc.
4. Rebuilds with the generated configuration
5. Produces native binary

### Alternative: Simple Build (May require manual config)

If the tracing agent approach doesn't work, try the standard build:

**Windows:**
```bash
build-native.bat
```

**Mac/Linux:**
```bash
chmod +x build-native.sh
./build-native.sh
```

This automatically falls back to JAR mode if native build fails.

### Usage

```bash
# Check compatibility (default: BACKWARD)
./build/avro-checks-quarkus-cli-0.0.1-RC1-runner -f new-schema.json old-schema.json

# With specific compatibility level
./build/avro-checks-quarkus-cli-0.0.1-RC1-runner -f new.json old.json --level FULL

# With historical schemas (for transitive checks)
./build/avro-checks-quarkus-cli-0.0.1-RC1-runner -f new.json old.json --with-history older1.json,older2.json

# Help
./build/avro-checks-quarkus-cli-0.0.1-RC1-runner --help
```

## Comparison

| Method | Size | Java Required | Startup | Notes |
|--------|------|---------------|---------|-------|
| **Native Image (with agent)** | ~20-30MB | No | Instant | ✅ Recommended - auto-configures reflection |
| **Native Image (simple)** | ~20-30MB | No | Instant | ⚠️ May fail - needs manual config |
| **JAR Mode** | ~5MB JAR | Yes | Medium | ✅ Always works, requires Java runtime |

## Features

- **Standalone native binary**: No JVM required on target machine
- **Fast startup**: Near-instant execution
- **Small footprint**: Minimal memory usage
- **Cross-platform**: Builds available for Windows, macOS, and Linux

## Development Mode (JAR)

For development/testing without native compilation:

```bash
../gradlew :avro-checks-quarkus-cli:quarkusBuild
java -jar build/quarkus-app/quarkus-run.jar --help
```

## Project Structure

```
avro-checks-quarkus-cli/
├── build.gradle.kts                 Gradle configuration with Quarkus plugin
├── build-native.bat                 Windows native build script (with JAR fallback)
├── build-native.sh                  Unix native build script (with JAR fallback)
├── build-native-with-agent.sh       Tracing agent builder (recommended)
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/waver/avro/cli/
│   │   │       └── AvroChecksCommand.java    Picocli command
│   │   └── resources/
│   │       ├── application.properties        Quarkus config
│   │       └── META-INF/native-image/        GraalVM native hints
│   └── test/
│       └── java/
```

## Native Image Configuration

The native image includes:
- Avro schema parsing classes
- All compatibility check logic
- Picocli command framework
- No reflection warnings at runtime

## Exit Codes

- `0` - Compatible
- `1` - Incompatible (migration advice printed)
- `2` - Error (parse error, file not found, etc.)
