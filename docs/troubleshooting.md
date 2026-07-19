# Troubleshooting and known limitations

## The app says no API key is stored

Open **More > AI**, paste a personal DeepSeek key, tap **Encrypt & save**, then run the connection test. Backups deliberately do not contain keys.

## A question has no sources

GameSide uses dedicated game Wikis, not general Wikipedia. Check connectivity, search the Wiki tab directly, or edit the game and enter the base HTTPS URL of its MediaWiki-powered game Wiki. Some games have no compatible Wiki or use anti-bot protection; the assistant then labels the answer as general model knowledge.

## The wrong game is used

Open **Games** and tap the intended profile so it shows `ACTIVE GAME`. Context is always manually scoped in MVP; foreground-app monitoring is not enabled.

## A mapped game will not launch

Edit the profile and enter the exact Android package name. Launch works only for an installed launcher activity. Emulated ROMs and streamed console/PC titles normally need to be started in their emulator or streaming app.

## The second display is missing

Open **More > Displays** and inspect Android's reported displays. Reconnect or re-enable the lower display, then retry. Android simulated overlays validate activity placement but cannot validate physical touch. Use the single-screen companion fallback if no eligible display is exposed.

## The companion disappears when a game starts

Install version `1.1.1-companion-hotfix` or newer, launch the lower-screen companion, and enable **GameSide controller shortcut** in Android Accessibility. In **More > Displays**, confirm the session is active, **Keep companion active while gaming** is on, and a secondary target display is shown. Use **Restore companion now** if automatic restore is unavailable.

Automatic restore waits 750 ms, has a five-second cooldown, and stops after three attempts per minute. If status becomes `failed`, use the long Menu shortcut or manual restore and select **Copy diagnostics**. The copied log is privacy-safe; never add an API-key or private chat manually.

## APK update is rejected

The new APK was signed by a different key or has an older/equal version code. Preserve the original private signing key. When moving from debug to private release, export JSON, uninstall, install the release, import, and enter the API key again.

## Current MVP limitations

- Physical AYN Thor lower-screen touch, firmware focus/lid behavior, and simultaneous real-game operation still require the target hardware acceptance run.
- Voice questions and user-triggered screenshot/vision help are intentionally post-MVP.
- Wiki discovery currently targets compatible wiki.gg/Fandom/MediaWiki sites or a manually configured HTTPS Wiki.
- Game launch automation covers mapped Android launcher packages, not individual emulator ROMs.
- Markdown markers in provider output may display as plain text; answers and links remain usable.
- Cover-art picking, automatic foreground-game detection, cloud sync, accounts, game packs, payments, and social features are not part of this private MVP.
