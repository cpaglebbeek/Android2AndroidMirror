package nl.icthorse.android2androidmirror.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton pub/sub-brug tussen discovery, ADB-client, scrcpy-pipeline en UI.
 * Houdt de UI los van de netwerk-/decode-laag (zelfde patroon als CaptureState in AutoMirror).
 */
object ConnectionState {

    enum class Phase { IDLE, DISCOVERING, PAIRING, CONNECTING, PUSHING_SERVER, STREAMING, ERROR }

    /** Een ontdekte bron (Z Fold 6) op het tethering-netwerk. */
    data class Source(
        val name: String,
        val host: String,
        val connectPort: Int?,   // _adb-tls-connect._tcp (kan null zijn vóór pairing)
        val pairPort: Int?,      // _adb-tls-pairing._tcp (alleen tijdens pairing zichtbaar)
        val isGatewayHint: Boolean = false,
    )

    private val _phase = MutableStateFlow(Phase.IDLE)
    val phase: StateFlow<Phase> = _phase.asStateFlow()

    private val _sources = MutableStateFlow<List<Source>>(emptyList())
    val sources: StateFlow<List<Source>> = _sources.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /** Bron-resolutie zodra de scrcpy-server de video-header heeft gestuurd. */
    private val _sourceResolution = MutableStateFlow<Pair<Int, Int>?>(null)
    val sourceResolution: StateFlow<Pair<Int, Int>?> = _sourceResolution.asStateFlow()

    fun setPhase(p: Phase) { _phase.value = p }
    fun setSources(s: List<Source>) { _sources.value = s }
    fun setError(msg: String?) { _error.value = msg; if (msg != null) _phase.value = Phase.ERROR }
    fun setSourceResolution(w: Int, h: Int) { _sourceResolution.value = w to h }
}
