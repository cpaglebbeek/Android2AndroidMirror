package nl.icthorse.android2androidmirror.scrcpy

import java.io.OutputStream

/**
 * Beslispunt 5b — scrcpy control-protocol: serialiseer touch- en key-events vanaf de
 * cartablet en stuur ze over de control-socket terug naar de scrcpy-server, die ze via
 * InputManager in de Z Fold 6 injecteert.
 *
 * Het binaire protocol is compact en gedocumenteerd in scrcpy (app/src/control_msg.c):
 *   INJECT_TOUCH_EVENT (type=2): action(1) + pointerId(8) + x(4) + y(4) + w(2) + h(2)
 *                                + pressure(2) + actionButton(4) + buttons(4)
 *   INJECT_KEYCODE     (type=0): action(1) + keycode(4) + repeat(4) + metaState(4)
 *   INJECT_TEXT        (type=1): len(4) + utf8-bytes   (handig voor on-screen keyboard)
 */
class ControlChannel(private val out: OutputStream) {

    private companion object {
        const val TYPE_INJECT_KEYCODE: Byte = 0
        const val TYPE_INJECT_TEXT: Byte = 1
        const val TYPE_INJECT_TOUCH: Byte = 2
    }

    /** x/y al gemapt naar BRON-coördinaten (zie input/TouchMapper). */
    @Synchronized
    fun sendTouch(action: Int, pointerId: Long, x: Int, y: Int, srcW: Int, srcH: Int, pressure: Float) {
        // TODO(v0.0.1): bytes conform INJECT_TOUCH_EVENT samenstellen en out.write(...) + flush()
    }

    @Synchronized
    fun sendKey(action: Int, keycode: Int, repeat: Int = 0, metaState: Int = 0) {
        // TODO(v0.0.1): bytes conform INJECT_KEYCODE
    }

    /** Snel pad voor on-screen-keyboard-tekst (i.p.v. losse keycodes). */
    @Synchronized
    fun sendText(text: String) {
        // TODO(v0.0.1): bytes conform INJECT_TEXT (UTF-8)
    }
}
