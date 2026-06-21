package nl.icthorse.android2androidmirror.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import nl.icthorse.android2androidmirror.session.MirrorSession
import nl.icthorse.android2androidmirror.state.ConnectionState

/**
 * Setup-/discovery-scherm. Toont via mDNS ontdekte bronnen (Z Fold 6) en een veld voor de
 * 6-cijferige wireless-debugging-code. Beslispunt 4.
 *
 * Flow: "Zoek" → bron verschijnt → (eerste keer) code invullen → "Verbind". Na de eenmalige
 * pairing blijft het cert vertrouwd en is de code niet meer nodig.
 */
@Composable
fun PairingScreen() {
    val phase by ConnectionState.phase.collectAsState()
    val sources by ConnectionState.sources.collectAsState()
    val error by ConnectionState.error.collectAsState()

    var pairingCode by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Android2AndroidMirror — v0.0.1-Tesla")
        Text("Status: $phase")
        error?.let { Text("Fout: $it") }

        Button(onClick = { MirrorSession.startDiscovery() }) {
            Text("Zoek Z Fold 6 (autodiscovery)")
        }

        OutlinedTextField(
            value = pairingCode,
            onValueChange = { pairingCode = it.filter(Char::isDigit).take(6) },
            label = { Text("Pairing-code (eenmalig, 6 cijfers)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(sources) { src ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(src.name)
                        Text(
                            "connect: ${src.connectPort ?: "—"}   pairing: ${src.pairPort ?: "—"}",
                        )
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                MirrorSession.connect(
                                    source = src,
                                    pairingCode = pairingCode.ifBlank { null },
                                )
                            },
                        ) {
                            Text(if (src.pairPort != null && pairingCode.isNotBlank()) "Pair + verbind" else "Verbind")
                        }
                    }
                }
            }
        }
    }
}
