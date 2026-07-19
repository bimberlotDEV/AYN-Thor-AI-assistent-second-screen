# Device test results

## Huawei P30 Lite — 2026-07-18

Test environment:

- Model: HUAWEI MAR-LX1A (P30 Lite)
- Android: 9 / API 28
- EMUI build: 9.1.0.317
- Built-in display: 1080 × 2312, 480 dpi, 60 Hz, internal touch
- Initial display-test APK: debug `0.1.0-poc`; latest installed candidate: debug `1.0.0-mvp`; application ID `com.gameside.ai`

Validated behavior:

- ADB installation and cold launch completed successfully.
- GameSide detected the built-in display as display 0, primary, touch-capable, and activity-capable.
- The single-screen fallback opened `CompanionActivity` in its separate companion task.
- The companion layout rendered correctly at 1080 × 2312 and remained vertically scrollable.
- The touch proof incremented from 0 to 1 and reported the correct display.
- No `AndroidRuntime` crash was recorded during install, launch, fallback, touch, task switching, overlay creation, or overlay removal.
- Android's simulated secondary display was detected at 1240 × 1080 and 320 dpi as a non-touch presentation display.
- The simulated display was recommended by the selection policy and exposed as activity-capable.
- `MainActivity` remained on display 0 while `CompanionActivity` ran in its own stack and window on the simulated display.
- Removing the simulated display returned the system to one display without a crash; the primary dashboard resumed on display 0.
- The simulated display received different IDs across sessions, confirming that display IDs must be treated as ephemeral.
- The original `overlay_display_devices` setting was restored to `null` and temporary on-device test artifacts were removed.

Local-first slice validation:

- Fresh install showed the privacy onboarding and the action remained fully visible above Huawei's system navigation area.
- Completing onboarding created `gameside_settings.preferences_pb`; force-stop and cold restart opened the game library instead of repeating onboarding.
- Manual creation and edit produced an active `Elden Ring` profile with the selected platform and spoiler level.
- The profile remained present after force-stop and cold restart.
- The on-device Room database (`gameside.db`) and DataStore file were confirmed inside the app sandbox.
- The Room instrumentation test ran on Android 9 and verified transactional relation replacement plus foreign-key cascade deletion.
- The Keystore instrumentation test verified encrypted credential write, read, presence, and removal on the Huawei hardware.
- Full JVM tests, debug lint, debug APK assembly, installation, and launch passed after the system-inset UI fix.
- No app crash occurred during onboarding, profile creation/edit, persistence restart, or instrumentation execution.

Chat slice validation:

- Database schema 1 migrated to schema 2 on the existing Huawei installation without losing the `Elden Ring` profile or onboarding state.
- The Ask screen opened with `Elden Ring` as active game and rendered the spoiler-aware question field.
- The provider screen exposed masked credential entry, encrypted storage action, current DeepSeek V4 model selection, and connection testing.
- Three on-device instrumentation tests passed: game relations/cascade, chat ordering/cascade, and Keystore credential round-trip/removal.
- JVM prompt tests, debug assembly, and Android lint passed.
- The user entered the credential directly in the secure on-device field and confirmed a real streamed DeepSeek answer.

Knowledge/citation slice validation:

- The existing Huawei database migrated from schema 2 to schema 3 without losing the active game, prior chat, or Keystore-backed credential.
- A live question, `When was Elden Ring released?`, retrieved Wikipedia evidence, generated the correct answer, included an inline citation, and rendered persistent clickable source buttons.
- Retrieval falls back without citations when no question-specific match exists; weak results are filtered rather than shown as evidence.
- Unit tests cover evidence ranking and numbered prompt context. All JVM tests, debug lint, APK assembly, and three Huawei instrumentation tests passed.
- No `AndroidRuntime` or Room errors were recorded after the schema migration, install, launch, source retrieval, generation, or citation rendering.

Game-wiki provider validation:

- Automatic Elden Ring source discovery selected a live MediaWiki-powered game wiki without a stored game list.
- A live `How do I get Moonveil?` request retrieved the dedicated Moonveil article, generated an answer with inline `[1]` references, and displayed the persisted clickable `Moonveil` source.
- The rendered-page fallback was required and validated because the selected wiki.gg instance does not expose MediaWiki's optional TextExtracts module.
- Wikipedia is no longer used as the default or fallback knowledge provider.

Personal tools validation:

