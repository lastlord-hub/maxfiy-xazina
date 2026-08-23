package com.maxfiyxazina.app

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Bu klass foydalanuvchining bosh-parol-hosilasi shifrlash kalitini
 * Android Keystore'da saqlaydi. Android Keystore qurilmaning maxsus xavfsiz
 * hardware qismida (agar mavjud bo'lsa - StrongBox yoki TEE) ishlaydi,
 * ya'ni kalitning o'zi hech qachon oddiy xotiraga yoki diskka ochiq holda
 * yozilmaydi - faqat operatsion tizim darajasida himoyalangan.
 *
 * Muhim: bu kalit foydalanuvchining ASOSIY bosh-paroli emas, balki undan
 * hosil qilingan shifrlash kaliti. Biometrika faqat shu kalitni qayta
 * olish uchun "qulf" vazifasini bajaradi.
 */
class VaultKeystoreManager(private val context: Context) {

    private val ANDROID_KEYSTORE = "AndroidKeyStore"
    private val KEYSTORE_ALIAS = "maxfiy_xazina_wrapper_key"
    private val PREFS_NAME = "vault_keystore_prefs"
    private val WRAPPED_KEY_PREF = "wrapped_vault_key"
    private val IV_PREF = "wrapped_vault_key_iv"

    private fun getOrCreateWrapperKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)

        val existingKey = keyStore.getKey(KEYSTORE_ALIAS, null) as? SecretKey
        if (existingKey != null) return existingKey

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEYSTORE_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(true)
            .apply {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    setUserAuthenticationParameters(30, KeyProperties.AUTH_BIOMETRIC_STRONG)
                } else {
                    @Suppress("DEPRECATION")
                    setUserAuthenticationValidityDurationSeconds(30)
                }
            }
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    /**
     * Xazina shifrlash kalitini (rawKeyB64) Keystore'dagi wrapper kalit bilan
     * "o'raydi" (qayta shifrlaydi) va natijani SharedPreferences'da saqlaydi.
     * Wrapper kalitning o'zi hech qachon xotiradan chiqmaydi - u Keystore ichida qoladi.
     */
    fun saveVaultKey(rawKeyB64: String) {
        try {
            val wrapperKey = getOrCreateWrapperKey()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, wrapperKey)
            val iv = cipher.iv
            val encrypted = cipher.doFinal(Base64.decode(rawKeyB64, Base64.NO_WRAP))

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString(WRAPPED_KEY_PREF, Base64.encodeToString(encrypted, Base64.NO_WRAP))
                .putString(IV_PREF, Base64.encodeToString(iv, Base64.NO_WRAP))
                .apply()
        } catch (e: Exception) {
            // Agar saqlashda xato bo'lsa, biometrika funksiyasi shunchaki ishlamaydi -
            // foydalanuvchi bosh parol bilan kirishda davom etadi.
        }
    }

    /**
     * Saqlangan xazina kalitini qaytaradi. Bu chaqiriq faqat foydalanuvchi
     * biometrika orqali muvaffaqiyatli tasdiqlangandan keyin ishlatilishi kerak,
     * chunki wrapper kalit setUserAuthenticationRequired(true) bilan himoyalangan.
     */
    fun getVaultKey(): String? {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val wrappedB64 = prefs.getString(WRAPPED_KEY_PREF, null) ?: return null
            val ivB64 = prefs.getString(IV_PREF, null) ?: return null

            val wrapperKey = getOrCreateWrapperKey()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, Base64.decode(ivB64, Base64.NO_WRAP))
            cipher.init(Cipher.DECRYPT_MODE, wrapperKey, spec)
            val decrypted = cipher.doFinal(Base64.decode(wrappedB64, Base64.NO_WRAP))
            Base64.encodeToString(decrypted, Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    fun clearVaultKey() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}
