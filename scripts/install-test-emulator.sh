#!/usr/bin/env bash
# Optional reproducible emulator setup; SDK licenses remain interactive.
set -euo pipefail
command -v rg >/dev/null || { printf 'Missing ripgrep: run bash scripts/install-system-tools.sh\n' >&2; exit 1; }
cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.."
export JAVA_HOME="${JAVA_HOME:-$HOME/.local/opt/beurer-bf990/android-studio-rabbit1/jbr}"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
export PATH="$JAVA_HOME/bin:$PATH"
bash scripts/install-android-tools.sh --sdk-packages 'emulator' 'system-images;android-36;google_apis;x86_64'
avd_name=bf990-api36
if ! "$ANDROID_HOME/cmdline-tools/15859902/bin/avdmanager" list avd | rg -q "Name: $avd_name$"; then
  printf 'no\n' | "$ANDROID_HOME/cmdline-tools/15859902/bin/avdmanager" create avd \
    --name "$avd_name" --package 'system-images;android-36;google_apis;x86_64' --device 'pixel_6'
fi
printf 'Start with: %s/emulator/emulator -avd %s -no-snapshot -no-audio\n' "$ANDROID_HOME" "$avd_name"
