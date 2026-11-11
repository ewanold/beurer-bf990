# Current work queue

## Current checkpoint

- [x] Owner confirmed the APK signed by the original PC's key updated successfully.
- [x] Owner reported the revised measurement cards and floating charts look better; current weight is now formatted to one decimal in the latest build.
- [ ] Test real backup transfer and the restore picker.

- [x] Copy the presumed original PC debug key into ignored shared storage, configure debug signing, rebuild and verify the APK signer.
- [ ] Check shared signing directory access controls and protect a backup of the key; verify retained app data as part of the real backup test.

Five standard displayed metrics are validated by the constrained offline decoder; 45 tests pass, including protocol-candidate parser and disposable-user deletion safeguards. No capture is running. Resume from [HANDOVER.md](HANDOVER.md), including its closing checkpoint and [protocol test plan](PROTOCOL_TEST_PLAN.md).

## Measurement validation and protocol research

- [x] Coordinate/complete another announced 180 s observation capture: 6 weight/body pairs; timestamps suggest 4 stored and 2 current readings.
- [x] Read supplied display observations privately; HR absent this time but user reports prior successful HR. Timing remains incomplete.
- [x] Distinguish current/history readings and compare observations. Both entries match both current candidates; do not claim unique cycle assignment.
- [x] Implement constrained 0E/0398 offline decoding and validate five observed metrics within display rounding.
- [ ] Identify useful private FFF5/FFF7/FFF8 meanings using evidence rather than plausible ranges; heart rate is low priority at the owner’s request.
- [x] Add synthetic decoder/comparison tests; all 33 tests pass, no personal fixture data.

## Completed foundation

- [x] Document observed BF990 protocol and measurement sequence in a standalone guide.
- [x] Verify real BF990 identity/battery/features and app-owned connection cleanup.
- [x] Preserve service/characteristic relationships, flags, and descriptors in capture inventory.
- [x] Validate private local profile input; register/authorize one user with recovery state.
- [x] Verify profile readback/revision, and reuse configured user without rewriting demographics.
- [x] Verify explicit FFFF/FFF4 byte 00 measurement start and actual standard/private reception.
- [x] Complete 180 s repeat: 3 distinct weight/body pairs, 9 private payloads, status 01; timestamps suggest 2 stored + 1 current reading.
- [x] Pass 33 unit tests; verify Bash syntax of all four launchers.
- [x] Remove cross-application/third-party-project references and consolidate resumable documentation.

## Deferred decisions

- [ ] Verify activity-level command before adding any activity writes.
- [ ] Investigate reconnect behavior only if needed for reliable repeated capture.

- [ ] If HR is investigated later, require a known successful displayed value before mapping a field. Preserve unavailable HR as null.
- [ ] Broaden decoder layouts/units only with evidence; keep BMR/soft-lean/impedance raw until verified.

## Android application

- [x] Implement and validate Android BLE discovery, consent, subscriptions and restart against the actual BF990. Owner reported 2026-11-11 after three days of phone use: measurements read and displayed correctly, user switching and settings changes work.
- [ ] Implement household user selection by scale index, local name per phone, birth-date verification, registration and profile modification with safe recovery of uncertain registration outcomes.
- [ ] Investigate how to enumerate occupied indices and authorize an existing user on a newly installed phone; do not infer that an inaccessible index is free.
- [x] Verify selected-user deletion on one disposable BF990 user, reauthorize user 1 and check rejected test credentials after reconnect.
- [ ] Verify whether deleted-user slot and stored measurements are removed or reusable; do not infer full user enumeration.
- [ ] Discard received measurements older than five minutes; do not import invalid, uncertain or implausibly future timestamps as current. Preserve raw packets with accepted normalized measurements and implement daily, weekly and monthly retention without mixing users.
- [ ] Read the pre-correction scale clock and assess incoming measurement age against it (account for elapsed time); discard readings older than five minutes before setting phone-local time if drift exceeds approximately one minute. Validate transfer/clock-write ordering, DST, long-term drift, battery replacement and queued readings.
- [ ] If setup or synchronization causes the measurement window to expire, show a timeout and offer another reading.
- [x] Add measurement display, charts, and user-controlled backup and restore. Backup zip with share/save/restore implemented 2026-11-11; emulator-validated with synthetic data.
- [ ] Transfer a real backup to the second phone (Telegram/email/Downloads) and check the imported charts; click through Restore from file.
- [ ] Show scale battery percentage using a fresh Battery Service 180F/2A19 read; indicate when no current reading is available.
- [x] Install Android SDK and Gradle; build the debug skeleton.
- [x] Test the Android app and verify scale communication on hardware. Owner reported 2026-11-11 after three days of phone use: measurements read and displayed correctly, user switching and settings changes work.

