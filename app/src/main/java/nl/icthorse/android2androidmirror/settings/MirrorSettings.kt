package nl.icthorse.android2androidmirror.settings

import android.content.Context

/**
 * Beslispunt 2b — instelbare transport-modus.
 *
 *  - [USB_BOOTSTRAP_WIFI] (default): cartablet als USB-host stuurt `tcpip:5555` naar de bron en
 *    gaat daarna draadloos verder over de Z-Fold-hotspot. USB-kabel mag erna los.
 *  - [FULL_USB]: de hele mirror loopt over de USB-kabel (geen netwerk, snelst).
 *  - [WIRELESS]: de oorspronkelijke Android 11+ pairing-flow (vereist dat de bron op een
 *    Wi-Fi-netwerk zit — werkt dus NIET als de Z Fold zelf de hotspot is).
 */
enum class MirrorMode { USB_BOOTSTRAP_WIFI, FULL_USB, WIRELESS }

object MirrorSettings {
    private const val PREFS = "a2am_settings"
    private const val KEY_MODE = "mirror_mode"

    fun mode(context: Context): MirrorMode {
        val name = context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_MODE, MirrorMode.USB_BOOTSTRAP_WIFI.name)
        return runCatching { MirrorMode.valueOf(name!!) }.getOrDefault(MirrorMode.USB_BOOTSTRAP_WIFI)
    }

    fun setMode(context: Context, mode: MirrorMode) {
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MODE, mode.name)
            .apply()
    }
}
