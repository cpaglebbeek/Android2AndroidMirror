# scrcpy-server.jar (NIET in de repo)

Dit project hergebruikt de officiële **scrcpy-server** (Apache-2.0) als de geprivilegieerde
helft die op de BRON (Z Fold 6) onder shell-UID draait (beslispunt 2a).

De jar wordt **niet** in git gecommit. Plaats hem hier vóór de build:

    app/src/main/assets/scrcpy-server/scrcpy-server.jar

Haal hem op met:

    ./tools/fetch-scrcpy-server.sh

**Vereiste versie:** moet exact matchen met `ScrcpyServer.SERVER_VERSION` (nu **3.1**).
De versie wordt als eerste arg aan `com.genymobile.scrcpy.Server` meegegeven; mismatch
laat de server weigeren te starten.

Bron: https://github.com/Genymobile/scrcpy/releases  (bestand `scrcpy-server-v3.1`)
