# Handover: 2026-11-08

## Offline protocol test foundation: 2026-11-08

Added an offline BLE advertising AD-structure parser and synthetic tests for complete, absent, malformed/truncated, unrelated, duplicate, unknown-field, multiple-index, and empty-pending cases. Raw advertising bytes are retained. The `0x181D` interpretation is labeled BF600 candidate only; no BF990 advertising semantics are verified. See [protocol test plan](PROTOCOL_TEST_PLAN.md) for the private evidence template, abort conditions, and coordinated live stages.

Candidate user-list behavior remains a test design only. BF600 entry/terminator/status layout is explicitly identified as hypothetical; no BF990 endpoint or parser is claimed. Full suite: **40 tests passed** using the command below. No live device operation occurred. Next action is owner-coordinated passive advertising/inventory before any request investigation.

## BF990 advertising scan observations: 2026-11-08

Two owner-coordinated 60-second BlueZ LE discovery scans were recorded while waking the scale; no measurement was taken during either scan, and neither connected to GATT or sent device writes. Each scan exposed one set of BF990 advertising properties, including ServiceData, but neither ServiceData key matched the BF600 candidate UUID `0x181D`. This does not show that BF990 uses another ID or identify the data: BlueZ exposed only one property set in each window, and the scans were not raw radio captures. Results are inconclusive for queued-data advertising.

Raw captures remain private in `database/captures/advertising-20261108T095640-084422Z.jsonl` and `database/captures/advertising-20261108T100416-807271Z.jsonl`. Do not copy their bytes, address, or device path into public notes. A new scanner entry point is `python3 -m beu_connect.advertise_capture`; it records BlueZ ServiceData/ManufacturerData properties without connecting. User-list request remains deferred: the existing inventory has not shown the BF600-required notification-capable endpoint, so do not write its candidate request.

## Before/after advertising attempt: 2026-11-08

An owner-coordinated baseline scan, one scale measurement with no app connected, and a post-measurement scan were completed. The user observed the Bluetooth icon appear late during measurement and blink after waking the scale for the second scan. Captures are `database/captures/advertising-20261108T100959-541110Z.jsonl` and `database/captures/advertising-20261108T101322-236314Z.jsonl`. Each contained exactly one property snapshot, recorded before discovery started, and no subsequent property events. The ServiceData, ManufacturerData and UUID properties matched, but these snapshots may be cached; the comparison is inconclusive and does not establish whether new advertising occurred or changed. Raw `btmon` capture failed with insufficient monitor-channel permissions; noninteractive sudo requires a password. No scale connection or write was performed. A raw HCI capture with administrator assistance and active discovery would be a useful next test; store it privately and avoid publishing other devices' traffic.

## Raw advertising capture after measurement: 2026-11-08

With the owner running `btmon` as administrator, an active 60-second discovery scan captured live HCI advertising events from the known scale after the earlier measurement. The private raw file is `database/captures/advertising-raw-test.btsnoop`; it may include traffic from other nearby devices. The scale address appeared in 379 HCI LE events, including packets with the scale name, so this is stronger evidence of live advertising than cached BlueZ properties. Two reported packet lengths were observed (30 and 14 bytes). This is a post-measurement capture only: no comparable raw pre-measurement baseline exists, and pending-data meaning remains unverified. No GATT connection or device write was made during this scan. Further analysis should compare raw advertisement structures across controlled states without exposing addresses or private payloads.

## Raw before/after advertising comparison: 2026-11-08

Owner ran a privileged monitor across two 60-second active scans, with a normal scale measurement between them and no app connection. HR was unavailable in that measurement. Private capture: `database/captures/advertising-raw-comparison.btsnoop` (may include nearby devices). The first scan contained 203 scale advertisement and 203 scan-response payloads; the second contained 260 of each. All baseline payload variants reappeared after the measurement. One additional 30-byte advertisement variant appeared 45 times after the measurement, differing from the baseline at zero-based payload offset 23, inside manufacturer-specific AD type FF; the remaining 215 advertisements matched baseline. The 14-byte scan-response payload did not change. This is an observed correlation, not proof that the changed byte is a pending-data counter or caused by the measurement: existing readings could already be pending, transient state and other explanations remain possible. Repeat with controlled state and timing before interpreting it. No GATT connection or scale write occurred.

## Disposable user creation, measurement and deletion: 2026-11-08

