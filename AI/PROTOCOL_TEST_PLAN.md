# BF990 protocol candidate test plan

This is a private-evidence template for coordinated future tests. Synthetic parser results establish only software behavior, not BF990 behavior. Store raw recordings and personal details only under ignored `database/`.

## Per-case record

- Date/time and timezone; BF990 model and firmware; software revision/commit.
- Case and safety class: passive/read-only, non-destructive write, or destructive.
- Full service and characteristic UUIDs; fresh observed properties; connection proof from live reads.
- Prerequisites and independently known expected state (record sensitive details privately).
- Pre-test observations; exact request and complete response bytes with timing, stored privately.
- Post-test observations; repeat result; reconnect/persistence check where applicable.
- Outcome: PASS, FAIL, or INCONCLUSIVE, with rejected, unsupported, timed-out, malformed, and incomplete outcomes distinguished.
- Abort reason and whether any outcome is uncertain.

Abort before a write if endpoint, properties, selection, expected state, or safety prerequisites differ. Stop immediately on unexpected state, disconnect, malformed response, uncertain write outcome, or indication that an existing profile could be affected. Never use the household profile for deletion.

## Staged live plan

1. Passive advertising and fresh identity/inventory reads; compare complete raw advertisements before/after a known measurement, with a second known user only when available. Pending-data absence says nothing about occupied slots.
2. Investigate user-list request only if the actual candidate endpoint and notify/indicate/write properties support the proposed sequence. One coordinated request; compare with independently known occupied indices; repeat after reconnect. No response is inconclusive.
3. Existing-user consent/profile access and history only after inventory is understood; preserve current profile state and compare complete raw packets against independently known dates.
4. Clock only with explicit owner agreement and recorded pre-read/timezone; verify fresh read and persistence after reconnect.
5. Deletion last, only with separate explicit agreement and a disposable test profile, verified selection, and safeguarded pre/post inventory.

## Candidate user-list synthetic scope

The BF600 reference candidate uses an event byte `00`, a fixed entry body (index, three initials, little-endian birth year, month, day, one-byte height, gender, activity), completion `01`, and no-user status `02`. This layout is not verified on BF990; notably the recorded BF990 profile height is two bytes. Keep candidate fixtures synthetic and test partial/truncated entries, unknown event/status codes, duplicate indices, completion after entries, no-user status, and missing terminator as distinct outcomes. Do not turn incomplete or absent responses into an empty list.
