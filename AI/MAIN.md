# Beurer Connect

## Goal and requirements

Read BF990 weight/body-composition measurements directly over Bluetooth on Linux. Keep profiles, credentials, captures, and observed values local. Wi-Fi setup failed and cloud sharing is unwanted. Document verified BF990 behavior without assuming compatibility with other models.

## Current checkpoint: 2026-11-11

The owner reports three days of correct phone operation (measurements, user switching, settings). Backup/restore via a dated zip (share, save to file, restore/open with) is implemented and emulator-validated with synthetic data; see [HANDOVER.md](HANDOVER.md#closing-checkpoint-2026-11-11--backup-and-restore). Registration and clock correction remain open.

## Previous checkpoint: 2026-11-09

The owner reports that Android user readout and the latest profile update seem to work on the phone. Individual date/height persistence and recovery cases still need confirmation. The app has a current-reading screen, per-user retention and charts, theme/language settings, and optional readout of shared-code users 1–8. Registration, backup/restore and clock correction remain open. Publication considerations and the latest limits are in [HANDOVER.md](HANDOVER.md#closing-checkpoint-2026-11-09).

## Earlier checkpoint: 2026-11-08

**Connection, local user registration, consent, profile readback, and measurement reception are verified on BF990 firmware V02.13.** The latest three-minute observation capture received six distinct weight/body pairs, 18 private payloads, and status 01. Timestamps suggest four stored readings and two current readings. An offline decoder now validates weight, BMI, body fat, body water and muscle against the supplied display observations; private metrics/HR remain unidentified.

Both supplied worksheet entries match the two current readings within display rounding, but numbered cycle assignment is ambiguous. Private comparison reports are in `database/analysis/`. HR failed for these cycles but has worked before. Next, identify private metrics using known successful HR/other values. No capture is running. The complete closing checkpoint, exact private-file locations and next-session commands are in [HANDOVER.md](HANDOVER.md).

## Runtime

Native Python with system dbus/GLib and BlueZ. No packages were installed during implementation. The original connection probe is read-only; separate commands subscribe, register/authorize a local user, verify profile writes, and explicitly start measurements. There is no Wi-Fi/cloud upload implementation.

## Documentation map

- [HANDOVER.md](HANDOVER.md): complete resumable checkpoint, commands, files, limits.
- [TODOS.md](TODOS.md): current work queue.
- [OBSERVATIONS.md](OBSERVATIONS.md): next display-versus-packet verification session.
- [PROTOCOL_TEST_HANDOFF.md](PROTOCOL_TEST_HANDOFF.md): bounded offline test assignment and live-test safeguards for a separate session.
- [ARCHITECTURE.md](ARCHITECTURE.md): modules, runtime, storage.
- [LOCAL_PROFILE.md](LOCAL_PROFILE.md): registration, consent, recovery state.
- [CAPTURE.md](CAPTURE.md): capture workflow and experiment results.
- [DEVICE_API.md](DEVICE_API.md): BF990 evidence and protocol limits.
- [DATA_FORMAT.md](DATA_FORMAT.md): observed packet layouts and unknown fields.
- [BF990_RESEARCH.md](BF990_RESEARCH.md): public search limits and primary references.
- [LOG.md](LOG.md): dated milestones.
- [DOCUMENTATION.md](DOCUMENTATION.md): maintenance policy.
- [BF990 protocol observations](../docs/BF990-protocol.md): observed GATT flow, packet layouts and limits.
- [README](../README.md): command usage.

## Android preparation

The Android app has a provisional BLE current-reading path, private latest-reading storage and an emulator-only synthetic packet mode. It builds and its synthetic display was exercised; physical Android BLE and user onboarding still require validation. SQLite daily/weekly/monthly retention and basic charts are implemented with synthetic emulator checks; backup remains unimplemented; see [HANDOVER.md](HANDOVER.md).

## Household user model

Household members agree on scale user indices. The app should list the scale’s available indices and offer an Add user option. On each new phone, a person selects their index and enters a local display name; the app should verify the date of birth against the selected scale profile. If a user is demonstrably missing, offer registration. Settings should offer profile modification. Scale-side deletion is deferred. Enumeration, verification on a new phone and existing-user consent recovery still need protocol validation; an unknown or inaccessible index is not proof of an empty slot. See [HANDOVER.md](HANDOVER.md).

## Measurement import policy

The owner wants measurements older than five minutes discarded, because readings under a selected user may belong to someone else. Read the scale clock before correction; assess incoming timestamps against that clock, then correct drift beyond roughly one minute. If the measurement window expires during setup, offer another reading. Invalid or uncertain timestamps must not silently become current readings. HR research is low priority. See [HANDOVER.md](HANDOVER.md#measurement-freshness-and-user-discovery-decisions).

## Language and password terminology

The owner requests internationalization, a Settings language selector, and user-facing “Password (4 digits)” terminology for the existing scale consent code. English and German, system language fallback, and a Settings selector are implemented; see [HANDOVER.md](HANDOVER.md#android-design-and-localization-2026-11-09).
