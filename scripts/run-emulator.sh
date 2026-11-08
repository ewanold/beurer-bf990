#!/usr/bin/env bash
# Starts the optional emulator installed by install-test-emulator.sh; no downloads.
set -euo pipefail
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
exec "$ANDROID_HOME/emulator/emulator" -avd bf990-api36 \
  -gpu auto -cores 2 -no-boot-anim "$@"
