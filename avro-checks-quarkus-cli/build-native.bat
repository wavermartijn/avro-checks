@echo off
setlocal

echo ============================================
echo  avro-checks Native Image Builder (Windows)
echo ============================================
echo.

REM Check for GraalVM
where native-image.cmd >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo ERROR: native-image.cmd not found in PATH.
    echo Please install GraalVM and ensure native-image is on your PATH.
    echo.
    echo Download: https://www.graalvm.org/downloads/
    exit /b 1
)

echo [1/3] Building avro-checks library...
cd ..\avro-checks
if exist ..\gradlew.bat (
    call ..\gradlew.bat build publishToMavenLocal -x test
) else (
    echo ERROR: Gradle wrapper not found
    exit /b 1
)
if %ERRORLEVEL% neq 0 (
    echo Library build failed!
    exit /b 1
)
cd ..\avro-checks-quarkus-cli

echo.
echo [2/3] Building native image with Quarkus...
echo This may take several minutes...
echo.

if exist ..\gradlew.bat (
    call ..\gradlew.bat :avro-checks-quarkus-cli:buildNative -x test
) else (
    echo ERROR: Gradle wrapper not found
    exit /b 1
)

if %ERRORLEVEL% neq 0 (
    echo Native image build failed!
    exit /b 1
)

echo.
echo [3/3] Verifying native executable...
if exist build\avro-checks-quarkus-cli-0.0.1-RC1-runner.exe (
    echo SUCCESS: Native executable created!
    echo Location: build\avro-checks-quarkus-cli-0.0.1-RC1-runner.exe
    echo.
    echo Testing executable...
    build\avro-checks-quarkus-cli-0.0.1-RC1-runner.exe --version
) else (
    echo ERROR: Native executable not found!
    exit /b 1
)

echo.
echo ============================================
echo  Build Complete!
echo ============================================
echo.
echo Usage: build\avro-checks-quarkus-cli-0.0.1-RC1-runner.exe --help

endlocal
