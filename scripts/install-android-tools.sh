#!/usr/bin/env bash
set -euo pipefail
if [[ ${1:-} == --help ]]; then
  echo 'Usage: bash scripts/install-android-tools.sh [--sdk-packages PACKAGE_ID ...]'
  echo 'Linux x86-64; installs pinned Studio and SDK CLI tools. SDK licenses are interactive.'
  exit 0
fi
if [[ $# -gt 0 && $1 != --sdk-packages ]]; then echo 'Unknown option' >&2; exit 2; fi
if [[ ${1:-} == --sdk-packages ]]; then
  shift
  [[ $# -gt 0 ]] || { echo 'Provide SDK package IDs' >&2; exit 2; }
fi
[[ $(uname -s) == Linux && $(uname -m) == x86_64 ]] || { echo 'Linux x86-64 required' >&2; exit 1; }
for tool in curl tar unzip sha256sum mktemp; do
  command -v "$tool" >/dev/null || { echo "Missing prerequisite: $tool" >&2; exit 1; }
done
studio_dir="$HOME/.local/opt/beurer-bf990/android-studio-rabbit1"
sdk_dir="${ANDROID_HOME:-$HOME/Android/Sdk}"
temp_root="${TMPDIR:-$HOME/.cache}"
mkdir -p -- "$temp_root"
work_dir=$(mktemp -d "$temp_root/bf990-android.XXXXXXXX")
trap 'rm -rf -- "$work_dir"' EXIT
fetch() {
  curl --fail --location --retry 3 --proto '=https' --tlsv1.2 "$1" -o "$2"
  printf '%s  %s\n' "$3" "$2" | sha256sum --check --status
}
if [[ ! -x "$studio_dir/bin/studio.sh" ]]; then
  [[ ! -e "$studio_dir" ]] || { echo "Incomplete install exists: $studio_dir" >&2; exit 1; }
  fetch 'https://edgedl.me.gvt1.com/android/studio/ide-zips/2026.2.1.8/android-studio-rabbit1-linux.tar.gz' "$work_dir/studio.tar.gz" 'f8775c67cf899d9133712a2d7ee3ef7231e07fb16dbe0544c47e5f5babd5931b'
  tar -xzf "$work_dir/studio.tar.gz" -C "$work_dir"
  mkdir -p -- "$(dirname -- "$studio_dir")"
  mv -- "$work_dir/android-studio" "$studio_dir"
fi
cli_dir="$sdk_dir/cmdline-tools/15859902"
if [[ ! -x "$cli_dir/bin/sdkmanager" ]]; then
  [[ ! -e "$cli_dir" ]] || { echo "Incomplete install exists: $cli_dir" >&2; exit 1; }
  fetch 'https://dl.google.com/android/repository/commandlinetools-linux-15859902_latest.zip' "$work_dir/cli.zip" '4e4c464f145a7512b57d088ac6c278c03c9eea610886b35a5e0804e74eedf583'
  unzip -q "$work_dir/cli.zip" -d "$work_dir/cli"
  mkdir -p -- "$sdk_dir/cmdline-tools"
  mv -- "$work_dir/cli/cmdline-tools" "$cli_dir"
fi
export JAVA_HOME="$studio_dir/jbr"
export ANDROID_HOME="$sdk_dir"
export PATH="$JAVA_HOME/bin:$cli_dir/bin:$sdk_dir/platform-tools:$PATH"
if [[ $# -gt 0 ]]; then
  # Install prompts only for licenses required by the requested packages.
  # --licenses would also ask about unrelated packages contributors do not need.
  "$cli_dir/bin/sdkmanager" --sdk_root="$sdk_dir" --install "$@"
  for package in "$@"; do
    package_path=${package//;/\/}
    [[ -f "$sdk_dir/$package_path/source.properties" ]] || { echo "SDK package not installed: $package" >&2; exit 1; }
  done
fi
printf 'Studio: %s/bin/studio.sh\nSDK: %s\nJAVA_HOME: %s\n' "$studio_dir" "$sdk_dir" "$JAVA_HOME"
