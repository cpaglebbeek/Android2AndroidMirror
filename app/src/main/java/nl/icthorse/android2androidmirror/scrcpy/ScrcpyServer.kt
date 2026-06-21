package nl.icthorse.android2androidmirror.scrcpy

import nl.icthorse.android2androidmirror.adb.AdbClient

/**
 * Beslispunt 2a — de officiële scrcpy-server.jar (Apache-2.0) op de BRON pushen en starten.
 * We herbouwen het geprivilegieerde deel NIET; we hergebruiken scrcpy's bewezen server.
 *
 * De jar wordt NIET in de repo gecommit. tools/fetch-scrcpy-server.sh haalt de juiste
 * versie op tijdens de build (zie app/src/main/assets/scrcpy-server/README.md).
 *
 * Beslispunt 3 — conservatief low-latency-profiel voor de zwakke decoder:
 *   max_size=1024  video_bit_rate=2M  max_fps=24  video_codec=h264  control=true
 */
object ScrcpyServer {

    /** Moet exact matchen met de gebundelde jar (zie assets/scrcpy-server/README.md). */
    const val SERVER_VERSION = "3.1"
    const val REMOTE_PATH = "/data/local/tmp/scrcpy-server.jar"
    const val ASSET_PATH = "scrcpy-server/scrcpy-server.jar"

    // Beslispunt 3 — defaults afgestemd op een trage Android-14 cartablet-decoder.
    data class Profile(
        val maxSize: Int = 1024,
        val videoBitRate: String = "2M",
        val maxFps: Int = 24,
        val videoCodec: String = "h264",
        val audio: Boolean = false,   // beslispunt 5 — audio uitgesteld/optioneel (BT-A2DP volstaat vaak)
        val control: Boolean = true,  // beslispunt 5b — touch+keyboard in v0.0.1
    )

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
            "cleanup=true"

    /**
     * Pusht de jar en start de server. Geeft de geopende shell-stream terug waaruit
     * VideoStream + ControlChannel hun sockets halen (tunnel_forward).
     */
    fun start(adb: AdbClient, jarBytes: ByteArray, profile: Profile = Profile()): Result<AutoCloseable> {
        adb.pushServer(jarBytes, REMOTE_PATH).onFailure { return Result.failure(it) }
        return adb.openShell(launchCommand(profile))
        // TODO(v0.0.1): video- en control-socket onderscheiden (scrcpy stuurt eerst de
        //   "dummy byte" + device-naam + video-header; daarna de tweede socket voor control).
    }
}
