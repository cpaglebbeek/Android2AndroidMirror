# Architectuur & beslissingen — Android2AndroidMirror

Vastgelegde Fase-1-beslispunten (newp-protocol, sessie 2026-06-21). Elke regel is
verantwoord — sanitycheck-vriendelijk.

## Probleemstelling

- **Bron:** Samsung Z Fold 6 (krachtig). **Doel:** Android-14 aftermarket cartablet (traag —
  zwakke CPU/weinig RAM; Maps/Spotify lopen al stroef).
- De Z Fold 6 doet tethering/hotspot; de tablet hangt aan dat netwerk.
- Eis: scherm **zichtbaar én bestuurbaar** (touch + on-screen keyboard) op de tablet, lage
  latency, lichtst mogelijke render op de zwakke tablet, makkelijkst mogelijke setup
  (autodiscovery).

## Topologie — 1 APK, alleen op de cartablet (de bron heeft géén app)

Er is **één APK**, te installeren **alleen op de cartablet** (het doel). De app kent geen
rol-keuze en doet geen rol-detectie: **hij is per definitie de client/viewer/controller**.

- **Doel (cartablet):** draait deze app — discovery → pairen → verbinden → scrcpy-server.jar
  pushen → server starten → H.264 decoden → touch/keyboard terugsturen.
- **Bron (Z Fold 6):** draait **geen geïnstalleerde app**. Enige gebruikersactie: Wireless
  debugging aan. De zware capture/encode gebeurt door de officiële **scrcpy-server.jar**, die
  als asset in onze APK zit (`assets/scrcpy-server/scrcpy-server.jar`), bij het verbinden over
  ADB naar `/data/local/tmp/` wordt gepusht en daar onder de **shell-UID** start
  (`cleanup=true` ruimt op bij afsluiten).

Gevolg: er is bewust **geen symmetrische app** en geen "ben ik bron of doel?"-logica. Dit is
ook wat no-root mogelijk maakt — op de bron hoeft niets met capture-/inject-rechten te worden
geïnstalleerd; de scrcpy-server erft die rechten via de ADB-shell-UID.

## Beslispunten

| # | Onderwerp | Besluit | Reden |
|---|---|---|---|
| 1 | Control-/inputpad | **scrcpy-model (wireless ADB, no-root)** | Enige no-root pad met volledige touch+echt keyboard+laagste latency; legt de zware encode op de sterke bron |
| 2 | Tooling | **scrcpy-server embedden + eigen dunne client (libadb-android)** — ⚠️ *herzien in v0.0.1-impl, was dadb* | Het geprivilegieerde, bewezen deel hergebruiken; alleen de lichte tablet-client zelf bouwen. dadb bleek géén Android 11+ draadloze pairing (SPAKE2+TLS) te kunnen en spreekt alleen plaintext-ADB → onbruikbaar voor de Wireless-Debugging-flow. libadb-android (MuntashirAkon) levert wél pairing+TLS+shell+sync, no-root. |
| 2b | Transport-modi | **USB-ADB-host erbij — `USB_BOOTSTRAP_WIFI` (primair) + `FULL_USB` (fallback) + bestaande `WIRELESS`, instelbaar** | Wireless Debugging kán niet aan terwijl de Z Fold zelf de hotspot is (vereist Wi-Fi-client). USB-debugging heeft die eis niet. Cartablet wordt USB-host: bootstrap stuurt `tcpip:5555` en gaat dan draadloos verder; full-USB draait de hele mirror over de kabel (snelst, geen netwerk). libadb levert geen USB-transport → **gevendord** (beslispunt 2 → lokale `:libadb`-module) en uitgebreid met een `AdbChannel`-seam; alle USB-specifieke code (`UsbAdbChannel`/`UsbAdbHost`) zit in de app. De codec/handshake/multiplexer van libadb worden ongewijzigd hergebruikt. |
| 3 | Transport + codec | **scrcpy-native TCP via ADB-tunnel · H.264 · 1024px / 2 Mbps / 24 fps · low-latency** | Lokaal tether-net is schoon → TCP volstaat; H.264 = universeel HW-decode; downscale aan de bron ontlast beide kanten |
| 4 | Discovery + pairing | **mDNS/NSD autodiscovery + gateway-IP-hint + eenmalig pairen** | Precies hoe Android Studio draadloze toestellen vindt; pairing-sleutel blijft vertrouwd |
| 5 | Scope v0.0.1 | **view + touch + keyboard (alles-in-één)**; audio uitgesteld/optioneel | Volledige eis meteen; audio kan via BT-A2DP naar de autoradio i.p.v. via de mirror |
| 6 | Ecosysteem | **sub-master `Meta_Auto`** | Groepeert de 5 in-auto-display-projecten |
| 7 | Visibility + licentie | **PUBLIC · AGPL-3.0 + NOTICE** | Breed herbruikbaar; scrcpy = Apache-2.0; libadb-android = dual GPL-3.0-or-later OR Apache-2.0 (beide AGPL-3.0-compatibel); conscrypt = Apache-2.0 |
| 8 | Naam + thema | **Android2AndroidMirror · teleoperatie-pioniers · v0.0.1-Tesla** | Thema raakt de USP (besturing op afstand) |

