package com.sanat.fraudguard

import android.content.ContentValues
import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object FraudHistory {

    data class Event(
        val id: Long,
        val time: Long,
        val type: String,
        val source: String,
        val score: Int,
        val level: String,
        val action: String,
        val details: String,
        val verificationStatus: Int,
        val reputationStatus: String,
        val reputationScore: Int,
        val reputationProvider: String,
        val reputationDetails: String
    )

    private const val LEGACY_PREFS = "fraud_history"
    private const val LEGACY_EVENTS = "events"
    private const val MIGRATION_PREFS = "fraud_history_migration"
    private const val MIGRATION_DONE = "sqlite_migration_done"

    @Synchronized
    fun add(
        context: Context,
        type: String,
        source: String,
        score: Int,
        level: String,
        details: String,
        action: String = "ALLOW",
        verificationStatus: Int = -1
    ): Long {

        migrateLegacyIfNeeded(context)

        val db = FraudEventDbHelper(context.applicationContext).writableDatabase

        val values = ContentValues().apply {
            put("time", System.currentTimeMillis())
            put("type", type)
            put("source", source)
            put("score", score.coerceIn(0, 100))
            put("level", level)
            put("action", action)
            put("details", details)
            put("verification_status", verificationStatus)
            put("reputation_status", "NOT_CHECKED")
            put("reputation_score", -1)
        }

        val id = db.insert("fraud_events", null, values)

        trimOldEvents(context)

        return id
    }

    fun count(context: Context): Int {
        migrateLegacyIfNeeded(context)

        val db = FraudEventDbHelper(
            context.applicationContext
        ).readableDatabase

        db.rawQuery(
            "SELECT COUNT(*) FROM fraud_events",
            null
        ).use { cursor ->
            return if (cursor.moveToFirst()) {
                cursor.getInt(0)
            } else {
                0
            }
        }
    }

    fun list(
        context: Context,
        limit: Int = 100
    ): List<Event> {

        migrateLegacyIfNeeded(context)

        val db = FraudEventDbHelper(
            context.applicationContext
        ).readableDatabase

        val result = mutableListOf<Event>()

        db.query(
            "fraud_events",
            null,
            null,
            null,
            null,
            null,
            "time DESC",
            limit.coerceIn(1, 1000).toString()
        ).use { cursor ->

            val idIndex = cursor.getColumnIndexOrThrow("id")
            val timeIndex = cursor.getColumnIndexOrThrow("time")
            val typeIndex = cursor.getColumnIndexOrThrow("type")
            val sourceIndex = cursor.getColumnIndexOrThrow("source")
            val scoreIndex = cursor.getColumnIndexOrThrow("score")
            val levelIndex = cursor.getColumnIndexOrThrow("level")
            val actionIndex = cursor.getColumnIndexOrThrow("action")
            val detailsIndex = cursor.getColumnIndexOrThrow("details")
            val verificationIndex =
                cursor.getColumnIndexOrThrow("verification_status")
            val reputationStatusIndex =
                cursor.getColumnIndexOrThrow("reputation_status")
            val reputationScoreIndex =
                cursor.getColumnIndexOrThrow("reputation_score")
            val reputationProviderIndex =
                cursor.getColumnIndexOrThrow("reputation_provider")
            val reputationDetailsIndex =
                cursor.getColumnIndexOrThrow("reputation_details")

            while (cursor.moveToNext()) {
                result += Event(
                    id = cursor.getLong(idIndex),
                    time = cursor.getLong(timeIndex),
                    type = cursor.getString(typeIndex),
                    source = cursor.getString(sourceIndex),
                    score = cursor.getInt(scoreIndex),
                    level = cursor.getString(levelIndex),
                    action = cursor.getString(actionIndex),
                    details = cursor.getString(detailsIndex) ?: "",
                    verificationStatus =
                        cursor.getInt(verificationIndex),
                    reputationStatus =
                        cursor.getString(reputationStatusIndex)
                            ?: "NOT_CHECKED",
                    reputationScore =
                        cursor.getInt(reputationScoreIndex),
                    reputationProvider =
                        cursor.getString(reputationProviderIndex)
                            ?: "",
                    reputationDetails =
                        cursor.getString(reputationDetailsIndex)
                            ?: ""
                )
            }
        }

        return result
    }

    fun updateReputation(
        context: Context,
        eventId: Long,
        provider: String,
        status: String,
        reputationScore: Int,
        details: String
    ) {

        val db = FraudEventDbHelper(
            context.applicationContext
        ).writableDatabase

        val values = ContentValues().apply {
            put("reputation_status", status)
            put(
                "reputation_score",
                reputationScore.coerceIn(0, 100)
            )
            put("reputation_provider", provider)
            put("reputation_details", details)
        }

        db.update(
            "fraud_events",
            values,
            "id = ?",
            arrayOf(eventId.toString())
        )

        val event = findById(context, eventId)

        if (event != null) {
            val combined =
                maxOf(event.score, reputationScore)

            val newLevel =
                when {
                    combined >= 85 -> "CRITICAL"
                    combined >= 65 -> "HIGH"
                    combined >= 40 -> "SUSPICIOUS"
                    combined >= 20 -> "LOW"
                    else -> "SAFE"
                }

            val combinedDetails =
                if (event.details.isBlank()) {
                    "Live reputation: $details"
                } else {
                    event.details +
                        "\n\nLive reputation: " +
                        details
                }

            val combinedValues = ContentValues().apply {
                put("score", combined)
                put("level", newLevel)
                put("details", combinedDetails)
            }

            db.update(
                "fraud_events",
                combinedValues,
                "id = ?",
                arrayOf(eventId.toString())
            )
        }
    }

    fun findById(
        context: Context,
        eventId: Long
    ): Event? {

        val db = FraudEventDbHelper(
            context.applicationContext
        ).readableDatabase

        db.query(
            "fraud_events",
            null,
            "id = ?",
            arrayOf(eventId.toString()),
            null,
            null,
            null,
            "1"
        ).use { cursor ->

            if (!cursor.moveToFirst()) {
                return null
            }

            return Event(
                id = cursor.getLong(
                    cursor.getColumnIndexOrThrow("id")
                ),
                time = cursor.getLong(
                    cursor.getColumnIndexOrThrow("time")
                ),
                type = cursor.getString(
                    cursor.getColumnIndexOrThrow("type")
                ),
                source = cursor.getString(
                    cursor.getColumnIndexOrThrow("source")
                ),
                score = cursor.getInt(
                    cursor.getColumnIndexOrThrow("score")
                ),
                level = cursor.getString(
                    cursor.getColumnIndexOrThrow("level")
                ),
                action = cursor.getString(
                    cursor.getColumnIndexOrThrow("action")
                ),
                details = cursor.getString(
                    cursor.getColumnIndexOrThrow("details")
                ) ?: "",
                verificationStatus = cursor.getInt(
                    cursor.getColumnIndexOrThrow(
                        "verification_status"
                    )
                ),
                reputationStatus = cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        "reputation_status"
                    )
                ) ?: "NOT_CHECKED",
                reputationScore = cursor.getInt(
                    cursor.getColumnIndexOrThrow(
                        "reputation_score"
                    )
                ),
                reputationProvider = cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        "reputation_provider"
                    )
                ) ?: "",
                reputationDetails = cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        "reputation_details"
                    )
                ) ?: ""
            )
        }
    }

    fun clear(context: Context) {

        val db = FraudEventDbHelper(
            context.applicationContext
        ).writableDatabase

        db.delete(
            "fraud_events",
            null,
            null
        )

        context.getSharedPreferences(
            LEGACY_PREFS,
            Context.MODE_PRIVATE
        ).edit()
            .remove(LEGACY_EVENTS)
            .apply()
    }

    private fun trimOldEvents(context: Context) {

        val db = FraudEventDbHelper(
            context.applicationContext
        ).writableDatabase

        db.execSQL(
            """
            DELETE FROM fraud_events
            WHERE id NOT IN (
                SELECT id
                FROM fraud_events
                ORDER BY time DESC
                LIMIT 1000
            )
            """.trimIndent()
        )
    }

    @Synchronized
    private fun migrateLegacyIfNeeded(context: Context) {

        val migrationPrefs =
            context.getSharedPreferences(
                MIGRATION_PREFS,
                Context.MODE_PRIVATE
            )

        if (
            migrationPrefs.getBoolean(
                MIGRATION_DONE,
                false
            )
        ) {
            return
        }

        val legacy =
            context.getSharedPreferences(
                LEGACY_PREFS,
                Context.MODE_PRIVATE
            )

        val raw =
            legacy.getString(
                LEGACY_EVENTS,
                "[]"
            ) ?: "[]"

        try {
            val array = JSONArray(raw)
            val db = FraudEventDbHelper(
                context.applicationContext
            ).writableDatabase

            for (i in 0 until array.length()) {

                val old = array.optJSONObject(i)
                    ?: continue

                val values = ContentValues().apply {
                    put(
                        "time",
                        old.optLong(
                            "time",
                            System.currentTimeMillis()
                        )
                    )
                    put(
                        "type",
                        old.optString(
                            "type",
                            "UNKNOWN"
                        )
                    )
                    put(
                        "source",
                        old.optString(
                            "source",
                            "UNKNOWN"
                        )
                    )
                    put(
                        "score",
                        old.optInt(
                            "score",
                            50
                        ).coerceIn(0, 100)
                    )
                    put(
                        "level",
                        old.optString(
                            "level",
                            "UNKNOWN"
                        )
                    )
                    put(
                        "action",
                        "LEGACY"
                    )
                    put(
                        "details",
                        old.optString(
                            "details",
                            ""
                        )
                    )
                    put(
                        "verification_status",
                        -1
                    )
                    put(
                        "reputation_status",
                        "LEGACY"
                    )
                    put(
                        "reputation_score",
                        -1
                    )
                }

                db.insert(
                    "fraud_events",
                    null,
                    values
                )
            }

        } catch (_: Exception) {
        }

        migrationPrefs.edit()
            .putBoolean(
                MIGRATION_DONE,
                true
            )
            .apply()
    }
}
