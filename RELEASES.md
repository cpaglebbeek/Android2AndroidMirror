# Releases — Android2AndroidMirror

Codenaam-thema: **Afstandsbedienings-/teleoperatie-pioniers**.
Geplande reeks: Tesla → Torres → Ducretet → Branly → Richardson (VNC) → Marconi.

| Versie | Code | Datum | Status | Inhoud |
|--------|------|-------|--------|--------|
| 0.0.1-Tesla | 1 | 2026-06-21 | skeleton | Repo-skelet: alle componentgrenzen, Gradle/Compose-config, dadb-dep, ARCHITECTURE met 8 besluiten. Nog geen werkende APK. |
| 0.0.1-Tesla | 2 | 2026-06-21 | implemented + compileert | Volledige v0.0.1-impl: **tooling-pivot dadb→libadb-android** (dadb kon niet pairen, zie BUGLIST B1). libadb pairing(SPAKE2+TLS)/connect + sync-push; AdbMdns-discovery; scrcpy dual-socket-handshake; MediaCodec H.264 low-latency decode; control-protocol (touch/key/text); MirrorSession-coördinator + Compose-UI met IME. **`assembleDebug` groen** (Android SDK alsnog op HC55 geïnstalleerd); APK bevat libspake2.so + libconscrypt_jni.so + scrcpy-server.jar. Enige compile-fout = naamclash App/App() (B2, gefixt). Nog niet op toestel getest. |

| 0.0.2-Torres | 3 | 2026-06-22 | implemented + compileert | **USB-ADB-host transport (beslispunt 2b)** zodat de Z Fold de hotspot kan blijven (Wireless Debugging kan niet aan in hotspot-modus). libadb **gevendord** als `:libadb`-module + `AdbChannel`-seam (socket-pad ongewijzigd). Nieuw: `UsbAdbChannel` (ADB over `bulkTransfer`), `UsbAdbHost`, `UsbPermission`, `MirrorMode`-instelling (USB_BOOTSTRAP_WIFI/FULL_USB/WIRELESS) + Settings-UI, `AdbClient.connectUsb/connectTcp/bootstrapTcpipOverUsb`, `MirrorSession.startUsb` (bootstrap `tcpip:5555` → TCP `gateway:5555`, of full-USB). `assembleDebug` groen op HC55. **Nog niet op hardware getest** (BUGLIST R11–R15; OTG-test gate). |

### Tooling/repo-hygiëne (2026-06-22, geen APK-wijziging)
- **Gradle-wrapper toegevoegd** (`gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, v8.11.1) — ontbrak; `./gradlew assembleDebug` (zoals CLAUDE.md voorschrijft) werkte daardoor nergens. Build via wrapper bevestigd groen op HC55.
- **Statische protocol-review** scrcpy v3.1: handshake/framing/control byte-exact bevonden (zie BUGLIST R7); gebundelde `scrcpy-server.jar` byte-identiek aan officiële v3.1.

## Gepland

- **0.0.2-Torres (verifiëren op hardware):** OTG-test cartablet (R11), dan op Z Fold 6 → cartablet:
  USB-bootstrap (`tcpip:5555` → wifi) én full-USB end-to-end; bulkTransfer-framing (R12/R13),
  RSA-dialoog (R14) en bootstrap→wifi-overgang (R15) bevestigen. Plus de openstaande
  scrcpy-runtime-checks van 0.0.1 (R7/R8) die nog op hardware moeten.
- **0.0.3-Ducretet:** robuuste reconnect + autodiscovery-UX + letterbox/rotatie-correctie +
  instelbare bitrate/resolutie + kioskmodus/autostart in de auto.
- **0.1.0-Richardson:** optioneel audio (alleen als BT-A2DP niet volstaat) + tuning.
