#!/usr/bin/env bash
# Compiles the platform-independent + data/di Kotlin sources against small API
# stubs. This is NOT an Android build: it catches Kotlin syntax/type/suspend
# mistakes in 42 sources when Gradle/Maven are unavailable, but it cannot check
# Compose UI, Room's annotation processor, Hilt codegen or resource linking.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"

KOTLINC="${KOTLINC:-kotlinc}"
COROUTINES_JAR="${COROUTINES_JAR:-}"
if [[ -z "$COROUTINES_JAR" ]]; then
  if [[ -n "${KOTLIN_HOME:-}" && -f "$KOTLIN_HOME/lib/kotlinx-coroutines-core-jvm.jar" ]]; then
    COROUTINES_JAR="$KOTLIN_HOME/lib/kotlinx-coroutines-core-jvm.jar"
  elif command -v "$KOTLINC" >/dev/null 2>&1; then
    HOME_GUESS="$(dirname "$(dirname "$(command -v "$KOTLINC")")")"
    if [[ -f "$HOME_GUESS/lib/kotlinx-coroutines-core-jvm.jar" ]]; then
      COROUTINES_JAR="$HOME_GUESS/lib/kotlinx-coroutines-core-jvm.jar"
    fi
  fi
fi
if [[ ! -f "$COROUTINES_JAR" ]]; then
  echo "Set COROUTINES_JAR to kotlinx-coroutines-core-jvm.jar" >&2
  exit 1
fi

WORK="${WORK:-/tmp/shooting-compile-check}"
rm -rf "$WORK"
mkdir -p "$WORK"
python3 scripts/offline-compile-check/generate_r_stub.py "$ROOT" "$WORK/stubs"

FILES="$(find app/src/main/java/ir/hoboho/shooting/core \
              app/src/main/java/ir/hoboho/shooting/domain \
              app/src/main/java/ir/hoboho/shooting/data \
              app/src/main/java/ir/hoboho/shooting/di -name '*.kt' \
         | grep -v -E 'PublicDownloadSaver' | sort | tr '\n' ' ')"
STUBS="$(find scripts/offline-compile-check/stubs "$WORK/stubs" -name '*.kt' | sort | tr '\n' ' ')"
echo "Compiling $(echo $FILES | wc -w) Kotlin sources against API stubs ..."
# shellcheck disable=SC2086
"$KOTLINC" $FILES $STUBS -cp "$COROUTINES_JAR" -d "$WORK/classes"
echo "Offline Kotlin compile check: OK"
