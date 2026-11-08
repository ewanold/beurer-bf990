# Next measurement: record what the display shows

## Purpose

Match a known display reading to its raw Bluetooth packets so numeric scaling and unknown private fields can be verified. The scale can send stored readings, so a list of numbers without measurement times is insufficient.

The [official BF990 manual](https://res.cloudinary.com/beurer/image/upload/v1793003486/stibo-live/10379_BF990_2026-10-15_04_IM1_BEU.pdf), English pages 11-13, describes sequential display of weight, user label, BMI, body fat, body water, muscle percentage, and heart rate. Record other values only if actually shown. A limited digit display can cycle through several values; do not assume a missing field is zero.

## Private worksheet

Use `database/observations.md` for actual values. It is inside ignored local storage. Leave public/project Markdown free of health values and credentials. If a field is not shown, write `not shown`; if missed, write `missed`; copy an error message literally. Do not infer a value from a different cycle or another data source.

## Session procedure

1. Prepare the worksheet and note the local date/time zone. Have the scale nearby. A local recording of the display can help transcribe fast-changing values; uploading it is unnecessary.
2. Coordinate a capture, using the already-configured user:

   ```bash
   bash scripts/setup-profile.sh --start-measurement --listen-seconds 180 --timeout 35
   ```

3. Wait for LISTENING, then perform one complete measurement. For body analysis, stand barefoot and use the hand electrodes as described in the manual. Note the user label and any errors. Keep still until the cycle completes.
4. Record approximate local start/end times and every displayed value with the exact unit and decimal digits. Keep cycle 1 and cycle 2 separate. Do not change profile/activity settings between comparison cycles.
5. Record the capture filename printed by the command. Mark which rows are observed values, not decoder output. Two complete cycles are useful if practical, but one well-recorded cycle is enough to begin.

## Values to record

| Field | Unit / note |
|---|---|
| Weight | kg, lb, or st; retain shown decimal precision |
| User label | e.g. U:1, unknown-user icon, or literal message |
| BMI | Unitless |
| Body fat | % |
| Body water | % |
| Muscle | Copy label and unit; distinguish % from kg |
| Heart rate | bpm; record error if no HR result |
| Other displayed values | Exact label/value/unit, e.g. energy or bone value only if shown |
| Cycle start/end | Local time with seconds if practical; note time zone |
| Conditions/errors | Hand unit used, weight-only cycle, missed display, error code |

Not all manufacturer-advertised values are necessarily displayed or present in standard BLE packets. Heart rate/private field mapping is still unknown. Use these observations to validate, not to assume support.

## Developer follow-up

Compare packet timestamps first, then decode standard fields with flag/length checks. Preserve raw bytes and missing-value semantics. Compare private packet candidates across distinct known readings; do not publish a field based solely on a plausible range. Document evidence and remaining uncertainty in DATA_FORMAT/HANDOVER.

## Current observations analyzed

The filled local worksheet was compared offline on 2026-11-08. All five observed metrics agree within display rounding; similar candidate values/incomplete times prevent unique cycle numbering. HR failed for these observations but the user reports successful HR previously. Keep the original worksheet intact; use a separate local worksheet for a future successful-HR sample and record its exact time. See [DATA_FORMAT.md](DATA_FORMAT.md) for validation scope.
