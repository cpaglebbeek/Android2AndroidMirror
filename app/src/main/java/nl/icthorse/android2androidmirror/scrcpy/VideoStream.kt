package nl.icthorse.android2androidmirror.scrcpy

import android.media.MediaCodec
import android.media.MediaFormat
import android.view.Surface
import nl.icthorse.android2androidmirror.state.ConnectionState
import java.io.DataInputStream
import java.io.InputStream

/**
 * Beslispunt 3 — H.264 (AVC) decode op de zwakke cartablet: lees de scrcpy-videostream van
 * de ADB-tunnel-socket en voer de packets in een hardware MediaCodec-decoder die rechtstreeks
 * naar de SurfaceView rendert. Lichtste pad voor een trage decoder.
 *
 * scrcpy frame-meta (send_frame_meta=true): elk packet vooraf 12 bytes (big-endian):
 *   pts(8)  — bit63 = CONFIG-packet (SPS/PPS), bit62 = keyframe, rest = pts in µs
 *   len(4)  — lengte van het volgende packet
 *
 * Latency-keuzes: KEY_LOW_LATENCY=1, geen jitter-buffer, frames meteen renderen (geen
 * A/V-sync want audio loopt niet via dit pad — beslispunt 5).
 */
class VideoStream(
    input: InputStream,
    private val outputSurface: Surface,
    private val width: Int,
    private val height: Int,
) {
    private val data = DataInputStream(input)
    private var codec: MediaCodec? = null
    @Volatile private var running = false
    private var thread: Thread? = null

    fun start() {
        if (running) return
        running = true
        thread = Thread({ runLoop() }, "scrcpy-video").apply { start() }
    }

    private fun runLoop() {
        try {
            ConnectionState.setPhase(ConnectionState.Phase.STREAMING)
            val mc = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            mc.configure(lowLatencyFormat(width, height), outputSurface, null, 0)
            mc.start()
            codec = mc

            val info = MediaCodec.BufferInfo()
            while (running) {
                // 12-byte frame-meta lezen.
                val pts = data.readLong()
                val len = data.readInt()
                val isConfig = (pts and PACKET_FLAG_CONFIG) != 0L
                val ptsUs = pts and PACKET_PTS_MASK

                val packet = ByteArray(len)
                data.readFully(packet)

                // Packet in een input-buffer schuiven (kan even wachten bij volle decoder).
                var inIndex: Int
                do {
                    inIndex = mc.dequeueInputBuffer(DEQUEUE_TIMEOUT_US)
                    drainOutput(mc, info)
                } while (inIndex < 0 && running)
                if (!running) break

                val inBuf = mc.getInputBuffer(inIndex)!!
                inBuf.clear()
                inBuf.put(packet)
                val flags = if (isConfig) MediaCodec.BUFFER_FLAG_CODEC_CONFIG else 0
                mc.queueInputBuffer(inIndex, 0, len, ptsUs, flags)

                drainOutput(mc, info)
            }
        } catch (_: InterruptedException) {
            // normale stop
        } catch (t: Throwable) {
            if (running) ConnectionState.setError("videostream: ${t.message}")
        } finally {
            stopCodec()
        }
    }

    /** Klaarstaande output-frames meteen naar de Surface renderen (render=true). */
    private fun drainOutput(mc: MediaCodec, info: MediaCodec.BufferInfo) {
        while (true) {
            val outIndex = mc.dequeueOutputBuffer(info, 0)
            if (outIndex < 0) break
            mc.releaseOutputBuffer(outIndex, true)
        }
    }

    private fun lowLatencyFormat(w: Int, h: Int): MediaFormat =
        MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, w, h).apply {
            setInteger(MediaFormat.KEY_LOW_LATENCY, 1) // API 30+; veilig genegeerd op ouder
        }

    fun stop() {
        running = false
        thread?.interrupt()
        thread = null
    }

    private fun stopCodec() {
        codec?.run { runCatching { stop() }; runCatching { release() } }
        codec = null
    }

    private companion object {
        const val PACKET_FLAG_CONFIG = 1L shl 63
        // bit62 = keyframe; voor een decoder zonder seek niet nodig om apart te behandelen.
        const val PACKET_PTS_MASK = (1L shl 62) - 1L
        const val DEQUEUE_TIMEOUT_US = 10_000L
    }
}
