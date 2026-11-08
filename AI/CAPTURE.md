# Local Bluetooth capture

## Current result

BF990 Bluetooth-only profile setup, consent, explicit measurement start, and actual standard/private data reception succeeded. An initial successful 180 s repeat ran 2026-11-07 14:51:21-14:54:21 UTC. The latest observation window began at 15:40:02 UTC and also completed cleanly; its six-record result and subsequent comparison are below.

It captured three distinct weight frames (2A9D, 15 bytes), three body frames (2A9C, 14 bytes), three each of private FFF5/FFF7/FFF8 (20/20/6 bytes), and one FFF4 status byte 01. Standard flag-derived lengths match. Under the scale-status convention, 01 is consistent with success. Assuming host-local scale time, two readings predate the window and one is current. Counts do not equal new cycles.

Raw captures are local JSONL under ignored `database/captures/`; exact useful filenames are in [HANDOVER.md](HANDOVER.md). No capture is currently running. Next coordinate display observations using [OBSERVATIONS.md](OBSERVATIONS.md).

## Commands and boundaries

```bash
bash scripts/capture.sh --seconds 60
bash scripts/setup-profile.sh --start-measurement --listen-seconds 180 --timeout 35
```

The first command records full GATT inventory and subscribes to all available notify/indicate characteristics without proprietary command writes. It is an active GATT client, not a passive radio/phone-traffic sniffer: StartNotify/StopNotify configure remote subscription state. It cannot observe a phone's command exchange.

The second command reuses the configured user, performs UDS consent, subscribes to standard and private FFFF data, then explicitly starts a selected-user measurement via FFFF/FFF4 <- 00. No demographics are rewritten for configured state. Preserve local profile-state/consent. All commands select the intended scale and release only their own subscriptions/connections. No network/cloud upload is implemented.

LISTENING marks the beginning of the requested duration; scan/connect/authorization/setup/cleanup occur outside it. An early disconnect ends capture; there is no automatic reconnect. A command can exit successfully without receiving any measurements. Unique files are never intentionally overwritten and each event is flushed immediately.

## Experiment history

1. Initial 60 s, 21 subscriptions: three identity Value-change events, no measurements. Identity changes can be connection-time/cached updates, not measurement notifications.
2. User reported weight/person/BT display and confirmed official profile creation had never completed because Wi-Fi failed. Exact unknown-user icon match was not visually confirmed.
3. Read-only User Index reconnect attempt timed out; it did not establish selection state.
4. Local UDS registration, consent and profile readback succeeded. Passive post-consent 60 s still received no measurements.
5. Explicit FFFF/FFF4 00 after consent produced standard weight/body and FFF5/FFF7/FFF8 data. User reported changed display.
6. First long-run attempt timed out awaiting consent before listening. State remained configured, no duplicate user created.
7. Retry with timeout 35 completed a full 180 s and received the packets summarized above.

## Inventory and decoding limits

Private services: FFFF, FFEF, FFCF. FFFF/FFF1 is write-without-response/notify, FFF2 read/write-without-response, FFF4 write-without-response/notify. Only FFF4 measurement-start behavior is hardware-verified; other command meanings must not be inferred from properties alone.

Heart rate and private field meanings remain unidentified. Structural validation is recorded in [DATA_FORMAT.md](DATA_FORMAT.md). Five standard metrics now match supplied display observations; remaining/private fields still need evidence. Keep raw packets and distinguish missing values, weight-only results, history, and retransmissions.

## Latest observation capture: 2026-11-07 15:40 UTC

User requested another session and was notified when LISTENING began. Command: `bash scripts/setup-profile.sh --start-measurement --listen-seconds 180 --timeout 35`. Capture: `database/captures/setup-20261107T153953-843965Z.jsonl`. Listening began at 15:40:02 UTC and completed the full 180 s with clean exit 0. Existing profile was authorized and reused without demographics/revision rewrites.

Received six weight frames, six body frames, six each of FFF5/FFF7/FFF8 (18 private payloads), and one FFF4 status 01. All six weight frames are distinct. Assuming a local-time scale clock, two readings fall within this window and four precede it. Do not count these as six new cycles. Metadata-only summary: `database/captures/latest-observation-capture-summary.json`.

At the end of this capture, the display notes had not yet been analyzed. The subsequent offline validation below resolves five standard metrics; private/HR mapping remains pending. No capture is running.

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
