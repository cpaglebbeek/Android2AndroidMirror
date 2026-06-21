package nl.icthorse.android2androidmirror.scrcpy

import java.io.DataOutputStream
import java.io.OutputStream

/**
 * Beslispunt 5b — scrcpy control-protocol: serialiseer touch- en key-events vanaf de
 * cartablet en stuur ze over de control-socket terug naar de scrcpy-server, die ze via
 * InputManager in de Z Fold 6 injecteert.
 *
 * Het binaire protocol is big-endian (network order), zie scrcpy `app/src/control_msg.c`:
 *   INJECT_KEYCODE     (type=0): action(1) + keycode(4) + repeat(4) + metaState(4)        = 14
 *   INJECT_TEXT        (type=1): len(4) + utf8-bytes (max 300)
 *   INJECT_TOUCH_EVENT (type=2): action(1) + pointerId(8) + x(4) + y(4) + w(2) + h(2)
 *                                + pressure(2, u16fp) + actionButton(4) + buttons(4)        = 32
 */
class ControlChannel(out: OutputStream) {

    private val data = DataOutputStream(out)

    /**
     * x/y al gemapt naar BRON-coördinaten (zie input/TouchMapper). srcW/srcH = de bron-frame-
     * grootte waar de coördinaten relatief aan zijn (uit de scrcpy video-header).
     *
     * @param action AMOTION_EVENT_ACTION_* (0=DOWN, 1=UP, 2=MOVE) — gelijk aan MotionEvent.
     */
    @Synchronized
    fun sendTouch(action: Int, pointerId: Long, x: Int, y: Int, srcW: Int, srcH: Int, pressure: Float) {
        data.writeByte(TYPE_INJECT_TOUCH)
        data.writeByte(action)
        data.writeLong(pointerId)
        data.writeInt(x)
        data.writeInt(y)
        data.writeShort(srcW)
        data.writeShort(srcH)
        data.writeShort(floatToU16Fp(pressure))
        data.writeInt(0)                 // actionButton — n.v.t. voor vinger-touch
        data.writeInt(if (action == ACTION_UP) 0 else BUTTON_PRIMARY) // buttons (down/move = primary)
        data.flush()
    }

    /** @param action KeyEvent.ACTION_DOWN(0) / ACTION_UP(1). keycode = Android KEYCODE_*. */
    @Synchronized
    fun sendKey(action: Int, keycode: Int, repeat: Int = 0, metaState: Int = 0) {
        data.writeByte(TYPE_INJECT_KEYCODE)
        data.writeByte(action)
        data.writeInt(keycode)
        data.writeInt(repeat)
        data.writeInt(metaState)
        data.flush()
    }

    /** Snel pad voor on-screen-keyboard-tekst (i.p.v. losse keycodes). */
    @Synchronized
    fun sendText(text: String) {
        if (text.isEmpty()) return
        var bytes = text.toByteArray(Charsets.UTF_8)
        if (bytes.size > TEXT_MAX) bytes = bytes.copyOf(TEXT_MAX) // scrcpy kapt op 300 bytes
        data.writeByte(TYPE_INJECT_TEXT)
        data.writeInt(bytes.size)
        data.write(bytes)
        data.flush()
    }

    private companion object {
        const val TYPE_INJECT_KEYCODE = 0
        const val TYPE_INJECT_TEXT = 1
        const val TYPE_INJECT_TOUCH = 2
        const val TEXT_MAX = 300
        const val ACTION_UP = 1            // MotionEvent.ACTION_UP
        const val BUTTON_PRIMARY = 1       // AMOTION_EVENT_BUTTON_PRIMARY

        /** scrcpy sc_float_to_u16fp: 0f→0, 1f→0xFFFF, lineair. */
        fun floatToU16Fp(value: Float): Int {
            val v = value.coerceIn(0f, 1f)
            val u = (v * 65536f).toInt()
            return if (u >= 0xFFFF) 0xFFFF else u
        }
    }
}