With explicit owner coordination, created disposable user 2 with synthetic profile in separate ignored state. The existing user 1 remained intact. Registration assigned index 2; consent, selected-index readback and synthetic profile readback succeeded. A measurement was captured under user 2; the owner saw user 2 on the scale and the 2A9D packet carried index 2. Private capture: `database/captures/setup-20261108T151011-967824Z.jsonl`.

After separately verifying consent for user 1 and then user 2 and checking the test profile, UDS 2A9F opcode 03 received a success indication and selected index read back FF. User 1 was reauthorized. On a fresh connection, the deleted user 2 credentials were rejected with unauthorized, and user 1 was authorized and selected again. This supports deletion for this BF990/firmware and selected disposable profile; it does not prove full slot enumeration, slot reuse, deletion of all stored measurements, or behavior on other firmware. Private deletion capture: `database/captures/delete-test-20261108T151332-405464Z.jsonl`. No actual profile, measurement or consent bytes are in public notes.

Test state remains in ignored `database/test-user-2-state.json` with phase deletion_acknowledged. Preserve it and the original user state. The scripts `setup-test-user.sh`, `delete-test-user.sh` and `verify-deleted-test-user.sh` are for the specific coordinated experiment; do not replay or create another user automatically. Index selection and uncertain registration/deletion outcomes are guarded. The full suite passed 45 tests. User-list enumeration remains unknown; verification of both users was by separate consent/readback, not a list.

## Battery display requirement

The user requests a scale battery percentage in the Android app. A live standard Battery Service 180F/2A19 read previously succeeded; the Python connection probe validates it. Display a fresh percentage when connected and indicate unavailable/stale data accurately. Android support remains to be implemented.

## Measurement freshness and user discovery decisions

The owner wants the app to discard incoming scale readings older than five minutes, including queued readings, because someone else may have used the scale under the selected user. Determine age from a trustworthy measurement timestamp and local clock; do not import a reading with an invalid, uncertain or implausibly future timestamp as a current measurement. The app must preserve raw bytes alongside normalized values for measurements it does accept. Existing private research captures remain untouched.

The owner proposes reading the scale clock and, if it differs from the phone's local clock by more than roughly one minute, setting it to local time. BF990 clock characteristic, encoding, timezone, write behavior and battery-change reset are hypotheses to verify before implementing synchronization. Assess timestamps against the clock state at receipt, not by silently re-dating old readings after a clock correction.

The owner suspects user discovery may involve probing indices up to 12 or 16, and suggests initials could identify users. No index-probing command, maximum, or writable initials capability is established for BF990. The BF600 list describes a three-byte initials field but marks it unsupported (FF bytes); do not treat it as a working BF990 identifier or substitute it for the saved consent code. Investigate safely without guessing codes or equating inaccessible indices with empty slots. Local names remain an available fallback.

The owner reports that HR has succeeded only rarely and considers it unimportant. Do not prioritize HR protocol research; leave unverified HR unavailable. Older captures and historical transfer behavior remain useful for protocol research but app import follows the five-minute rule above.

## BF990 clock read: 2026-11-08

A fresh connection verified BF990 identity and read standard Current Time 1805/2A2B, whose observed properties are read/write/notify. The ten-byte value decoded as a valid local date/time approximately 223 seconds behind the host local clock at that instant. It exceeded the owner's proposed one-minute correction threshold, so the five-minute freshness rule cannot safely rely on an uncorrected scale clock. Private raw report: `database/captures/clock-20261108T153931-291101Z.json`. The probe `scripts/check-clock.sh` only reads. No clock write was performed. One read does not establish ticking, timezone/DST behavior, persistence, or the effect of a time write; verify those separately before automatic correction.

## BF990 clock correction verified: 2026-11-08

With the owner's instruction, wrote host local time once to BF990 1805/2A2B using the standard ten-byte Current Time layout and an acknowledged write. The prior value and write intent were saved before the operation. Immediate fresh readback differed by about one second from host local time. After disconnecting and reconnecting, another live read was within two seconds and had advanced, establishing persistence and clock progression over that short interval. Private reports: `database/captures/clock-set-20261108T154051-974328Z.jsonl` and `database/captures/clock-20261108T154105-871166Z.json`. No user profile was changed. Timezone/DST behavior, long-term drift, battery replacement, and effect on already queued measurements remain unverified; do not assume time-setting retimestamps old records. `scripts/test-clock-set.sh` performs a real clock write and must only be used when explicitly intended; `scripts/check-clock.sh` is read-only.

