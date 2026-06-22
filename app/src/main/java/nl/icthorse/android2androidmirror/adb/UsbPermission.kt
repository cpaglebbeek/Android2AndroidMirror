package nl.icthorse.android2androidmirror.adb

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.IOException
import kotlin.coroutines.resume

/**
 * Beslispunt 2b — runtime USB-permissie + open een [UsbAdbChannel]. De cartablet moet expliciete
 * toestemming krijgen om de Z-Fold-USB-interface te claimen; dat gaat via een PendingIntent-
 * broadcast (de standaard USB-host-flow). Daarna pas opent [UsbAdbHost] het channel.
 *
 * BUGLIST R14 — de RSA-"Sta debugging toe?"-dialoog verschijnt op de BRON bij de eerste connect,
 * niet hier; deze permissie is enkel de host-kant (cartablet claimt de USB-interface).
 */
object UsbPermission {
    private const val ACTION = "nl.icthorse.android2androidmirror.USB_PERMISSION"

    /**
     * Zoekt het ADB-USB-device, vraagt zo nodig permissie, en opent een [UsbAdbChannel].
     * Suspend: wacht op de gebruiker als de permissiedialoog verschijnt.
     */
    suspend fun acquireChannel(context: Context): UsbAdbChannel {
        val ctx = context.applicationContext
        val usbManager = ctx.getSystemService(Context.USB_SERVICE) as UsbManager
        val device = UsbAdbHost.findAdbDevice(usbManager)
            ?: throw IOException("Geen ADB-USB-device — kabel aangesloten en USB-debugging aan op de bron?")
        if (!usbManager.hasPermission(device)) {
            requestPermission(ctx, usbManager, device)
            if (!usbManager.hasPermission(device)) {
                throw IOException("USB-permissie geweigerd")
            }
        }
        return UsbAdbHost.open(usbManager, device)
    }

    private suspend fun requestPermission(context: Context, usbManager: UsbManager, device: UsbDevice) =
        suspendCancellableCoroutine<Unit> { cont ->
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(c: Context, intent: Intent) {
                    if (intent.action != ACTION) return
                    runCatching { context.unregisterReceiver(this) }
                    if (cont.isActive) cont.resume(Unit)
                }
            }
            ContextCompat.registerReceiver(
                context, receiver, IntentFilter(ACTION), ContextCompat.RECEIVER_NOT_EXPORTED,
            )
            val piFlags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
            val pi = PendingIntent.getBroadcast(
                context, 0, Intent(ACTION).setPackage(context.packageName), piFlags,
            )
            cont.invokeOnCancellation { runCatching { context.unregisterReceiver(receiver) } }
            usbManager.requestPermission(device, pi)
        }
}
