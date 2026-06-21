# Android2AndroidMirror — projectinstructies

No-root Android→Android scherm-mirror **+ besturing** via het scrcpy-model.
Onderdeel van ecosysteem **Meta_Auto**. PUBLIC, AGPL-3.0.

## Architectuur
Zie [ARCHITECTURE.md](ARCHITECTURE.md) — de 8 Fase-1-beslispunten zijn daar verantwoord.
Kort: cartablet = dunne client (dadb + MediaCodec-decode + scrcpy control-protocol); de
officiële scrcpy-server.jar draait onder shell-UID op de bron via wireless ADB.

## Stack
Kotlin 2.0.21 · Compose BOM 2024.12.01 · AGP 8.7.3 · Gradle 8.11.1 · Java 21 ·
minSdk 26 / target+compileSdk 35 · package `nl.icthorse.android2androidmirror`.

## Build (vereist Android SDK — bv. de Mac, NIET de HC55-server)
```bash
./tools/fetch-scrcpy-server.sh    # Apache-2.0 jar, niet in repo
./gradlew assembleDebug
```
Deploy via `/APKDeploy` naar HorseAPK.

## Regels
- Elke wijziging → versie + versionCode ophogen (version.json + app/build.gradle.kts).
- BUGLIST.md en RELEASES.md vanaf dag 1 bijhouden.
- scrcpy-server.jar NOOIT committen (.gitignore + NOTICE-attributie).
- `ScrcpyServer.SERVER_VERSION` moet matchen met de gefetchte jar.
- Releasenaam-thema: afstandsbedienings-/teleoperatie-pioniers (Tesla → Torres → …).
