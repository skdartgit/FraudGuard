package com.sanat.fraudguard

import com.chaquo.python.Python
import org.json.JSONObject

object PythonBridge {

    private fun python(): Python {
        return Python.getInstance()
    }

    fun analyzeCall(
        number: String,
        verificationStatus: Int
    ): JSONObject {

        return try {
            val module = python().getModule("fraud_engine")

            val result = module.callAttr(
                "analyze_call",
                number,
                verificationStatus
            ).toString()

            JSONObject(result)
        } catch (e: Exception) {
            JSONObject()
                .put("type", "CALL")
                .put("risk_score", 50)
                .put("risk_level", "UNKNOWN")
                .put("number", number)
                .put(
                    "reasons",
                    org.json.JSONArray().put(
                        "Python analysis was unavailable; using safe fallback."
                    )
                )
        }
    }

    fun analyzeSms(
        sender: String,
        message: String
    ): JSONObject {

        return try {
            val module = python().getModule("fraud_engine")

            val result = module.callAttr(
                "analyze_sms",
                sender,
                message
            ).toString()

            JSONObject(result)
        } catch (e: Exception) {
            JSONObject()
                .put("type", "SMS")
                .put("risk_score", 50)
                .put("risk_level", "UNKNOWN")
                .put("sender", sender)
                .put(
                    "reasons",
                    org.json.JSONArray().put(
                        "Python analysis was unavailable."
                    )
                )
        }
    }
}
