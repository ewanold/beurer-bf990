# Handover: 2026-11-08

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
