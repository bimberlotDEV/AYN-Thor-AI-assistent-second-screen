# Private release and distribution

## Install the currently published test APK

The direct-download APK in `downloads/` is the minified `1.1.2-lower-screen` test release. It is signed with the same local Android test/debug certificate as the previous private build so compatible test installations can be updated in place; it is not a long-term production-signed release.

To install that published build without a PC, follow the Dutch [user guide](gebruikershandleiding.md). To build and install a development APK from source, use:

```powershell
./gradlew.bat clean assembleDebug
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r app\build\outputs\apk\debug\app-debug.apk
```

`-r` preserves local data only when the installed app was signed by the same key. The APK contains no DeepSeek credential; enter a key directly on each device under **More > AI**.

## Create a long-lived private release key

Create the key outside the repository and keep two encrypted backups. Losing it means future APK updates cannot be installed over existing releases.

```powershell
$signingDir = Join-Path $env:LOCALAPPDATA "GameSideAI\signing"
New-Item -ItemType Directory -Force -Path $signingDir | Out-Null
keytool -genkeypair -v -keystore "$signingDir\gameside-release.jks" -alias gameside -keyalg RSA -keysize 4096 -validity 10000
```

`keytool` asks for passwords interactively. Do not put the keystore, passwords, or `keystore.properties` in Git.

Set signing values for the current PowerShell process without writing them to the project:

```powershell
$storeSecret = Read-Host "Keystore password" -AsSecureString
$keySecret = Read-Host "Key password" -AsSecureString
$env:GAMESIDE_KEYSTORE_PATH = Join-Path $env:LOCALAPPDATA "GameSideAI\signing\gameside-release.jks"
$env:GAMESIDE_KEYSTORE_PASSWORD = [Net.NetworkCredential]::new("", $storeSecret).Password
$env:GAMESIDE_KEY_ALIAS = "gameside"
$env:GAMESIDE_KEY_PASSWORD = [Net.NetworkCredential]::new("", $keySecret).Password
./gradlew.bat clean lintDebug test assembleRelease
```

With all four variables present, `app/build/outputs/apk/release/app-release.apk` is signed. Without them, Gradle intentionally produces `app-release-unsigned.apk` for R8 verification only. `assembleBetaRelease` is the separate minified test-key-signed distribution path and must not be used as a production release.

Verify before sharing:

```powershell
$buildTools = Join-Path $env:LOCALAPPDATA "Android\Sdk\build-tools\36.1.0"
& "$buildTools\apksigner.bat" verify --verbose --print-certs app\build\outputs\apk\release\app-release.apk
Get-FileHash app\build\outputs\apk\release\app-release.apk -Algorithm SHA256
```

Send the APK and SHA-256 value through separate trusted channels when practical. Friends enable Android's **Install unknown apps** permission only for the file manager used to open the APK, install it, then disable that permission again.

## Moving from the debug build to the private release

Debug and private-release keys differ, so Android will reject an in-place upgrade:

1. In the debug app, use **More > Privacy > Export JSON**.
2. Confirm the backup file is available outside the app.
3. Uninstall the debug build. This deletes the on-device encrypted API key.
4. Install the signed private release.
5. Complete onboarding, import the JSON backup, and enter the API key again.

Future releases signed with the same private key can use `adb install -r` or normal APK update installation. Increase `versionCode` for every distributed update.
