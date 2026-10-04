package com.sanat.fraudguard

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class FraudEventDbHelper(
    context: Context
) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE fraud_events (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                time INTEGER NOT NULL,
                type TEXT NOT NULL,
                source TEXT NOT NULL,
                score INTEGER NOT NULL,
                level TEXT NOT NULL,
                action TEXT NOT NULL DEFAULT 'ALLOW',
                details TEXT,
                verification_status INTEGER NOT NULL DEFAULT -1,
                reputation_status TEXT NOT NULL DEFAULT 'NOT_CHECKED',
                reputation_score INTEGER NOT NULL DEFAULT -1,
                reputation_provider TEXT,
                reputation_details TEXT
            )
            """.trimIndent()
        )

        db.execSQL(
            "CREATE INDEX idx_fraud_events_time ON fraud_events(time DESC)"
        )

        db.execSQL(
            "CREATE INDEX idx_fraud_events_source ON fraud_events(source)"
        )
    }

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {
        if (oldVersion < 2) {
            db.execSQL(
                "ALTER TABLE fraud_events ADD COLUMN action TEXT NOT NULL DEFAULT 'ALLOW'"
            )
            db.execSQL(
                "ALTER TABLE fraud_events ADD COLUMN verification_status INTEGER NOT NULL DEFAULT -1"
            )
            db.execSQL(
                "ALTER TABLE fraud_events ADD COLUMN reputation_status TEXT NOT NULL DEFAULT 'NOT_CHECKED'"
            )
            db.execSQL(
                "ALTER TABLE fraud_events ADD COLUMN reputation_score INTEGER NOT NULL DEFAULT -1"
            )
            db.execSQL(
                "ALTER TABLE fraud_events ADD COLUMN reputation_provider TEXT"
            )
            db.execSQL(
                "ALTER TABLE fraud_events ADD COLUMN reputation_details TEXT"
            )
        }
    }

    companion object {
        private const val DATABASE_NAME = "fraud_guard_events.db"
        private const val DATABASE_VERSION = 2
    }
}
