# GameSide AI

GameSide AI is a private, native Android gaming companion designed to remain usable on the lower screen of dual-screen handhelds while a game runs on the primary display.

## Current milestone

The repository currently contains the technical proof of concept required before AI development:

- live Android display discovery and diagnostics;
- capability-based secondary-display selection without hard-coded display IDs;
- a dedicated companion activity launched on a selected display;
- a touch interaction test that survives activity recreation;
- explicit launch of a mapped Android game package on the primary display;
- safe single-screen fallback;
- an OLED-friendly Compose interface;
- isolated device/domain/UI modules and selection-policy tests.

AI, wiki, database, notes, and checklist features intentionally follow only after this foundation is validated.

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

The proof of concept requests no sensitive permissions, performs no analytics, records no audio or screen content, has no network permission, and stores no credentials. It queries only launcher activities to support an explicitly entered game package.

See [device testing](docs/device-testing.md) for the Huawei P30 Lite, simulated displays, and AYN Thor acceptance procedure.

The module boundaries and display data flow are documented in [architecture](docs/architecture.md).

Recorded hardware and simulated-display results are available in [device test results](docs/test-results.md).
