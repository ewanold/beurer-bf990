# Current work queue

## Current checkpoint

Five standard displayed metrics are validated by the constrained offline decoder; 33 tests pass. No capture is running. Resume from [HANDOVER.md](HANDOVER.md), including its closing checkpoint.

## Next: private metric and successful-HR validation

- [x] Coordinate/complete another announced 180 s observation capture: 6 weight/body pairs; timestamps suggest 4 stored and 2 current readings.
- [x] Read supplied display observations privately; HR absent this time but user reports prior successful HR. Timing remains incomplete.
- [x] Distinguish current/history readings and compare observations. Both entries match both current candidates; do not claim unique cycle assignment.
- [x] Implement constrained 0E/0398 offline decoding and validate five observed metrics within display rounding.
- [ ] Identify private FFF5/FFF7/FFF8 meanings, including possible heart-rate/segmental fields, using evidence rather than plausible ranges.
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

- [ ] Decide whether a graphical interface is wanted.
- [ ] Verify activity-level command before adding any activity writes.
- [ ] Investigate reconnect behavior only if needed for reliable repeated capture.

- [ ] Capture a known successful displayed HR with its time before validating any private HR field. Preserve failed HR as unavailable, not zero.
- [ ] Broaden decoder layouts/units only with evidence; keep BMR/soft-lean/impedance raw until verified.

## Android application

- [ ] Implement and validate Android BLE discovery, consent, subscriptions and restart against the actual BF990.
- [ ] Implement user selection, registration and deletion with safe recovery of uncertain registration outcomes.
- [ ] Preserve raw packets and verified normalized measurements; implement daily, weekly and monthly retention without mixing users.
- [ ] Add measurement display, charts, and user-controlled backup and restore.
- [x] Install Android SDK and Gradle; build the debug skeleton.
- [ ] Test the Android app and verify scale communication on hardware.