- Database schema 3 migrated to schema 4 on the existing Huawei installation without losing games, chat history, citations, or the encrypted provider credential.
- Four Huawei instrumentation tests now pass; the new test verifies offline saved-answer, note, checklist, item-toggle, and game-cascade behavior.
- A real Moonveil answer was bookmarked from chat and appeared under the new `Saved` navigation destination with its original question, answer, and stored source count.
- The Saved screen, note/checklist creation dialogs, delete actions, and offline checklist toggles render without AndroidRuntime or Room errors.

Offline game-wiki validation:

- Database schema 4 migrated to schema 5 on the existing Huawei installation without losing the active game, chat, personal tools, onboarding state, or Keystore-backed credential.
- Five Huawei instrumentation tests pass. The new test verifies wiki-document persistence, explicit cache clearing, and foreign-key cascade deletion.
- A live Wiki search against the automatically selected Elden Ring game wiki downloaded pages and exposed them under `Downloaded pages - available offline`.
- Force-stop and cold restart retained the downloaded pages, proving the Wiki destination renders persisted cache data rather than transient search state.
- The active `minimal` spoiler policy hid cached article previews and required an intentional source-open action.
- The Wiki destination reported current connectivity, exposed a clear-cache control, and rendered without `AndroidRuntime`, Room, SQLite, or migration errors.
- JVM tests (including strict spoiler evidence limits), debug lint, and debug APK assembly passed with Room schema 5.

Privacy controls validation:

- The Huawei Privacy screen correctly reported the existing local game, conversation, saved answer, Wiki cache, and encrypted DeepSeek-key status.
- Every selective deletion and the full reset are guarded by a destructive-action confirmation dialog. The conversation confirmation was opened and cancelled on-device, preserving the user's data.
- Six Huawei instrumentation tests pass. The new privacy test verifies per-category counts and confirms that chat, personal tools, and cache can be cleared independently without deleting the game profile.
- Full JVM tests, debug lint, debug APK assembly, install-over-existing-data, navigation, and dialog cancellation passed without `AndroidRuntime` or SQLite errors.
- The complete reset implementation clears all Room tables, encrypted credential preferences, the app's Android Keystore key, and DataStore preferences; it is intentionally not executed against the user's live test data.

Backup and restore validation:

- Android's system document picker exported a version-1 JSON backup from the live Huawei data containing one game, one conversation with citations, and one saved answer.
- Inspection confirmed `containsCredentials: false`, no API-key pattern, and no downloaded Wiki-cache collection in the file.
- Importing the same document reported one game, one conversation, and one personal item; the merge completed without duplicate, foreign-key, SQLite, or runtime errors.
- Seven Huawei instrumentation tests pass. The new backup test proves an imported profile update preserves an existing conversation instead of triggering a parent-row cascade.
- Import is bounded to 10 MB and 20,000 records per collection, accepts only user-selected `content://` documents, validates supported enums and HTTPS sources, and rejects broken references before writing.
- The temporary backup created in the phone's Downloads folder was removed after validation; the user's in-app data and encrypted key remain intact.

Conversation and cost-control validation:

- The Huawei Ask screen renders copy, retry, convert-to-checklist, and bookmark actions without crowding the answer card at 1080 x 2312 / 480 dpi.
- Conversation history listed the existing Elden Ring session, exposed accessible rename/delete controls, and opened the rename dialog; the dialog was cancelled so live data remained unchanged.
- Starting a new conversation displayed a clean empty state and did not persist an unused session or make a DeepSeek request.
- The Ask screen reports the 2,000-character question boundary, fourteen-message context window, active spoiler mode, and configured maximum output tokens.
- AI settings expose 512, 900, and 1500 token presets with a clear cost warning. Exact price claims are intentionally avoided.
- Chat DAO coverage now verifies session listing order, rename, individual delete, message/citation persistence, and game cascade. Seven Huawei tests, JVM tests, lint, assembly, install, and cold launch pass without runtime or SQLite errors.

Release-candidate and full companion validation:

