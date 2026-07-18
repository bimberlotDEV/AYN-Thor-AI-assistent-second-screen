# Privacy and security

## Data that remains on the device

- game profiles, progress, spoiler settings, package mappings, and custom Wiki URLs;
- conversations and their citations;
- saved answers, notes, and checklists;
- downloaded Wiki documents and UI/provider settings;
- the DeepSeek credential, encrypted with AES/GCM using a non-exportable Android Keystore key.

Android cloud backup is disabled. GameSide has no account, analytics SDK, advertising SDK, microphone permission, screenshot permission, background service, or continuous app monitoring.

## Data sent after an explicit question

- The detected/configured HTTPS game Wiki receives a search query and page requests.
- DeepSeek receives the active game context, spoiler policy, the latest fourteen messages, bounded retrieved evidence, and the submitted question.
- A connection test sends only a minimal test prompt.

The app does not log request bodies, response bodies, credentials, or decrypted key material. AI and community-Wiki answers can still be wrong; citations show the factual basis but are not a guarantee.

## User controls

**More > Privacy** shows record counts and offers confirmed deletion of conversations, personal tools, Wiki cache, or the provider key. Full reset clears Room, DataStore, encrypted preferences, and the app's Keystore entry, then restarts onboarding.

JSON export excludes credentials and cached Wiki article text. Import accepts only a user-selected `content://` document, enforces size/record limits, validates supported values, HTTPS URLs, and references, then merges in one database transaction.

## Threat boundary

GameSide protects secrets from accidental repository inclusion, normal app-file access, backups, and casual device inspection. It cannot protect data on a rooted/compromised device, a device unlocked by another person, malicious firmware, provider-account compromise, or content opened in an external browser. Keep the phone/handheld updated and protect the DeepSeek account with its available security controls.

