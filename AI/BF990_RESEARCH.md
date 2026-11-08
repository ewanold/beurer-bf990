# BF990 public protocol research: 2026-11-07

## Search outcome and limits

Public searches for BF990 / BF 990 with Bluetooth, API, GATT, transmission protocol and reverse engineering did not locate a BF990-specific public transmission specification. This does not prove none exists. No authenticated developer account was accessed. The user reports registering with Beurer and finding only BF600 protocol material available.

## Primary references

- [Official BF990 product page](https://www.beurer.com/global/p/10379/): advertised measurements, app/connectivity information, consumer downloads.
- [Official BF990 instructions, revision 2026-10-15](https://res.cloudinary.com/beurer/image/upload/v1793003486/stibo-live/10379_BF990_2026-10-15_04_IM1_BEU.pdf): display legend, measurement procedure, user recognition, Bluetooth transfer, Wi-Fi default behavior. English pages 11-13 describe sequential displayed values and errors.
- [Beurer developer information](https://www.beurer.com/de/service/app-welt/) and [developer portal](https://connect.beurer.com/developer/en-us/): protocol downloads after registration; no public BF990 download was verified.
- [Bluetooth SIG UDS procedure description](https://www.bluetooth.com/wp-content/uploads/Files/Specification/HTML/BLP_v1.1.1/out/en/index-en.html): standard new-user registration, assigned index, consent code and subsequent authorization.
- [Android Bluetooth debugging](https://source.android.com/docs/core/connect/bluetooth/verifying_debugging): full HCI logging if phone-to-device command analysis becomes necessary. This has not been needed for the successful local path.
- [BF990 protocol observations](../docs/BF990-protocol.md): project observations and uncertainty.

## Current conclusions

Common standard services were a useful starting point, while private mappings and feature values differ. Actual BF990 responses now establish new-user registration, consent, profile readback, and measurement-start behavior. These are stronger evidence than service presence or a model-similarity assumption. See [DEVICE_API.md](DEVICE_API.md).

A local FFFF/FFF4 00 start after saved-user consent and subscriptions produced standard weight/body and private packets. The owner prefers local Bluetooth because Wi-Fi failed and data sharing is unwanted. A phone/cloud workflow is not required for the verified implementation.

Remaining research: compare display values to raw standard packets, then identify private metrics using repeated controlled observations. Do not infer all 25 advertised values are displayed or present in standard services. If phone captures are ever used, keep them private and ensure a real Bluetooth measurement transfer is captured rather than Wi-Fi-only synchronization.
