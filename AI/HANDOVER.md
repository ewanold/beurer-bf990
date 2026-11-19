# Handover

This page holds only the current checkpoint, the rules that are still in force, and the private files to preserve. The history of earlier checkpoints, experiments and decisions is in [LOG.md](LOG.md); the full former handover text is under [Detailed checkpoint history](LOG.md#detailed-checkpoint-history-moved-from-handovermd-on-2026-11-19).

## Current checkpoint: 2026-11-19

The Android app is in daily use on the owner's phone with the BF990 (firmware V02.13). Measurements are read and displayed correctly, and user switching and settings changes work (owner report, 2026-11-11). The owner committed the backup/restore and healthy-range work (`3186e7d`, `c6a1d5c`).

Changes on 2026-11-19, not yet committed:

- History charts on the Timeline page show a dashed least-squares trend line over the displayed points of the selected range (`Trend.java`, `HistoryChart.java`). The axis is widened so that both line endpoints are visible. In the "6 months" and "All" ranges weekly and monthly averages count as single points. Fewer than two distinct days give no line. The debug build passed and a numeric check of the fit passed. A visual check is still needed: the emulator was killed for lack of free RAM on optiplex-3060-3.
- `README.md` was rewritten for app users, with the owner's screenshots in `images/`. Linux tool documentation moved to [LINUX_TOOLS.md](LINUX_TOOLS.md). Earlier handover sections moved to [LOG.md](LOG.md).
- At the owner's request, name and birth date in `images/page-3a.jpg` are pixelated. Height and the measurement values in the screenshots stay visible by the owner's choice.

- Selected tabs and time-range buttons are filled with the accent blue, and the others are outlined. The title, Settings headings and the Connect and Save user buttons use the accent. Status messages are green (ready or done) or amber (searching or connecting). The owner reviewed this on the phone and finds it better. Follow-up: the Connect / repeat button is at the top of the Current page until today's measurement arrives or while an error is shown, and all Settings action buttons use the accent. The follow-up build passed and is not yet viewed on a device.

## Next steps

1. Check the trend line and the new colors on the phone. Decide on a slope label (e.g. kg per week) and on optional per-metric colors.
2. Transfer a real backup to the second phone and click through Restore from file.
3. Check the shared signing directory access controls and keep a backup of `database/signing/debug.keystore`.
4. Remaining features: user registration on an empty scale, clock correction, battery display, error/reconnect paths. See [TODOS.md](TODOS.md).

## Rules in force

- **Fresh readings only:** the app ignores scale readings older than five minutes, judged against the scale clock read before any correction. Imports from a backup are exempt. Background: [owner decisions](LOG.md#measurement-freshness-and-user-discovery-decisions), [clock ordering](LOG.md#clock-correction-and-measurement-ordering), [queued transfer timing](LOG.md#queued-versus-new-measurement-timing-2026-11-08).
- **One reading per user and day:** a later reading replaces the earlier one. Daily values older than 28 days become weekly averages, older than six months monthly averages. Raw packets are kept forever in `raw_archive` and are the source for backups.
- **Shared password:** at the owner's request the app uses one fixed code for all scale users. Source and APK therefore contain the user 1 credential; publishing either exposes it. Never copy the value into documentation. See [shared-code change](LOG.md#shared-code-android-change-2026-11-09).
- **No guessing:** never guess consent codes and never treat an inaccessible index as an empty slot. Scale-side user enumeration is not verified. See [user discovery](LOG.md#user-discovery-and-initials-probe-2026-11-09).
- **Profile writes:** birth date and height changes are guarded by a previously verified baseline, readback and a recovery marker. Validate failure and interruption cases only on disposable user 2; user 1 must be preserved. See [controlled updates](LOG.md#controlled-android-birth-date-and-height-updates-2026-11-09).
- **Experiment scripts** (`setup-test-user.sh`, `delete-test-user.sh`, `test-multi-user.sh`, `test-shared-user.sh`, `test-clock-set.sh` and similar) belong to specific coordinated experiments. Do not rerun them without checking the current scale state. `check-clock.sh` is read-only; `test-clock-set.sh` writes the clock.
- **Heart rate** is low priority; leave it unavailable (null, not zero). See [heart-rate clarification](LOG.md#heart-rate-clarification).
- **Icon:** the Flaticon launcher icon needs attribution, and its terms for redistribution in a public source repository must be checked before publication.

## Private local files: preserve them

All under ignored `database/`; never print their personal values or copy them into documentation.

- `profile-input.json`, `profile-state.json` (index and consent code of the Linux-registered user; do not delete to retry), `profile-state.json.lock` (transient; confirm no setup process before removing), `profile-input.before-json-fix.txt`.
- `multi-user-test-2-shared-state.json`: recovery state of the current disposable user 2. Older `test-user-2-state.json`, `multi-user-test-2-state.json` and `multi-user-test-3-state.json` record acknowledged deletions; keep them.
- `observations.md` (filled display worksheet), `analysis/observations-20261108.*` (comparison results), `captures/` (raw recordings; the list of notable captures is in [LOG.md](LOG.md#private-local-files-preserve-them)).
- `signing/debug.keystore`: the shared debug signing key. Every build PC must use it, otherwise the phone refuses the update. Not in Git.

## Where to find things

- App build, phone installation, emulator: [DEVELOPMENT.md](DEVELOPMENT.md).
- Linux connection, capture and decoder tools: [LINUX_TOOLS.md](LINUX_TOOLS.md).
- Protocol and packet layouts: [BF990 protocol](../docs/BF990-protocol.md), [DATA_FORMAT.md](DATA_FORMAT.md), [DEVICE_API.md](DEVICE_API.md).
- Status and documentation map: [MAIN.md](MAIN.md). Work queue: [TODOS.md](TODOS.md). History: [LOG.md](LOG.md).
- Resume a chat locally with `./claude-resume.sh` or `./codex-resume.sh` (ignored, hostname-mapped).
