package nl.icthorse.android2androidmirror.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import nl.icthorse.android2androidmirror.state.ConnectionState

/**
 * Beslispunt 4 — autodiscovery via mDNS/NSD over de Z Fold 6 tethering-wifi.
 *
 * Android adverteert wireless-debugging via twee mDNS-services:
 *   _adb-tls-connect._tcp  → de daadwerkelijke (willekeurige) debug-poort
 *   _adb-tls-pairing._tcp  → de pairing-poort (alleen zichtbaar tijdens "Koppel met code")
 *
 * Daarnaast: de Z Fold 6 IS de tethering-gateway, dus de default-gateway uit DHCP
 * is een gratis IP-hint (beslispunt 4b) als mDNS traag/uit is.
 *
 * TODO(v0.0.1): NsdManager.discoverServices() voor beide service-types; resultaten
 *   resolven naar host+poort en in ConnectionState.setSources() pushen.
 * TODO: gatewayHint() implementeren via ConnectivityManager.linkProperties.routes.
 */
class AdbDiscovery(context: Context) {

    private val nsd = context.getSystemService(Context.NSD_SERVICE) as NsdManager

    companion object {
        const val SERVICE_CONNECT = "_adb-tls-connect._tcp"
        const val SERVICE_PAIRING = "_adb-tls-pairing._tcp"
    }

    fun start() {
        ConnectionState.setPhase(ConnectionState.Phase.DISCOVERING)
        // TODO: nsd.discoverServices(SERVICE_CONNECT, NsdManager.PROTOCOL_DNS_SD, connectListener)
        //       nsd.discoverServices(SERVICE_PAIRING, NsdManager.PROTOCOL_DNS_SD, pairingListener)
    }

    fun stop() {
        // TODO: nsd.stopServiceDiscovery(...) voor beide listeners
    }

    /** Gratis IP-hint: de tethering-gateway = de bron (Z Fold 6). */
    fun gatewayHint(): String? {
        // TODO: ConnectivityManager → activeNetwork → linkProperties → routes → isDefaultRoute → gateway
        return null
    }

    @Suppress("unused")
    private fun NsdServiceInfo.toSource(): ConnectionState.Source =
        ConnectionState.Source(
            name = serviceName,
            host = host?.hostAddress ?: "",
            connectPort = port.takeIf { serviceType.contains("connect") },
            pairPort = port.takeIf { serviceType.contains("pairing") },
        )
}
