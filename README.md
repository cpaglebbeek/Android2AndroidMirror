# Android2AndroidMirror

Live scherm-mirror **én besturing** van het ene Android-toestel naar het andere — zonder
root. De **bron** (een Samsung Z Fold 6) wordt vanaf een **trage Android-14 aftermarket
cartablet** bekeken én bediend (touch + on-screen keyboard), over de tethering-wifi van de
bron.

> Status: **v0.0.1-Tesla (versionCode 2)** — volledig geïmplementeerd en **compileert**:
> `./gradlew assembleDebug` is groen, de debug-APK is gebouwd (incl. `libspake2.so` +
> `libconscrypt_jni.so` + gebundelde `scrcpy-server.jar`). **Nog niet op een toestel getest** —
> end-to-end hardware-smoke (Z Fold 6 → cartablet) is de volgende stap. Tooling-pivot t.o.v.
> het skelet: **dadb → libadb-android** (dadb kon niet draadloos pairen; zie
> [BUGLIST.md](BUGLIST.md) B1). Zie ook [ARCHITECTURE.md](ARCHITECTURE.md) /
> [RELEASES.md](RELEASES.md).

## 1 APK — alleen op de cartablet; de bron heeft géén app

Er is **één APK**, en die hoort **alleen op de cartablet** (het trage doel dat kijkt + bestuurt).
De app hoeft niet te weten of hij "bron" of "doel" is: **hij is altijd het doel/de client**.
Er is dus geen rol-keuze of rol-detectie.

| | Bron = Z Fold 6 | Doel = cartablet |
|---|---|---|
| Onze APK? | ❌ nee | ✅ ja (deze app) |
| Wat draait er? | de via ADB gepushte **scrcpy-server.jar** onder shell-UID | onze client: discovery → pair → connect → jar pushen → server starten → decode → input terug |
| Gebruikersactie | alleen **Wireless debugging aan** (QS-tegel) | app installeren + verbinden |

De bron draait geen geïnstalleerde app: de zware capture/encode gebeurt door de
**scrcpy-server.jar** die als asset in onze APK zit (`assets/scrcpy-server/scrcpy-server.jar`),
op het moment van verbinden over ADB naar de Z Fold 6 wordt geschoven en daar onder de
shell-UID start (en zichzelf met `cleanup=true` weer opruimt). Juist daarom is no-root
haalbaar: we hoeven op de bron géén app met capture-/inject-rechten te installeren — de
scrcpy-server krijgt die rechten via de ADB-shell-UID.

## Hoe het werkt

Dit is het **scrcpy-model, no-root**:

1. De cartablet (deze app) ontdekt de Z Fold 6 via **mDNS/NSD** op het tether-netwerk.
2. Eenmalig **pairen** met de wireless-debugging-code (Android 11+). Daarna blijft de
   sleutel vertrouwd.
3. De app pusht de officiële **scrcpy-server.jar** naar de Z Fold 6 en start die onder de
   `shell`-UID via ADB — dat geeft het recht om input te injecteren, zónder root.
4. De server captured + encodeert H.264 op de **krachtige** bron; de **zwakke** tablet doet
   alleen lichte H.264-decode naar een `SurfaceView`.
5. Touch + keyboard van de tablet gaan via het scrcpy **control-protocol** terug en worden
   in de Z Fold 6 geïnjecteerd.

De zware last ligt dus op het sterke toestel; de tablet doet het lichte werk.

## Bouwen (op een machine met Android SDK, bv. de Mac)

```bash
./tools/fetch-scrcpy-server.sh      # haalt de Apache-2.0 scrcpy-server.jar (niet in repo)
./gradlew assembleDebug
```

Sideload de APK op de cartablet. Zet op de Z Fold 6 eenmalig **Wireless debugging** aan
(Ontwikkelaarsopties; tip: Quick-Settings-tegel).

## De enige handmatige rit-handeling

Wireless debugging kan niet zonder root programmatisch aan; je tikt die toggle bij
instappen één keer aan. Daarna pakt de tablet alles automatisch op.

## Licentie

[AGPL-3.0](LICENSE). Bevat/gebruikt scrcpy (Apache-2.0), libadb-android (GPL-3.0-or-later
OR Apache-2.0) en Conscrypt (Apache-2.0) — zie [NOTICE](NOTICE).

## Familie (ecosysteem `Meta_Auto`)

AutoMirror · Autoredirect · HorseOnTheGo · MirrorCast (archived) · **Android2AndroidMirror**
