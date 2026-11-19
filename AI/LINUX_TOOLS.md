# Linux tools

Developer and research tools that run on a Linux PC. They were used to prove the BF990 connection, register the first scale user, and work out the measurement format before the Android app existed. Moved here from the README on 2026-11-19; the README now addresses app users.

## Linux connection proof

A small Linux command-line app that scans for a Beurer scale, establishes or reuses a Bluetooth GATT connection, and proves communication through fresh characteristic reads. Tested successfully with a **BF990** on 2026-11-07.

```bash
bash scripts/beu-connect.sh --name BF990
```

Wake the scale and keep it nearby. Confirm pairing in the desktop Bluetooth prompt if requested. Close any competing phone app. This app does not initiate pairing itself or guess PINs. With no selector the app selects a unique `BF` plus numeric model name; multiple scales require explicit selection.

```bash
bash scripts/beu-connect.sh --scan-only
bash scripts/beu-connect.sh --name BF990 --scan-seconds 5 --timeout 25
bash scripts/beu-connect.sh --address AA:BB:CC:DD:EE:FF --output database/proof.json
```

The report defaults to `database/connection-latest.json`, relative to the project, even when launched elsewhere. It contains UTC timestamps, selected device identity, services, characteristic UUIDs, raw read bytes, decoded identity/battery values, individual read errors, and verification flags. Each run replaces the default report; choose a separate `--output` to retain an earlier run. Reports contain the device address and are excluded from Git.

`CONNECTION PROVEN` requires a successful live read and a still-connected, services-resolved device. Full scale-profile verification additionally requires successful four-byte feature reads from both Weight Scale and Body Composition services. A cached discovery entry or paired status alone does not prove communication.

Exit status: 0 = both scale services verified (or scan-only completed); 1 = runtime, cleanup, or report error; 2 = partial/failed profile verification; 130 = interrupted. A successful scan-only run never claims a connection.

## Verified result

On 2026-11-07 the app read manufacturer **Beurer**, model **BF990**, firmware **V02.13**, battery **100%**, weight feature `9f 00 00 00`, body composition feature `cf 10 00 00`. All six reads succeeded. The app reused the user-confirmed connection and preserved it afterwards.

Successful standard GATT reads do not establish proprietary protocol compatibility. The connection-proof command performs no GATT writes. The separate local setup/capture commands below register a user and receive measurements; private metric decoding remains under development.

## Runtime and checks

Requires Linux, BlueZ, a powered Bluetooth adapter, and system Python with `dbus` (`python3-dbus` on Debian/Ubuntu). Those dependencies were already available on the test machine; no packages were installed. The launcher deliberately uses `/usr/bin/python3` so distro D-Bus bindings remain available. No Node runtime, pip environment, web server, or cloud account is needed.

```bash
env PYTHONPATH=src /usr/bin/python3 -B -m unittest discover -s tests -v
bash -n scripts/beu-connect.sh
```

Run as the logged-in desktop user with access to the host system D-Bus. In a Codex sandbox that hides host D-Bus, the probe must run with approved host access. An unavailable bus, powered-off adapter, sleeping scale, competing connection, or GATT authorization failure is reported as an error rather than a simulated success. BlueZ may return cached discovery entries: subsequent live reads are the verification.

The app releases its own discovery session and disconnects only connections it opens. Existing connections remain connected.

## Capture measurement events locally

Run `bash scripts/capture.sh --seconds 60`, wake the scale, and wait for `LISTENING` before weighing. Raw events and full GATT inventory are saved locally to a unique JSONL file under ignored `database/captures/`. This requires system `dbus` and `gi` Python bindings. The command subscribes to notifications/indications without sending proprietary commands or uploading data. See [capture status and limitations](CAPTURE.md). The first completed minute enabled 21 subscriptions but received only identity-property events, not measurements.

## Bluetooth-only local profile setup

`bash scripts/setup-profile.sh --listen-seconds 60` creates/configures one scale user from ignored `database/profile-input.json` and then listens for a reference measurement. The first BF990 run successfully registered/authorized a user and verified date of birth, sex, height, and database revision through readback. No Wi-Fi or cloud is involved. Keep `database/profile-state.json`: it stores the assigned index and consent code, and prevents duplicate registration after an interrupted request. See [local profile setup](LOCAL_PROFILE.md).

For an explicit measurement-start capture using the saved user: `bash scripts/setup-profile.sh --start-measurement --listen-seconds 180`. This option sends 00 to FFF4 under service FFFF after consent. On the verified BF990 it produced standard weight/body and private packets. The original capture command only subscribes and sends no measurement-start command. See [observed packet layouts](DATA_FORMAT.md).

## Next verification session

Record displayed weight, BMI, body fat, body water, muscle percentage, heart rate and any other actually shown values, with units and cycle times. Use the private `database/observations.md` worksheet. [OBSERVATIONS.md](OBSERVATIONS.md) gives the procedure; [HANDOVER.md](HANDOVER.md) has the current checkpoint. Five displayed standard metrics now agree with the offline decoder; private-field and HR mapping remain next. Connection and local setup already work.

## Decode and compare saved readings offline

```bash
bash scripts/decode.sh database/captures/setup-20261107T153953-843965Z.jsonl --observations database/observations.md
```

The decoder supports the observed metric 0E weight and 0398 body layouts. It saves decoded values, raw bytes and optional display comparisons under ignored `database/analysis/`; console output contains counts/agreement only. Both supplied observations agree for weight, BMI, body fat, water and muscle within display rounding. Similar values do not uniquely number the cycles. HR/private metrics remain undecoded. Unsupported packets warn rather than guessing. See [data format and validation](DATA_FORMAT.md).

## Command summary

From the repository root:

```bash
bash scripts/beu-connect.sh --name BF990
bash scripts/capture.sh --seconds 60
bash scripts/decode.sh database/captures/setup-20261107T153953-843965Z.jsonl --observations database/observations.md
bash scripts/setup-profile.sh --start-measurement --listen-seconds 180 --timeout 35
env PYTHONPATH=src /usr/bin/python3 -B -m unittest discover -s tests -v
```

The proof command reads identity/battery/features. `capture.sh` only subscribes and records Value changes. The setup command reuses an existing configured user, performs consent, subscribes, then sends 00 to FFF4 under FFFF when `--start-measurement` is explicit. It skips demographics/revision writes for phase `configured`. Wait for LISTENING before stepping on the scale; the duration excludes connection/setup. It disconnects only a connection it opened.

Runtime requires `/usr/bin/python3` with dbus and gi, a powered hci0, and access to the host system D-Bus. A restricted sandbox may hide host D-Bus; run with approved host access.
