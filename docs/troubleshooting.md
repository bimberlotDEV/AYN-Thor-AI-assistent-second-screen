# Troubleshooting and known limitations

## The app says no API key is stored

Open **More > AI**, paste a personal DeepSeek key, tap **Encrypt & save**, then run the connection test. Backups deliberately do not contain keys.

## A question has no sources

GameSide uses dedicated game Wikis, not general Wikipedia. Check connectivity, search the Wiki tab directly, or edit the game and enter the base HTTPS URL of its MediaWiki-powered game Wiki. Some games have no compatible Wiki or use anti-bot protection; the assistant then labels the answer as general model knowledge.

## The wrong game is used

Open **Games** and tap the intended profile so it shows `ACTIVE GAME`. Context is always manually scoped in MVP; foreground-app monitoring is not enabled.

## A mapped game will not launch

Use **Games > Detect apps** first. If a game is not classified by Android, edit or create its profile and enter the exact package name. For emulator titles use **Scan ROM folder**; the play action opens a matched installed emulator when one can be identified.

## The second display is missing

Open **More > Displays** and inspect Android's reported displays. Reconnect or re-enable the lower display, then restart GameSide. Android simulated overlays validate activity placement but cannot validate physical touch. If no eligible display is exposed, GameSide stays on the current screen automatically.

## GameSide disappears below when a game starts above

Version `1.1.3-game-detection` keeps a lightweight invisible task on display 0 behind the user's current upper app/launcher. This mirrors the stable order in which something is opened above before GameSide starts below. If GameSide still closes, reopen it through its icon or long Menu shortcut and report the upper app/game and whether GameSide had been opened first.

## APK update is rejected

The new APK was signed by a different key or has an older/equal version code. Preserve the original private signing key. When moving from debug to private release, export JSON, uninstall, install the release, import, and enter the API key again.

## Current MVP limitations

- Physical AYN Thor lower-screen touch, firmware focus/lid behavior, and simultaneous real-game operation still require the target hardware acceptance run.
- Voice questions and user-triggered screenshot/vision help are intentionally post-MVP.
- Wiki discovery currently targets compatible wiki.gg/Fandom/MediaWiki sites or a manually configured HTTPS Wiki.
- ROM detection can map profiles to emulator launcher packages, but direct per-ROM launching depends on emulator-specific APIs and is not universal.
- Markdown markers in provider output may display as plain text; answers and links remain usable.
- Cover-art picking, automatic foreground-game detection, cloud sync, accounts, game packs, payments, and social features are not part of this private MVP.