- The versioned `1.0.0-mvp` debug APK installed as an in-place upgrade on the Huawei without losing the game, conversation, saved answer, Wiki cache, settings, or encrypted key.
- The single-screen fallback started `CompanionActivity` in its own task and rendered the complete Ask/Wiki/Saved/Games/More UI with the existing Elden Ring conversation and all answer actions.
- The obsolete touch-proof-only companion screen was removed. The same full interface is now launched through `ActivityOptions.launchDisplayId` for eligible secondary displays.
- A mapped Android package now exposes an explicit launch action in Games; `GameLauncher` targets display 0 and returns actionable invalid/missing/blocked errors.
- The clean JVM suite, debug lint, seven Huawei tests, and minified R8 release build with `lintVitalRelease` passed. A 1.63 MB test-key-signed release verified with APK Signature Scheme v2, installed as an upgrade, and reported version `1.0.0-mvp`; long-lived private signing is configured through external environment/Gradle values and documented separately.
- No `AndroidRuntime`, Room, or SQLite error was recorded during install, cold launch, task creation, or rendering the complete companion activity.
- A final simulated-display run detected a fresh non-hardcoded overlay display ID, kept `MainActivity` on display 0, and launched the complete `CompanionActivity` task on display 4 without a crash. The original `overlay_display_devices=null` setting was restored after both final runs.
- Final APK inspection found only internet, network-state, and Android's non-exported dynamic-receiver compatibility permission. `CompanionActivity`, Room's service, and AndroidX startup components are non-exported; only the required launcher activity is exported.

Not yet validated:

- Physical secondary-display touch; the Android overlay correctly reports no touch.
- Concurrent game on the primary physical screen and companion touch on the lower AYN Thor screen.
- Real AYN display disable/re-enable, lid, focus, and firmware behavior.
- Launching a mapped game package on the primary display. Profile text entry now works through the existing keyboard, but no third-party game package was selected or launched during this run.

## Controller-first 1.1.0 build — 2026-07-19

Completed without a device:

- Version `1.1.0-controller` / code 2 compiles as debug and minified R8 release.
- JVM coverage passes for question composition, spoiler instructions, controller command mapping, repeated/non-controller filtering and long-press policy.
- Room schema 6 is exported and the schema-5-to-6 migration test APK compiles; the migration adds per-game Quick Question favorites with a foreign-key cascade.
- Android lint and `lintVitalRelease` pass with zero errors.
- The 1.65 MB test-key-signed APK verifies with APK Signature Schemes v2 and v3 and contains no microphone or screenshot permission.
- Repository APK SHA-256: `6C0263EF05C07A2924B31C637AF070CE990B9BF63A71A78A653B4C0ABAF12FBE`.

Still required for the 1.1 hardware gate:

- The Huawei was no longer listed by ADB on 2026-07-19, so the nine compiled Room/Keystore/migration instrumentation tests and install-over-existing-schema-5 check could not be rerun in this session.
- AYN Thor controller key calibration, global long-press delivery, focus routing, lower-display launch and real-game coexistence remain physical-device acceptance tests.

The Huawei results validate the standards-based display/activity implementation but do not replace the AYN Thor acceptance gate.

## Companion hotfix 1.1.1 build — 2026-07-19

Trigger and diagnosis:

- User testing on the physical AYN Thor established that launching any game on the upper display removed the entire lower `CompanionActivity` task and returned the lower display to the Thor launcher.
- No game-profile selection or package-specific behavior was required to reproduce it. USB/ADB diagnostics were unavailable, so this release adds bounded, privacy-safe lifecycle diagnostics.

Completed locally:

- `CompanionActivity` changed from `singleTask` to a retained, separate `singleTop` task; every launch/restore uses `NEW_TASK`, `CLEAR_TOP`, and `SINGLE_TOP`.
- A persisted session coordinator keeps the session active across normal `onStop`/`onDestroy` and same-process recreation until the user explicitly stops it.
- The limited Accessibility service normalizes only window-state changes and retains `canRetrieveWindowContent=false`; GameSide, Android system UI, permission dialogs, and launcher/home packages are filtered.
- Restore requests wait 750 ms, revalidate the target display, use a five-second cooldown, allow at most three automatic requests per minute, and use generation IDs to invalidate stale work.
- Display removal leaves the session `temporarilyDisplaced`; display return selects a currently valid secondary display. Manual restore and the long controller shortcut remain fallbacks.
- The Displays UI reports Accessibility/session/target status and exposes keep-active, manual restore, explicit stop, and a fifty-event privacy-safe diagnostics copy action.
- JVM tests cover state retention until explicit stop, cooldown, the one-minute restore limit, manual fallback, launcher/system filtering, and target-display fallback after reconnect.
- Full JVM tests, Android lint, debug assembly, minified R8 release assembly, and the minified test distribution build completed successfully. No Room schema change was needed; existing schema and backup regression tests remained green.
- APK metadata reports version `1.1.1-companion-hotfix`, version code 3, minSdk 26, and targetSdk 36. APK Signature Scheme v2 verification passed with the existing Android test certificate.
- Published APK size: 1,673,555 bytes. SHA-256: `B0C99BE77F6948D3EAA8BCD9EADBB1E56C4657D632700D5635ED328A0E34EB95`.