## Componenten (cartablet-client)

```
App (Conscrypt+PRNGFixes-init) · MainActivity ── ConnectionState (StateFlow-brug)
   │
   └─ session/MirrorSession    (coördinator: discovery→pair→connect→push→start→video+control)
        ├─ discovery/AdbDiscovery   (libadb AdbMdns: connect+pairing services + gateway-hint)
        ├─ adb/MirrorAdbManager     (AbsAdbConnectionManager: RSA-key + X509-cert, persistent)
        ├─ adb/AdbClient            (libadb: pair/connect/open + sync-push van de jar)
        ├─ scrcpy/ScrcpyServer      (jar pushen + app_process starten + dual-socket handshake)
        ├─ scrcpy/VideoStream       (H.264 frame-meta → MediaCodec low-latency → Surface)
        ├─ scrcpy/ControlChannel    (touch+key+text serialiseren → control-socket)
        ├─ input/TouchMapper        (tablet-coörd → bron-coörd, letterbox-correct)
        ├─ input/KeyboardBridge     (IME → INJECT_TEXT / INJECT_KEYCODE)
        └─ ui/{PairingScreen,MirrorScreen}
```

## Pairing/TLS-realiteit (v0.0.1-impl)

De Android 11+ Wireless-Debugging-flow is **TLS-only** en autoriseert een nieuw client-cert
via een **SPAKE2-pairing** met de 6-cijfercode. De oorspronkelijke tooling-keuze (dadb) kan
dit niet: dadb spreekt alleen plaintext-ADB en heeft geen pairing-API. Daarom is in de
implementatie overgestapt op **libadb-android** (MuntashirAkon, uit App Manager), dat de
volledige stack levert: `AbsAdbConnectionManager.pair()/connect()/openStream()` + ingebouwde
`AdbMdns`. Pushen van de scrcpy-server.jar doen we zelf via het ADB **sync**-protocol
(`AdbClient.pushServer`), want libadb heeft geen push-helper. Het client-cert wordt in
app-private opslag bewaard → pairen is écht eenmalig (beslispunt 4 intact).

## Bewuste niet-keuzes

- **Geen root** op de Z Fold 6 (Knox/bankapps) → daarom het ADB-shell-pad.
- **Geen WebRTC/relay** → overkill op een schoon lokaal tether-net; breekt de scrcpy-koppeling.
- **Geen MediaProjection in deze app** → capture gebeurt op de bron via de scrcpy-server.
- **Geen audio in v0.0.1** → waarschijnlijk overbodig (BT-A2DP naar autoluidsprekers).

## Bekende wrijving

Wireless debugging staat op OneUI vaak uit na reboot en kan niet zonder root
programmatisch aan → eenmalige toggle per rit (QS-tegel). Dit is het minimaal haalbare.
