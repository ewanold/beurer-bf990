# Beurer Connect

## Goal and requirements

Read BF990 weight/body-composition measurements directly over Bluetooth on Linux. Keep profiles, credentials, captures, and observed values local. Wi-Fi setup failed and cloud sharing is unwanted. Document verified BF990 behavior without assuming compatibility with other models.

## Current checkpoint: 2026-11-08

**Connection, local user registration, consent, profile readback, and measurement reception are verified on BF990 firmware V02.13.** The latest three-minute observation capture received six distinct weight/body pairs, 18 private payloads, and status 01. Timestamps suggest four stored readings and two current readings. An offline decoder now validates weight, BMI, body fat, body water and muscle against the supplied display observations; private metrics/HR remain unidentified.

Both supplied worksheet entries match the two current readings within display rounding, but numbered cycle assignment is ambiguous. Private comparison reports are in `database/analysis/`. HR failed for these cycles but has worked before. Next, identify private metrics using known successful HR/other values. No capture is running. The complete closing checkpoint, exact private-file locations and next-session commands are in [HANDOVER.md](HANDOVER.md).

## Runtime

Native Python with system dbus/GLib and BlueZ. No packages were installed during implementation. The original connection probe is read-only; separate commands subscribe, register/authorize a local user, verify profile writes, and explicitly start measurements. There is no Wi-Fi/cloud upload implementation.

## Documentation map

- [HANDOVER.md](HANDOVER.md): complete resumable checkpoint, commands, files, limits.
- [TODOS.md](TODOS.md): current work queue.
- [OBSERVATIONS.md](OBSERVATIONS.md): next display-versus-packet verification session.
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

A buildable Android skeleton is in `android/`. BLE reception, user management, storage, charts and backup/restore still need implementation and hardware validation; see [HANDOVER.md](HANDOVER.md).
