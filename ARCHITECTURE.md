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

## Beslispunten

| # | Onderwerp | Besluit | Reden |
|---|---|---|---|
| 1 | Control-/inputpad | **scrcpy-model (wireless ADB, no-root)** | Enige no-root pad met volledige touch+echt keyboard+laagste latency; legt de zware encode op de sterke bron |
| 2 | Tooling | **scrcpy-server embedden + eigen dunne client (dadb)** | Het geprivilegieerde, bewezen deel hergebruiken; alleen de lichte tablet-client zelf bouwen |
| 3 | Transport + codec | **scrcpy-native TCP via ADB-tunnel · H.264 · 1024px / 2 Mbps / 24 fps · low-latency** | Lokaal tether-net is schoon → TCP volstaat; H.264 = universeel HW-decode; downscale aan de bron ontlast beide kanten |
| 4 | Discovery + pairing | **mDNS/NSD autodiscovery + gateway-IP-hint + eenmalig pairen** | Precies hoe Android Studio draadloze toestellen vindt; pairing-sleutel blijft vertrouwd |
| 5 | Scope v0.0.1 | **view + touch + keyboard (alles-in-één)**; audio uitgesteld/optioneel | Volledige eis meteen; audio kan via BT-A2DP naar de autoradio i.p.v. via de mirror |
| 6 | Ecosysteem | **sub-master `Meta_Auto`** | Groepeert de 5 in-auto-display-projecten |
| 7 | Visibility + licentie | **PUBLIC · AGPL-3.0 + NOTICE** | Breed herbruikbaar; scrcpy+dadb zijn Apache-2.0 (AGPL-compatibel) |
| 8 | Naam + thema | **Android2AndroidMirror · teleoperatie-pioniers · v0.0.1-Tesla** | Thema raakt de USP (besturing op afstand) |

## Componenten (cartablet-client)

```
MainActivity ── ConnectionState (StateFlow-brug) ──┐
   │                                                │
   ├─ discovery/AdbDiscovery   (mDNS/NSD + gateway-hint)
   ├─ adb/AdbClient            (dadb: pair/connect/push/shell)
   ├─ scrcpy/ScrcpyServer      (jar pushen + app_process starten, profiel bp3)
   ├─ scrcpy/VideoStream       (H.264 socket → MediaCodec → Surface)
   ├─ scrcpy/ControlChannel    (touch+key serialiseren → control-socket)
   ├─ input/TouchMapper        (tablet-coörd → bron-coörd, letterbox-correct)
   ├─ input/KeyboardBridge     (IME → INJECT_TEXT / INJECT_KEYCODE)
   └─ ui/{PairingScreen,MirrorScreen}
```

## Bewuste niet-keuzes

- **Geen root** op de Z Fold 6 (Knox/bankapps) → daarom het ADB-shell-pad.
- **Geen WebRTC/relay** → overkill op een schoon lokaal tether-net; breekt de scrcpy-koppeling.
- **Geen MediaProjection in deze app** → capture gebeurt op de bron via de scrcpy-server.
- **Geen audio in v0.0.1** → waarschijnlijk overbodig (BT-A2DP naar autoluidsprekers).

## Bekende wrijving

Wireless debugging staat op OneUI vaak uit na reboot en kan niet zonder root
programmatisch aan → eenmalige toggle per rit (QS-tegel). Dit is het minimaal haalbare.
