# Release checklist

- [ ] Working tree contains only intended changes; secret scan finds no API-key pattern.
- [ ] `./gradlew.bat clean test lintDebug assembleDebug assembleRelease` succeeds.
- [ ] All Room migration/instrumentation tests pass on the minimum practical Android device.
- [ ] Fresh install, onboarding, game creation, API-key entry, sourced question, bookmark, note, checklist, Wiki cache, export/import, and privacy confirmations are exercised.
- [ ] Process restart preserves active game, conversations, tools, settings, and Wiki cache.
- [ ] Single-screen fallback opens the complete companion UI in `CompanionActivity`.
- [ ] AYN Thor: full UI runs touchably on lower display while a mapped game remains on primary; display removal/reconnect and rotation are tested.
- [ ] Release `versionCode` and `versionName` are updated.
- [ ] Release APK is signed with the long-lived private key and `apksigner verify --print-certs` succeeds.
- [ ] APK SHA-256 is recorded and the install/upgrade path is tested.
- [ ] Known limitations and privacy behavior match the shipped build.
- [ ] Tag the reviewed commit only after all applicable boxes pass.

