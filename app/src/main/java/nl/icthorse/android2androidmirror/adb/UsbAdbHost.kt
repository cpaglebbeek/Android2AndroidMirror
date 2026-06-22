package nl.icthorse.android2androidmirror.adb

import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import java.io.IOException

/**
 * Beslispunt 2b — bouwstenen om de cartablet als USB-ADB-host de Z Fold 6 te laten aanspreken.
 *
 * Enumeratie zoekt het standaard ADB-interfacedescriptor (class 0xFF / subclass 0x42 / protocol
 * 0x01) en de bijbehorende bulk IN/OUT-endpoints. [open] claimt de interface en levert een
 * [UsbAdbChannel] dat aan libadb's `connect(AdbChannel)` gevoerd kan worden.
 *
 * De USB-permissieflow (PendingIntent-broadcast + receiver) is contextgebonden en wordt in de
 * sessie-laag afgehandeld (F4); deze helper veronderstelt dat de permissie al verleend is.
 */
object UsbAdbHost {

    /** Standaard ADB-interfacedescriptor (zie AOSP `lib/usb/usb_linux.cpp`). */
    private const val ADB_CLASS = 0xFF        // UsbConstants.USB_CLASS_VENDOR_SPEC
    private const val ADB_SUBCLASS = 0x42
    private const val ADB_PROTOCOL = 0x01

    /** Het eerste aangesloten USB-device dat een ADB-interface aanbiedt, of null. */
    fun findAdbDevice(usbManager: UsbManager): UsbDevice? =
        usbManager.deviceList.values.firstOrNull { findAdbInterface(it) != null }

    /** De ADB-interface van een device, of null als die er niet is. */
    fun findAdbInterface(device: UsbDevice): UsbInterface? {
        for (i in 0 until device.interfaceCount) {
            val iface = device.getInterface(i)
            if (iface.interfaceClass == ADB_CLASS &&
                iface.interfaceSubclass == ADB_SUBCLASS &&
                iface.interfaceProtocol == ADB_PROTOCOL
            ) {
                return iface
            }
        }
        return null
    }

    /**
     * Claim de ADB-interface en open een [UsbAdbChannel]. Vereist dat de app al
     * USB-permissie voor [device] heeft (anders gooit [UsbManager.openDevice] / is permissie nodig).
     *
     * @throws IOException als de interface/endpoints ontbreken of de claim faalt.
     */
    fun open(usbManager: UsbManager, device: UsbDevice): UsbAdbChannel {
        val iface = findAdbInterface(device)
            ?: throw IOException("Geen ADB-interface op dit USB-device (Wireless/USB-debugging aan?)")

        var bulkIn: UsbEndpoint? = null
        var bulkOut: UsbEndpoint? = null
        for (e in 0 until iface.endpointCount) {
            val ep = iface.getEndpoint(e)
            if (ep.type != UsbConstants.USB_ENDPOINT_XFER_BULK) continue
            if (ep.direction == UsbConstants.USB_DIR_IN) bulkIn = ep else bulkOut = ep
        }
        if (bulkIn == null || bulkOut == null) {
            throw IOException("ADB-interface mist bulk IN/OUT-endpoints")
        }

        val connection = usbManager.openDevice(device)
            ?: throw IOException("Kon USB-device niet openen (permissie verleend?)")
        if (!connection.claimInterface(iface, true)) {
            connection.close()
            throw IOException("Kon de ADB-interface niet claimen (in gebruik door een andere host?)")
        }
        return UsbAdbChannel(connection, iface, bulkIn, bulkOut)
    }
}
