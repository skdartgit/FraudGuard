package com.sanat.fraudguard

import android.Manifest
import android.app.Activity
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    companion object {
        private const val REQUEST_CALL_ROLE = 1001
        private const val REQUEST_SMS_ROLE = 1002
        private const val REQUEST_PERMISSIONS = 1003
        private const val REQUEST_NOTIFICATION = 1004
    }

    private lateinit var statusText: TextView

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        NotificationHelper.ensureChannel(this)

        buildUi()

        requestNotificationPermission()
    }

    override fun onResume() {
        super.onResume()

        if (::statusText.isInitialized) {
            updateStatus()
        }
    }

    private fun buildUi() {

        val background =
            Color.rgb(255, 253, 245)

        val buttonColor =
            Color.rgb(107, 91, 62)

        val textColor =
            Color.rgb(41, 37, 31)

        val root =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setBackgroundColor(
                    background
                )

                setPadding(
                    28,
                    28,
                    28,
                    28
                )
            }

        val scroll =
            ScrollView(this)

        val content =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
            }

        fun text(
            value: String,
            size: Float = 16f
        ): TextView {

            return TextView(this).apply {

                text = value

                textSize = size

                setTextColor(
                    textColor
                )

                setPadding(
                    0,
                    8,
                    0,
                    8
                )
            }
        }

        val title =
            text(
                "🛡️ FraudGuard",
                28f
            )

        title.gravity =
            Gravity.CENTER_HORIZONTAL

        content.addView(title)

        content.addView(
            text(
                "Android fraud, spam, phishing and privacy guard",
                16f
            )
        )

        statusText =
            text(
                "",
                15f
            )

        content.addView(statusText)

        fun button(
            label: String,
            action: () -> Unit
        ): Button {

            return Button(this).apply {

                text = label

                setTextColor(Color.WHITE)

                setBackgroundColor(
                    buttonColor
                )

                setOnClickListener {
                    action()
                }

                val params =
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )

                params.setMargins(
                    0,
                    8,
                    0,
                    8
                )

                layoutParams = params
            }
        }

        content.addView(
            button(
                "Enable Call Protection"
            ) {
                requestCallScreeningRole()
            }
        )

        content.addView(
            button(
                "Enable SMS Protection"
            ) {
                requestSmsRole()
            }
        )

        val autoBlock =
            Switch(this).apply {

                text =
                    "Automatically block risk ≥ 90"

                textSize = 16f

                setTextColor(
                    textColor
                )

                isChecked =
                    getSharedPreferences(
                        "settings",
                        MODE_PRIVATE
                    )
                        .getBoolean(
                            "auto_block_high_risk",
                            false
                        )

                setOnCheckedChangeListener { _, checked ->

                    getSharedPreferences(
                        "settings",
                        MODE_PRIVATE
                    )
                        .edit()
                        .putBoolean(
                            "auto_block_high_risk",
                            checked
                        )
                        .apply()
                }
            }

        content.addView(autoBlock)

        content.addView(
            button(
                "Run Privacy & Security Audit"
            ) {
                runAudit()
            }
        )

        content.addView(
            button(
                "Open Android Privacy Settings"
            ) {
                try {

                    startActivity(
                        Intent(
                            Settings.ACTION_PRIVACY_SETTINGS
                        )
                    )

                } catch (_: Exception) {

                    Toast.makeText(
                        this,
                        "Privacy settings unavailable.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )

        content.addView(
            button(
                "Send Test SMS"
            ) {

                startActivity(
                    Intent(
                        this,
                        ComposeSmsActivity::class.java
                    )
                )
            }
        )

        content.addView(
            button(
                "Clear Fraud History"
            ) {

                FraudHistory.clear(this)

                Toast.makeText(
                    this,
                    "Fraud history cleared.",
                    Toast.LENGTH_SHORT
                ).show()

                updateStatus()
            }
        )

        content.addView(
            text(
                """
                HOW PROTECTION WORKS

                Calls:
                Android → CallScreeningService → Kotlin →
                Python fraud engine → risk score → alert/block

                SMS:
                Android → SMS receiver → Kotlin →
                Python fraud engine → URL analysis →
                risk score → alert

                Python checks:
                • scam language
                • financial/KYC/UPI patterns
                • OTP/PIN/CVV/password requests
                • suspicious URLs
                • URL shorteners
                • IP-address URLs
                • punycode
                • suspicious domains
                • remote-access software
                • APK/install requests
                • urgency/social engineering

                The application NEVER opens a suspicious URL
                merely to analyze it.
                """.trimIndent(),
                14f
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

    private fun updateStatus() {

        val roleManager =
            getSystemService(RoleManager::class.java)

        val callRole =
            roleManager.isRoleHeld(
                RoleManager.ROLE_CALL_SCREENING
            )

        val smsRole =
            roleManager.isRoleHeld(
                RoleManager.ROLE_SMS
            )

        val contactsGranted =
            checkSelfPermission(
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED

        val smsGranted =
            if (Build.VERSION.SDK_INT >= 23) {
                checkSelfPermission(
                    Manifest.permission.RECEIVE_SMS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }

        val notificationsGranted =
            if (Build.VERSION.SDK_INT >= 33) {
                checkSelfPermission(
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }

        val events =
            FraudHistory.count(this)

        val latest =
            FraudHistory.latest(this)

        val latestText =
            if (latest != null) {

                val type =
                    latest.optString(
                        "type",
                        "UNKNOWN"
                    )

                val source =
                    latest.optString(
                        "source",
                        "Unknown"
                    )

                val score =
                    latest.optInt(
                        "score",
                        0
                    )

                val level =
                    latest.optString(
                        "level",
                        "UNKNOWN"
                    )

                val details =
                    latest.optString(
                        "details",
                        "No additional information."
                    )

                """

                Latest event:
                $type
                Source: $source
                Risk: $score/100 ($level)
                Details: $details
                """.trimIndent()

            } else {
                "Latest event: NONE"
            }

        statusText.text =
            """
            Protection status

            Call screening: ${
                if (callRole) "ACTIVE ✅"
                else "NOT ENABLED ⚠️"
            }

            Contacts permission: ${
                if (contactsGranted) "GRANTED ✅"
                else "NOT GRANTED ⚠️"
            }

            SMS protection: ${
                if (smsRole) "ACTIVE ✅"
                else "NOT ENABLED ⚠️"
            }

            SMS receive permission: ${
                if (smsGranted) "GRANTED ✅"
                else "NOT GRANTED ⚠️"
            }

            Notifications: ${
                if (notificationsGranted) "GRANTED ✅"
                else "NOT GRANTED ⚠️"
            }

            Recorded security events: $events

            Python engine: EMBEDDED

            $latestText
            """.trimIndent()
    }

    private fun requestCallScreeningRole() {

        val roleManager =
            getSystemService(RoleManager::class.java)

        if (
            !roleManager.isRoleAvailable(
                RoleManager.ROLE_CALL_SCREENING
            )
        ) {

            Toast.makeText(
                this,
                "Call screening role is not available on this device.",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        if (
            roleManager.isRoleHeld(
                RoleManager.ROLE_CALL_SCREENING
            )
        ) {

            Toast.makeText(
                this,
                "Call protection is active. Checking required permissions...",
                Toast.LENGTH_SHORT
            ).show()

            requestRequiredPermissions()
            updateStatus()

            return
        }

        try {

            startActivityForResult(
                roleManager.createRequestRoleIntent(
                    RoleManager.ROLE_CALL_SCREENING
                ),
                REQUEST_CALL_ROLE
            )

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Could not open call-protection role: ${
                    e.message ?: "unknown error"
                }",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun requestSmsRole() {

        val roleManager =
            getSystemService(RoleManager::class.java)

        if (
            Build.VERSION.SDK_INT < 29
        ) {

            Toast.makeText(
                this,
                "SMS protection requires Android 10 or newer.",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        if (
            !roleManager.isRoleAvailable(
                RoleManager.ROLE_SMS
            )
        ) {

            Toast.makeText(
                this,
                "SMS role is not available on this device.",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        if (
            roleManager.isRoleHeld(
                RoleManager.ROLE_SMS
            )
        ) {

            Toast.makeText(
                this,
                "FraudGuard is already the default SMS app.",
                Toast.LENGTH_SHORT
            ).show()

            requestRequiredPermissions()
            updateStatus()

            return
        }

        try {

            startActivityForResult(
                roleManager.createRequestRoleIntent(
                    RoleManager.ROLE_SMS
                ),
                REQUEST_SMS_ROLE
            )

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Could not open SMS role request: ${
                    e.message ?: "unknown error"
                }",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun requestRequiredPermissions() {

        val permissions =
            mutableListOf(
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.READ_SMS,
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.SEND_SMS
            )

        if (
            Build.VERSION.SDK_INT >= 33
        ) {

            permissions.add(
                Manifest.permission.POST_NOTIFICATIONS
            )
        }

        val missing =
            permissions.filter {

                checkSelfPermission(it) !=
                    PackageManager.PERMISSION_GRANTED
            }

        if (missing.isNotEmpty()) {

            requestPermissions(
                missing.toTypedArray(),
                REQUEST_PERMISSIONS
            )
        }
    }

    private fun requestNotificationPermission() {

        if (
            Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            requestPermissions(
                arrayOf(
                    Manifest.permission.POST_NOTIFICATIONS
                ),
                REQUEST_NOTIFICATION
            )
        }
    }

    private fun runAudit() {

        val result =
            PrivacyAudit.run(this)

        val message =
            PrivacyAudit.asText(result)

        Toast.makeText(
            this,
            message,
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {

        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )

        if (requestCode == REQUEST_CALL_ROLE) {

            val roleManager =
                getSystemService(RoleManager::class.java)

            if (
                roleManager.isRoleHeld(
                    RoleManager.ROLE_CALL_SCREENING
                )
            ) {

                Toast.makeText(
                    this,
                    "Call protection enabled. Please allow Contacts permission if requested.",
                    Toast.LENGTH_LONG
                ).show()

                requestRequiredPermissions()

            } else {

                Toast.makeText(
                    this,
                    "Call protection was not enabled.",
                    Toast.LENGTH_LONG
                ).show()
            }

            updateStatus()
        }

        if (requestCode == REQUEST_SMS_ROLE) {

            val roleManager =
                getSystemService(RoleManager::class.java)

            if (
                roleManager.isRoleHeld(
                    RoleManager.ROLE_SMS
                )
            ) {

                Toast.makeText(
                    this,
                    "SMS protection enabled. Please allow the requested permissions.",
                    Toast.LENGTH_LONG
                ).show()

                requestRequiredPermissions()

            } else {

                Toast.makeText(
                    this,
                    "FraudGuard was not selected as the default SMS app.",
                    Toast.LENGTH_LONG
                ).show()
            }

            updateStatus()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (requestCode == REQUEST_PERMISSIONS) {

            val denied =
                permissions.indices
                    .filter {
                        grantResults[it] !=
                            PackageManager.PERMISSION_GRANTED
                    }
                    .map {
                        permissions[it]
                    }

            if (denied.isEmpty()) {

                Toast.makeText(
                    this,
                    "Required protection permissions granted.",
                    Toast.LENGTH_LONG
                ).show()

            } else {

                Toast.makeText(
                    this,
                    "Some permissions were denied. Protection may be limited.",
                    Toast.LENGTH_LONG
                ).show()
            }

            updateStatus()
        }

        if (requestCode == REQUEST_NOTIFICATION) {

            updateStatus()
        }
    }
}
