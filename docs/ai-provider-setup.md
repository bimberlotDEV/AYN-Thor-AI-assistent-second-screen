# DeepSeek provider setup

GameSide AI supports a user-supplied DeepSeek API key. No shared key is bundled in the APK.

## Configure on the device

1. Open **Settings** in GameSide AI.
2. Paste a DeepSeek API key into **DeepSeek API key**.
3. Tap **Encrypt & save**.
4. Tap **Test DeepSeek connection**.
5. Return to **Ask**, select a game if needed, and submit a question.

The key is encrypted with an AES/GCM key held by Android Keystore. The encrypted payload is stored in private app storage, Android backup is disabled, and the key is never written to Room, DataStore, source code, Gradle configuration, or logs.

## Models and data flow

The default is `deepseek-v4-flash` in non-thinking mode for lower latency and cost. `deepseek-v4-pro` can be selected in Settings. GameSide uses the official `https://api.deepseek.com/chat/completions` endpoint and streaming responses.

Only an explicitly submitted request leaves the device. It contains:

- the active game title and platform category;
- configured progress and spoiler preference;
- a compact system prompt;
- up to fourteen recent messages in that game's current conversation;
- the new question.

Game profiles and completed chat messages remain in the local Room database. Removing the credential prevents future provider requests without deleting local game or chat data.

Official reference: [DeepSeek API quick start](https://api-docs.deepseek.com/).

## Current limitation

This first chat slice uses general model knowledge. Wiki retrieval and verified source citations are not attached yet, so the prompt explicitly forbids fabricated citations and asks the model to identify uncertainty. Source-backed retrieval is the next knowledge phase.
