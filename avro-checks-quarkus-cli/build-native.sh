#!/bin/bash
set -e

# Setup GraalVM environment
export GRAALVM_HOME=/usr/lib/graalvm
export JAVA_HOME=$GRAALVM_HOME
export PATH=$GRAALVM_HOME/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin

echo "============================================"
echo "  avro-checks Native Image Builder (Unix)"
echo "============================================"
echo ""
echo "JAVA_HOME: $JAVA_HOME"
echo ""

# Check for GraalVM
if ! command -v native-image &> /dev/null; then
    echo "ERROR: native-image not found in PATH."
    echo "Please install GraalVM and ensure native-image is on your PATH."
    echo ""
    echo "Download: https://www.graalvm.org/downloads/"
    exit 1
fi

echo "[1/3] Building avro-checks library..."
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR/.."
# Use direct java invocation to avoid gradlew script issues
$JAVA_HOME/bin/java -cp gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain :avro-checks:build :avro-checks:publishToMavenLocal -x test --no-daemon
cd avro-checks-quarkus-cli

echo ""
echo "[2/3] Building native image with Quarkus..."
echo "This may take several minutes..."
echo ""

# Try native image build
echo "Attempting native image build..."
echo "Note: Apache Avro uses reflection which may cause native build to fail."
echo "If this fails, a JAR will be created instead."
echo ""

if $JAVA_HOME/bin/java -cp ../gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain :avro-checks-quarkus-cli:nativeImage -Dquarkus.package.type=native -x test --no-daemon 2>&1; then
    echo ""
    echo "[3/3] Verifying native executable..."

    # Determine executable extension based on OS
    if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "cygwin" || "$OSTYPE" == "win32" ]]; then
        EXECUTABLE="build/avro-checks-quarkus-cli-runner.exe"
    else
        EXECUTABLE="build/avro-checks-quarkus-cli-runner"
    fi

    if [ -f "$EXECUTABLE" ]; then
        echo "SUCCESS: Native executable created!"
        echo "Location: $EXECUTABLE"
        echo ""
        echo "Testing executable..."
        "$EXECUTABLE" --version
        echo ""
        echo "============================================"
        echo "  Build Complete! (Native)"
        echo "============================================"
        echo ""
        echo "Usage: $EXECUTABLE --help"
        exit 0
    fi
fi

# Native build failed, try JAR mode
echo ""
echo "Native image build failed or not available."
echo "Falling back to JAR mode (requires Java runtime)..."
echo ""

echo "[2b] Building JAR mode..."
$JAVA_HOME/bin/java -cp ../gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain :avro-checks-quarkus-cli:quarkusBuild -x test --no-daemon

JAR_FILE="build/quarkus-app/quarkus-run.jar"
if [ -f "$JAR_FILE" ]; then
    echo ""
    echo "SUCCESS: JAR created!"
    echo "Location: $JAR_FILE"
    echo ""
    echo "Testing JAR..."
    $JAVA_HOME/bin/java -jar "$JAR_FILE" --version
    echo ""
    echo "============================================"
    echo "  Build Complete! (JAR Mode)"
    echo "============================================"
    echo ""
    echo "Usage: java -jar $JAR_FILE --help"
    echo ""
    echo "Note: This requires Java to be installed."
    echo "To create a native binary without Java dependency,"
    echo "additional GraalVM configuration is needed."
else
    echo "ERROR: Neither native executable nor JAR found!"
    exit 1
fi
