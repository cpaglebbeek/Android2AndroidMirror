package nl.icthorse.android2androidmirror.adb

import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import io.github.muntashirakon.adb.AdbChannel
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * Beslispunt 2b — een [AdbChannel] bovenop een USB-ADB-host (de cartablet als OTG-host die de
 * Z Fold 6 als ADB-device aanspreekt). Levert de ruwe ADB-byte-stromen waarover libadb's
 * ongewijzigde A_CNXN/A_AUTH-handshake + stream-multiplexer draaien (geen TLS — adbd over USB
 * en op classic `tcp:5555` stuurt A_AUTH, geen A_STLS).
 *
 * Framing: ADB over USB is op device-niveau een seriële byte-stroom — adbd leest 24 header-bytes
 * en daarna `data_length` payload-bytes. We hoeven dus alleen de bytes in volgorde over de
 * bulk-endpoints te duwen; transfer-grenzen mogen vrij vallen. Grote berichten knippen we in
 * stukken van [MAX_BULK] (de historische Android-bulkTransfer-limiet).
 *
 * BUILD-BLIND (crypto-/transport-layer-peel): bulkTransfer-timeoutsemantiek, ZLP-gedrag en de
 * RSA-autorisatiedialoog zijn alleen op echte hardware te bewijzen. Zie BUGLIST R11–R14.
 */
class UsbAdbChannel(
    private val connection: UsbDeviceConnection,
    private val iface: UsbInterface,
    private val bulkIn: UsbEndpoint,
    private val bulkOut: UsbEndpoint,
) : AdbChannel {

    @Volatile private var open = true

    private val input = UsbInputStream()
    private val output = UsbOutputStream()

    override fun getInputStream(): InputStream = input
    override fun getOutputStream(): OutputStream = output
    override fun isConnected(): Boolean = open

    override fun close() {
        if (!open) return
        open = false
        // Deblokkeert een lopende bulkTransfer(IN) en geeft de interface terug.
        runCatching { connection.releaseInterface(iface) }
        runCatching { connection.close() }
    }

    /** Bufferende lees-stroom: vult uit één bulkTransfer en serveert byte-voor-byte/per-blok. */
    private inner class UsbInputStream : InputStream() {
        private val buf = ByteArray(MAX_BULK)
        private var pos = 0
        private var lim = 0

        override fun read(): Int {
            val one = ByteArray(1)
            val n = read(one, 0, 1)
            return if (n <= 0) -1 else one[0].toInt() and 0xFF
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            if (len == 0) return 0
            if (pos >= lim) {
                if (!fill()) return -1
            }
            val n = minOf(len, lim - pos)
            System.arraycopy(buf, pos, b, off, n)
            pos += n
            return n
        }

        /** Blokkeert tot er data is (timeout 0 = oneindig) of het transport sluit. */
        private fun fill(): Boolean {
            while (open) {
                val n = connection.bulkTransfer(bulkIn, buf, buf.size, READ_TIMEOUT_MS)
                if (n > 0) {
                    pos = 0; lim = n
                    return true
                }
                // n < 0: bij timeout 0 betekent dit doorgaans een echte fout/ontkoppeling.
                if (n < 0 && !open) return false
                if (n < 0) return false
                // n == 0: niets gelezen, opnieuw proberen zolang open.
            }
            return false
        }

        override fun available(): Int = lim - pos
    }

    /** Schrijf-stroom: elke write() is één volledig ADB-bericht; in [MAX_BULK]-stukken verzonden. */
    private inner class UsbOutputStream : OutputStream() {
        override fun write(b: Int) {
            write(byteArrayOf(b.toByte()), 0, 1)
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            if (!open) throw IOException("USB-channel gesloten")
            var sent = 0
            while (sent < len) {
                val chunk = minOf(MAX_BULK, len - sent)
                val tmp = if (off == 0 && sent == 0 && chunk == b.size) b
                else b.copyOfRange(off + sent, off + sent + chunk)
                val n = connection.bulkTransfer(bulkOut, tmp, chunk, WRITE_TIMEOUT_MS)
                if (n < 0) throw IOException("USB bulkTransfer(OUT) faalde (n=$n) na $sent/$len bytes")
                sent += n
            }
        }

        override fun flush() {
            // Niets te doen: bulkTransfer verzendt direct.
        }
    }

    private companion object {
        // Historische veilige bovengrens voor één UsbDeviceConnection.bulkTransfer (MAX_USBFS).
        const val MAX_BULK = 16384

        // 0 = oneindig blokkeren op de lees-endpoint (achtergrond-connectie-thread wil dit).
        const val READ_TIMEOUT_MS = 0

        // Eindige schrijf-timeout zodat een vastlopende OUT-endpoint niet eeuwig hangt.
        const val WRITE_TIMEOUT_MS = 5000
    }
}
