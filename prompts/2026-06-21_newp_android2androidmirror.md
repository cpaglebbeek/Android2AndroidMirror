# Sessie 2026-06-21 — newp "android2androidMirror"

**Trigger:** `verder met android2androidmirror` (hervat van sessie `b58b859d` waar `newp
"android2androidMirror"` Fase 0 + Fase 1 stap 1/2 had gedaan).

**Protocol:** newp = Fase 0 /verifyrules → Fase 1 beslispunten (ONE_BY_ONE §6b) → Fase 2
skeleton → Fase N+1 /sanitycheck.

## Begrip
- Bron = Z Fold 6 (krachtig). Doel = Android-14 aftermarket cartablet (traag).
- Z Fold 6 doet tethering; tablet hangt aan dat net.
- Eis: scherm zichtbaar **én** bestuurbaar (touch + on-screen keyboard) op de tablet, lage
  latency, lichte render op de zwakke tablet, autodiscovery.

## Fase 1 — beslissingen (akkoord Christian, één-per-response)
1. Control-pad → **scrcpy-model (wireless ADB, no-root)**
2. Tooling → **scrcpy-server embedden + dadb-client**
3. Transport+codec → **scrcpy-native TCP / H.264 / 1024px·2Mbps·24fps low-latency**
4. Discovery → **mDNS/NSD + gateway-hint + eenmalig pairen (QS-toggle)**
5. Scope v0.0.1 → **alles-in-één: view + touch + keyboard** (audio uitgesteld/optioneel)
6. Ecosysteem → **nieuwe sub-master `Meta_Auto`**
7. Visibility → **PUBLIC AGPL-3.0 + NOTICE** (scrcpy+dadb = Apache-2.0; eerdere GPL-aanname
   weerlegd)
8. Naam+thema → **Android2AndroidMirror** · teleoperatie-pioniers · **v0.0.1-Tesla**

## Fase 2 — skeleton (deze sessie)
- Repo `~/projects/Android2AndroidMirror` → GitHub `cpaglebbeek/Android2AndroidMirror` PUBLIC.
- Volledig broncode-skelet (componentgrenzen + TODO's), Gradle/Compose-config, dadb-dep,
  LICENSE(AGPL-3.0), NOTICE, ARCHITECTURE.md (8 besluiten), version.json, BUGLIST/RELEASES.
- Sub-master `Meta_Auto` aangemaakt + GitHub PUBLIC.
- PROJECTS.json: ecosystem `Meta_Auto` + repo-entry geregistreerd.
- **Geen APK** — HC55 heeft geen Android SDK; compile-verificatie + jar-fetch + APK = Mac-follow-up.

## Open / follow-up
- Mac: `fetch-scrcpy-server.sh` + `./gradlew assembleDebug` (compile-verificatie van het skelet).
- Implementatie v0.0.1-Tesla: dadb pair/connect/push → server starten → H.264-decode →
  SurfaceView → touch → keyboard (end-to-end Z Fold 6 → cartablet).
- Consolidatie: bestaande AutoMirror/Autoredirect/HorseOnTheGo-entries onder Meta_Auto
  hergroeperen (leven op de Mac).
- Fase N+1: /sanitycheck op de repo.
