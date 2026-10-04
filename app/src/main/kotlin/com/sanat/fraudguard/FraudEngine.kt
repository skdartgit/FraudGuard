package com.sanat.fraudguard

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object FraudEngine {

    fun analyzeCall(
        context: Context,
        number: String,
        verificationStatus: Int
    ): JSONObject {

        val pythonResult = PythonBridge.analyzeCall(
            number,
            verificationStatus
        )

        val localScore = localNumberScore(number)

        var score = maxOf(
            pythonResult.optInt("risk_score", 50),
            localScore
        )

        if (verificationStatus >= 0) {
            if (verificationStatus == 2) {
                score = minOf(100, score + 35)
            } else if (verificationStatus == 1) {
                score = maxOf(0, score - 10)
            }
        }

        val level = when {
            score >= 80 -> "HIGH"
            score >= 50 -> "SUSPICIOUS"
            score >= 25 -> "LOW"
            else -> "LOW"
        }

        pythonResult.put("risk_score", score)
        pythonResult.put("risk_level", level)

        return pythonResult
    }

    fun analyzeSms(
        context: Context,
        sender: String,
        message: String
    ): JSONObject {

        return PythonBridge.analyzeSms(
            sender,
            message
        )
    }


    private fun localNumberScore(number: String): Int {

        val cleaned = number.replace(
            Regex("[^0-9+]"),
            ""
        )

        if (cleaned.isEmpty()) {
            return 70
        }

        val digits = cleaned.filter { it.isDigit() }

        if (digits.length < 7) {
            return 55
        }

        if (digits.all { it == digits.first() }) {
            return 75
        }

        val repeated =
            digits.windowed(2)
                .count { it[0] == it[1] }

        if (digits.length >= 8 &&
            repeated >= digits.length / 2
        ) {
            return 60
        }

        return 10
    }
}
