package nl.icthorse.android2androidmirror.adb

import android.content.Context
import io.github.muntashirakon.adb.AdbChannel
import io.github.muntashirakon.adb.AdbStream
import java.io.OutputStream

/**
 * Dunne wrapper rond [MirrorAdbManager] (libadb-android) — beslispunt 1+2 (herzien).
 *
 * Biedt precies wat de scrcpy-pijplijn nodig heeft:
 *  - pair(host, pairPort, code)  → eenmalige Android 11+ draadloze pairing (SPAKE2+TLS)
 *  - connect(host, connectPort)  → TLS-ADB-verbinding (cert blijft daarna vertrouwd)
 *  - pushServer(bytes, path)     → scrcpy-server.jar via het ADB sync-protocol (libadb
 *                                  heeft geen push-helper → hier zelf het SEND-frame)
 *  - open(destination)           → ruwe AdbStream (shell-start + localabstract-sockets)
 *
 * Eenmaal verbonden draait de scrcpy-server onder de `shell`-UID op de BRON en mag die
 * `InputManager.injectInputEvent` aanroepen — touch+keyboard-teruginjectie zónder root.
 */
class AdbClient(context: Context) {

    private val manager = MirrorAdbManager.getInstance(context)

    val isConnected: Boolean get() = manager.isConnected

    /** Eenmalige pairing met de 6-cijferige wireless-debugging-code (beslispunt 4). */
    fun pair(host: String, pairPort: Int, code: String): Result<Unit> = runCatching {
        check(manager.pair(host, pairPort, code)) { "pairing geweigerd door de bron" }
    }

    /** TLS-verbinden met de (mDNS-ontdekte) connect-poort. */
    fun connect(host: String, connectPort: Int): Result<Unit> = runCatching {
        check(manager.connect(host, connectPort)) { "verbinden geweigerd (gepaird?)" }
    }

    /**
     * Beslispunt 2b — verbinden over een [AdbChannel]-transport (USB-ADB-host). libadb voert
     * dezelfde A_CNXN/A_AUTH-handshake uit; de eerste keer triggert dit op de bron de RSA-
     * "Sta debugging toe?"-dialoog. Geen TLS (adbd over USB stuurt A_AUTH, geen A_STLS).
     */
    fun connectUsb(channel: AdbChannel): Result<Unit> = runCatching {
        check(manager.connect(channel)) { "USB-verbinden geweigerd door de bron" }
    }

    /**
     * Beslispunt 2b — classic plaintext-ADB op een TCP-poort (bv. `gatewayHint():5555` na de
     * USB-bootstrap). Zelfde call als [connect]: libadb blijft plaintext zolang de daemon geen
     * A_STLS stuurt, en `tcp:5555`-adbd doet dat niet.
     */
    fun connectTcp(host: String, port: Int): Result<Unit> = runCatching {
        check(manager.connect(host, port)) { "TCP-verbinden geweigerd (autorisatie?)" }
    }

    /**
     * Beslispunt 2b — USB-bootstrap: zet de bron-adbd via USB in TCP-modus op [port] (zoals
     * `adb tcpip 5555`), zodat de adbd daarna óók op de hotspot-interface luistert. Verbindt over
     * het [channel], opent de host-service `tcpip:<port>` (dat levert de command af), leest de
     * statusregel best-effort, en verbreekt de USB-verbinding **zonder de sleutel te vernietigen**
     * (zodat de TCP-reconnect met dezelfde, reeds vertrouwde sleutel kan).
     *
     * @return de statusregel van de daemon (bv. "restarting in TCP mode port: 5555"), indien gelezen.
     */
    fun bootstrapTcpipOverUsb(channel: AdbChannel, port: Int = 5555): Result<String> = runCatching {
        check(manager.connect(channel)) { "USB-verbinden geweigerd door de bron" }
        try {
            val response = StringBuilder()
            manager.openStream("tcpip:$port").use { stream ->
                val buf = ByteArray(256)
                val inp = stream.openInputStream()
                // adbd schrijft één statusregel en sluit dan de stream → begrensde lees-lus.
                var n = inp.read(buf, 0, buf.size)
                while (n > 0) {
                    response.append(String(buf, 0, n, Charsets.UTF_8))
                    if (response.length >= 512) break
                    n = inp.read(buf, 0, buf.size)
                }
            }
            response.toString().trim()
        } finally {
            // GEEN close() — dat zou de private key vernietigen; alleen de verbinding sluiten.
            runCatching { manager.disconnect() }
        }
    }

