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
- Room persistence with an exported version-1 schema;
- DataStore app settings and Android Keystore-backed encrypted credential storage contracts.

AI, wiki retrieval, notes, and checklist features build on these local domain and repository boundaries. No fake AI behavior is exposed in the UI.

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

The app requests no sensitive permissions, performs no analytics, records no audio or screen content, and currently has no network permission. Profiles and settings remain local. The credential store is ready for later user-supplied provider keys and encrypts values with an Android Keystore AES/GCM key; no API key is bundled or currently requested. Game lookup remains limited to launcher activities and explicitly entered packages.

See [device testing](docs/device-testing.md) for the Huawei P30 Lite, simulated displays, and AYN Thor acceptance procedure.

The module boundaries and display data flow are documented in [architecture](docs/architecture.md).

Recorded hardware and simulated-display results are available in [device test results](docs/test-results.md).
