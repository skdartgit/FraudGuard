package com.sanat.fraudguard

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class ReputationSettingsActivity : Activity() {

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

    private fun buildUi() {

        val config =
            ReputationConfig.get(this)

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

        val scroll =
            ScrollView(this)

        val content =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
            }

        val title =
            TextView(this).apply {
                text =
                    "🌐 Live Reputation Intelligence"
                textSize = 25f
                setTextColor(
                    textColor
                )
            }

        content.addView(title)

        content.addView(
            TextView(this).apply {
                text =
                    """
                    This is an optional second layer.

                    FraudGuard first performs local analysis.
                    Live reputation is then checked in the
                    background when internet access is available.

                    Phone numbers:
                    IPQualityScore can return fraud/risk signals.

                    URLs:
                    Google Web Risk can check whether a URL
                    matches supported threat lists.

                    Privacy:
                    • SMS message bodies are NOT sent.
                    • Only the phone number or URL being checked
                      is sent to the selected provider.
                    • API keys are encrypted using Android Keystore.
                    • Do not put production API keys inside source code.
                    """.trimIndent()
                textSize = 15f
                setTextColor(
                    textColor
                )
                setPadding(
                    0,
                    16,
                    0,
                    16
                )
            }
        )

        val enabled =
            Switch(this).apply {
                text =
                    "Enable live reputation checks"
                textSize = 16f
                setTextColor(
                    textColor
                )
                isChecked =
                    config.enabled
            }

        content.addView(enabled)

        val webRisk =
            EditText(this).apply {
                hint =
                    "Google Web Risk API key"
                inputType =
                    InputType.TYPE_CLASS_TEXT or
                        InputType.TYPE_TEXT_VARIATION_PASSWORD
                setTextColor(
                    textColor
                )
                setSingleLine(true)
                setText(
                    config.webRiskKey
                )
            }

        content.addView(webRisk)

        val ipqs =
            EditText(this).apply {
                hint =
                    "IPQualityScore API key"
                inputType =
                    InputType.TYPE_CLASS_TEXT or
                        InputType.TYPE_TEXT_VARIATION_PASSWORD
                setTextColor(
                    textColor
                )
                setSingleLine(true)
                setText(
                    config.ipqsKey
                )
            }

        content.addView(ipqs)

        val save =
            Button(this).apply {
                text =
                    "Save Reputation Settings"
                setTextColor(
                    Color.WHITE
                )
                setBackgroundColor(
                    buttonColor
                )

                setOnClickListener {

                    ReputationConfig.save(
                        this@ReputationSettingsActivity,
                        enabled.isChecked,
                        webRisk.text.toString(),
                        ipqs.text.toString()
                    )

                    Toast.makeText(
                        this@ReputationSettingsActivity,
                        "Reputation settings saved.",
                        Toast.LENGTH_SHORT
                    ).show()

                    finish()
                }
            }

        content.addView(
            save,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

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
}