## Clock correction and measurement ordering

On connection, read the scale clock before correcting it. Compare received measurement timestamps with the scale's pre-correction clock, accounting for elapsed time, and discard readings older than five minutes. Do not reinterpret queued timestamps against the corrected clock or silently treat invalid/uncertain/future timestamps as current. Once the relevant incoming data has been assessed, correct the scale to phone-local time if the difference exceeds about one minute. If setup or synchronization makes the active measurement window expire or no measurement arrives, report the timeout and offer another reading. The owner considers rare timezone-change retries acceptable. This is the requested app behavior; Android implementation and the interaction between transfer and clock writes still require validation. The earlier drift may date from factory setup but its cause is not established.

## Queued-versus-new measurement timing: 2026-11-08

Owner recorded a measurement while disconnected, then woke the scale for the existing-user capture and later made a fresh measurement. One trial produced 14 earlier weight frames, then one fresh weight frame. The explicit FFFF/FFF4 start was sent 1.80 s after consent success; the first earlier frame arrived almost immediately after that command (1.80 s after consent). Earlier frames continued until 29.16 s after consent, roughly 1.4–2.3 s apart. The owner confirmed being late for the fresh measurement; its weight frame arrived 97.97 s after consent, with a measurement timestamp about 8 s before receipt. FFF4 status 01 followed at 100.04 s. The first two earlier frames were about 28 and 128 seconds old on receipt, and the third about 350 seconds old, using local timestamp interpretation. Private full capture: `database/captures/setup-20261108T160312-336499Z.jsonl`.

This single run demonstrates that queued frames may continue for tens of seconds, not just milliseconds. No separate end-of-queue marker was identified; the status followed the fresh measurement. The explicit start command was sent before the first observed frame, so this trial cannot distinguish authorization-triggered from start-triggered history transfer. Do not classify by arrival delay alone or use a short silence as conclusive queue completion. Apply freshness based on the pre-correction scale clock and measurement timestamps; preserve timely fresh readings even while old records are transferring. Find a reliable transfer boundary or design clock correction/measurement retry around this uncertainty before implementing the requested ordering.

## Android current-reading implementation: 2026-11-08

The Android debug app now contains a foreground BLE client, runtime permission requests, BF990 identity and battery reads, pre-correction clock read, explicit existing-user index/consent authorization, measurement indications, explicit FFFF/FFF4 start, timestamp filtering against the advancing scale clock, bounded packet decoding, and a current-reading screen with explanatory operation/GATT errors and a repeat action. Accepted latest weight and body packet bytes are stored privately per user in app preferences and restored on startup. A later-arriving older reading cannot replace a more recent displayed reading. Emulator-only synthetic measurements exercise the same packet decoder and show a synthetic battery level; this is an app-level simulator, not Android Bluetooth radio emulation.

The debug APK built and the emulator displayed the synthetic weight, BMI, fat, water, muscle and battery. A separate offline synthetic Java decoder check passed. The real Android BLE session has NOT been tested against the physical scale. Existing user setup currently requires manually entering the saved index and consent code; no Android registration, scale user listing, date-of-birth verification or credential transfer exists. The code is stored in Android app-private preferences, not a backup. Scale clock correction and queue-completion detection are not implemented; no automatic time write occurs. Session handling and hardware interactions remain provisional. RRD retention, diagrams, backup/restore and profile management remain unimplemented. The simulator displays in-memory test data and does not represent a real device or validate GATT behavior. Do not claim the broader app is complete.

## Android diagrams and retention checkpoint

Android now stores accepted readings per scale user and local day in SQLite; a later reading replaces that day. Daily records older than 28 calendar days are condensed to weighted weekly summaries, split at month boundaries, and weekly groups older than six months are condensed to monthly summaries. Valid values are averaged using per-metric counts. Raw weight/body packet bytes are retained in a separate per-user/day archive; this archive grows over time and must be included in future backup/export design. The diagrams page selects metric and 28-day, six-month or all-time range. Emulator-only sample history is segregated under reserved synthetic index 255 and labelled; sample reset affects only this synthetic index.

