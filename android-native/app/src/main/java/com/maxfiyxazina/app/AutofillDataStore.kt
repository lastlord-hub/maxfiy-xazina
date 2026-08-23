package com.maxfiyxazina.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Autofill xizmati uchun yozuvlarni saqlaydigan yordamchi.
 *
 * MUHIM XAVFSIZLIK ESLATMASI:
 * Bu yerda parollar ILOVANING o'z SharedPreferences papkasida saqlanadi, u esa
 * Android tomonidan operatsion tizim darajasida boshqa ilovalardan izolyatsiya
 * qilingan (har bir ilova faqat o'z papkasiga kira oladi - "sandboxing").
 * Bu qo'shimcha shifrlash o'rnini bosmaydi, balki uni to'ldiradi: root qilinmagan
 * qurilmada boshqa hech qanday ilova bu faylni o'qiy olmaydi.
 *
 * Kelajakda kuchaytirish uchun: bu faylni ham qo'shimcha ravishda Keystore
 * kaliti bilan shifrlash mumkin (hozircha oddiy JSON, chunki fayl allaqachon
 * ilova-darajasida izolyatsiya qilingan va MainActivity orqali faqat foydalanuvchi
 * o'zi tasdiqlagan yozuvlarni yuboradi).
 */
object AutofillDataStore {
    private const val PREFS_NAME = "vault_autofill_store"
    private const val ENTRIES_KEY = "entries_json"

    fun saveEntries(context: Context, entriesJson: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(ENTRIES_KEY, entriesJson).apply()
    }

    fun getEntries(context: Context): List<AutofillEntry> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(ENTRIES_KEY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<AutofillEntry>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    AutofillEntry(
                        id = obj.optString("id"),
                        name = obj.optString("name"),
                        login = obj.optString("login"),
                        password = obj.optString("password")
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun clearEntries(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(ENTRIES_KEY).apply()
    }
}

data class AutofillEntry(
    val id: String,
    val name: String,
    val login: String,
    val password: String
)
