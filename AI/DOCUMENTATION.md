# Documentation maintenance

This folder is the standalone durable knowledge base. It must support resumption without chat history or another application repository.

- MAIN: concise current goal/status and complete navigation.
- HANDOVER: latest verified checkpoint, exact repeat commands, private-file locations, limits and next actions.
- TODOS: concise active queue; completed requirements separated from future work.
- OBSERVATIONS: user procedure/checklist for matching displayed values to packets.
- ARCHITECTURE: component/runtime/storage boundaries.
- LOCAL_PROFILE: setup, consent and recovery behavior.
- CAPTURE: commands, experiments, reception evidence and capture limits.
- DEVICE_API: source facts versus BF990 hardware observations.
- DATA_FORMAT: observed packet structure and decoding uncertainty.
- BF990_RESEARCH: dated search limits and manufacturer/specification references.
- LOG: short dated milestones.

Update affected files together and replace stale current statements rather than appending contradictory checkpoints. Historical milestones belong in LOG/CAPTURE. Do not add references to unrelated application projects or third-party example projects. Retain relevant manufacturer, Bluetooth-standard, and runtime-dependency API references.

Keep source under src/, tests under tests/, conversions under docs/. Actual health observations, device addresses, consent codes, demographic values, and raw captures belong only under ignored database/. It is acceptable to document private file paths and packet structure without reproducing personal values. Templates and examples must be visibly blank/synthetic.

A successful command is not itself measurement proof. Separate connection, setup, payload reception, structural parsing, and numeric validation. Do not claim private field identities or universal model compatibility from plausible values.

Offline comparison now exists for the two observed layouts. The latest private results are `database/analysis/observations-20261108.json` and `.md`; public status reports only field agreement, scaling, tests and uncertainty. HR is intermittent per user observation; unknown private HR remains null, not zero.
