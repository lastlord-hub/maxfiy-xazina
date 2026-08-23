package com.maxfiyxazina.app

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.PersistableBundle
import android.provider.MediaStore
import android.webkit.JavascriptInterface
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.getcapacitor.BridgeActivity
import java.io.OutputStream

/**
 * MainActivity - Capacitor asosidagi asosiy ekran.
 *
 * Bu klass JavaScript kodimizga (www/index.html ichida) "AndroidBridge" nomli
 * obyekt orqali native imkoniyatlarga (Biometrika, Keystore, Clipboard, Autofill,
 * Fayl saqlash) kirish imkonini beradi.
 */
class MainActivity : BridgeActivity() {

    private lateinit var vaultKeystore: VaultKeystoreManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vaultKeystore = VaultKeystoreManager(applicationContext)

        // JavaScript'ga "AndroidBridge" nomli obyektni qo'shamiz
        bridge.webView.addJavascriptInterface(AndroidBridgeInterface(), "AndroidBridge")
    }

    inner class AndroidBridgeInterface {

        @JavascriptInterface
        fun isBiometricAvailable(): Boolean {
            val biometricManager = BiometricManager.from(applicationContext)
            return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
                    BiometricManager.BIOMETRIC_SUCCESS
        }

        @JavascriptInterface
        fun authenticateBiometric() {
            runOnUiThread {
                val executor = ContextCompat.getMainExecutor(applicationContext)
                val activity = this@MainActivity as FragmentActivity

                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Xazinani ochish")
                    .setSubtitle("Barmoq izingizni tasdiqlang")
                    .setNegativeButtonText("Bekor qilish")
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                    .build()

                val biometricPrompt = BiometricPrompt(activity, executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            super.onAuthenticationSucceeded(result)
                            bridge.webView.post {
                                bridge.webView.evaluateJavascript(
                                    "window.biometricAuthCallback && window.biometricAuthCallback(true)", null
                                )
                            }
                        }

                        override fun onAuthenticationFailed() {
                            super.onAuthenticationFailed()
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            super.onAuthenticationError(errorCode, errString)
                            bridge.webView.post {
                                bridge.webView.evaluateJavascript(
                                    "window.biometricAuthCallback && window.biometricAuthCallback(false)", null
                                )
                            }
                        }
                    })

                biometricPrompt.authenticate(promptInfo)
            }
        }

        /**
         * Shifrlash kalitini Android Keystore orqali himoyalangan holda saqlaydi.
         */
        @JavascriptInterface
        fun saveEncryptionKey(rawKeyB64: String) {
            vaultKeystore.saveVaultKey(rawKeyB64)
        }

        @JavascriptInterface
        fun getEncryptionKey() {
            runOnUiThread {
                val keyB64 = vaultKeystore.getVaultKey()
                bridge.webView.post {
                    bridge.webView.evaluateJavascript(
                        "window.keyRetrievalCallback && window.keyRetrievalCallback(${
                            if (keyB64 != null) "\"$keyB64\"" else "null"
                        })", null
                    )
                }
            }
        }

        /**
         * Clipboard'ga nusxalaydi. Android 13+ da tizim clipboard-ni "sensitive"
         * deb belgilaydi, shu bilan skrinshotlarda va clipboard tarixida ko'rinmaydi.
         */
        @JavascriptInterface
        fun copyToClipboard(text: String) {
            runOnUiThread {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("password", text)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val extras = PersistableBundle()
                    extras.putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
                    clip.description.extras = extras
                }
                clipboard.setPrimaryClip(clip)
            }
        }

        /**
         * Yozuvlar o'zgarganda Autofill xizmatiga signal beradi.
         */
        @JavascriptInterface
        fun syncAutofillData(entriesJson: String) {
            AutofillDataStore.saveEntries(applicationContext, entriesJson)
        }

        /**
         * Shifrlangan zaxira faylini qurilmaning ko'rinadigan "Downloads" (Yuklamalar)
         * papkasiga saqlaydi. MediaStore API orqali ishlaydi (Android 10+ scoped storage
         * talablariga mos), qo'shimcha ruxsat so'ralishi shart emas.
         */
        @JavascriptInterface
        fun saveFileToDownloads(fileName: String, content: String): Boolean {
            return try {
                val resolver = applicationContext.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }
                }

                val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                } else {
                    @Suppress("DEPRECATION")
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val file = java.io.File(downloadsDir, fileName)
                    android.net.Uri.fromFile(file)
                }

                if (uri == null) return false

                val outputStream: OutputStream? = resolver.openOutputStream(uri)
                outputStream?.use { it.write(content.toByteArray(Charsets.UTF_8)) }
                outputStream != null
            } catch (e: Exception) {
                false
            }
        }
    }
}
