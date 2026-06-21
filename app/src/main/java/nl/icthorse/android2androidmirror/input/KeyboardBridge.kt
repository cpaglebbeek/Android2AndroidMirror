package nl.icthorse.android2androidmirror.input

import nl.icthorse.android2androidmirror.scrcpy.ControlChannel

/**
 * Beslispunt 5b — on-screen keyboard van de cartablet → key-events op de Z Fold 6.
 *
 * Aanpak: een onzichtbare, gefocuste invoer in MirrorScreen vangt IME-invoer. Tekst gaat
 * via het snelle INJECT_TEXT-pad; navigatie-/functietoetsen (Enter, Backspace, Tab, pijlen)
 * via INJECT_KEYCODE. De cartablet toont zijn eigen soft-keyboard; de BRON hoeft er geen
 * te tonen.
 */
class KeyboardBridge(private val control: ControlChannel) {

    /** Gewone tekst die de IME oplevert. */
    fun onText(text: String) {
        if (text.isNotEmpty()) control.sendText(text)
    }

    /** Speciale toetsen die niet als tekst kunnen (Android KeyEvent.KEYCODE_*). */
    fun onKey(action: Int, keycode: Int, metaState: Int = 0) {
        control.sendKey(action, keycode, metaState = metaState)
    }
}
