package com.sanat.fraudguard

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FraudEventActivity : Activity() {

    private val background =
        Color.rgb(255, 253, 245)

    private val buttonColor =
        Color.rgb(107, 91, 62)

    private val textColor =
        Color.rgb(41, 37, 31)

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        buildUi()
    }

    override fun onResume() {
        super.onResume()

        buildUi()
    }

    private fun buildUi() {

        val root =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
                setBackgroundColor(
                    background
                )
                setPadding(
                    24,
                    24,
                    24,
                    24
                )
            }

        val title =
            TextView(this).apply {
                text =
                    "🛡️ Fraud History"
                textSize = 26f
                setTextColor(
                    textColor
                )
                setPadding(
                    0,
                    0,
                    0,
                    16
                )
            }

        root.addView(title)

        val clear =
            Button(this).apply {
                text =
                    "Clear Fraud History"
                setTextColor(
                    Color.WHITE
                )
                setBackgroundColor(
                    buttonColor
                )
                setOnClickListener {
                    FraudHistory.clear(
                        this@FraudEventActivity
                    )
                    buildUi()
                }
            }

        root.addView(
            clear,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        val scroll =
            ScrollView(this)

        val content =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
            }

        val events =
            FraudHistory.list(
                this,
                200
            )

        if (events.isEmpty()) {

            content.addView(
                TextView(this).apply {
                    text =
                        "No fraud/security events recorded yet."
                    textSize = 17f
                    setTextColor(
                        textColor
                    )
                    setPadding(
                        0,
                        24,
                        0,
                        24
                    )
                }
            )

        } else {

            for (event in events) {
                content.addView(
                    eventView(event)
                )
            }
        }

        scroll.addView(content)

        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(root)
    }

    private fun eventView(
        event: FraudHistory.Event
    ): TextView {

        val time =
            SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                Locale.getDefault()
            ).format(
                Date(event.time)
            )

        val verification =
            when (event.verificationStatus) {
                1 -> "PASSED"
                2 -> "FAILED"
                0 -> "UNKNOWN"
                else -> "N/A"
            }

        val reputation =
            if (
                event.reputationScore >= 0
            ) {
                "${event.reputationProvider}: " +
                    "${event.reputationScore}/100 " +
                    "(${event.reputationStatus})"
            } else {
                event.reputationStatus
            }

        return TextView(this).apply {

            text =
                """
                $time

                ${event.type} — ${event.source}

                FraudGuard risk:
                ${event.score}/100 — ${event.level}

                Action:
                ${event.action}

                Caller verification:
                $verification

                Live reputation:
                $reputation

                ${event.details}

                ${if (event.reputationDetails.isNotBlank())
                    "Reputation details:\n${event.reputationDetails}"
                else
                    ""}
                """.trimIndent()

            textSize = 15f

            setTextColor(
                textColor
            )

            setPadding(
                0,
                18,
                0,
                18
            )
        }
    }
}