    /** Verbreek de huidige verbinding maar behoud de sleutel (i.t.t. [close]). */
    fun disconnect(): Result<Unit> = runCatching { manager.disconnect() }

    /** Open een ruwe ADB-stream naar een willekeurige destination (bv. localabstract:scrcpy). */
    fun open(destination: String): AdbStream = manager.openStream(destination)

    /**
     * scrcpy-server.jar naar /data/local/tmp/ pushen via het ADB sync-protocol.
     * mode 0100644 (regulier bestand, rw-r--r--).
     */
    fun pushServer(jarBytes: ByteArray, remotePath: String, mode: Int = 33188): Result<Unit> =
        runCatching {
            manager.openStream("sync:").use { sync ->
                val out = sync.openOutputStream()
                val inp = sync.openInputStream()

                // SEND: id + le32(len) + "path,mode"
                val pathHeader = "$remotePath,$mode".toByteArray(Charsets.UTF_8)
                out.write("SEND".toByteArray(Charsets.UTF_8))
                out.writeLe32(pathHeader.size)
                out.write(pathHeader)

                // DATA: id + le32(chunkLen) + chunk  (max 64 KiB per chunk)
                var offset = 0
                while (offset < jarBytes.size) {
                    val chunk = minOf(SYNC_CHUNK, jarBytes.size - offset)
                    out.write("DATA".toByteArray(Charsets.UTF_8))
                    out.writeLe32(chunk)
                    out.write(jarBytes, offset, chunk)
                    offset += chunk
                }

                // DONE: id + le32(mtime in seconden)
                out.write("DONE".toByteArray(Charsets.UTF_8))
                out.writeLe32((System.currentTimeMillis() / 1000L).toInt())
                out.flush()

                // Antwoord: 8 bytes = id(4) + le32(len). "OKAY" = succes, "FAIL" = + foutmelding.
                val resp = inp.readFully(8)
                val id = String(resp, 0, 4, Charsets.UTF_8)
                if (id != "OKAY") {
                    val msgLen = resp.le32(4)
                    val msg = if (msgLen > 0) String(inp.readFully(msgLen), Charsets.UTF_8) else "onbekend"
                    error("sync push faalde ($id): $msg")
                }
            }
        }

    fun close() {
        runCatching { manager.close() }
    }

    private companion object {
        const val SYNC_CHUNK = 64 * 1024

        fun OutputStream.writeLe32(v: Int) {
            write(v and 0xFF)
            write((v ushr 8) and 0xFF)
            write((v ushr 16) and 0xFF)
            write((v ushr 24) and 0xFF)
        }

        fun ByteArray.le32(off: Int): Int =
            (this[off].toInt() and 0xFF) or
                ((this[off + 1].toInt() and 0xFF) shl 8) or
                ((this[off + 2].toInt() and 0xFF) shl 16) or
                ((this[off + 3].toInt() and 0xFF) shl 24)

        fun java.io.InputStream.readFully(n: Int): ByteArray {
            val buf = ByteArray(n)
            var read = 0
            while (read < n) {
                val r = read(buf, read, n - read)
                if (r < 0) error("ADB sync-stream onverwacht gesloten ($read/$n)")
                read += r
            }
            return buf
        }
    }
}