## Other-model protocol candidates

- [x] Add synthetic advertising-structure parser coverage and a coordinated live-test checklist. Candidate user-list layout remains design-only; no BF990 behavior is verified.
- [x] Repeat two 60 s BlueZ advertising scans without measurement; BF600 candidate UUID was not observed, result inconclusive. Raw captures remain private.
- [ ] Verify pending-data advertisements, user-list request and historical-transfer behavior on BF990 with the staged procedure in [BF990 protocol](../docs/BF990-protocol.md#repeatable-verification-plan).
- [ ] Inventory and verify clock, settings, activity, reference-weight and additional status candidates; do not assume matching short UUIDs imply matching services or layouts.
- [ ] Create synthetic fixtures and repeatable capture procedures before coordinated live tests; no unknown writes or deletion of existing profiles.

- [ ] Investigate whether BF990 supports safe indexed user discovery (range and command unknown) or writable initials; do not guess consent codes or assume 12/16 slots.

- [ ] Account for measured queued transfer spanning roughly 29 seconds after consent (14 earlier frames); no queue-complete marker identified. Do not use milliseconds or a short inactivity gap as proof of completion.

## Android current-reading checkpoint

- [x] Build provisional Android BLE consent/measurement path and emulator synthetic decoder display.
- [x] Validate real Android BLE, operation sequencing and notification handling on BF990. Owner reported 2026-11-11 after three days of phone use: measurements read and displayed correctly, user switching and settings changes work.
- [ ] Deliberately test error, reconnect and timeout paths (not covered by normal use).
- [ ] Implement user registration, selection/discovery and consent transfer; current screen requires manually supplied existing index/code.
- [ ] Implement verified clock correction after handling queued readings and test timestamp/queue ordering.
- [x] Add per-user daily storage, weighted weekly/monthly rollups and selectable diagrams; synthetic emulator checks completed.
- [ ] Validate retention edge cases, same-day replacement and real data on device; backup including raw archive implemented 2026-11-11.

## Language and terminology

- [x] Add English and German Android string resources and a language selector in Settings, with system default fallback.
- [x] Replace user-facing “consent code” wording with “Password (4 digits)”, without changing the protocol or exposing credentials.
- [x] Confirm the five-chart, three-range-button, measurement-card and theme layouts on the phone. Owner reported 2026-11-11 after three days of phone use: measurements read and displayed correctly, user switching and settings changes work.
- [ ] Review translation coverage for technical exception details and expand languages as needed.

- [ ] Validate optional Android birth-date verification on BF990, including mismatch/read failure and early notifications; investigate verified user enumeration before offering selection by birth date.

- [x] Run the prepared two-user experiment: users 2 and 3 and original user 1 individually verified; preserve recovery states.
- [ ] Investigate a verified BF990 user-list operation with the two disposable profiles present; known-index readbacks do not establish enumeration.

- [x] Compare pre-consent User Data reads with known user 2 and inventory the User Data service: unselected fields were FF and no standard initials characteristic was present.
- [ ] Find and verify a BF990 list/initials mechanism without assuming read-only 2A9A can select a slot or interpreting unselected FF as a missing user.

- [x] Run the owner-authorized shared-code disposable-user change and scan indices 1–12 on BF990.
- [x] User reports Android shared-code user readout works on the phone.
- [ ] Implement safe registration and empty-scale onboarding; extend profile modification beyond date and height. The fixed code exposes the current user 1 credential in the APK and source.

- [ ] Before public release, verify that the icon’s Flaticon terms permit distributing its PNG in the public source repository; retain in-app attribution.
- [ ] Keep sequential code testing as an owner-approved last-resort recovery idea only; lockout, timing on one index and empty-slot identification are unknown.

- [ ] Test Android height read, selected-user write, revision readback, mismatch, interruption and recovery on disposable BF990 user 2 before applying profile changes to user 1.

- [x] Owner reports the Android profile update works in normal use (2026-11-11).
- [ ] Validate profile mismatch and interruption recovery on disposable user 2 (not covered by normal use).
