# Releases — Android2AndroidMirror

Codenaam-thema: **Afstandsbedienings-/teleoperatie-pioniers**.
Geplande reeks: Tesla → Torres → Ducretet → Branly → Richardson (VNC) → Marconi.

| Versie | Code | Datum | Status | Inhoud |
|--------|------|-------|--------|--------|
| 0.0.1-Tesla | 1 | 2026-06-21 | skeleton | Repo-skelet: alle componentgrenzen, Gradle/Compose-config, dadb-dep, ARCHITECTURE met 8 besluiten. Nog geen werkende APK. |
| 0.0.1-Tesla | 2 | 2026-06-21 | implemented + compileert | Volledige v0.0.1-impl: **tooling-pivot dadb→libadb-android** (dadb kon niet pairen, zie BUGLIST B1). libadb pairing(SPAKE2+TLS)/connect + sync-push; AdbMdns-discovery; scrcpy dual-socket-handshake; MediaCodec H.264 low-latency decode; control-protocol (touch/key/text); MirrorSession-coördinator + Compose-UI met IME. **`assembleDebug` groen** (Android SDK alsnog op HC55 geïnstalleerd); APK bevat libspake2.so + libconscrypt_jni.so + scrcpy-server.jar. Enige compile-fout = naamclash App/App() (B2, gefixt). Nog niet op toestel getest. |

## Gepland

- **0.0.1-Tesla (verifiëren op toestel):** op de Mac `fetch-scrcpy-server.sh` + `./gradlew
  assembleDebug`, dan op Z Fold 6 → cartablet end-to-end testen; scrcpy v3.1-handshake/framing
  (R7) en Conscrypt-init (R8) bevestigen.
- **0.0.2-Torres:** robuuste reconnect + autodiscovery-UX + letterbox/rotatie-correctie.
- **0.0.3-Ducretet:** instelbare bitrate/resolutie + kioskmodus/autostart in de auto.
- **0.1.0-Richardson:** optioneel audio (alleen als BT-A2DP niet volstaat) + tuning.
