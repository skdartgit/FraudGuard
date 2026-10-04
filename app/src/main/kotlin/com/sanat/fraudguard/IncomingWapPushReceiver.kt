package com.sanat.fraudguard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Declared so FraudGuard satisfies the Android requirements
 * for the SMS role.
 *
 * FraudGuard's current protection pipeline analyzes ordinary
 * incoming SMS messages through IncomingSmsReceiver.
 *
 * MMS/WAP-PUSH handling is intentionally not analyzed here yet.
 */
class IncomingWapPushReceiver : BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        // Reserved for future MMS/WAP-PUSH analysis.
    }
}
