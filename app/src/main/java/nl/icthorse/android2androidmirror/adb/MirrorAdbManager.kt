package nl.icthorse.android2androidmirror.adb

import android.content.Context
import android.os.Build
import android.sun.misc.BASE64Encoder
import android.sun.security.provider.X509Factory
import android.sun.security.x509.AlgorithmId
import android.sun.security.x509.CertificateAlgorithmId
import android.sun.security.x509.CertificateExtensions
import android.sun.security.x509.CertificateIssuerName
import android.sun.security.x509.CertificateSerialNumber
import android.sun.security.x509.CertificateSubjectName
import android.sun.security.x509.CertificateValidity
import android.sun.security.x509.CertificateVersion
import android.sun.security.x509.CertificateX509Key
import android.sun.security.x509.KeyIdentifier
import android.sun.security.x509.PrivateKeyUsageExtension
import android.sun.security.x509.SubjectKeyIdentifierExtension
import android.sun.security.x509.X500Name
import android.sun.security.x509.X509CertImpl
import android.sun.security.x509.X509CertInfo
import io.github.muntashirakon.adb.AbsAdbConnectionManager
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Date
import java.util.Random

/**
 * Concrete [AbsAdbConnectionManager] voor de cartablet-client (beslispunt 2, herzien).
 *
 * libadb-android levert de hele ADB-TLS-stack (pairing/connect/shell/sync); wij hoeven
 * alleen het langlevende client-keypair + zelfondertekend X509-cert te leveren waarmee de
 * Z Fold 6 ons na de eenmalige pairing blijft vertrouwen. Key + cert worden in app-private
 * opslag bewaard, zodat pairen écht eenmalig is (beslispunt 4).
 *
 * Dit is een 1-op-1 Kotlin-port van het officiële libadb-voorbeeld (AdbConnectionManager),
 * licentie GPL-3.0-or-later OR Apache-2.0.
 */
class MirrorAdbManager private constructor(context: Context) : AbsAdbConnectionManager() {

    private val clientKey: PrivateKey
    private val clientCert: Certificate

    init {
        // De server-kant (scrcpy-server start via shell) is API-afhankelijk; geef libadb de
        // SDK-versie van de BRON niet — we kennen die niet vooraf. De API-waarde stuurt vooral
        // het ADB-protocol-niveau van de client; SDK_INT van de tablet volstaat als ondergrens.
        api = Build.VERSION.SDK_INT

        val storedKey = readPrivateKey(context)
        val storedCert = readCertificate(context)
        if (storedKey != null && storedCert != null) {
            clientKey = storedKey
            clientCert = storedCert
        } else {
            val keyPairGenerator = KeyPairGenerator.getInstance("RSA").apply {
                initialize(2048, SecureRandom.getInstance("SHA1PRNG"))
            }
            val generated = keyPairGenerator.generateKeyPair()
            clientKey = generated.private

            val subject = "CN=Android2AndroidMirror"
            val algorithmName = "SHA512withRSA"
            // Eén jaar geldig — pairing-vertrouwen op het toestel overleeft de certvervaldatum.
            val expiry = System.currentTimeMillis() + 365L * 86_400_000L
            val notBefore = Date()
            val notAfter = Date(expiry)
            val x500Name = X500Name(subject)

            val extensions = CertificateExtensions().apply {
                set(
                    "SubjectKeyIdentifier",
                    SubjectKeyIdentifierExtension(KeyIdentifier(generated.public).identifier),
                )
                set("PrivateKeyUsage", PrivateKeyUsageExtension(notBefore, notAfter))
            }
            val certInfo = X509CertInfo().apply {
                set("version", CertificateVersion(2))
                set("serialNumber", CertificateSerialNumber(Random().nextInt() and Int.MAX_VALUE))
                set("algorithmID", CertificateAlgorithmId(AlgorithmId.get(algorithmName)))
                set("subject", CertificateSubjectName(x500Name))
                set("key", CertificateX509Key(generated.public))
                set("validity", CertificateValidity(notBefore, notAfter))
                set("issuer", CertificateIssuerName(x500Name))
                set("extensions", extensions)
            }
            val cert = X509CertImpl(certInfo)
            cert.sign(clientKey, algorithmName)
            clientCert = cert

            writePrivateKey(context, clientKey)
            writeCertificate(context, cert)
        }
    }

    override fun getPrivateKey(): PrivateKey = clientKey
    override fun getCertificate(): Certificate = clientCert
    override fun getDeviceName(): String = "Android2AndroidMirror"

    companion object {
        @Volatile
        private var instance: MirrorAdbManager? = null

        fun getInstance(context: Context): MirrorAdbManager =
            instance ?: synchronized(this) {
                instance ?: MirrorAdbManager(context.applicationContext).also { instance = it }
            }

        private fun certFile(context: Context) = File(context.filesDir, "adb-cert.pem")
        private fun keyFile(context: Context) = File(context.filesDir, "adb-private.key")

        private fun readCertificate(context: Context): Certificate? {
            val f = certFile(context)
            if (!f.exists()) return null
            return f.inputStream().use {
                CertificateFactory.getInstance("X.509").generateCertificate(it)
            }
        }

        private fun writeCertificate(context: Context, certificate: Certificate) {
            certFile(context).outputStream().use { os ->
                os.write(X509Factory.BEGIN_CERT.toByteArray(StandardCharsets.UTF_8))
                os.write('\n'.code)
                BASE64Encoder().encode(certificate.encoded, os)
                os.write('\n'.code)
                os.write(X509Factory.END_CERT.toByteArray(StandardCharsets.UTF_8))
            }
        }

        private fun readPrivateKey(context: Context): PrivateKey? {
            val f = keyFile(context)
            if (!f.exists()) return null
            val bytes = f.readBytes()
            return KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(bytes))
        }

        private fun writePrivateKey(context: Context, privateKey: PrivateKey) {
            keyFile(context).outputStream().use { it.write(privateKey.encoded) }
        }
    }
}
