# BF990 captured data formats

## Scope

Observed on BF990 firmware V02.13 after local UDS registration/consent and explicit FFFF/FFF4 byte 00 measurement start. Personal byte values remain in ignored `database/captures/setup-*.jsonl`, not in this document. Packet layouts and five numeric conversions below are now checked against actual BF990 captures and the two supplied display observations. The decoder is deliberately limited to those observed layouts; private fields and other numeric units remain unvalidated.

## Standard Weight Measurement: 2A9D

Observed flags **0E**, 15 bytes, little endian. Flags select timestamp, user index, and BMI/height; imperial-unit flag is clear.

| Offset | Length | Field |
|---|---|---|
| 0 | 1 | Flags |
| 1 | 2 | Weight raw |
| 3 | 7 | Bluetooth Date Time: year uint16, month/day/hour/minute/second uint8 |
| 10 | 1 | User index |
| 11 | 2 | BMI raw |
| 13 | 2 | Height raw |

The packet timestamp forms a valid calendar date and its year matches the host. Height encoded in standard 0.001 m units matches the saved local profile height, confirming the integer-centimetre UDS input storage is reflected correctly in this measurement metadata. Weight/BMI numeric factors now agree with the supplied observations; actual personal values are not printed here. New packets must be parsed by their own flags, not assumed to retain this fixed layout.

## Standard Body Composition Measurement: 2A9C

Observed flags **0398**, 14 bytes, little endian. The metric-unit flag is clear. BMR, muscle percentage, soft lean mass, body water mass, and impedance fields are selected. Timestamp, user index, and multipacket flags are clear in these packets.

| Offset | Length | Field |
|---|---|---|
| 0 | 2 | Flags |
| 2 | 2 | Body fat raw |
| 4 | 2 | BMR raw |
| 6 | 2 | Muscle percentage raw |
| 8 | 2 | Soft lean mass raw |
| 10 | 2 | Body water mass raw |
| 12 | 2 | Impedance raw |

This exact size matches the flag-derived field count. Body packets lack their own user/timestamp here, so associate them with the correct preceding weight packet using stream order and explicit session boundaries. Do not assume one body packet per notification in a future multipacket variant. Body percentage, energy, mass, and impedance units must come from verified standard/device behavior; the BF600 prose is inconsistent and insufficient as a universal decoder.

## Private FFFF characteristics

| Characteristic | Observed bytes | Current interpretation |
|---|---|---|
| FFF4 | 1 | Response 01 after explicit measurement start; consistent with success in related-model documentation |
| FFF5 | 20 | Unknown private payload; retain raw |
| FFF7 | 20 | Unknown private payload; retain raw |
| FFF8 | 6 | Unknown private payload; retain raw |

No segmental/heart-rate/visceral-fat mapping is claimed yet. Do not infer field identities solely from plausible ranges. Repeated packets may be historical transfers or retransmissions; compare timestamps and raw packet identity before equating packet counts with measurement cycles.

The filled private worksheet was analyzed offline; preserve it. Use a separate local worksheet for a future known successful HR/private-metric validation session; see [OBSERVATIONS.md](OBSERVATIONS.md).

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
