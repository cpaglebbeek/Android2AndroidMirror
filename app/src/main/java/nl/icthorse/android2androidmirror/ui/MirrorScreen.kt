package nl.icthorse.android2androidmirror.ui

import android.view.SurfaceView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Live-scherm: fullscreen SurfaceView waarop de H.264-decoder rendert (VideoStream),
 * met een touch-listener die tikken via TouchMapper → ControlChannel terugstuurt en
 * een onzichtbaar IME-veld voor de on-screen keyboard (KeyboardBridge). Beslispunt 5b.
 *
 * TODO(v0.0.1):
 *  - SurfaceHolder.Callback → Surface doorgeven aan VideoStream
 *  - setOnTouchListener → TouchMapper.map() → ControlChannel.sendTouch()
 *  - focusbaar EditText/InputConnection → KeyboardBridge
 */
@Composable
fun MirrorScreen() {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            SurfaceView(ctx).apply {
                // TODO: holder.addCallback(...) + setOnTouchListener(...)
            }
        },
    )
}
