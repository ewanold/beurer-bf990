#!/usr/bin/env bash
set -euo pipefail
project_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
export JAVA_HOME="${JAVA_HOME:-$HOME/.local/opt/beurer-bf990/android-studio-rabbit1/jbr}"
[[ -x "$JAVA_HOME/bin/java" ]] || { echo "JDK not found: $JAVA_HOME" >&2; exit 1; }
[[ -f "$ANDROID_HOME/platforms/android-36/source.properties" ]] || { echo "Android API 36 SDK is not installed" >&2; exit 1; }
cd -- "$project_dir/android"
exec bash ./gradlew "${@:-assembleDebug}"
