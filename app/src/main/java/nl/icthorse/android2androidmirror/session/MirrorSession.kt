package nl.icthorse.android2androidmirror.session

import android.content.Context
import android.view.MotionEvent
import android.view.Surface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import nl.icthorse.android2androidmirror.adb.AdbClient
import nl.icthorse.android2androidmirror.discovery.AdbDiscovery
import nl.icthorse.android2androidmirror.input.KeyboardBridge
import nl.icthorse.android2androidmirror.input.TouchMapper
import nl.icthorse.android2androidmirror.scrcpy.ControlChannel
import nl.icthorse.android2androidmirror.scrcpy.ScrcpyServer
import nl.icthorse.android2androidmirror.scrcpy.VideoStream
import nl.icthorse.android2androidmirror.state.ConnectionState

/**
 * Centrale coördinator (cartablet). Knoopt de lagen aan elkaar:
 *   discovery → (pair) → connect → jar pushen → server starten → video-decode + control.
 *
 * Singleton, geïnitialiseerd met de applicationContext in MainActivity. De UI (PairingScreen /
 * MirrorScreen) praat alleen met dit object; de netwerk-/decode-details blijven hier.
 */
object MirrorSession {

    private lateinit var appContext: Context
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var discovery: AdbDiscovery? = null
    private var adb: AdbClient? = null
    private var pipeline: ScrcpyServer.Pipeline? = null
    private var video: VideoStream? = null

    private val touchMapper = TouchMapper()
    private var control: ControlChannel? = null
    private var keyboard: KeyboardBridge? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    // ---- discovery --------------------------------------------------------

    fun startDiscovery() {
        if (discovery == null) discovery = AdbDiscovery(appContext)
        discovery?.start()
    }

    fun stopDiscovery() {
        discovery?.stop()
    }

    // ---- verbinden + starten ---------------------------------------------

    /**
     * Verbind met een ontdekte bron en start de mirror. Als [pairingCode] gegeven is en de
     * bron een pairing-poort adverteert, wordt eerst eenmalig gepaird (beslispunt 4).
     */
    fun connect(source: ConnectionState.Source, pairingCode: String?) {
        scope.launch {
            try {
                ConnectionState.setError(null)
                val client = AdbClient(appContext).also { adb = it }

                // 1) Eenmalige pairing (alleen indien nodig).
                if (!pairingCode.isNullOrBlank() && source.pairPort != null) {
                    ConnectionState.setPhase(ConnectionState.Phase.PAIRING)
                    client.pair(source.host, source.pairPort, pairingCode).getOrThrow()
                }

                // 2) TLS-verbinden met de connect-poort.
                val connectPort = source.connectPort
                    ?: error("geen connect-poort gevonden — laat 'Wireless debugging' aan en zoek opnieuw")
                ConnectionState.setPhase(ConnectionState.Phase.CONNECTING)
                client.connect(source.host, connectPort).getOrThrow()

                // 3) scrcpy-server.jar uit de assets pushen + starten + sockets opzetten.
                val jarBytes = readJarOrThrow()
                val pipe = ScrcpyServer.start(client, jarBytes).getOrThrow()
                pipeline = pipe

                control = ControlChannel(pipe.controlOut)
                keyboard = KeyboardBridge(control!!)
                touchMapper.updateSource(pipe.srcWidth, pipe.srcHeight)

                // Server→client control-berichten (clipboard e.d.) wegslurpen zodat de
                // control-socket niet volloopt en blokkeert.
                drainControl(pipe)

                stopDiscovery()
                // UI mag nu naar MirrorScreen; de video start zodra de Surface beschikbaar is.
                ConnectionState.setPhase(ConnectionState.Phase.STREAMING)
            } catch (t: Throwable) {
                ConnectionState.setError(t.message ?: t.javaClass.simpleName)
                disconnect()
            }
        }
    }

    private fun readJarOrThrow(): ByteArray = try {
        appContext.assets.open(ScrcpyServer.ASSET_PATH).use { it.readBytes() }
    } catch (t: Throwable) {
        error("scrcpy-server.jar ontbreekt — draai tools/fetch-scrcpy-server.sh vóór de build")
    }

    private fun drainControl(pipe: ScrcpyServer.Pipeline) {
        scope.launch {
            val buf = ByteArray(4096)
            runCatching {
                while (pipe.controlIn.read(buf) >= 0) { /* negeren */ }
            }
        }
    }

    // ---- Surface + input (vanuit MirrorScreen) ---------------------------

    fun attachSurface(surface: Surface) {
        val pipe = pipeline ?: return
        if (video != null) return
        video = VideoStream(pipe.videoIn, surface, pipe.srcWidth, pipe.srcHeight).also { it.start() }
    }

    fun detachSurface() {
        video?.stop()
        video = null
    }

    fun updateViewSize(width: Int, height: Int) = touchMapper.updateView(width, height)

    /** Tablet-touch → bron-coördinaten → scrcpy control. Single-pointer in v0.0.1. */
    fun onTouch(event: MotionEvent): Boolean {
        val ctrl = control ?: return false
        val pipe = pipeline ?: return false
        val src = touchMapper.map(event.x, event.y) ?: return true // tik in de letterbox-rand
        val action = when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> MotionEvent.ACTION_DOWN
            MotionEvent.ACTION_MOVE -> MotionEvent.ACTION_MOVE
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> MotionEvent.ACTION_UP
            else -> return true
        }
        val pressure = if (action == MotionEvent.ACTION_UP) 0f else 1f
        scope.launch {
            runCatching {
                ctrl.sendTouch(action, event.getPointerId(0).toLong(), src.first, src.second, pipe.srcWidth, pipe.srcHeight, pressure)
            }
        }
        return true
    }

    fun onText(text: String) = keyboard?.onText(text)
    fun onKey(action: Int, keycode: Int, metaState: Int = 0) = keyboard?.onKey(action, keycode, metaState)

    // ---- afsluiten --------------------------------------------------------

    fun disconnect() {
        detachSurface()
        runCatching { pipeline?.close() }
        pipeline = null
        control = null
        keyboard = null
        runCatching { adb?.close() }
        adb = null
        if (ConnectionState.phase.value != ConnectionState.Phase.ERROR) {
            ConnectionState.setPhase(ConnectionState.Phase.IDLE)
        }
    }
}
