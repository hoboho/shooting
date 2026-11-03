#!/usr/bin/env bash
# Runs the pure-Kotlin JVM unit tests without Gradle/Android SDK.
#
# The 13 test sources use the standard `org.junit.Test` / `org.junit.Assert`
# API, so `./gradlew testDebugUnitTest` runs them unchanged with real JUnit.
# This harness substitutes a tiny reflection runner for environments where
# Maven Central / Google Maven are unreachable. It is NOT an Android build.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

KOTLINC="${KOTLINC:-kotlinc}"
if ! command -v "$KOTLINC" >/dev/null 2>&1; then
  echo "kotlinc not found (set KOTLINC=/path/to/kotlinc)" >&2
  exit 1
fi
KOTLINC_BIN="$(command -v "$KOTLINC")"
KOTLIN_HOME="${KOTLIN_HOME:-$(dirname "$(dirname "$KOTLINC_BIN")")}"
OUT="${OUT:-/tmp/shooting-offline-tests}"

# Android- or coroutine-dependent files are deliberately excluded here: this
# harness only compiles the platform-independent core/domain logic.
MAIN="$(find app/src/main/java/ir/hoboho/shooting/core \
             app/src/main/java/ir/hoboho/shooting/domain -name '*.kt' \
        | grep -v -E 'StringProvider|BackupManager|PdfReportEngine|FileSharer|PublicDownloadSaver|domain/repository/Repositories.kt' \
        | sort | tr '\n' ' ')"
TESTS="$(find app/src/test -name '*.kt' | sort | tr '\n' ' ')"
CLASSES="$(find app/src/test -name '*Test.kt' \
           | sed 's#app/src/test/java/##; s#/#.#g; s#\.kt$##' \
           | sort | tr '\n' ' ')"

rm -rf "$OUT"
echo "Compiling pure-Kotlin sources + tests ..."
# shellcheck disable=SC2086
"$KOTLINC" $MAIN $TESTS \
  scripts/offline-test-harness/JUnitStubs.kt \
  scripts/offline-test-harness/BuildConfigStub.kt \
  scripts/offline-test-harness/TestRunner.kt -d "$OUT"
echo "Running unit tests ..."
# shellcheck disable=SC2086
java -cp "$OUT:$KOTLIN_HOME/lib/*" TestRunnerKt $CLASSES
