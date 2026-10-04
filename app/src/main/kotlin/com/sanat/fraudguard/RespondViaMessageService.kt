package com.sanat.fraudguard

import android.app.Service
import android.content.Intent
import android.os.IBinder

class RespondViaMessageService : Service() {

    override fun onBind(
        intent: Intent?
    ): IBinder? {
        return null
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        /*
         * This component exists so FraudGuard qualifies
         * for the Android SMS role.
         *
         * Normal message composition is handled by
         * ComposeSmsActivity.
         */

        stopSelf(startId)

        return START_NOT_STICKY
    }
}
