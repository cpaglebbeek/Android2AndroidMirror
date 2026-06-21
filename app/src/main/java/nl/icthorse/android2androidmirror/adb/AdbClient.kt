package nl.icthorse.android2androidmirror.adb

import android.content.Context
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
