# Architecture

## Goal of the current slice

The current release combines the validated Android multi-display boundary with a tested local-first persistence foundation. It intentionally contains no pretend chat or wiki behavior.

## Modules

- `app` owns Android entry points, manifest policy, application wiring, and the configurable application ID.
- `core` owns the reusable OLED-first Compose theme and will later contain common UI primitives.
- `domain` is Android-free. It owns display and game models, repository contracts, settings, credential contracts, and deterministic selection policy.
- `device` is the only module that knows about `DisplayManager`, `ActivityOptions`, package launching, and Android display flags.
- `data` owns Room entities and DAO code, DataStore settings, Keystore encryption, and repository implementations.
- `features` owns onboarding, the game library, display presentation state, and Compose screens; it depends on domain contracts rather than framework implementations.

Hilt connects device and data implementations to their domain contracts at the application boundary. UI code never enumerates displays, opens a database, handles encryption, or launches packages directly.

## Local data flow

1. Onboarding completion and active-game selection are written to a preferences DataStore.
2. Manual game profiles and their package/wiki relations are written transactionally to Room.
3. Foreign keys cascade related rows when a profile is deleted.
4. Game-library state combines repository flows with the saved active profile and exposes immutable UI state.
5. Provider credentials use AES/GCM values encrypted by a non-exportable Android Keystore key. Plaintext keys are not placed in Room or DataStore.

## AI chat flow

1. `ChatViewModel` resolves the manually selected active game and its latest local thread.
2. `GamePromptBuilder` scopes the request to that game, platform, progress, and spoiler policy.
3. Only the fourteen most recent local messages are included to limit cost and context growth.
4. `TextAiProvider` is a domain interface; `DeepSeekTextAiProvider` is the first implementation.
5. The provider streams SSE text deltas from DeepSeek V4 and supports coroutine cancellation.
6. Completed user and assistant messages are persisted in Room. Partial output is deliberately not saved after cancellation.
7. Provider errors are mapped to actionable messages without exposing response bodies, credentials, prompts, or stack traces.

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
- No prompts, provider credentials, screenshots, notes, or analytics are collected in this milestone.
- Android backup remains disabled, so local profiles, settings, and future encrypted values are not copied to cloud backup.

## Next architecture increment

Add provider-neutral chat and MediaWiki domain interfaces, then implement an explicitly configured OpenAI-compatible provider and sourced retrieval. Provider calls remain disabled until the user supplies credentials and submits a request.
