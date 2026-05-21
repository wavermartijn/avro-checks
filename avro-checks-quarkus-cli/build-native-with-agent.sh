#!/bin/bash
set -e

# Setup GraalVM environment
export GRAALVM_HOME=/usr/lib/graalvm
export JAVA_HOME=$GRAALVM_HOME
export PATH=$GRAALVM_HOME/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin

echo "============================================"
echo "  avro-checks Native Image with Tracing Agent"
echo "============================================"
echo ""
echo "JAVA_HOME: $JAVA_HOME"
echo ""

# Check for GraalVM
if ! command -v native-image &> /dev/null; then
    echo "ERROR: native-image not found in PATH."
    echo "Please install GraalVM and ensure native-image is on your PATH."
    exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR/.."

echo "[1/4] Building avro-checks library..."
$JAVA_HOME/bin/java -cp gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain :avro-checks:build :avro-checks:publishToMavenLocal -x test --no-daemon

echo ""
echo "[2/4] Building Quarkus JAR..."
$JAVA_HOME/bin/java -cp gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain :avro-checks-quarkus-cli:quarkusBuild -x test --no-daemon

cd avro-checks-quarkus-cli

JAR_FILE="build/quarkus-app/quarkus-run.jar"
if [ ! -f "$JAR_FILE" ]; then
    echo "ERROR: JAR file not found at $JAR_FILE"
    exit 1
fi

# Create directory for agent output
mkdir -p src/main/resources/META-INF/native-image/com.waver.avro/avro-checks-quarkus-cli

# Check if agent is available
AGENT_PATH="$GRAALVM_HOME/lib/tracing-agent.jar"
if [ ! -f "$AGENT_PATH" ]; then
    # Try alternative location
    AGENT_PATH="$GRAALVM_HOME/lib/svm/bin/tracing-agent.jar"
fi
if [ ! -f "$AGENT_PATH" ]; then
    echo "WARNING: Tracing agent not found at expected locations."
    echo "Trying native-image-agent instead..."
    AGENT_LIB="$GRAALVM_HOME/lib/svm/liblibrary-support.a"
fi

echo ""
echo "[3/4] Running with tracing agent to collect metadata..."
echo "This will execute the CLI to capture all reflection/resource usage."
echo ""

# Create config directory
CONFIG_DIR="src/main/resources/META-INF/native-image/com.waver.avro/avro-checks-quarkus-cli"

# Run with agent using the agentlib approach
# The agent generates reflect-config.json, resource-config.json, etc.
echo "Running: avro-checks --help"
$JAVA_HOME/bin/java \
    -agentlib:native-image-agent=config-output-dir=$CONFIG_DIR \
    -jar build/quarkus-app/quarkus-run.jar \
    --help 2>/dev/null || true

echo ""
echo "Running: avro-checks with sample schemas..."
$JAVA_HOME/bin/java \
    -agentlib:native-image-agent=config-merge-dir=$CONFIG_DIR \
    -jar build/quarkus-app/quarkus-run.jar \
    -f src/test/resources/schemas/order-v1.json src/test/resources/schemas/order-v2-optional-field.json \
    --level BACKWARD 2>/dev/null || true

echo ""
echo "Generated configuration files:"
ls -la $CONFIG_DIR/ 2>/dev/null || echo "  (no files yet - will be created during native build)"

echo ""
echo "[4/4] Building native image..."
echo "Note: Using Quarkus native build with generated configuration"
echo ""

# Rebuild JAR with the new configuration in resources
cd ..
$JAVA_HOME/bin/java -cp gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain :avro-checks-quarkus-cli:quarkusBuild -x test --no-daemon

cd avro-checks-quarkus-cli

# Try native image build
if $JAVA_HOME/bin/java -cp ../gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain :avro-checks-quarkus-cli:nativeImage -Dquarkus.package.type=native -x test --no-daemon 2>&1; then
    echo ""
    echo "SUCCESS: Native executable created!"
    echo "Location: build/avro-checks-quarkus-cli-runner"
    echo ""
    echo "Testing..."
    ./build/avro-checks-quarkus-cli-runner --version
    echo ""
    echo "============================================"
    echo "  Build Complete! (Native)"
    echo "============================================"
else
    echo ""
    echo "Native build failed. Configuration collected in:"
    echo "  $CONFIG_DIR"
    echo ""
    echo "You may need to run the agent with more test scenarios"
    echo "to capture all reflection usage patterns."
    exit 1
fi
