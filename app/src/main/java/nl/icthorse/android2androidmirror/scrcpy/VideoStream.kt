package nl.icthorse.android2androidmirror.scrcpy

import android.media.MediaCodec
import android.media.MediaFormat
import android.view.Surface
import nl.icthorse.android2androidmirror.state.ConnectionState
import java.io.InputStream

/**
 * Beslispunt 3 — H.264 (AVC) decode op de zwakke cartablet: lees de scrcpy-videostream
 * (NAL-units) van de ADB-tunnel-socket, voer ze in een hardware MediaCodec-decoder die
 * rechtstreeks naar de SurfaceView rendert. Lichtste pad voor een trage decoder.
 *
 * Latency-keuzes: KEY_LOW_LATENCY=1, geen jitter-buffer, frames meteen renderen
 * (geen A/V-sync want audio loopt niet via dit pad — beslispunt 5).
 */
class VideoStream(
    private val input: InputStream,
    private val outputSurface: Surface,
) {
    private var codec: MediaCodec? = null

    fun start() {
        ConnectionState.setPhase(ConnectionState.Phase.STREAMING)
        // TODO(v0.0.1):
        //  1. scrcpy video-header lezen: codec-id + breedte + hoogte → ConnectionState.setSourceResolution()
        //  2. MediaCodec.createDecoderByType("video/avc"); format met KEY_LOW_LATENCY
        //  3. configure(format, outputSurface, null, 0); start()
        //  4. lus: NAL-frame uit input → dequeueInputBuffer → queueInputBuffer
        //          → dequeueOutputBuffer → releaseOutputBuffer(idx, render=true)
    }

    private fun lowLatencyFormat(width: Int, height: Int): MediaFormat =
        MediaFormat.createVideoFormat("video/avc", width, height).apply {
            setInteger(MediaFormat.KEY_LOW_LATENCY, 1) // API 30+; veilig genegeerd op ouder
        }

    fun stop() {
        codec?.run { stop(); release() }
        codec = null
    }
}
