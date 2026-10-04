package com.sanat.fraudguard

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.SmsManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast

class ComposeSmsActivity : Activity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        val root =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    24,
                    24,
                    24,
                    24
                )
            }

        val number =
            EditText(this).apply {

                hint = "Phone number"

                setText(
                    intent?.data
                        ?.schemeSpecificPart
                        ?: ""
                )
            }

        val message =
            EditText(this).apply {

                hint = "Message"

                minLines = 5
            }

        val send =
            Button(this).apply {

                text = "Send SMS"

                setOnClickListener {

                    sendSms(
                        number.text.toString(),
                        message.text.toString()
                    )
                }
            }

        root.addView(number)

        root.addView(message)

        root.addView(send)

        setContentView(root)
    }

    private fun sendSms(
        destination: String,
        text: String
    ) {

        if (
            destination.isBlank() ||
            text.isBlank()
        ) {

            Toast.makeText(
                this,
                "Enter number and message.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        if (
            checkSelfPermission(
                Manifest.permission.SEND_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            requestPermissions(
                arrayOf(
                    Manifest.permission.SEND_SMS
                ),
                5001
            )

            return
        }

        try {

            val manager =
                SmsManager.getDefault()

            val parts =
                manager.divideMessage(text)

            if (parts.size == 1) {

                manager.sendTextMessage(
                    destination,
                    null,
                    text,
                    null,
                    null
                )

            } else {

                manager.sendMultipartTextMessage(
                    destination,
                    null,
                    parts,
                    null,
                    null
                )
            }

            Toast.makeText(
                this,
                "SMS sent.",
                Toast.LENGTH_SHORT
            ).show()

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "SMS failed: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
