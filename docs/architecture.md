# Architecture

## Goal of the current slice

The current release proves the Android multi-display boundary before any AI, network, or persistence code is introduced. It intentionally contains no pretend chat or wiki behavior.

## Modules

- `app` owns Android entry points, manifest policy, application wiring, and the configurable application ID.
- `core` owns the reusable OLED-first Compose theme and will later contain common UI primitives.
- `domain` is Android-free. It owns display models, the repository contract, and deterministic selection policy.
- `device` is the only module that knows about `DisplayManager`, `ActivityOptions`, package launching, and Android display flags.
- `features` owns presentation state and Compose screens; it depends on domain contracts rather than framework implementations.

Hilt connects `DisplayRepository` to `AndroidDisplayRepository` at the application boundary. UI code never enumerates displays or launches packages directly.

## Display flow

1. `AndroidDisplayRepository` observes `DisplayManager` and emits immutable capability snapshots.
2. `DisplaySelectionPolicy` considers only non-primary displays that Android reports can host activities. A saved signature will take precedence in a later persistence phase; otherwise the smallest eligible display is recommended.
3. The primary dashboard shows every display and its relevant diagnostics.
4. `SecondaryDisplayLauncher` starts `CompanionActivity` in a separate task using `ActivityOptions.launchDisplayId`.
5. If no secondary display is available, the same companion activity opens on the current display.
6. A user-triggered mapped game launch explicitly targets `Display.DEFAULT_DISPLAY`.

Display IDs are treated as ephemeral. No AYN model name, display ID, or fixed resolution affects behavior.

## Security decisions

- No app component is exported except the launcher activity.
- Cloud backup is disabled; future personal export will be explicit.
- No sensitive Android permission is requested.
- Game discovery is limited to launcher activities and explicit package names; `QUERY_ALL_PACKAGES` is not used.
- Cleartext networking is disabled even though this milestone has no internet permission.
- No prompts, credentials, screenshots, notes, or analytics exist in this milestone.

## Next architecture increment

After the display flow is validated on a real device, add the `data` module with Room, DataStore, Keystore-backed credential storage, repository implementations, and manual game profiles. AI and MediaWiki providers remain behind domain interfaces and are added only after that local-first foundation is tested.
