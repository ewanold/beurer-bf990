# Project log

## 2026-11-07

- Established standalone native Linux connection-proof app and focused Markdown status files.
- Investigated related-model protocol material as background; BF990 behavior was validated separately.
- Verified live BF990 / V02.13 identity, battery, and both standard feature reads. Recorded feature/private-service differences from BF600.
- Public search did not locate a BF990-specific transmission specification. Retained manufacturer and standard references; documented source ambiguities.
- Initial 60 s capture enabled 21 subscriptions but received only identity-property changes. User confirmed official profile setup had failed with Wi-Fi; local Bluetooth and private data storage were chosen.
- Validated locally supplied demographics, normalizing JSON quotes with a private backup. Implemented persisted single-user registration intent, consent, field readback, and recovery; actual BF990 setup succeeded. Activity unchanged.
- Passive post-consent minute received no measurements. Explicit FFFF/FFF4 00 then produced standard weight/body and three private packet types; user reported a changed display.
- Completed requested 180 s repeat after retrying one pre-listening consent timeout with timeout 35. Received three distinct weight/body pairs, nine private payloads, and status 01; timestamps suggest two stored and one current reading.
- Last implementation validation: 23 unit tests and all launcher syntax checks passed. Numeric decoding/private metric meanings remain unverified.
- Removed cross-application and third-party-project references. Rewrote current state/handover/TODOs to remove stale checkpoints, added display-observation guidance and a blank private worksheet. Documentation-only validation checked links, required capture files and syntax; no new Bluetooth session started.

- Repeated 180 s capture on user request, listening from 15:40:02 UTC. Clean exit; six distinct weight/body pairs, 18 private payloads, status 01. Timestamp comparison suggests four stored and two current readings. Saved a local metadata summary; awaiting the user's display notes before numeric verification.

## 2026-11-08

- Compared the filled private worksheet against the six-record capture. Both entries agree on five metrics within display rounding; two current candidates are not uniquely distinguishable. No health values printed.
- Added a constrained offline decoder/comparison and private readable/JSON reports. Verified weight/body percent/water conversion factors on observed samples; preserved unknown raw/private fields and null HR.
- Fixed compact number-plus-kg parsing; all 33 synthetic/unit tests and decoder-launcher syntax pass. No new Bluetooth session/device writes.
- User clarified absent HR was exceptional and earlier HR worked. Recorded intermittent failure, manufacturer grip/stillness advice, and need for a known successful HR sample before private-field mapping.

- Saved final closing checkpoint in HANDOVER with current commands, local artifact paths, five-metric validation, intermittent HR clarification, ambiguity/decoder limits and next-session actions. Confirmed no app capture process or setup lock remains; kept profile, observations and captures intact. Documentation links and four launcher syntax checks verified; no new measurement session started.

## 2026-11-08: Android project preparation

Initialized Git, moved the four Linux launchers to `scripts/`, and added a Codex resume launcher. Created an Android application skeleton; measurement reception, users, retention, charts, and backup/restore are not implemented. Android SDK and Gradle were not found locally, so the skeleton has not been built or run. The existing 33 Python tests passed after moving the launchers.

### Android environment preparation

Added a Linux x86-64 installer for pinned checksum-verified Studio and SDK command-line tools, the checksum-pinned Gradle wrapper, and a contributor setup guide. Selected AGP 9.1.1, Gradle 9.3.1 and API 36 based on an existing verified configuration; this project has not yet been built. SDK licenses require interactive acceptance. The local chat resume launcher and editor state are ignored; AI documentation remains eligible for Git.

The first Android Studio installation attempt downloaded the archive but failed extracting because `/tmp` lacked space. Its temporary files were cleaned up. The installer now defaults temporary files to `~/.cache` (overridable with `TMPDIR`) before retrying.

Retry installed Android Studio and SDK command-line tools with checksum verification. Bundled Java 25.0.3 runs. SDK package installation was declined pending the Android SDK license decision; the project has not yet been built.

After the user accepted the SDK license, verified API 36 revision 2, build tools 36.0.0 and platform tools 37.0.1. The first `bash scripts/build-android.sh` debug skeleton build passed on the network share with a nonfatal SDK XML version warning. No phone test or BF990 Android implementation was performed.

Added the MIT license and a public-facing README introduction and AI handover guide; retained the Linux proof instructions and documented the Android skeleton status.

Created a standalone BF990 protocol guide from project observations and moved the local reference and its reproductions out of the publishable tree into ignored private storage.
