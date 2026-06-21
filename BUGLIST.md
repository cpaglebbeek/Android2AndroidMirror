# Buglist — Android2AndroidMirror

(Vanaf dag 1 bijgehouden — les uit MirrorCast, waar dit retroactief moest.)

| ID | Datum | Ernst | Status | Omschrijving |
|----|-------|-------|--------|--------------|
| —  | —     | —     | —      | Nog geen bugs; skeleton-fase. |

## Verwachte risico's (te bewaken bij implementatie)

- **R1** OneUI zet Wireless Debugging uit na reboot → eenmalige toggle nodig (by design, geen bug).
- **R2** scrcpy-server-versie moet exact matchen met `ScrcpyServer.SERVER_VERSION`, anders weigert de server.
- **R3** dadb-pairing-API kan per versie verschillen — pin de dadb-versie.
- **R4** Trage decoder kan bij 1024px/24fps nog haperen → bitrate/res verder verlagen (bp3 is conservatief startpunt).
- **R5** Touch-coördinaten: letterbox/rotatie verkeerd mappen = tikken naast doel (zie TouchMapper).
- **R6** DRM/FLAG_SECURE-content op de bron blijft zwart in de mirror (scrcpy-eigenschap, geen workaround).
