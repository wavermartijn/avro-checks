# avro-checks-quarkus-cli

> Quarkus-based CLI for avro-checks with GraalVM native compilation support.

This module provides a native-compiled CLI that runs without requiring Java installation.

## Prerequisites

- [GraalVM](https://www.graalvm.org/downloads/) with native-image tool installed
- Maven 3.9+ (or use the included Maven wrapper)

## Quick Start

### Build Native Image

**Windows:**
```bash
build-native.bat
```

**Mac/Linux:**
```bash
chmod +x build-native.sh
./build-native.sh
```

### Usage

```bash
# Check compatibility (default: BACKWARD)
./target/avro-checks-quarkus-cli-0.0.1-RC1-runner -f new-schema.json old-schema.json

# With specific compatibility level
./target/avro-checks-quarkus-cli-0.0.1-RC1-runner -f new.json old.json --level FULL

# With historical schemas (for transitive checks)
./target/avro-checks-quarkus-cli-0.0.1-RC1-runner -f new.json old.json --with-history older1.json,older2.json

# Help
./target/avro-checks-quarkus-cli-0.0.1-RC1-runner --help
```

## Features

- **Native compilation**: No JVM required - runs as standalone binary
- **Fast startup**: Near-instant execution
- **Small footprint**: Minimal memory usage
- **Cross-platform**: Builds available for Windows, macOS, and Linux

## Building JAR Mode (Non-Native)

For development/testing without native compilation:

```bash
mvn package
java -jar target/quarkus-app/quarkus-run.jar --help
```

## Project Structure

```
avro-checks-quarkus-cli/
├── pom.xml                          Maven configuration
├── build-native.bat                 Windows native build script
├── build-native.sh                  Unix native build script
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