Debug APK builds and was installed on the emulator. Synthetic history generated 80 observations with 10 retained daily, 53 weekly samples, 17 monthly samples and 80 raw entries; repeating generation produced the same counts. User confirmed charts are visible. Phone BLE, actual readings, clock correction, user setup, backup and long-term retention transitions remain to be tested; the existing-user screen currently requires index and consent code. This is a debug APK, not a signed release. Emulator sample creation is synchronous and may briefly block the UI.

## Android page navigation visibility fix

The owner reported top page selection was not visible on a phone. The app now uses a no-action-bar theme and applies system-bar/display-cutout insets to the root layout. The debug build passed; emulator inspection showed the tabs below the top system inset and the Settings tab opened successfully. Phone verification is pending. The rebuilt debug APK is `android/app/build/outputs/apk/debug/app-debug.apk`.

## Phone and diagram layout feedback

The owner installed the debug app on a phone, authorized the existing scale user with the saved consent code, and reported that connection and diagram values work. The current daily database key is user plus local day and later accepted readings replace that day; real same-day database contents were not separately inspected. The diagrams page now presents all five metrics vertically on one scrollable page with 28 days, six months and all time buttons. Debug build and emulator six-month button selection passed; phone verification of this updated layout is pending. User management, clock correction and backup remain incomplete.

## Next-session requirements: language and password wording

The owner requests internationalization support with a language selector in Settings. No language set or fallback policy was specified; choose sensible defaults or clarify if needed. Move user-facing text to Android string resources and ensure the selection is applied consistently throughout the app. In the UI, replace the technical label “consent code” with approachable wording such as “Password (4 digits)”, including related help and error text. This refers to the existing scale user consent code; do not change the underlying protocol, credentials, or permit an error message to leak the password. These are documented requirements only and have not been implemented.

The owner tested the prior debug APK on a phone: authorization with the saved code worked, and diagram values looked correct. The subsequent APK shows all five charts vertically with three range buttons; it built and was tested on the emulator, but has not yet been confirmed on the phone. Same-day readings use one database row per user/day, so the last accepted reading replaces the earlier one. The owner plans to resume tomorrow. User onboarding, backup/restore, clock correction and other tasks remain open; see [TODOS](TODOS.md).

## Closing checkpoint: ready to resume

Saved on 2026-11-08 at the user's request to close the chat. No Beurer app/capture process is running and no setup lock remains. There is no background service to keep alive.

- Working: local Bluetooth connection, one persisted user, consent, profile readback, explicit measurement start, standard/private capture, and constrained offline decoding/comparison.
- Verified against the filled worksheet: weight, BMI, body fat, body water and muscle within display rounding. Both current packet candidates match both entries, so individual cycle numbering is ambiguous.
- Heart rate failed in the recorded cycles but **has worked previously**, per the user. Its private packet field is unidentified; keep it unavailable/null rather than zero. Do not interpret this as absent capability.
- Latest private capture: `database/captures/setup-20261107T153953-843965Z.jsonl` (6 weight/body pairs, 18 private payloads, 4 stored + 2 current by assumed local timestamps).
- Private comparison results: `database/analysis/observations-20261108.json` and `.md`. Preserve original `database/observations.md` and `database/profile-state.json`.
- Last implementation validation: **33 tests passed**. All four launchers have valid Bash syntax. This closing pass checks documentation/files, not another hardware session.
- Next useful evidence: a known successful displayed HR plus exact cycle time, or another known private metric. Coordinate any measurement window before starting it. Use a new local worksheet; do not overwrite the existing observations.
- Do not recreate the existing user, guess private field meanings, upload health data, or reintroduce unrelated-project references.

Offline inspection can resume immediately:

```bash
bash scripts/decode.sh database/captures/setup-20261107T153953-843965Z.jsonl --observations database/observations.md
```

This creates a new unique report by default. For a future coordinated capture, reuse `bash scripts/setup-profile.sh --start-measurement --listen-seconds 180 --timeout 35`; never delete profile state to retry.

## Current outcome

BF990 firmware V02.13 has been connected, configured with one local user, authorized, and made to send actual standard and private measurement packets over Bluetooth. Wi-Fi/cloud is unnecessary for this verified path. The observed protocol is summarized in [BF990-protocol.md](../docs/BF990-protocol.md).

