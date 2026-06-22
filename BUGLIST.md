# Buglist — Android2AndroidMirror

(Vanaf dag 1 bijgehouden — les uit MirrorCast, waar dit retroactief moest.)

| ID | Datum | Ernst | Status | Omschrijving |
|----|-------|-------|--------|--------------|
| B1 | 2026-06-21 | Hoog | Opgelost (pivot) | **dadb kan niet draadloos pairen.** Broninspectie van `dev.mobile:dadb:1.2.10` toonde geen pairing-API (geen SPAKE2/TLS) en alleen plaintext-ADB → onbruikbaar voor de Android 11+ Wireless-Debugging-flow (TLS-only). R3 gematerialiseerd, erger dan verwacht. **Fix:** tooling-pivot naar **libadb-android 3.1.1** (beslispunt 2 herzien). |
| B2 | 2026-06-21 | Laag | Opgelost | **Naamclash `App`.** De `Application`-klasse `App` botste met de Compose-functie `App()` in MainActivity (zelfde package) → "Overload resolution ambiguity" bij `compileDebugKotlin`. Enige compile-fout uit de eerste echte build op HC55. **Fix:** Composable hernoemd `App()` → `Root()`. |

## Verwachte risico's (te bewaken bij implementatie)

- **R1** OneUI zet Wireless Debugging uit na reboot → eenmalige toggle nodig (by design, geen bug).
- **R2** scrcpy-server-versie moet exact matchen met `ScrcpyServer.SERVER_VERSION`, anders weigert de server.
- **R3** ~~dadb-pairing-API~~ → opgelost als B1 (pivot naar libadb-android). Nu: libadb-versie pinnen (3.1.1) + JitPack-beschikbaarheid bewaken.
- **R4** Trage decoder kan bij 1024px/24fps nog haperen → bitrate/res verder verlagen (bp3 is conservatief startpunt).
- **R5** Touch-coördinaten: letterbox/rotatie verkeerd mappen = tikken naast doel (zie TouchMapper).
- **R6** DRM/FLAG_SECURE-content op de bron blijft zwart in de mirror (scrcpy-eigenschap, geen workaround).
- **R7** **scrcpy v3.1 handshake/framing was op tabel build-blind geschreven.** Dummy-byte + 64-byte device-naam + codec-meta + 12-byte frame-meta moeten op echt toestel geverifieerd worden; bij mismatch wijken de offsets in `ScrcpyServer`/`VideoStream` af. **Statisch geverifieerd 2026-06-22** tegen het scrcpy v3.1-protocol: handshake-volgorde (dummy-byte → control-connect → device-meta, de subtiele forward-mode-volgorde) klopt; frame-meta `pts(8,bit63=config/bit62=key)`+`len(4)` klopt; control-protocol byte-exact (TOUCH=32B, KEYCODE=14B, TEXT 4B-len+max300, `floatToU16Fp`=`sc_float_to_u16fp`). Gebundelde `scrcpy-server.jar` is byte-identiek aan de officiële v3.1 (sha256 `958f0944…`). Runtime-bevestiging op hardware blijft staan, maar offset-risico is sterk verlaagd. Klein: TEXT-truncatie kan UTF-8-codepoint afkappen (scrcpy gebruikt codepoint-grens) — edge-case >300B.
- **R8** **Conscrypt-provider vereist** voor TLS naar remote adbd; zonder `App`-init (Security.insertProviderAt) faalt pairing/connect.
- **R9** Connect-poort kan vlak ná pairing nog niet via mDNS bekend zijn → soms "zoek opnieuw" nodig vóór verbinden (autoConnect-fallback is follow-up).
- **R10** v0.0.1 = single-pointer touch + tekst/Enter/Backspace; multi-touch, pijl-/functietoetsen en scroll zijn follow-up.

## USB-transport (beslispunt 2b) — build-blind risico's

Reden voor het USB-pad: Wireless Debugging kan op de Z Fold **niet** aan terwijl die zelf de
hotspot is (vereist Wi-Fi-client). USB-debugging heeft die eis niet → via de cartablet als
USB-host kunnen we `tcpip:5555` activeren (bootstrap) of de hele mirror over USB draaien (fallback).

- **R11** **Cartablet OTG-host onbekend.** Een goedkope Android-14 cartablet ondersteunt mogelijk geen USB-host/OTG. Goedkope vroege check: USB-stick via OTG → mount hij? Zonder OTG-host vervalt het hele USB-pad. (Manifest declareert `android.hardware.usb.host` als optioneel.)
- **R12** **bulkTransfer-timeoutsemantiek build-blind.** `UsbAdbChannel` leest met timeout 0 (= oneindig blokkeren) en behandelt `n<0` als ontkoppeling; ZLP-gedrag (payload veelvoud van max-packet) niet afgehandeld. Alleen op hardware te bevestigen.
- **R13** **ADB-over-USB write-framing build-blind.** Elk bericht (24B header + payload) wordt als byte-stroom in ≤16KB-stukken over de bulk-OUT gestuurd; aanname dat adbd 24+`data_length` sequentieel leest. Verifiëren op toestel; bij mismatch herzien.
- **R14** **RSA-autorisatie + USB-debugging-eis.** De ADB-interface verschijnt alleen als USB-debugging op de Z Fold aanstaat; de eerste connect triggert de "Sta debugging toe?"-RSA-dialoog (eenmalig). Permissie-broadcastflow (PendingIntent + receiver) wordt in de sessie-laag (F4) afgehandeld.
- **R15** **Bootstrap→wifi-overgang.** Na `tcpip:5555` over USB moet de 5555-listener op de hotspot-interface verschijnen en blijven tot reboot; plaintext TCP-reconnect naar `gatewayHint():5555`. Timing/persistentie op toestel bevestigen.

> Crypto-/transport-layer-peel (memory-feedback): deze multi-laag USB+ADB-stack onthult bugs
> laag-voor-laag; **hardware-smoke is een verplichte release-gate**, JVM-builds bewijzen niets.
