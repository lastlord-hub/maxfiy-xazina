package com.maxfiyxazina.app

import android.app.PendingIntent
import android.app.assist.AssistStructure
import android.content.Intent
import android.os.Build
import android.os.CancellationSignal
import android.service.autofill.*
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import androidx.annotation.RequiresApi

/**
 * VaultAutofillService - Android'ning umumiy Autofill Framework'iga ulanadigan xizmat.
 *
 * Bu xizmat ishga tushganda (foydalanuvchi boshqa biror ilovada login/parol
 * maydoniga bosganda) Android bizga o'sha ekranning tuzilishini (AssistStructure)
 * yuboradi. Biz bu tuzilmani tekshirib, login/parol maydonlarini topamiz va
 * agar xazinamizda mos yozuv bo'lsa, uni tizim tepasida taklif sifatida ko'rsatamiz.
 *
 * MUHIM: Bu xizmat parollarning shifrlangan asl nusxasiga emas, balki
 * AutofillDataStore orqali saqlangan (ilova-darajasida izolyatsiya qilingan)
 * nusxaga kiradi. Xazina umuman ochilmagan bo'lsa ham, Autofill ma'lumotlari
 * foydalanuvchi oxirgi marta ilovani ochib, "saqlash"ga rozi bo'lgandan keyin
 * mavjud bo'ladi.
 */
@RequiresApi(Build.VERSION_CODES.O)
class VaultAutofillService : AutofillService() {

    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback
    ) {
        val structure = request.fillContexts.lastOrNull()?.structure
        if (structure == null) {
            callback.onSuccess(null)
            return
        }

        val fields = findAutofillFields(structure)
        if (fields.usernameId == null && fields.passwordId == null) {
            callback.onSuccess(null)
            return
        }

        val entries = AutofillDataStore.getEntries(applicationContext)
        if (entries.isEmpty()) {
            callback.onSuccess(null)
            return
        }

        val responseBuilder = FillResponse.Builder()

        // Har bir saqlangan yozuv uchun bitta taklif chipini yaratamiz
        entries.take(5).forEach { entry ->
            val presentation = RemoteViews(packageName, android.R.layout.simple_list_item_1)
            presentation.setTextViewText(android.R.id.text1, "🔑 ${entry.name} — ${entry.login}")

            val datasetBuilder = Dataset.Builder(presentation)
            fields.usernameId?.let { datasetBuilder.setValue(it, AutofillValue.forText(entry.login), presentation) }
            fields.passwordId?.let { datasetBuilder.setValue(it, AutofillValue.forText(entry.password), presentation) }

            responseBuilder.addDataset(datasetBuilder.build())
        }

        callback.onSuccess(responseBuilder.build())
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        // Foydalanuvchi boshqa ilovada YANGI login/parol kiritganda, Android bizdan
        // "buni saqlaysizmi?" deb so'raydi. Hozircha bu funksiya ma'lumotni asosiy
        // ilova ochilganda qo'lda qo'shishni talab qiladi (xavfsizlik uchun -
        // avtomatik saqlash tasdiqlashsiz amalga oshmasligi kerak).
        // Kengaytirish: bu yerga tizim "saqlash" dialogini ko'rsatib, asosiy ilovaga
        // signal yuborish logikasi qo'shilishi mumkin.
        callback.onSuccess()
    }

    private data class AutofillFields(
        var usernameId: AutofillId? = null,
        var passwordId: AutofillId? = null
    )

    private fun findAutofillFields(structure: AssistStructure): AutofillFields {
        val result = AutofillFields()
        for (i in 0 until structure.windowNodeCount) {
            val windowNode = structure.getWindowNodeAt(i)
            traverseNode(windowNode.rootViewNode, result)
        }
        return result
    }

    private fun traverseNode(node: AssistStructure.ViewNode, result: AutofillFields) {
        val hints = node.autofillHints
        if (hints != null) {
            for (hint in hints) {
                when (hint) {
                    android.view.View.AUTOFILL_HINT_USERNAME,
                    android.view.View.AUTOFILL_HINT_EMAIL_ADDRESS -> {
                        if (result.usernameId == null) result.usernameId = node.autofillId
                    }
                    android.view.View.AUTOFILL_HINT_PASSWORD -> {
                        if (result.passwordId == null) result.passwordId = node.autofillId
                    }
                }
            }
        } else {
            // Ba'zi ilovalar autofillHints belgilamaydi - inputType orqali taxmin qilamiz
            val inputType = node.inputType
            val isPasswordField = (inputType and android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD) != 0 ||
                    (inputType and android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD) != 0
            if (isPasswordField && result.passwordId == null) {
                result.passwordId = node.autofillId
            }
        }

        for (i in 0 until node.childCount) {
            traverseNode(node.getChildAt(i), result)
        }
    }
}
