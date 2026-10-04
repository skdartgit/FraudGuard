package com.sanat.fraudguard

import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.provider.Telephony.Sms
import org.json.JSONArray
import java.util.concurrent.Executors

class IncomingSmsReceiver : BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        if (
            intent.action !=
            Telephony.Sms.Intents.SMS_DELIVER_ACTION
        ) {
            return
        }

        val pendingResult =
            goAsync()

        val appContext =
            context.applicationContext

        Executors.newSingleThreadExecutor().execute {

            try {

                val messages =
                    Telephony.Sms.Intents
                        .getMessagesFromIntent(intent)

                if (messages.isNullOrEmpty()) {
                    return@execute
                }

                val sender =
                    messages.firstOrNull()
                        ?.originatingAddress
                        ?: "Unknown"

                val body =
                    messages.joinToString("") {
                        it.messageBody ?: ""
                    }

                /*
                 * Because this app is the default SMS app,
                 * it is responsible for storing the message.
                 */
                try {

                    val values =
                        ContentValues().apply {

                            put(
                                Sms.ADDRESS,
                                sender
                            )

                            put(
                                Sms.BODY,
                                body
                            )

                            put(
                                Sms.DATE,
                                System.currentTimeMillis()
                            )

                            put(
                                Sms.READ,
                                0
                            )

                            put(
                                Sms.TYPE,
                                Sms.MESSAGE_TYPE_INBOX
                            )
                        }

                    appContext.contentResolver.insert(
                        Sms.Inbox.CONTENT_URI,
                        values
                    )

                } catch (_: Exception) {
                    // Do not allow storage problems to crash
                    // the SMS receiver.
                }

                val result =
                    FraudEngine.analyzeSms(
                        appContext,
                        sender,
                        body
                    )

                val score =
                    result.optInt(
                        "risk_score",
                        50
                    )

                val level =
                    result.optString(
                        "risk_level",
                        "UNKNOWN"
                    )

                val reasons =
                    result.optJSONArray(
                        "reasons"
                    )

                val reasonText =
                    if (reasons != null) {

                        buildString {

                            for (
                                i in 0 until reasons.length()
                            ) {

                                if (i > 0) {
                                    append(" • ")
                                }

                                append(
                                    reasons.optString(i)
                                )
                            }
                        }

                    } else {
                        "No additional information."
                    }

                FraudHistory.add(
                    appContext,
                    "SMS",
                    sender,
                    score,
                    level,
                    reasonText
                )

                if (score >= 50) {

                    val prefix =
                        when {
                            score >= 80 ->
                                "🚨 HIGH-RISK SMS"

                            else ->
                                "⚠️ SUSPICIOUS SMS"
                        }

                    NotificationHelper.alert(
                        appContext,
                        prefix,
                        "$sender — risk $score/100. $reasonText",
                        (sender + body).hashCode()
                    )
                }

            } finally {

                pendingResult.finish()
            }
        }
    }
}
