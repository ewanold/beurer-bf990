# Android development setup

The Android app is an early skeleton, not yet a working scale app. These steps target Linux x86-64; other operating systems need their own Android Studio and SDK setup. Run commands from the repository root. Allow several gigabytes of free space for both archives and extracted tools. The installer uses ~/.cache for temporary files by default; set TMPDIR to select a filesystem with sufficient space.

1. Check prerequisites: `uname -sm` and `command -v bash curl tar unzip sha256sum mktemp`. Java 17 or newer is required for the Android Gradle Plugin; the installer supplies Studio's JDK.
2. Run `bash scripts/install-android-tools.sh`. It downloads pinned Android Studio and SDK command-line archives and checks SHA-256 before extraction. Default paths are `~/.local/opt/beurer-bf990/android-studio-rabbit1` and `~/Android/Sdk`; `ANDROID_HOME` overrides the SDK path. Existing installations are reused. See Google's [Android Studio terms and downloads](https://developer.android.com/studio).
3. Run `bash scripts/install-android-tools.sh --sdk-packages 'platforms;android-36' 'build-tools;36.0.0' 'platform-tools'`. Read the SDK license prompts and accept them only if you agree. Package IDs must be quoted. SDK packages are resolved by Google and are not covered by the bootstrap archive hashes; platform-tools resolves to the current repository revision.
4. In the same terminal, set `export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"` and `export JAVA_HOME="$HOME/.local/opt/beurer-bf990/android-studio-rabbit1/jbr"`. Verify with `"$JAVA_HOME/bin/java" -version` and `"$ANDROID_HOME/platform-tools/adb" version`.
5. From the repository root run `bash scripts/build-android.sh`. The checked-in Gradle wrapper downloads Gradle 9.3.1 and checks its archive SHA-256. The first build also downloads the Android Gradle Plugin and dependencies from Google and Maven Central. These dependency artifacts do not yet have a checked-in verification manifest. The expected APK is `android/app/build/outputs/apk/debug/app-debug.apk`. The debug skeleton build passed on this host on 2026-11-08, with a nonfatal SDK XML version warning.
6. Alternatively launch `~/.local/opt/beurer-bf990/android-studio-rabbit1/bin/studio.sh` and open the `android` directory. Select the same SDK and Studio JDK. To test on a phone, enable USB debugging, authorize the computer on the phone, check `"$ANDROID_HOME/platform-tools/adb" devices`, and install the debug APK with `"$ANDROID_HOME/platform-tools/adb" install -r android/app/build/outputs/apk/debug/app-debug.apk` from the repository root.

The app uses AGP 9.1.1, Gradle 9.3.1, compile/target API 36 and minimum API 26. The SDK used to compile does not determine the minimum phone version. This combination built the skeleton successfully on this host; the app has not been tested on an Android device. The first build may expose filesystem locking issues on a network share; use a local checkout if needed. A release requires a separately protected signing key; never commit signing keys, personal profiles, captures or measurements.

The local `codex-resume.sh` is ignored because its chat ID belongs to one developer. Future contributors can use their own Codex session and the checked-in AI handover instead. See [MAIN.md](MAIN.md), [HANDOVER.md](HANDOVER.md), and [TODOS.md](TODOS.md).

## Optional Android emulator

Install the emulator and API 36 Google APIs x86-64 image with `bash scripts/install-test-emulator.sh`, then start it with `bash scripts/run-emulator.sh`. SDK package revisions are selected by Google; the bootstrap checksum does not cover them. The launcher uses automatic graphics and Quick Boot. KVM access is recommended. If graphics fail, try `bash scripts/run-emulator.sh -gpu software`. Once Android has booted, from the repository root install and launch the debug build:

```bash
"$ANDROID_HOME/platform-tools/adb" devices -l
"$ANDROID_HOME/platform-tools/adb" -s emulator-5554 install -r android/app/build/outputs/apk/debug/app-debug.apk
"$ANDROID_HOME/platform-tools/adb" -s emulator-5554 shell am start -n org.beurer.bf990/.MainActivity
```

Use the serial reported by `adb devices -l` if different. The AVD manager on this host printed a missing `devices.xml` warning while creating the Pixel 6 AVD, but created a usable configuration. On 2026-11-08 the app installed and its placeholder activity launched; automatic graphics selected software rendering due to a GPU-driver warning. No BF990 connection was tested.
