# Architecture

## Goal of the current slice

The current release combines the validated Android multi-display boundary with local-first profiles, real DeepSeek chat, conservative source-backed retrieval, and an offline-capable game-wiki browser.

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
6. Saved answers, notes, checklists, and checklist items are stored under the active game in Room schema 4 and cascade when that game is deleted.
7. Saved-answer citations are encoded as structured JSON inside the saved record so the bookmark remains independent if chat history is cleared.
8. Retrieved wiki documents are stored per game in Room schema 5. Cached documents expire for network retrieval after seven days, remain visible offline, and cascade when their game is deleted.
9. Controller Quick Question favorites are stored per game in Room schema 6, exported in JSON backups, and cascade when their game is deleted.
10. Controller input is normalized in the device layer. A strictly limited optional Accessibility service observes only controller key events for the global long-press shortcut and cannot retrieve window content.
9. `PrivacyRepository` combines Room counts with encrypted-credential presence and owns selective deletion. Full reset clears Room, encrypted preferences and their Android Keystore key, then DataStore so onboarding restarts.
10. `BackupRepository` uses Android's user-selected document URIs. Versioned JSON contains profiles, relations, conversations, citations, and personal tools, but never credentials or cached article text. Import applies size, type, enum, URL, and foreign-key reference validation before one Room transaction merges records.
11. Profile persistence uses Room `@Upsert`, avoiding SQLite `REPLACE` parent deletion and its unintended foreign-key cascades during edits or imports.

## AI chat flow

1. `ChatViewModel` resolves the manually selected active game and its latest local thread.
2. `GamePromptBuilder` scopes the request to that game, platform, progress, and spoiler policy.
3. Only the fourteen most recent local messages are included to limit cost and context growth.
4. `TextAiProvider` is a domain interface; `DeepSeekTextAiProvider` is the first implementation.
5. The provider streams SSE text deltas from DeepSeek V4 and supports coroutine cancellation.
6. Completed user and assistant messages are persisted in Room. Partial output is deliberately not saved after cancellation.
7. Provider errors are mapped to actionable messages without exposing response bodies, credentials, prompts, or stack traces.
8. Chat sessions are listed per game and can be selected, renamed, or individually deleted. Starting a new conversation remains transient until the first question, avoiding empty database records.
9. Context is hard-limited to fourteen recent messages. The user chooses a 512, 900, or 1500 maximum-output-token preset; no guessed currency estimate is shown because billing belongs to the provider account.

## Knowledge retrieval flow

1. `GameKnowledgeProvider` separates search and document retrieval from the chat and AI vendors.
2. `MediaWikiGameKnowledgeProvider` uses a profile's configured HTTPS wiki or probes conventional wiki.gg/Fandom hosts derived from the active game title.
3. It searches the selected game wiki and supports both the optional TextExtracts module and a sanitized rendered-page fallback used by wiki.gg.
4. `CachingGameKnowledgeProvider` serves fresh matching documents locally and writes successful network retrievals to Room before the shared `KnowledgeRetriever` ranks them.
5. `KnowledgeRetriever` fetches candidate documents concurrently, rejects documents without question-specific term overlap, ranks the remainder, and keeps only results close to the best score. Evidence limits become stricter as spoiler protection increases.
6. Numbered evidence is added to the system prompt. The model may cite only those numbers and must disclose when it falls back to general knowledge.
7. The exact source metadata and short excerpt used for an answer are stored with that assistant message in Room schema 3.
8. Source buttons remain available in local chat history and open the original HTTPS page.
9. The Wiki destination exposes search, connectivity state, cached pages, cache clearing, and spoiler-aware previews through the same provider contracts used by chat.
10. Retrieval failures never block chat; they produce an explicitly unsourced model-knowledge request instead of fabricated citations.

## Display flow

1. `AndroidDisplayRepository` observes `DisplayManager` and emits immutable capability snapshots.
2. `DisplaySelectionPolicy` considers only non-primary displays that Android reports can host activities. A saved signature will take precedence in a later persistence phase; otherwise the smallest eligible display is recommended.
3. The primary dashboard shows every display and its relevant diagnostics.
4. `SecondaryDisplayLauncher` starts `CompanionActivity` in a separate task using `ActivityOptions.launchDisplayId`; that activity hosts the complete Ask/Wiki/Saved/Games/More product UI and the same local state as the launcher activity.
5. If no secondary display is available, the complete companion activity opens on the current display.
6. A user-triggered mapped game launch explicitly targets `Display.DEFAULT_DISPLAY`.

The launcher checks its current display on every start. When launched on display 0 and a secondary display is available, `SecondaryDisplayLauncher` immediately opens the complete interface there and finishes the temporary primary activity. When GameSide already starts on a secondary display, it stays there. There is no persisted companion session or background restore loop.

Display IDs are treated as ephemeral. No AYN model name, display ID, or fixed resolution affects behavior.

## Security decisions

- No app component is exported except the launcher activity.
- Cloud backup is disabled; personal export happens only through the explicit Android document picker.
- No microphone, camera, screenshot, location, contacts, or storage permission is requested. The optional controller shortcut uses Android's user-enabled `BIND_ACCESSIBILITY_SERVICE` flow with key-filtering capability and no window-content capability.
- Game discovery is limited to launcher activities and explicit package names; `QUERY_ALL_PACKAGES` is not used.
- Cleartext networking is disabled. DeepSeek and game-wiki traffic uses HTTPS.
- No provider credentials, screenshots, notes, or analytics are collected. The submitted question and compact recent context leave the device only for the requested answer.
- Android backup remains disabled, so local profiles, settings, and encrypted values are not copied to cloud backup.
- Every destructive privacy action requires explicit confirmation; category deletion preserves unrelated data.

## Next architecture increment

Run the physical AYN Thor acceptance checklist, then consider post-MVP voice and user-triggered screenshot input.
