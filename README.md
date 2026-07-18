# GameSide AI

GameSide AI is a private, native Android gaming companion designed to remain usable on the lower screen of dual-screen handhelds while a game runs on the primary display.

## Current milestone

The repository now contains the validated display proof of concept plus the first local-first product slice:

- live Android display discovery and diagnostics;
- capability-based secondary-display selection without hard-coded display IDs;
- a dedicated companion activity launched on a selected display;
- a touch interaction test that survives activity recreation;
- explicit launch of a mapped Android game package on the primary display;
- safe single-screen fallback;
- an OLED-friendly Compose interface;
- isolated device/domain/UI modules and selection-policy tests;
- first-run privacy onboarding with persistent completion state;
- a searchable manual game library with add, edit, delete, pin, active-game, platform, package, and spoiler controls;
- Room persistence with exported schemas and tested forward migrations;
- DataStore app settings and Android Keystore-backed encrypted credential storage contracts.
- per-game DeepSeek V4 chat with streaming output, cancellation, local history, model selection, and spoiler-aware prompts;
- a provider-neutral knowledge contract plus automatic game-wiki detection, retrieval, ranking, and persisted clickable citations.
- offline saved answers, personal notes, and checklists scoped to the active game;
- a dedicated per-game Wiki browser with search, clickable sources, a seven-day local document cache, offline status, cache clearing, and spoiler-aware previews.
- a local-data privacy dashboard with category counts, confirmed selective deletion, encrypted-key removal, and a full on-device reset.

The sourced AI-chat slice supports MediaWiki-powered game sites such as wiki.gg and Fandom. A profile can override automatic discovery with its own HTTPS game-wiki URL. If no sufficiently relevant evidence is found, the app deliberately falls back to clearly unsourced general model knowledge. Assistant answers can be saved with their question and citations; notes, checklists, and previously downloaded wiki pages remain usable without a network connection. Strict spoiler modes hide cached article previews until the source is intentionally opened.

## Build

Requirements:

- JDK 17 or newer (the project compiles against Java 17);
- Android SDK 36;
- Android build-tools 36.x.

On Windows PowerShell:

```powershell
./gradlew.bat assembleDebug
./gradlew.bat test
./gradlew.bat lint
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

The application ID defaults to `com.gameside.ai`. Override it without editing source:

```powershell
./gradlew.bat assembleDebug -PGAME_SIDE_APPLICATION_ID=de.example.gameside
```

## Privacy at this milestone

The app requests internet access but no sensitive Android permissions. It performs no analytics and records no audio or screen content. Profiles, settings, chat history, citations, and cached wiki documents remain local. User-supplied API credentials are encrypted with an Android Keystore AES/GCM key; no API key is bundled. The Privacy screen shows local record counts and supports confirmed deletion by category, credential removal, or a complete reset. A submitted question may be sent to the detected or configured game wiki for evidence retrieval and to the configured DeepSeek provider for generation. Game lookup remains limited to launcher activities and explicitly entered packages.

See [device testing](docs/device-testing.md) for the Huawei P30 Lite, simulated displays, and AYN Thor acceptance procedure.

The module boundaries and display data flow are documented in [architecture](docs/architecture.md).

Recorded hardware and simulated-display results are available in [device test results](docs/test-results.md).

DeepSeek configuration and the exact request/privacy behavior are documented in [AI provider setup](docs/ai-provider-setup.md).
