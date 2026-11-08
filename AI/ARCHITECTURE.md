# Architecture

`beu-connect.sh` -> `/usr/bin/python3 -m beu_connect` -> native system D-Bus -> BlueZ -> Bluetooth LE scale.

`src/beu_connect/app.py` contains a small BlueZ transport, deterministic device selection, proof evaluation, CLI, and report writing. `tests/test_app.py` uses a fake transport to verify that unavailable devices and failed reads never become false success. No browser, network service, database engine, or cloud account is required.

## Probe sequence

1. Select a powered adapter and acquire this client's LE discovery session.
2. Scan for a bounded duration, then release this discovery session. BlueZ's returned object list can include cached entries.
3. Select explicit address/name, or one unambiguous `BF` numeric scale name.
4. Connect if necessary; wait for Connected and ServicesResolved.
5. Inventory only services attached to the selected device and their characteristics.
6. Perform fresh reads of manufacturer, model, firmware, battery, and both feature characteristics. Record raw hex and individual failures.
7. Prove communication only after a successful read and final connected/services-resolved check. Verify both scale services separately.
8. Disconnect only if the app opened the connection. Save a timestamped local JSON report even on failure.

The original connection-proof command uses no GATT WriteValue or notification subscription. The separate capture/setup modules below add subscriptions and explicit user/profile/measurement commands. Calls have D-Bus timeouts; scan and service resolution waits are bounded. Repeated per-characteristic timeouts can make the total run longer than `--timeout`.

## Persistence and scope

Default report: project-relative `database/connection-latest.json`, replaced on each run. A custom `--output` preserves selected runs. JSON stores selected device address, identity, inventory, raw reads, outcomes, and errors; unrelated nearby device identities are not persisted. There are no measurements or consent codes in the initial app. `database/` is excluded from Git.

Primary API references: [BlueZ Device](https://github.com/bluez/bluez/blob/master/doc/org.bluez.Device.rst) and [BlueZ GATT Characteristic](https://github.com/bluez/bluez/blob/master/doc/org.bluez.GattCharacteristic.rst).

## Local capture extension

`capture.sh` launches `beu_connect.capture` using system dbus/GLib. It records full parent-service mapping, flags/descriptors and raw Value-change events to unique local JSONL files, subscribes to available notify/indicate characteristics, then releases subscriptions and app-owned connections. It sends no proprietary commands and has no upload implementation. The original proof command retains its read-only behavior. See [CAPTURE.md](CAPTURE.md).

## Recoverable local registration

`setup-profile.sh` launches `beu_connect.setup_profile`. It validates local input, verifies model, subscribes to UDS and standard measurement indications, persists registration intent/consent before creating one new user, saves the assigned index, performs consent, and verifies profile writes through fresh readback. Unknown registration outcomes prevent another creation attempt. Later runs reuse the saved user. State is device/input-bound and protected from overlapping setup processes by a lock directory. This module uses standard UDS WriteValue; the original probe/capture commands retain their prior behavior. See [LOCAL_PROFILE.md](LOCAL_PROFILE.md).

## Offline decoding

`decode.sh` launches `beu_connect.decode` and reads only local JSONL/optional worksheet inputs. It decodes the two observed standard metric layouts, pairs body frames with a unique preceding weight inside a session, classifies history/current assuming host-local scale time, and saves a unique local report. Actual values are never printed. Private/HR fields remain undecoded; unsupported layouts warn. Comparison preserves ambiguous cycle assignments. See [DATA_FORMAT.md](DATA_FORMAT.md).