Status after physical feedback:

- No Android device was available to ADB during this build, so the compiled Room/Keystore instrumentation suite was not rerun.
- The physical Thor feedback showed that automatic restore did not keep GameSide below while a game ran above. This acceptance path was superseded by the simpler `1.1.2-lower-screen` behavior below.

## Automatic lower-screen build 1.1.2 — 2026-07-19

User decision and hardware result:

- The `1.1.1` restore loop did not keep GameSide below while a game ran above on the physical Thor.
- The reverse layout, GameSide above with a game below, remained stable.
- On user request, the companion-session feature and background restore behavior were removed rather than expanded with more permissions or a foreground service.

Completed locally:

- Every launcher start on display 0 now selects the available non-primary display, starts the complete GameSide interface there, and finishes the temporary upper activity.
- A launch already occurring on a secondary display stays there and does not create another redirect. A single-display device remains on its current display.
- More > Displays no longer exposes launch, restore, stop, keep-active, or diagnostics controls; it reports the automatic target and retains controller-shortcut configuration.
- The long Menu shortcut directly targets the current secondary display and does not read accessibility window content.
- Unit tests cover primary-to-lower redirect, already-lower behavior, and single-screen fallback.
- JVM tests, Android lint, debug assembly, the Room/Keystore instrumentation APK compile, minified R8 release, and minified signed beta release all passed when run in memory-safe separated Gradle jobs.
- APK metadata: `1.1.2-lower-screen`, version code 4, minSdk 26, targetSdk 36. Published size: 1,646,971 bytes.
- Published SHA-256: `8AFDC3C0AF3D428CA6706CEFF938E05D778D42636624891657BEA7CCC3ADFF11`.

Open hardware limitation:

- Thor firmware still closes GameSide below when an upper-screen game takes over. This release makes reopening GameSide below automatic and predictable but does not claim simultaneous coexistence in that orientation.

## Game detection and primary-anchor build 1.1.3 — 2026-07-20

Implemented:

- Native discovery queries launcher-visible apps and imports apps Android classifies with `ApplicationInfo.CATEGORY_GAME`.
- A bounded label/package classifier recognizes common emulator families including RetroArch, PPSSPP, Dolphin, Citra/Azahar/Lime3DS, Yuzu/Sudachi, NetherSX2/AetherSX2, DuckStation, DraStic/melonDS, Mupen64, Redream/Flycast, MAME, Lemuroid and Vita3K.
- The Games screen automatically scans installed games/emulators, supports manual refresh, filters already imported package names/titles, and provides individual or import-all actions.
- The opt-in Storage Access Framework folder picker persists read access only to the selected ROM tree. Scanning is capped at ten directory levels and 2,000 supported ROMs and does not request storage permission.
- ROM profiles support disc, cartridge, handheld and compressed extensions. Compatible installed emulator packages are associated by extension/folder hint where possible; generic formats fall back to RetroArch/Lemuroid when installed.
- Imported discoveries use deterministic IDs, default to minimal spoiler protection, and preserve normal editable game-profile behavior.
- `MainActivity` now remains as an excluded, background primary router task after launching GameSide below. Lower-origin launches create an equivalent transparent primary anchor. This targets the physical observation that Thor is stable when a primary task exists before the lower GameSide activity.

Validation:

- New unit coverage passes for lower-screen routing, emulator classification (including a Temu false-positive regression), extension-to-emulator mapping and missing-emulator fallback.
- Full JVM tests, debug lint, debug assembly, Room/Keystore instrumentation APK compilation, minified R8 release and minified test-key-signed beta build passed.
- APK metadata reports `1.1.3-game-detection`, version code 5, minSdk 26 and targetSdk 36. APK size: 1,663,547 bytes.
- APK SHA-256: `005A96C7B9D4D8F843B94E7542F424AE83C410682225507022FB1A8867C27822`.

Physical acceptance still required:

- Open GameSide below first, then launch at least three apps/games above and verify that the primary anchor prevents the intermittent lower activity closure.
- Verify installed Android-game and emulator detection against the actual Thor library, then select a real ROM directory and inspect title/emulator mapping. Directly launching a specific ROM is not claimed because emulator intent APIs differ.