The user supplied values in `database/observations.md`; the offline comparison now verifies five displayed metrics. Full private JSON/Markdown results are in `database/analysis/observations-20261108.*`. Next is identifying private fields with known successful observations, especially HR. No measurement capture is running. Do not start another timed window without coordinating with the user.

## User requirements and observations

- Keep all health data, profiles, credentials, and captures local. Wi-Fi setup failed; do not require cloud sharing.
- The official app never completed user-profile creation. Desktop Bluetooth pairing was confirmed by the user.
- Initially the scale showed weight, a person icon, BT indicator, then display-off. The manual's unknown-user icon was a possible match, not visually confirmed.
- The user supplied real profile details locally. JSON quotation marks were normalized with a local backup; details were never printed.
- The display changed after the explicit start command; actual measurement packets then arrived.
- Documentation must stand on its own, with no references to other application projects. Retain manufacturer/specification/dependency API references where useful.

## Commands

From the repository root:

```bash
bash scripts/beu-connect.sh --name BF990
bash scripts/capture.sh --seconds 60
bash scripts/decode.sh database/captures/setup-20261107T153953-843965Z.jsonl --observations database/observations.md
bash scripts/setup-profile.sh --start-measurement --listen-seconds 180 --timeout 35
env PYTHONPATH=src /usr/bin/python3 -B -m unittest discover -s tests -v
```

The proof command reads identity/battery/features. `capture.sh` only subscribes and records Value changes. The setup command reuses an existing configured user, performs consent, subscribes, then sends 00 to FFF4 under FFFF when `--start-measurement` is explicit. It skips demographics/revision writes for phase `configured`. Wait for LISTENING before stepping on the scale; the duration excludes connection/setup. It disconnects only a connection it opened.

The latest test run passed **33 unit tests**; the added decoder launcher passes Bash syntax. Earlier three launcher syntax checks also passed. No new hardware session was needed for offline validation. Runtime requires `/usr/bin/python3` with dbus and gi, a powered hci0, and access to host system D-Bus. Restricted sandbox execution could not access host D-Bus; approved host execution worked.

## Private local files: preserve them

- `database/profile-input.json`: validated user demographics. Do not print or copy into documentation.
- `database/profile-state.json`: phase configured, assigned user index, generated consent code, device binding, profile fingerprint, update time. **Do not delete it to retry**; losing it risks duplicate users/loss of access.
- `database/profile-state.json.lock`: transient lock directory. A crash can leave it behind; confirm no setup process remains before recovery. Do not bypass it automatically.
- `database/profile-input.before-json-fix.txt`: private original-input backup.
- `database/observations.md`: filled original display observations; preserve them.
- `database/captures/`: append-flushed, uniquely named JSONL recordings. Entire database directory is ignored by Git.

Useful captures, relative to repository root:

| File | Result |
|---|---|
| database/captures/20261107T143304-362701Z.jsonl | Initial 60 s, 21 subscriptions, identity-property events only |
| database/captures/setup-20261107T144619-900180Z.jsonl | Registration/profile readback succeeded; passive post-consent minute, no measurements |
| database/captures/setup-20261107T144911-351829Z.jsonl | First successful explicit-trigger transfer: weight/body + FFF5/FFF7/FFF8 |
| database/captures/setup-20261107T145037-930307Z.jsonl | Consent timed out before listening; no duplicate user created |
| database/captures/setup-20261107T145114-569511Z.jsonl | Successful 180 s repeat, 14:51:21-14:54:21 UTC, clean exit |

## Verified protocol behavior

Device identity: Beurer / BF990 / V02.13; battery was 100% at initial read. Weight feature 9f000000, body feature cf100000. Services include standard 181D/181B/181C and private FFFF/FFEF/FFCF.

UDS registration 01 and consent 02 returned matching success indications. User Index matched the assigned slot after consent. Date of birth (4 bytes), sex (1 byte), height (2-byte integer cm), and database revision (4 bytes) wrote/read back successfully. Weight-packet height metadata in 0.001 m units matches the local profile. Activity level was deliberately unchanged. New registration intent is persisted before sending; an unknown response outcome blocks another creation request. Later runs use the saved index/code.

After consent/private subscriptions, FFFF/FFF4 <- 00 produced:

- Weight 2A9D: 15 bytes, flags 0E.
- Body composition 2A9C: 14 bytes, flags 0398, no multipacket flag.
- Private FFF5/FFF7/FFF8: 20/20/6 bytes per observed set.
- FFF4 response 01, consistent with measurement success.

