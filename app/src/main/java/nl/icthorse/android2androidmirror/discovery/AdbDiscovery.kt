package nl.icthorse.android2androidmirror.discovery

import android.content.Context
import android.net.ConnectivityManager
import io.github.muntashirakon.adb.android.AdbMdns
import nl.icthorse.android2androidmirror.state.ConnectionState
import java.net.InetAddress

/**
 * Beslispunt 4 — autodiscovery via mDNS over de Z Fold 6 tethering-wifi, nu bovenop de
 * mDNS-implementatie van libadb-android ([AdbMdns]).
 *
 * Android adverteert wireless-debugging via twee mDNS-services:
 *   _adb-tls-connect._tcp  → de daadwerkelijke (willekeurige) TLS-debug-poort
 *   _adb-tls-pairing._tcp  → de pairing-poort (alleen zichtbaar tijdens "Koppel met code")
 *
 * We draaien beide tegelijk en voegen de waarnemingen per host samen tot één
 * [ConnectionState.Source]: de connect-poort om te streamen, de pairing-poort om (eenmalig)
 * te koppelen. De Z Fold 6 IS de tethering-gateway, dus de default-gateway is een gratis
 * IP-hint (beslispunt 4b) als mDNS traag is.
 */
class AdbDiscovery(private val context: Context) {

    private val connectMdns = AdbMdns(context, AdbMdns.SERVICE_TYPE_TLS_CONNECT, ::onConnect)
    private val pairingMdns = AdbMdns(context, AdbMdns.SERVICE_TYPE_TLS_PAIRING, ::onPairing)

    // Laatste waarneming per host (host -> poort); -1/0 ⇒ verdwenen.
    private val connectPorts = HashMap<String, Int>()
    private val pairingPorts = HashMap<String, Int>()

    fun start() {
        ConnectionState.setPhase(ConnectionState.Phase.DISCOVERING)
        connectMdns.start()
        pairingMdns.start()
    }

    fun stop() {
        runCatching { connectMdns.stop() }
        runCatching { pairingMdns.stop() }
    }

    @Synchronized
    private fun onConnect(host: InetAddress?, port: Int) {
        update(connectPorts, host, port)
    }

    @Synchronized
    private fun onPairing(host: InetAddress?, port: Int) {
        update(pairingPorts, host, port)
    }

    private fun update(map: HashMap<String, Int>, host: InetAddress?, port: Int) {
        val ip = host?.hostAddress ?: return
        if (port > 0) map[ip] = port else map.remove(ip)
        publish()
    }

    private fun publish() {
        val hosts = (connectPorts.keys + pairingPorts.keys).toSortedSet()
        val sources = hosts.map { ip ->
            ConnectionState.Source(
                name = ip,
                host = ip,
                connectPort = connectPorts[ip],
                pairPort = pairingPorts[ip],
            )
        }
        ConnectionState.setSources(sources)
    }

    /** Gratis IP-hint: de tethering-gateway = de bron (Z Fold 6). */
    @Suppress("DEPRECATION")
    fun gatewayHint(): String? {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return null
        val network = cm.activeNetwork ?: return null
        val lp = cm.getLinkProperties(network) ?: return null
        return lp.routes
            .firstOrNull { it.isDefaultRoute && it.gateway != null }
            ?.gateway
            ?.hostAddress
    }
}
