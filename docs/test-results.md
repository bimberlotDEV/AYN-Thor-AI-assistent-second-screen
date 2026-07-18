# Device test results

## Huawei P30 Lite — 2026-07-18

Test environment:

- Model: HUAWEI MAR-LX1A (P30 Lite)
- Android: 9 / API 28
- EMUI build: 9.1.0.317
- Built-in display: 1080 × 2312, 480 dpi, 60 Hz, internal touch
- APK: debug `0.1.0-poc`, application ID `com.gameside.ai`

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

Not yet validated:

- Physical secondary-display touch; the Android overlay correctly reports no touch.
- Concurrent game on the primary physical screen and companion touch on the lower AYN Thor screen.
- Real AYN display disable/re-enable, lid, focus, and firmware behavior.
- Launching a mapped game package on the primary display. Profile text entry now works through the existing keyboard, but no third-party game package was selected or launched during this run.

The Huawei results validate the standards-based display/activity implementation but do not replace the AYN Thor acceptance gate.
