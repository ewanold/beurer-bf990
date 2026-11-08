# Device API and evidence

## BF990 observations, 2026-11-07

Fresh identity reads: Beurer, BF990, firmware V02.13; battery initially 100%. Weight feature 9f000000 differs from BF600's documented b7000000. Body feature cf100000 differs from cf310000. Private services FFFF/FFEF/FFCF differ from BF600's FFF0/FEBA. Shared short UUIDs alone do not prove shared behavior.

## Verified local setup and transfer

- Standard UDS registration 01 and consent 02 returned matching success indications; selected index matched assigned user.
- Birth date 4 bytes, sex 1 byte, height 2-byte integer cm, database revision 4 bytes were written/read back. Weight measurement height metadata in standard 0.001 m units matches the profile.
- After consent and subscriptions, FFFF/FFF4 <- 00 produced standard 2A9D and 2A9C data plus private FFF5/FFF7/FFF8; FFF4 returned 01.
- Weight frames: 15 bytes, flags 0E; body frames: 14 bytes, flags 0398. Observed sizes match those flags.
- The full 180 s window contained three distinct weight/body pairs, nine private payloads, and status 01. Timestamps suggest two stored and one current reading.

These observations establish local Bluetooth registration/configuration/measurement reception on this firmware. No Wi-Fi/cloud path was used. Activity was unchanged. Full slot inventory, private field identities and remaining numeric units/calculations are not verified. Five displayed standard metrics are now checked as described below.

## Protocol limits

Use BF990 observations and verified standard behavior; related-model layouts or units cannot establish BF990 behavior. See [BF990 protocol](../docs/BF990-protocol.md), [DATA_FORMAT.md](DATA_FORMAT.md), [CAPTURE.md](CAPTURE.md), [LOCAL_PROFILE.md](LOCAL_PROFILE.md), and [public research](BF990_RESEARCH.md).

## Numeric verification: 2026-11-08

Two supplied observations agree with current packet candidates for weight ×0.005 kg, BMI ×0.1, body fat/muscle ×0.1%, and water mass ×0.005 kg converted to percent using paired weight. The candidates are too similar for unique cycle numbering. HR was absent in these cycles but has worked before according to the user; no private HR offset or error status is identified. See [DATA_FORMAT.md](DATA_FORMAT.md).