The last 180 s run had three distinct weight/body pairs, three each of FFF5/FFF7/FFF8, and one FFF4 response. Two weight timestamps predated the window and one fell within it, assuming host-local scale time. Do not call these three new cycles. Calendar/year and flag-derived length checks passed. See [DATA_FORMAT.md](DATA_FORMAT.md).

## Remaining work and implementation limits

A constrained offline decoder/comparison now supports the observed 0E/0398 metric layouts. No graphical comparison screen or private-field decoder exists. Heart rate, segmental values, visceral fat and private fields remain unidentified. Five standard displayed metrics now agree with observations; retain the limited sample/ambiguous cycle-assignment caveat. Do not guess from plausible numeric ranges. Validate standard units against display observations; BF600 prose has ambiguities. Preserve raw packets and handle history/deduplication by timestamp/user/packet identity.

Capture has no automatic reconnect. All subscriptions may succeed while no values arrive. Connection-time/cached identity Value changes are not measurement notifications. Setup success/exit 0 alone does not imply measurements were captured. A consent timeout occurred once; a fresh retry with timeout 35 succeeded. Never re-register merely because consent timed out.

## Next-session sequence

1. Read MAIN, this handover, TODOS, DATA_FORMAT and the newest validation section below.
2. Inspect private comparison files only when needed; do not print personal values. Do not overwrite the filled worksheet.
3. Expand the offline decoder only with verified packet/layout evidence. Preserve raw data and missing-value semantics.
4. When the user is ready, coordinate a capture with a successfully displayed HR and exact time, plus other private metrics if available. Do not guess HR offsets from plausible byte values.
5. Update status/log without reintroducing unrelated project references or personal data. No capture is currently running.

## Latest observation capture: 2026-11-07 15:40 UTC

User requested another session and was notified when LISTENING began. Command: `bash scripts/setup-profile.sh --start-measurement --listen-seconds 180 --timeout 35`. Capture: `database/captures/setup-20261107T153953-843965Z.jsonl`. Listening began at 15:40:02 UTC and completed the full 180 s with clean exit 0. Existing profile was authorized and reused without demographics/revision rewrites.

Received six weight frames, six body frames, six each of FFF5/FFF7/FFF8 (18 private payloads), and one FFF4 status 01. All six weight frames are distinct. Assuming a local-time scale clock, two readings fall within this window and four precede it. Do not count these as six new cycles. Metadata-only summary: `database/captures/latest-observation-capture-summary.json`.

At the end of that capture turn, display observations had not yet been analyzed. The subsequent 2026-11-08 offline comparison below supersedes that pending status for five standard metrics. Private metric/HR mapping remains pending; no capture is running.

## Display comparison verified: 2026-11-08

The user supplied two worksheet observations for the 2026-11-07 15:40 UTC capture. Both observations agree with both current packet candidates for weight, BMI, body fat, body water and muscle percentage within 0.05 of the respective displayed units. Because both candidates match the supplied values and timing notes are incomplete, numbered cycle-to-packet assignment is not unique. Do not force a one-to-one mapping.

Verified factors on these samples: weight uint16 × 0.005 kg; BMI × 0.1; body-fat and muscle percentages × 0.1; water-mass uint16 × 0.005 kg, then water mass / paired weight × 100 to obtain percent. This corrects reliance on the BF600 prose's 0.01 kg claim for BF990 decoding. Broader device/packet variants are not established by two similar observations.

An offline decoder/comparison command now exists. It accepts only the observed metric weight flags 0E/15-byte layout and body flags 0398/14-byte layout, validates boundaries, preserves raw bytes, avoids orphan/duplicate body associations, retains private packets undecoded, and leaves HR null. All-zero body payloads remain unavailable rather than becoming valid zero-valued physiology. Other flags/layouts generate warnings; this is not a universal Bluetooth decoder.

Private results: `database/analysis/observations-20261108.json` and its readable `.md` sibling. Six weight records decoded with zero warnings; both worksheet entries agree on all five metrics, with zero unique cycle assignments. Console/public documentation contains no actual health values. Command:

```bash
bash scripts/decode.sh database/captures/setup-20261107T153953-843965Z.jsonl --observations database/observations.md
```

