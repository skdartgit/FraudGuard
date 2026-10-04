package com.sanat.fraudguard

import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.Connection
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class FraudCallScreeningService :
    CallScreeningService() {

    private val executor =
        Executors.newSingleThreadExecutor()

    override fun onScreenCall(
        callDetails: Call.Details
    ) {

        if (
            callDetails.callDirection !=
            Call.Details.DIRECTION_INCOMING
        ) {
            return
        }

        val handle = callDetails.handle

        val number =
            handle?.schemeSpecificPart
                ?.trim()
                ?: ""

        if (number.isEmpty()) {
            allow(callDetails)
            return
        }

        val verificationStatus =
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                callDetails
                    .callerNumberVerificationStatus
            } else {
                -1
            }

        val future =
            executor.submit<org.json.JSONObject> {

                FraudEngine.analyzeCall(
                    this,
                    number,
                    verificationStatus
                )
            }

        val result = try {

            future.get(
                1500,
                TimeUnit.MILLISECONDS
            )

        } catch (_: Exception) {

            org.json.JSONObject()
                .put("risk_score", 50)
                .put("risk_level", "UNKNOWN")
                .put(
                    "reasons",
                    org.json.JSONArray().put(
                        "Analysis timed out; call was allowed."
                    )
                )
        }

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

                    for (i in 0 until reasons.length()) {

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
            this,
            "CALL",
            number,
            score,
            level,
            reasonText
        )

        if (score >= 70) {

            NotificationHelper.alert(
                this,
                "⚠️ Suspicious incoming call",
                "$number — risk $score/100. $reasonText",
                number.hashCode()
            )
        }

        /*
         * IMPORTANT:
         * Never automatically block a call merely because
         * a heuristic says it is suspicious.
         *
         * Auto-blocking is optional and disabled by default.
         */

        val autoBlock =
            getSharedPreferences(
                "settings",
                MODE_PRIVATE
            )
                .getBoolean(
                    "auto_block_high_risk",
                    false
                )

        if (autoBlock && score >= 90) {

            val response =
                CallScreeningService.CallResponse
                    .Builder()
                    .setDisallowCall(true)
                    .setRejectCall(true)
                    .setSkipCallLog(false)
                    .setSkipNotification(false)
                    .build()

            respondToCall(
                callDetails,
                response
            )

        } else {

            allow(callDetails)
        }
    }

    private fun allow(
        callDetails: Call.Details
    ) {

        val response =
            CallScreeningService.CallResponse
                .Builder()
                .setDisallowCall(false)
                .setRejectCall(false)
                .setSilenceCall(false)
                .setSkipCallLog(false)
                .setSkipNotification(false)
                .build()

        respondToCall(
            callDetails,
            response
        )
    }

    override fun onDestroy() {

        executor.shutdownNow()

        super.onDestroy()
    }
}
