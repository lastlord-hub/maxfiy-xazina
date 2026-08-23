package com.maxfiyxazina.app

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.os.PersistableBundle
import android.webkit.JavascriptInterface
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.getcapacitor.BridgeActivity

/**
 * MainActivity - Capacitor asosidagi asosiy ekran.
 *
 * Bu klass JavaScript kodimizga (www/index.html ichida) "AndroidBridge" nomli
 * obyekt orqali native imkoniyatlarga (Biometrika, Keystore, Clipboard, Autofill)
 * kirish imkonini beradi.
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
         * Kalitning o'zi qurilmaning xavfsiz hardware qismida (agar mavjud bo'lsa) qulflanadi
         * va faqat biometrika tasdiqlangandan keyin qaytarib olinishi mumkin.
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
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    val extras = PersistableBundle()
                    extras.putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
                    clip.description.extras = extras
                }
                clipboard.setPrimaryClip(clip)
            }
        }

        /**
         * Yozuvlar o'zgarganda Autofill xizmatiga signal beradi, u shifrlangan
         * ma'lumotni o'z DataStore'iga saqlaydi (parollarning o'zi hali ham
         * faqat Android Keystore orqali himoyalangan holda saqlanadi).
         */
        @JavascriptInterface
        fun syncAutofillData(entriesJson: String) {
            AutofillDataStore.saveEntries(applicationContext, entriesJson)
        }
    }
}
