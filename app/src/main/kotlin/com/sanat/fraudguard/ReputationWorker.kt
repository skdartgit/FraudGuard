package com.sanat.fraudguard

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters

class ReputationWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {

        val eventId =
            inputData.getLong(
                KEY_EVENT_ID,
                -1L
            )

        val kind =
            inputData.getString(
                KEY_KIND
            ) ?: return Result.failure()

        val indicator =
            inputData.getString(
                KEY_INDICATOR
            ) ?: return Result.failure()

        if (eventId <= 0L) {
            return Result.failure()
        }

        val config =
            ReputationConfig.get(
                applicationContext
            )

        if (!config.enabled) {
            return Result.success()
        }

        val result =
            when (kind) {

                KIND_PHONE -> {

                    if (config.ipqsKey.isBlank()) {
                        LiveReputationClient.Result(
                            "IPQualityScore",
                            "NOT_CONFIGURED",
                            -1,
                            "IPQS is not configured."
                        )
                    } else {
                        LiveReputationClient.checkPhone(
                            indicator,
                            config.ipqsKey
                        )
                    }
                }

                KIND_URL -> {

                    if (config.webRiskKey.isBlank()) {
                        LiveReputationClient.Result(
                            "Google Web Risk",
                            "NOT_CONFIGURED",
                            -1,
                            "Google Web Risk is not configured."
                        )
                    } else {
                        LiveReputationClient.checkUrl(
                            indicator,
                            config.webRiskKey
                        )
                    }
                }

                else -> {
                    return Result.failure()
                }
            }

        FraudHistory.updateReputation(
            applicationContext,
            eventId,
            result.provider,
            result.status,
            result.score,
            result.details
        )

        if (result.score >= 70) {

            val event =
                FraudHistory.findById(
                    applicationContext,
                    eventId
                )

            val title =
                when (kind) {
                    KIND_PHONE ->
                        "🚨 Live caller reputation alert"
                    else ->
                        "🚨 Live URL reputation alert"
                }

            val message =
                buildString {

                    append(indicator)
                    append(" — ")

                    append(result.provider)
                    append(" score ")
                    append(result.score)
                    append("/100.")

                    append(" ")
                    append(result.details)

                    if (event != null) {
                        append(
                            " Local FraudGuard score: "
                        )
                        append(event.score)
                        append("/100.")
                    }
                }

            NotificationHelper.alert(
                applicationContext,
                title,
                message,
                (eventId.toString() + indicator).hashCode()
            )
        }

        return Result.success()
    }

    companion object {

        private const val KEY_EVENT_ID =
            "event_id"

        private const val KEY_KIND =
            "kind"

        private const val KEY_INDICATOR =
            "indicator"

        const val KIND_PHONE =
            "PHONE"

        const val KIND_URL =
            "URL"

        fun enqueuePhone(
            context: Context,
            eventId: Long,
            phone: String
        ) {
            enqueue(
                context,
                eventId,
                KIND_PHONE,
                phone
            )
        }

        fun enqueueUrl(
            context: Context,
            eventId: Long,
            url: String
        ) {
            enqueue(
                context,
                eventId,
                KIND_URL,
                url
            )
        }

        private fun enqueue(
            context: Context,
            eventId: Long,
            kind: String,
            indicator: String
        ) {

            val data =
                Data.Builder()
                    .putLong(
                        KEY_EVENT_ID,
                        eventId
                    )
                    .putString(
                        KEY_KIND,
                        kind
                    )
                    .putString(
                        KEY_INDICATOR,
                        indicator
                    )
                    .build()

            val constraints =
                Constraints.Builder()
                    .setRequiredNetworkType(
                        NetworkType.CONNECTED
                    )
                    .build()

            val request =
                OneTimeWorkRequest.Builder(
                    ReputationWorker::class.java
                )
                    .setInputData(data)
                    .setConstraints(constraints)
                    .build()

            WorkManager.getInstance(
                context.applicationContext
            ).enqueueUniqueWork(
                "reputation-${eventId}-${kind}-${indicator.hashCode()}",
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }
}
