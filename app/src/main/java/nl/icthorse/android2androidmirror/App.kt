package nl.icthorse.android2androidmirror

import android.app.Application
import io.github.muntashirakon.adb.PRNGFixes
import org.conscrypt.Conscrypt
import java.security.Security

/**
 * Application-init voor de ADB-TLS-stack (libadb-android, beslispunt 2 herzien).
 *
 * Twee dingen MOETEN vóór het eerste pair/connect gebeuren:
 *  1. PRNGFixes.apply() — libadb's fix voor de zwakke SecureRandom op oudere Android-builds;
 *     de SPAKE2-pairing leunt op sterke randomness.
 *  2. Conscrypt als hoogste security-provider registreren. De libadb-README beveelt de
 *     custom Conscrypt aan wanneer je met een *remote* adbd TLS-verbindt (precies ons geval:
 *     de cartablet verbindt met de adbd van de Z Fold 6 over het tether-net), i.p.v. de
 *     platform-Conscrypt via hidden-API-bypass.
 */
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        PRNGFixes.apply()
        Security.insertProviderAt(Conscrypt.newProvider(), 1)
    }
}
