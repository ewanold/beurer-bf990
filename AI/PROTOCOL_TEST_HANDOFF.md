# BF990 protocol testing: separate-session handoff

## Assignment

Build a repeatable, evidence-based test foundation for the BF990 protocol candidates. Start with offline work and synthetic fixtures. The user intends to run this task in a separate Luna chat; the main Android application work remains a separate stream. The chats share one working tree: inspect `git status` and existing changes before editing, do not reset or overwrite another chat's work, and coordinate overlapping files with the user. Do not commit or publish unless asked.

Read [MAIN](MAIN.md), [HANDOVER](HANDOVER.md), [TODOS](TODOS.md), [capture inventory](CAPTURE.md), [local profile safety](LOCAL_PROFILE.md), and the [protocol candidate and verification plan](../docs/BF990-protocol.md#other-model-commands-to-investigate-unverified-on-bf990) first. Follow the project and shared wiki instructions. Current test command:

```bash
env PYTHONPATH=src /usr/bin/python3 -B -m unittest discover -s tests -v
```

## Facts and boundaries

- Actual BF990 firmware V02.13 has verified connection, one registered/authorized user, selected profile reads, and a measurement request; five standard measurements have limited display validation. The Android app is a UI preview, not a functioning scale client.
- BF600 describes a user list, deletion, pending-data advertising, time and other commands; their BF990 behavior is unverified. BF990 private service FFFF differs from BF600 FFF0. Observed BF990 FFFF/FFF2 has read/write-without-response but no notification property; do not assume it is the BF600 list endpoint. BF990 FFFF/FFF5 packets differ from the BF600 reference-weight read.
- Advertising, if it follows the BF600 candidate, identifies users with pending measurements, not all occupied slots. A timeout, missing event or inaccessible profile does not prove an index is empty. Do not infer history completeness or deletion from a status alone.
- The existing scale user, consent code, local profile state and recordings are valuable. Never reset the scale, delete or recreate an existing user, remove or alter `database/profile-state.json`, guess consent codes, or retry a registration with an uncertain result. Preserve raw packets. `database/` is ignored and holds private data: do not print, copy into fixtures, or commit addresses, codes, personal profiles, or measurements.
- No timed capture, interaction with the scale, or unverified device write is authorized by this handoff alone. Coordinate a live session with the owner. Clock writes require explicit agreement; deletion requires separate explicit agreement and a disposable test user, after verifying selection and safeguarding existing data. Never test deletion on the existing household profile. Stop on unexpected state or uncertain write outcome.

## First deliverable: offline test package

1. Inspect the existing Python capture and decoding interfaces and tests. Design a small, reusable test harness around existing code where appropriate; avoid changes to Android implementation and avoid live Bluetooth operations.
2. Add synthetic-only fixtures and meaningful automated tests for complete, absent, malformed and truncated BLE advertising structures, including service data with no pending indices, multiple indices, unrelated service data, duplicates, and unknown fields. Distinguish a BF600-format candidate parse from a verified BF990 interpretation. Keep original bytes available; do not treat absence as evidence of empty user slots.
3. Prepare tests or a clearly scoped test design for candidate user-list responses: partial/malformed messages, unknown codes, duplicates, completion versus no-user status, and missing terminator. Do not claim a BF990 parser or endpoint has been verified; clearly identify any BF600 layout used in a synthetic fixture. Implement only where a useful interface can be tested without inventing device behavior.
4. Provide a concise per-case checklist or template: firmware and software revision; safety class; full service/characteristic and observed properties; prerequisites and independently known expected state; request/response and timing recorded privately; pre/post observations; repeat and reconnect where applicable; PASS, FAIL or INCONCLUSIVE. Include abort conditions. Keep executable source in `src/`, tests in `tests/`, and local data in ignored `database/`.
5. Run the existing and new unit tests, report exact results and limitations, and update the applicable handover/TODO/log files. Do not promote a candidate to verified on the strength of synthetic tests. Present the resulting live-test plan to the owner for coordination.

## Subsequent coordinated live stages

Follow the ordered plan in the protocol guide: passive advertising and identity/inventory first; list-request investigation only after checking endpoint properties; then consent/profile access and history; clock last among non-destructive candidates; deletion only as a separate disposable-user experiment. One variable and one explicitly agreed operation at a time. Preserve complete raw evidence privately and compare against independently known state. Distinguish rejected, unsupported, timed-out and incomplete outcomes. Repeat when possible and verify persistence after reconnect. A successful response is not by itself proof of durable behavior.

## Review and reporting

A separate independent review should check test coverage, frame parsing, privacy, command safety and each proposed interpretation before any BF990 behavior is called verified. End the session with changed files, test results, what remains hypothetical, and the exact next coordinated action. Do not print secrets or measurements in that report.
