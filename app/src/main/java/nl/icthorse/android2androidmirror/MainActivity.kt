package nl.icthorse.android2androidmirror

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import nl.icthorse.android2androidmirror.state.ConnectionState
import nl.icthorse.android2androidmirror.ui.MirrorScreen
import nl.icthorse.android2androidmirror.ui.PairingScreen

/**
 * Enige Activity (cartablet-client). Schakelt tussen PairingScreen (setup/discovery)
 * en MirrorScreen (live view+control) op basis van ConnectionState.phase.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { App() }
    }
}

@Composable
private fun App() {
    val phase by ConnectionState.phase.collectAsState()
    MaterialTheme {
        Surface {
            when (phase) {
                ConnectionState.Phase.STREAMING -> MirrorScreen()
                else -> PairingScreen()
            }
        }
    }
}
