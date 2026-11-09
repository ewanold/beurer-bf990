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

- Captured two owner-coordinated 60 s BlueZ advertising-property scans without measurements, GATT connections, or writes. Each exposed one BF990 property set with ServiceData; neither matched BF600 candidate UUID 0x181D. Result is inconclusive; raw bytes remain in ignored private captures. Added reusable scanner and recorded limitations in handover/protocol guide.

- Added an offline BLE advertising structure parser, BF600-candidate-only pending-data extraction, synthetic parser coverage, and a staged live-test checklist. Full Python suite: 40 tests passed. No BF990 advertising/list behavior is inferred and no device operation occurred.

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

Installed and started the API 36 emulator, installed the debug skeleton and confirmed its activity entered the foreground. KVM was available; graphics fell back automatically to software. AVD creation printed a devices.xml warning but produced a usable configuration. No scale test was performed.

Added an Android navigation preview with separate current values, diagrams and settings pages. Scale connection, user setup, repeat, storage and backup remain disabled and unimplemented. No device protocol behavior is claimed by this UI.

The navigation preview built successfully and was installed and launched on the API 36 emulator. Functional scale, storage and backup validation remains outstanding.

The user clarified the household model: agreed scale indices, phone-local names, date-of-birth verification, registration for a missing user, and profile modification; scale-side deletion is deferred. Slot discovery and existing-user authorization on a new phone remain unverified.

The user clarified that the selection screen should list available scale indices and provide an Add user option. The method for retrieving the indices remains to be verified.

Documented BF600-derived candidate user enumeration, deletion, pending-data advertising, time and other commands with explicit BF990 uncertainty and a staged verification plan. No live commands or device writes were performed.

Prepared a separate-session protocol test handoff with synthetic-first deliverables, evidence criteria, shared-working-tree guidance and live-device safeguards.

Coordinated before/after measurement advertising scans; each recorded only a pre-discovery, potentially cached property snapshot, so identical properties are inconclusive. Raw monitor access required administrator privileges. No GATT connection or device write.

Owner-assisted privileged btmon capture during discovery recorded live scale advertising after a measurement. No comparable raw baseline exists; pending-data semantics remain unverified. Capture stays in ignored database.

Compared owner-coordinated raw advertisements before and after a measurement: one manufacturer-data byte had an additional transient variant after the measurement; causality and pending-data meaning remain unverified. HR was unavailable in that reading. No GATT connection or scale write.

Coordinated disposable user 2 registration, fake profile readback, measurement confirmed by display and packet, selected-user deletion, reauthorization of user 1, and post-reconnect rejection of test credentials. User enumeration and deletion of historical data remain unverified. Added guarded experiment scripts and synthetic safety tests; 45 tests passed.

Recorded user request to display scale battery percentage in Android using the already observed live Battery Service reading.

Recorded owner decisions: discard incoming readings older than five minutes, investigate clock drift correction and possible index/initials discovery without assuming support, and deprioritize HR.

Read BF990 Current Time live: 10-byte date/time, read/write/notify properties, approximately 223 seconds behind host local time. No clock write; ticking and time-setting remain unverified.

Owner-authorized clock test wrote local time once; immediate readback was within one second and a reconnect showed an advancing clock within two seconds. Long-term, DST and queued-reading effects remain unverified.

Recorded owner-directed ordering: evaluate measurement age against the pre-correction scale clock, then correct drift; offer another reading if setup causes a timeout.

Owner-coordinated queued-versus-new timing capture: 14 earlier weight frames arrived over roughly 29 seconds after consent; fresh reading arrived at roughly 98 seconds and final status at 100 seconds. No separate queue completion marker identified.

Implemented provisional Android BLE current-reading path, synthetic emulator packet display, private last-reading storage and detailed errors. Debug APK builds and synthetic UI/decoder checks passed; real BF990 Android communication, onboarding, clock correction, RRD and backup remain unverified or unimplemented.

Added SQLite per-user daily storage, weekly/monthly weighted rollups, raw-packet archive, selectable diagrams and segregated emulator sample history. Debug build passed; emulator generated 80 synthetic observations with stable counts across repeated sample generation. Physical-phone BLE and backup remain unverified.

Fixed Android top navigation visibility using system insets and a no-action-bar theme; debug build and emulator Settings navigation passed. Phone confirmation remains pending.

