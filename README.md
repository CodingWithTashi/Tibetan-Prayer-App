# Tibetan-Prayer-App
Tibetan Prayer App in kotlin using Android MVVM clean architecture.   

![Tibetan Prayer using MVVM architecture](https://github.com/CodingWithTashi/Tibetan-Prayer-App/blob/master/app/src/assets/demo.png?raw=true)

## Releases (GitHub Actions + fastlane)

Pushes and pull requests to `master` run **Android CI** (`main.yml`): stage debug build, unit tests and lint. Nothing ships until you run **Actions → Release → Run workflow** on `master`:

- **track**: internal, alpha (closed testing), beta (open testing) or production.
- **validate_only**: uploads to Play for validation without publishing; use it for a first check.

Release runs Android CI, then builds the signed production AAB and uploads it with its R8 mapping. `versionName` and `versionCode` come from `app/build.gradle.kts`, so bump `versionCode` before every release; the job stops before building if Play already has that code or a newer one. The lane lives in `fastlane/Fastfile`.

### Secrets

Settings → Secrets and variables → Actions:

| Secret | Value |
|---|---|
| `GOOGLE_SERVICES_JSON` | Base64 of `app/google-services.json` |
| `ANDROID_KEYSTORE_BASE64` | Base64 of the Play upload keystore |
| `ANDROID_STORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD` | The upload keystore's passwords and alias |
| `PLAY_STORE_JSON_KEY` | The `play-publisher` service account's JSON key, pasted whole |

The service account needs this app in Play Console → Users and permissions, with "Release apps to testing tracks" and "Release to production…".

To sign locally, put `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS` and `RELEASE_KEY_PASSWORD` in `~/.gradle/gradle.properties`; without them the production release build stays unsigned.
