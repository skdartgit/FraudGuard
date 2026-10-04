package com.sanat.fraudguard

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object FraudHistory {

    private const val PREFS = "fraud_history"
    private const val EVENTS = "events"
    private const val MAX_EVENTS = 100

    @Synchronized
    fun add(
        context: Context,
        type: String,
        source: String,
        score: Int,
        level: String,
        details: String
    ) {

        val prefs = context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        )

        val oldText = prefs.getString(
            EVENTS,
            "[]"
        ) ?: "[]"

        val array = JSONArray(oldText)

        val event = JSONObject()
            .put("time", System.currentTimeMillis())
            .put("type", type)
            .put("source", source)
            .put("score", score)
            .put("level", level)
            .put("details", details)

        val newArray = JSONArray()

        newArray.put(event)

        val start = maxOf(
            0,
            array.length() - MAX_EVENTS + 1
        )

        for (i in start until array.length()) {
            newArray.put(array.getJSONObject(i))
        }

        prefs.edit()
            .putString(EVENTS, newArray.toString())
            .apply()
    }

    fun count(context: Context): Int {

        val prefs = context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        )

        return try {
            JSONArray(
                prefs.getString(
                    EVENTS,
                    "[]"
                ) ?: "[]"
            ).length()
        } catch (_: Exception) {
            0
        }
    }

    fun clear(context: Context) {

        context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        ).edit()
            .remove(EVENTS)
            .apply()
    }
}