Owner confirmed Android phone authorization and diagram values. Changed diagrams to show five charts on a scrollable page and three time-range buttons; debug build and emulator range-selection check passed.

Recorded next-session requests for internationalization, a Settings language selector and “Password (4 digits)” terminology; no implementation requested. Owner will resume later.

## 2026-11-09 — Android design and localization

Added rounded measurement cards with illustrative scales, bordered charts, rounded navigation controls, light/dark/system appearance and English/German/system language selection. Renamed user-facing consent code to password; protocol remains unchanged. Debug build succeeded; phone visual verification and other open features remain.

## 2026-11-09 — Chart and navigation refinements

Moved bold, theme-aware chart titles into cards, padded the plot axes, added whole-number ticks and requested default ranges, and enabled horizontal page swipes. Debug build passed; phone verification remains.

## 2026-11-09 — Settings and header polish

Added rounded labeled settings cards, spaced and bordered navigation buttons, and localized title and current-user headings.

## 2026-11-09 — Timeline ticks and selected-user verification

Shortened navigation labels, made chart grid spacing uniform, removed the illustrative-range note, and added optional selected-user birth-date verification. Full user enumeration remains unverified; Android hardware testing is outstanding.

## 2026-11-09 — Two-user experiment prepared

Prepared a guarded test to register and individually verify two synthetic users, restore the original selected user, and preserve registration recovery states. The offline suite passed 50 tests; no live scale operation has run. User enumeration remains a separate unverified question.

## 2026-11-09 — Two-user test interrupted on reconnect

Synthetic user 2 was registered and its profile read back successfully. The connection for user 3 failed with D-Bus NoReply before registration; the saved user 3 state remains prepared. Preserve states and retry after waking the scale. Full enumeration and reselecting user 1 remain unconfirmed.

## 2026-11-09 — Two-user readback completed

Registered two synthetic profiles, verified indices 2 and 3 and their profile fields on a fresh connection, then verified and reselected user 1. Both test users remain; their recovery states are private. Full enumeration remains unverified.

## 2026-11-09 — User discovery probe

Live reads before authorization returned unselected index and unknown profile-field sentinels despite three occupied users; after known-user consent the test profile read back. The User Data inventory exposes no standard initials characteristic. User 1 was reauthorized.

## 2026-11-09 — Failed disposable-user authorization probe

One incorrect authorization for user 3 was rejected and left previously authorized user 2 selected; profile reads still matched user 2. No profile writes were attempted, and user 1 was reauthorized and verified.

## 2026-11-09 — Shared code and disposable-user preparation

Removed the Android password field and used the owner-requested fixed common code; debug build passes. Prepared guarded deletion and recreation of disposable users; device test remains pending. Publishing the code or APK exposes the existing credential.

## 2026-11-09 — Shared-code user scan

Deleted disposable user 3, recreated user 2 using user 1’s code, and verified user 1. A live scan found indices 1 and 2 accessible and 3–12 inaccessible; rejection does not prove absence. Android Settings now offers a shared-code scan and selection; hardware validation, registration and profile modification remain. Debug build and 54 Python tests passed.

## 2026-11-09 — User selection and launcher icon

Limited optional shared-code scanning to indices 1–8 without asserting the device maximum, clarified saving a phone-local default user, and added an attributed Flaticon launcher icon. Recorded sequential-code recovery as an untested last resort. Debug build passed; icon source redistribution terms need checking before public release.

## 2026-11-09 — Timeline controls and user-list placement

Shortened and spaced timeline buttons; moved discovered-user choices directly below the readout status and surfaced scan permission failures on Settings. Debug build passed; Android scan still needs hardware verification.

## 2026-11-09 — Simplified user readout

Owner confirmed Android readout works. Added padding and borders to readout and user buttons, removed explanatory and inaccessible-index text, and clear temporary results on save while retaining diagnostic feedback for incomplete reads.

## 2026-11-09 — Height setting

Added height to user readout and settings, with a queued scale update after selected-user checks, readback and recovery marker. Android height writes still require a disposable-user hardware test.

## 2026-11-09 — Controlled profile updates

Added birth-date updates guarded by a previously read profile date, readback and recovery state. Saving prompts the user to wake the scale and starts connecting. Debug build and synthetic date-decision checks passed; hardware verification remains.

## 2026-11-09 — Closing checkpoint

Owner reports that the latest Android profile update seems to work. Updated the handover and remaining work; individual field and recovery validation remains open.
