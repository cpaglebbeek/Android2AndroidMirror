package nl.icthorse.android2androidmirror.scrcpy

import io.github.muntashirakon.adb.AdbStream
import nl.icthorse.android2androidmirror.adb.AdbClient
import nl.icthorse.android2androidmirror.state.ConnectionState
import java.io.DataInputStream
import java.io.InputStream
import java.io.OutputStream

/**
 * Beslispunt 2a — de officiële scrcpy-server.jar (Apache-2.0) op de BRON pushen, starten en
 * de video- + control-sockets opzetten. We herbouwen het geprivilegieerde deel NIET.
 *
 * De jar wordt NIET in de repo gecommit. tools/fetch-scrcpy-server.sh haalt de juiste versie
 * op tijdens de build (zie app/src/main/assets/scrcpy-server/README.md).
 *
 * Handshake (tunnel_forward + send_dummy_byte + send_device_meta + send_codec_meta + control):
 *  1. video-socket verbinden → 1 dummy-byte lezen (server-gereed-signaal van tunnel_forward)
 *  2. control-socket verbinden (tweede connectie naar dezelfde localabstract-socket)
 *  3. op de video-socket: 64-byte device-naam, dan codec-meta = codecId(4)+w(4)+h(4) (big-endian)
 *  4. daarna de frame-stream (zie VideoStream).
 */
object ScrcpyServer {

    /** Moet exact matchen met de gebundelde jar (zie assets/scrcpy-server/README.md). */
    const val SERVER_VERSION = "3.1"
    const val REMOTE_PATH = "/data/local/tmp/scrcpy-server.jar"
    const val ASSET_PATH = "scrcpy-server/scrcpy-server.jar"
    private const val SOCKET = "localabstract:scrcpy"
    private const val DEVICE_NAME_LEN = 64

    // Beslispunt 3 — defaults afgestemd op een trage Android-14 cartablet-decoder.
    data class Profile(
        val maxSize: Int = 1024,
        val videoBitRate: String = "2M",
        val maxFps: Int = 24,
        val videoCodec: String = "h264",
        val audio: Boolean = false,   // beslispunt 5 — audio uitgesteld/optioneel (BT-A2DP volstaat vaak)
        val control: Boolean = true,  // beslispunt 5b — touch+keyboard in v0.0.1
    )

    /** Opgezette pijplijn: alles wat VideoStream + ControlChannel nodig hebben, plus sluitbaar. */
    class Pipeline(
        val videoIn: InputStream,
        val controlOut: OutputStream,
        val controlIn: InputStream,
        val srcWidth: Int,
        val srcHeight: Int,
        private val shell: AdbStream,
        private val videoSocket: AdbStream,
        private val controlSocket: AdbStream,
    ) : AutoCloseable {
        override fun close() {
            runCatching { controlSocket.close() }
            runCatching { videoSocket.close() }
            runCatching { shell.close() }
        }
    }

    /** Bouwt de app_process-startregel zoals scrcpy die zelf gebruikt. */
    fun launchCommand(p: Profile = Profile()): String =
        "CLASSPATH=$REMOTE_PATH app_process / com.genymobile.scrcpy.Server $SERVER_VERSION " +
            "tunnel_forward=true " +
            "video_codec=${p.videoCodec} " +
            "max_size=${p.maxSize} " +
            "video_bit_rate=${p.videoBitRate} " +
            "max_fps=${p.maxFps} " +
            "audio=${p.audio} " +
            "control=${p.control} " +
            "send_device_meta=true " +
            "send_frame_meta=true " +
            "send_codec_meta=true " +
            "send_dummy_byte=true " +
            "raw_stream=false " +
            "clipboard_autosync=false " +
            "cleanup=true"

    /**
     * Pusht de jar, start de server en zet de sockets op. Geeft een [Pipeline] terug of een
     * fout. Werkt op de aanroepende (IO-)thread; mag blokkeren.
     */
    fun start(adb: AdbClient, jarBytes: ByteArray, profile: Profile = Profile()): Result<Pipeline> = runCatching {
        ConnectionState.setPhase(ConnectionState.Phase.PUSHING_SERVER)
        adb.pushServer(jarBytes, REMOTE_PATH).getOrThrow()

        // Server starten; de shell-stream blijft open zolang de server draait.
        val shell = adb.open("shell:${launchCommand(profile)}")

        // R7/diagnose: lees de server-stdout/stderr mee zodat een mislukte start (corrupte jar →
        // ClassNotFound, onbekende optie, SELinux, version-mismatch) zichtbaar wordt i.p.v. een
        // kale socket-time-out. Daemon-thread; blokkeert de pijplijn niet.
        val serverLog = StringBuilder()
        Thread {
            runCatching {
                val s = shell.openInputStream()
                val buf = ByteArray(1024)
                var n = s.read(buf)
                while (n >= 0 && serverLog.length < 4000) {
                    synchronized(serverLog) { serverLog.append(String(buf, 0, n)) }
                    n = s.read(buf)
                }
            }
        }.apply { isDaemon = true; start() }

        // tunnel_forward: de localabstract-socket bestaat pas zodra de server luistert.
        // Kort retried verbinden tot de video-socket er is.
        val videoSocket = try {
            connectWithRetry(adb)
        } catch (t: Throwable) {
            Thread.sleep(300) // geef de server-log even tijd om de fout te tonen
            val log = synchronized(serverLog) { serverLog.toString().trim() }
            val detail = if (log.isBlank()) "(geen server-output — startte app_process wel?)" else log
            throw IllegalStateException("scrcpy-server-socket onbereikbaar. Server-output:\n$detail", t)
        }
        val videoIn = DataInputStream(videoSocket.openInputStream())

        // 1) dummy-byte (server-gereed).
        videoIn.readByte()

        // 2) control-socket (tweede connectie naar dezelfde luisterende socket).
        val controlSocket = adb.open(SOCKET)
        val controlOut = controlSocket.openOutputStream()
        val controlIn = controlSocket.openInputStream()

        // 3) device-naam (64 bytes) overslaan + codec-meta lezen.
        videoIn.skipFully(DEVICE_NAME_LEN)
        videoIn.readInt()                 // codecId (we forceren H.264) — gelezen, niet gebruikt
        val width = videoIn.readInt()
        val height = videoIn.readInt()
        ConnectionState.setSourceResolution(width, height)

        Pipeline(videoIn, controlOut, controlIn, width, height, shell, videoSocket, controlSocket)
    }

    private fun connectWithRetry(adb: AdbClient): AdbStream {
        var last: Throwable? = null
        repeat(20) { attempt ->
            try {
                return adb.open(SOCKET)
            } catch (t: Throwable) {
                last = t
                Thread.sleep(150L + attempt * 50L)
            }
        }
        throw IllegalStateException("kon niet verbinden met de scrcpy-server-socket", last)
    }

    private fun DataInputStream.skipFully(n: Int) {
        var remaining = n
        while (remaining > 0) {
            val s = skip(remaining.toLong())
            if (s <= 0) { read(); remaining-- } else remaining -= s.toInt()
        }
    }
}
