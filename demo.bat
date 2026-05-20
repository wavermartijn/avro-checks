@echo off
setlocal

echo ============================================================
echo  avro-checks demo
echo ============================================================
echo.

REM ── 1. Build and test ────────────────────────────────────────
echo [1/4] Building and running all tests...
call gradlew.bat build
if %ERRORLEVEL% neq 0 (
    echo BUILD FAILED. Aborting demo.
    exit /b 1
)
echo.

REM ── Write demo schemas to temp files ────────────────────────
set TMPDIR=%TEMP%\avro-checks-demo
if not exist "%TMPDIR%" mkdir "%TMPDIR%"

echo {"type":"record","name":"Order","namespace":"com.waver.avro","fields":[{"name":"id","type":"string"},{"name":"amount","type":"double"}]} > "%TMPDIR%\v1.json"
echo {"type":"record","name":"Order","namespace":"com.waver.avro","fields":[{"name":"id","type":"string"},{"name":"amount","type":"double"},{"name":"currency","type":"string","default":"USD"}]} > "%TMPDIR%\v2.json"
echo {"type":"record","name":"Order","namespace":"com.waver.avro","fields":[{"name":"id","type":"string"},{"name":"amount","type":"double"},{"name":"currency","type":"string"}]} > "%TMPDIR%\v3.json"

set JAR=avro-checks-cli\build\libs\avro-checks-cli-1.0.0-SNAPSHOT.jar

REM ── 2. Compatible: add field with default (BACKWARD) ─────────
echo [2/4] Demo — BACKWARD compatible change
echo   v2 adds 'currency' with default "USD" to v1
echo.
java -jar %JAR% -f "%TMPDIR%\v2.json" "%TMPDIR%\v1.json" BACKWARD
echo.

REM ── 3. Incompatible: add field without default (BACKWARD) ────
echo [3/4] Demo — BACKWARD incompatible change
echo   v3 adds 'currency' WITHOUT a default to v1
echo.
java -jar %JAR% -f "%TMPDIR%\v3.json" "%TMPDIR%\v1.json" BACKWARD
echo.

REM ── 4. Compatible: add any field (FORWARD) ───────────────────
echo [4/4] Demo — FORWARD compatible change
echo   v3 (new writer) vs v1 (old reader) — old reader ignores unknown fields
echo.
java -jar %JAR% -f "%TMPDIR%\v3.json" "%TMPDIR%\v1.json" FORWARD
echo.

echo ============================================================
echo  Demo complete.
echo ============================================================
endlocal