Default outputs are unique local files under ignored `database/analysis/`; explicit existing output paths are not overwritten. No Bluetooth session or device writes were performed for this analysis.

## Heart-rate clarification

The user reports HR was unavailable in these cycles but had worked previously. Record this as an intermittent unsuccessful reading, not absent device capability. The observed standard packet layout has no HR field; private metric mapping is not yet identified. A previous valid HR could still exist in undecoded private/history packets. No packet-level HR error code has been identified, and null is not zero.

The [official manual, English page 13](https://res.cloudinary.com/beurer/image/upload/v1793003486/stibo-live/10379_BF990_2026-10-15_04_IM1_BEU.pdf) describes no HR/HR-- as failed HR measurement and recommends barefoot contact, correct hand-electrode grip, and remaining still through completion. The exact cause here is unknown. Do not attribute the failure to Bluetooth or diagnose a hardware problem from these observations. A future known successful displayed HR, with timestamp, is needed to validate candidate private fields.

## Validation

33 unit tests and the decoder-launcher syntax check pass. Decoder tests use synthetic values only, covering factors, raw retention, unknown/invalid dates, unsupported flags/lengths, unavailable body/HR, frame-pairing boundaries, current/history classification, comparison ambiguity, decimal-comma parsing and compact number-plus-kg notation.

## Android project preparation

Git has been initialized. Linux launcher scripts are now under `scripts/`; the Python package remains under `src/`. `codex-resume.sh` resumes the current session. `android/` contains an application skeleton, not a functional measurement app. Android Studio, SDK and Gradle are installed on this host. Implement BLE and safe user lifecycle first, preserving the verified protocol constraints above, then storage, retention, charts, and share/restore. The 33 existing Python tests passed after launcher relocation. No hardware session was performed.

Android setup is now documented in [DEVELOPMENT.md](DEVELOPMENT.md), with a pinned bootstrap installer and Gradle wrapper. AGP 9.1.1/Gradle 9.3.1/API 36 built the debug skeleton successfully; hardware implementation remains. The local Codex resume script is ignored for publication; AI documentation is included.

Android Studio and SDK command-line tools installed on this host after a first `/tmp` space failure; bundled Java 25.0.3 runs. The user accepted the SDK license and installed platform API 36 revision 2, build tools 36.0.0 and platform tools 37.0.1. `bash scripts/build-android.sh` passed assembleDebug on the network-share checkout; the SDK XML version warning was nonfatal. APK: `android/app/build/outputs/apk/debug/app-debug.apk`. No phone or hardware test was performed.

An API 36 emulator was installed and launched on 2026-11-08. The debug skeleton was installed and its placeholder activity reached the foreground; the emulator remains running. KVM was available on the host, and automatic graphics fell back to software. AVD creation reported a missing devices.xml despite producing a usable AVD. See DEVELOPMENT.md for repeat commands. The activity does not yet implement measurements or other requested app features.

Android UI now has a navigation preview for current values, diagrams and settings. Scale connection, enumeration, registration, storage, repeat and backup are not implemented; the disabled controls do not perform device operations. Scale-side user enumeration and deletion are unverified and must not be guessed. Build and emulator validation should be checked after the latest UI change.

The navigation preview built successfully and was installed and launched on the API 36 emulator. No Android scale interaction or storage validation was performed.

## Household user requirements

Household members agree which scale index belongs to each person. The app should list the scale’s available user indices and offer Add user. On a new phone the user chooses an index and provides a phone-local display name; the app should verify the selected profile by date of birth. If the user is demonstrably absent, offer registration. Allow modifying user data through settings; scale-side deletion is deferred. Index enumeration, verifying an existing profile on a new phone, recovery/transfer of the consent code, and safe update behavior remain unverified. Do not equate an unknown or inaccessible slot with an empty slot, and preserve the pending-registration safeguard. These are requirements and open protocol questions, not implemented functionality.

## Other-model protocol candidates

The BF600 reference describes a user-list operation, deletion of the selected user, pending-data advertisements, clock setting and other operations. Their details and staged verification plan are in [BF990 protocol](../docs/BF990-protocol.md#other-model-commands-to-investigate-unverified-on-bf990). None of those additional behaviors is established on BF990; observed FFFF differs from the BF600 private service FFF0. Deletion is destructive and must only be investigated with a disposable user and explicit coordination. No new device operations were performed.
