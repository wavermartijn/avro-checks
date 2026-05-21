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
cd ..
# Use direct java invocation to avoid gradlew script issues
$JAVA_HOME/bin/java -cp gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain :avro-checks:build :avro-checks:publishToMavenLocal -x test --no-daemon
cd ../avro-checks-quarkus-cli

echo ""
echo "[2/3] Building native image with Quarkus..."
echo "This may take several minutes..."
echo ""

# Use direct java invocation to avoid gradlew script issues
$JAVA_HOME/bin/java -cp ../gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain :avro-checks-quarkus-cli:nativeImage -Dquarkus.package.type=native -x test --no-daemon

echo ""
echo "[3/3] Verifying native executable..."

# Determine executable extension based on OS
if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "cygwin" || "$OSTYPE" == "win32" ]]; then
    EXECUTABLE="build/avro-checks-quarkus-cli-0.0.1-RC1-runner.exe"
else
    EXECUTABLE="build/avro-checks-quarkus-cli-0.0.1-RC1-runner"
fi

if [ -f "$EXECUTABLE" ]; then
    echo "SUCCESS: Native executable created!"
    echo "Location: $EXECUTABLE"
    echo ""
    echo "Testing executable..."
    "$EXECUTABLE" --version
else
    echo "ERROR: Native executable not found!"
    exit 1
fi

echo ""
echo "============================================"
echo "  Build Complete!"
echo "============================================"
echo ""
echo "Usage: $EXECUTABLE --help"
