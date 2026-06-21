package nl.icthorse.android2androidmirror.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import nl.icthorse.android2androidmirror.state.ConnectionState

/**
 * Setup-/discovery-scherm. Toont via mDNS ontdekte bronnen (Z Fold 6) en, indien nog niet
 * gepaird, een veld voor de 6-cijferige wireless-debugging-code (beslispunt 4).
 *
 * TODO(v0.0.1): discovery starten (AdbDiscovery), pairing-code-veld, connect → ScrcpyServer.start().
 */
@Composable
fun PairingScreen() {
    val phase by ConnectionState.phase.collectAsState()
    val sources by ConnectionState.sources.collectAsState()
    val error by ConnectionState.error.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Android2AndroidMirror — v0.0.1-Tesla")
        Text("Status: $phase")
        error?.let { Text("Fout: $it") }

        Button(onClick = { /* TODO: AdbDiscovery(context).start() */ }) {
            Text("Zoek Z Fold 6 (autodiscovery)")
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(sources) { src ->
                Button(onClick = { /* TODO: pair/connect → start mirror */ }) {
                    Text("${src.name}  (${src.host})")
                }
            }
        }
    }
}
