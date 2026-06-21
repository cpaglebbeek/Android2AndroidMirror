# Buglist — Android2AndroidMirror

(Vanaf dag 1 bijgehouden — les uit MirrorCast, waar dit retroactief moest.)

| ID | Datum | Ernst | Status | Omschrijving |
|----|-------|-------|--------|--------------|
| B1 | 2026-06-21 | Hoog | Opgelost (pivot) | **dadb kan niet draadloos pairen.** Broninspectie van `dev.mobile:dadb:1.2.10` toonde geen pairing-API (geen SPAKE2/TLS) en alleen plaintext-ADB → onbruikbaar voor de Android 11+ Wireless-Debugging-flow (TLS-only). R3 gematerialiseerd, erger dan verwacht. **Fix:** tooling-pivot naar **libadb-android 3.1.1** (beslispunt 2 herzien). |

## Verwachte risico's (te bewaken bij implementatie)

- **R1** OneUI zet Wireless Debugging uit na reboot → eenmalige toggle nodig (by design, geen bug).
- **R2** scrcpy-server-versie moet exact matchen met `ScrcpyServer.SERVER_VERSION`, anders weigert de server.
- **R3** ~~dadb-pairing-API~~ → opgelost als B1 (pivot naar libadb-android). Nu: libadb-versie pinnen (3.1.1) + JitPack-beschikbaarheid bewaken.
- **R4** Trage decoder kan bij 1024px/24fps nog haperen → bitrate/res verder verlagen (bp3 is conservatief startpunt).
- **R5** Touch-coördinaten: letterbox/rotatie verkeerd mappen = tikken naast doel (zie TouchMapper).
- **R6** DRM/FLAG_SECURE-content op de bron blijft zwart in de mirror (scrcpy-eigenschap, geen workaround).
- **R7** **scrcpy v3.1 handshake/framing is op tabel build-blind geschreven** (HC55 heeft geen Android SDK). Dummy-byte + 64-byte device-naam + codec-meta + 12-byte frame-meta moeten op echt toestel geverifieerd worden; bij mismatch wijken de offsets in `ScrcpyServer`/`VideoStream` af.
- **R8** **Conscrypt-provider vereist** voor TLS naar remote adbd; zonder `App`-init (Security.insertProviderAt) faalt pairing/connect.
- **R9** Connect-poort kan vlak ná pairing nog niet via mDNS bekend zijn → soms "zoek opnieuw" nodig vóór verbinden (autoConnect-fallback is follow-up).
- **R10** v0.0.1 = single-pointer touch + tekst/Enter/Backspace; multi-touch, pijl-/functietoetsen en scroll zijn follow-up.
