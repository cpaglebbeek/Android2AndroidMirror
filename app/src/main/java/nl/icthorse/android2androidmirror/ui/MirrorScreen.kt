package nl.icthorse.android2androidmirror.ui

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.KeyEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import nl.icthorse.android2androidmirror.session.MirrorSession

/**
 * Live-scherm: fullscreen SurfaceView waarop de H.264-decoder rendert (VideoStream), met een
 * touch-listener die tikken via TouchMapper → ControlChannel terugstuurt, en een verborgen
 * EditText voor het on-screen keyboard (KeyboardBridge). Beslispunt 5b.
 *
 * v0.0.1: single-pointer touch + tekst via INJECT_TEXT, Enter/Backspace via INJECT_KEYCODE.
 * Multi-touch en pijl-/functietoetsen zijn follow-up (zie BUGLIST).
 */
@Composable
fun MirrorScreen() {
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx -> buildMirrorView(ctx) },
        )
        // Toetsenbord oproepen (de tablet toont zijn eigen soft-keyboard; de bron hoeft er geen).
        FloatingActionButton(
            onClick = { showKeyboard(activeImeTarget) },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) { Text("⌨") }
    }
}

private var activeImeTarget: EditText? = null

private fun buildMirrorView(ctx: Context): FrameLayout {
    val frame = FrameLayout(ctx)

    val surface = SurfaceView(ctx).apply {
        layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
        )
        holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                MirrorSession.attachSurface(holder.surface)
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                MirrorSession.updateViewSize(width, height)
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                MirrorSession.detachSurface()
            }
        })
        setOnTouchListener { _, event -> MirrorSession.onTouch(event) }
    }

    // Verborgen invoer die IME-tekst opvangt en als deltas doorstuurt.
    val ime = EditText(ctx).apply {
        layoutParams = FrameLayout.LayoutParams(1, 1, Gravity.BOTTOM)
        alpha = 0f
        isCursorVisible = false
        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (count > before) {
                    val added = s?.subSequence(start + before, start + count)?.toString()
                    if (!added.isNullOrEmpty()) MirrorSession.onText(added)
                } else if (before > count) {
                    // Verwijderde tekens → evenveel Backspace-keycodes.
                    repeat(before - count) {
                        MirrorSession.onKey(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL)
                        MirrorSession.onKey(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL)
                    }
                }
            }

            override fun afterTextChanged(s: Editable?) {
                if ((s?.length ?: 0) > 256) s?.clear() // buffer klein houden
            }
        })
        setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                MirrorSession.onKey(event.action, KeyEvent.KEYCODE_ENTER)
                true
            } else false
        }
    }
    activeImeTarget = ime

    frame.addView(surface)
    frame.addView(ime)
    return frame
}

private fun showKeyboard(target: EditText?) {
    val view = target ?: return
    view.requestFocus()
    val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
}
