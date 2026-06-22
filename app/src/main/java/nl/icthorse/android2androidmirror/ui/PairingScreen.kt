package nl.icthorse.android2androidmirror.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardOptions
import nl.icthorse.android2androidmirror.session.MirrorSession
import nl.icthorse.android2androidmirror.settings.MirrorMode
import nl.icthorse.android2androidmirror.settings.MirrorSettings
import nl.icthorse.android2androidmirror.state.ConnectionState

/**
 * Setup-scherm. Kiest eerst de transport-modus (beslispunt 2b) en toont daarna de bijpassende
 * flow: USB-modi krijgen één "Start"-knop; WIRELESS de mDNS-discovery + pairing-code.
 */
@Composable
fun PairingScreen() {
    val context = LocalContext.current
    val phase by ConnectionState.phase.collectAsState()
    val error by ConnectionState.error.collectAsState()

    var mode by remember { mutableStateOf(MirrorSettings.mode(context)) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Android2AndroidMirror — v0.0.2.2-Torres")
        Text("Status: $phase")
        error?.let { Text("Fout: $it") }

        ModeSelector(mode) { chosen ->
            mode = chosen
            MirrorSettings.setMode(context, chosen)
        }

        when (mode) {
            MirrorMode.WIRELESS -> WirelessFlow()
            else -> UsbFlow(mode)
        }
    }
}

@Composable
private fun ModeSelector(selected: MirrorMode, onSelect: (MirrorMode) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Verbindingsmodus")
            modeRow(selected, MirrorMode.USB_BOOTSTRAP_WIFI, "USB-bootstrap → wifi (aanbevolen)", onSelect)
            modeRow(selected, MirrorMode.FULL_USB, "Volledig over USB (kabel blijft)", onSelect)
            modeRow(selected, MirrorMode.WIRELESS, "Draadloos pairen (bron op wifi-netwerk)", onSelect)
        }
    }
}

@Composable
private fun modeRow(selected: MirrorMode, value: MirrorMode, label: String, onSelect: (MirrorMode) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().selectable(selected = selected == value, onClick = { onSelect(value) }),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected == value, onClick = { onSelect(value) })
        Text(label)
    }
}

@Composable
private fun UsbFlow(mode: MirrorMode) {
    val hint = when (mode) {
        MirrorMode.USB_BOOTSTRAP_WIFI ->
            "Sluit de Z Fold 6 met USB-C aan op de cartablet, zet USB-debugging aan, en zorg dat " +
                "de cartablet met de Z-Fold-hotspot verbonden is. Na de start mag de kabel los."
        MirrorMode.FULL_USB ->
            "Sluit de Z Fold 6 met USB-C aan op de cartablet en zet USB-debugging aan. De kabel " +
                "blijft tijdens het spiegelen aangesloten."
        else -> ""
    }
    Text(hint)
    Button(modifier = Modifier.fillMaxWidth(), onClick = { MirrorSession.startUsb() }) {
        Text("Start mirror (USB)")
    }
}

@Composable
private fun WirelessFlow() {
    val sources by ConnectionState.sources.collectAsState()
    var pairingCode by remember { mutableStateOf("") }

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
                    Text("connect: ${src.connectPort ?: "—"}   pairing: ${src.pairPort ?: "—"}")
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
