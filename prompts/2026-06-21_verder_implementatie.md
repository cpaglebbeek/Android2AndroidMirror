# Sessie 2026-06-21 — "verder met android2androidmirror" (implementatie v0.0.1)

**Trigger:** `verder met android2androidmirror` (hervat van de skeleton-sessie).
**Scope-akkoord (Christian):** volledige v0.0.1 end-to-end in één pass.

## WhatIf
- **Begrip:** skelet → werkende client (dadb pair/connect/push → scrcpy starten → H.264-decode
  → SurfaceView + touch + keyboard).
- **Plan:** TODO's invullen in afhankelijkheidsvolgorde + MirrorSession-coördinator toevoegen.
- **Impact:** ~9 files gewijzigd + 3 nieuw; geen versienaam-wijziging (0.0.1-Tesla), wel vc1→2.
- **Akkoord:** scope-keuze + (na bevinding) pad-keuze, beide expliciet bevestigd.

## 🔴 Bevinding tijdens uitvoering → architectuurpivot
Broninspectie van `dev.mobile:dadb:1.2.10` (repo gecloned naar /tmp): **dadb kan niet
draadloos pairen** — geen SPAKE2/TLS, geen `pair()`, alleen plaintext-ADB. Daarmee
onbruikbaar voor de Android 11+ Wireless-Debugging-flow (TLS-only). Beslispunt 2 botst met
beslispunt 4.

**Pad-keuze (Christian):** Pad B = vervang dadb door **libadb-android 3.1.1** (MuntashirAkon).
Geverifieerd via repo-clone: heeft `PairingConnectionCtx`/`PairingAuthCtx` (SPAKE2+TLS),
`AbsAdbConnectionManager.pair/connect/openStream`, ingebouwde `AdbMdns`. Dual-licensed
`GPL-3.0-or-later OR Apache-2.0` → AGPL-compatibel. Geen push-helper → ADB sync-SEND zelf.
Vereist sun-security-android 1.1 + Conscrypt 2.5.3 (+ provider-init in `App`).

## Resultaat
- Commit `45c14ba` op `main`, gepusht. vc1→2, stage skeleton→implemented (build-blind).
- 14 Kotlin-files; nieuw: App, adb/MirrorAdbManager, session/MirrorSession.
- Docs: ARCHITECTURE (besl. 2+7 herzien + pairing/TLS-sectie), BUGLIST (B1 + R7-R10),
  RELEASES/version, NOTICE, README, CLAUDE.md.

## Openstaand
Build + e2e op **Mac** (HC55 heeft geen SDK). **Hardware-smoke verplicht** — multi-laag
crypto-stack (Conscrypt + SPAKE2-pairing): bugs duiken laag-voor-laag pas op echt toestel op
(zie memory feedback_android_crypto_layer_peel). Verifieer scrcpy v3.1 handshake/framing (R7)
+ Conscrypt-init (R8).
