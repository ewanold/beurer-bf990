# Local user registration and recovery

## Verified state

A user profile was created directly on BF990 firmware V02.13 over Bluetooth, without Wi-Fi/cloud. UDS Register New User (01) and Consent (02) returned matching success indications. User Index matched the assigned slot. Date of birth, sex, height, and database revision were written and freshly read back. Activity level was not changed.

Profile height is a two-byte integer-centimetre UDS value on this verified device. The standard weight-packet height field in 0.001 m units matches that input. This does not independently validate all body-composition calculations.

## Files and command

- `database/profile-input.json`: actual date_of_birth (ISO), height_cm (whole number), sex (male/female).
- `database/profile-state.json`: generated consent code, assigned index, device binding, input fingerprint, phase, update time. Preserve it.
- `database/profile-state.json.lock`: excludes concurrent setup runs. Inspect processes before recovering a stale lock.
- `database/captures/setup-*.jsonl`: response/event logs and raw health payloads; local and ignored.

```bash
bash scripts/setup-profile.sh --start-measurement --listen-seconds 180 --timeout 35
```

An existing phase-configured user is authorized and reused; demographics/revision are not rewritten. The explicit start option subscribes to private FFFF indications and writes 00 to FFF4, which has produced actual BF990 standard/private measurements. Without the start option, the command only authorizes and listens after any required initial profile setup.

## Implemented registration sequence

1. Validate real inputs before connecting; verify the live BF990 model.
2. Resolve full characteristic UUIDs under their parent service; subscribe to UDS/measurement indications.
3. Before creating a user, require current User Index FF. This checks selection state, not the complete user-slot inventory.
4. Persist the locally generated code and registration-pending intent before transmitting one registration request.
5. Require a valid matching success indication with index; save that index immediately.
6. Send consent using the saved code/index; verify selected index.
7. For incomplete setup, read field widths, write validated profile fields, verify readback, update/read back latest database revision, and mark configured.
8. Optionally issue explicit measurement start; listen for the requested duration and clean up app-owned resources.

An unknown registration result blocks automatic new-user retries. Once an index is known, retry through consent rather than creation. Explicit registration rejection is also conservatively retained for inspection. Do not delete state to retry, invent demographics, guess codes, reset the scale, or overwrite/delete another user.

The [Bluetooth SIG UDS procedure description](https://www.bluetooth.com/wp-content/uploads/Files/Specification/HTML/BLP_v1.1.1/out/en/index-en.html) documents register/consent behavior. BF990 support above is established by actual device responses and readback, not solely by service presence.

## Validation and limits

33 unit tests passed in the latest run, including offline-decoder/comparison tests as well as input/payload validation, response matching, device/profile binding, persisted pending intent, timeout/duplicate prevention, configured-user reuse, height-width handling, revision wrap, and explicit-trigger subscription order/properties. Hardware registration/consent/readback/measurement reception succeeded. One consent timeout occurred; retry with timeout 35 succeeded without another registration.

No consent request payload or personal profile is printed. Private state is not encrypted by this app and must remain local. There is no upload implementation. Activity commands, arbitrary profile edits, full slot inventory, and decoded private metrics remain outside verified support.
