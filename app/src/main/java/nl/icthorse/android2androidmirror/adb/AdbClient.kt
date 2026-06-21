package nl.icthorse.android2androidmirror.adb

/**
 * Beslispunt 1+2 — in-app ADB-client op de cartablet via dadb (dev.mobile:dadb, Apache-2.0).
 *
 * dadb spreekt het ADB-protocol rechtstreeks (geen adb-binary nodig):
 *  - pair(host, pairPort, code)  → eenmalige wireless-debugging-koppeling (Android 11+)
 *  - connect(host, connectPort)  → TLS-ADB-verbinding (sleutel blijft daarna vertrouwd)
 *  - push(localJar, remotePath)  → scrcpy-server.jar naar /data/local/tmp/
 *  - open("shell:...")           → app_process starten + video/control-sockets tunnelen
 *
 * De server draait daarmee onder de `shell`-UID en mag `InputManager.injectInputEvent`
 * aanroepen — precies wat touch+keyboard terug-injectie in de Z Fold 6 mogelijk maakt,
 * zónder root (beslispunt 1A).
 */
class AdbClient {

    // private var dadb: Dadb? = null   // dev.mobile.dadb.Dadb

    /** Eenmalige pairing met de 6-cijferige code (beslispunt 4). */
    fun pair(host: String, pairPort: Int, code: String): Result<Unit> {
        // TODO: AdbKeyPair.read/generate() in app-private opslag (herbruikbaar = nooit meer pairen)
        // TODO: Dadb.create(host, pairPort).pair(...) — exacte dadb-pairing-API
        return Result.failure(NotImplementedError("pair() — v0.0.1 skeleton"))
    }

    /** Verbinden met de (mDNS-ontdekte) connect-poort. */
    fun connect(host: String, connectPort: Int): Result<Unit> {
        // TODO: dadb = Dadb.create(host, connectPort, keyPair)
        return Result.failure(NotImplementedError("connect() — v0.0.1 skeleton"))
    }

    /** scrcpy-server.jar naar het toestel pushen. */
    fun pushServer(localJarBytes: ByteArray, remotePath: String): Result<Unit> {
        // TODO: dadb.push(source, remotePath, mode=0644)
        return Result.failure(NotImplementedError("pushServer() — v0.0.1 skeleton"))
    }

    /** Shell-commando openen (server starten) en de onderliggende stream teruggeven. */
    fun openShell(command: String): Result<AutoCloseable> {
        // TODO: dadb.open("shell:$command") → AdbStream voor video/control
        return Result.failure(NotImplementedError("openShell() — v0.0.1 skeleton"))
    }

    fun close() {
        // TODO: dadb?.close()
    }
}
