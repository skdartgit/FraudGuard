package com.sanat.fraudguard

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.app.KeyguardManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.provider.Settings
import java.util.Locale

object PrivacyAudit {

    data class Result(
        val cameraApps: Int,
        val microphoneApps: Int,
        val locationApps: Int,
        val contactsApps: Int,
        val smsApps: Int,
        val callLogApps: Int,
        val overlayApps: Int,
        val installPackagesApps: Int,
        val accessibilityEnabled: Boolean,
        val deviceAdminEnabled: Boolean,
        val vpnActive: Boolean,
        val developerOptionsEnabled: Boolean,
        val adbEnabled: Boolean,
        val secureLockEnabled: Boolean,
        val unknownSourcesAllowed: Boolean
    )

    fun run(
        context: Context
    ): Result {

        val pm =
            context.packageManager

        var camera = 0
        var microphone = 0
        var location = 0
        var contacts = 0
        var sms = 0
        var callLog = 0
        var overlay = 0
        var installPackages = 0

        val packages =
            pm.getInstalledPackages(
                PackageManager.GET_PERMISSIONS
            )

        for (info in packages) {

            val permissions =
                info.requestedPermissions
                    ?: continue

            if (
                permissions.contains(
                    Manifest.permission.CAMERA
                )
            ) {
                camera++
            }

            if (
                permissions.contains(
                    Manifest.permission.RECORD_AUDIO
                )
            ) {
                microphone++
            }

            if (
                permissions.contains(
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) ||
                permissions.contains(
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            ) {
                location++
            }

            if (
                permissions.contains(
                    Manifest.permission.READ_CONTACTS
                )
            ) {
                contacts++
            }

            if (
                permissions.contains(
                    Manifest.permission.READ_SMS
                ) ||
                permissions.contains(
                    Manifest.permission.RECEIVE_SMS
                ) ||
                permissions.contains(
                    Manifest.permission.SEND_SMS
                )
            ) {
                sms++
            }

            if (
                permissions.contains(
                    Manifest.permission.READ_CALL_LOG
                ) ||
                permissions.contains(
                    Manifest.permission.WRITE_CALL_LOG
                )
            ) {
                callLog++
            }

            if (
                permissions.contains(
                    Manifest.permission.SYSTEM_ALERT_WINDOW
                )
            ) {
                overlay++
            }

            if (
                Build.VERSION.SDK_INT >= 26 &&
                permissions.contains(
                    Manifest.permission.REQUEST_INSTALL_PACKAGES
                )
            ) {
                installPackages++
            }
        }

        val accessibility =
            try {

                val enabled =
                    Settings.Secure.getString(
                        context.contentResolver,
                        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                    )

                !enabled.isNullOrBlank()

            } catch (_: Exception) {
                false
            }

        val deviceAdmin =
            try {

                val dpm =
                    context.getSystemService(
                        DevicePolicyManager::class.java
                    )

                !dpm.activeAdmins.isNullOrEmpty()

            } catch (_: Exception) {
                false
            }

        val vpn =
            try {

                val cm =
                    context.getSystemService(
                        ConnectivityManager::class.java
                    )

                cm.allNetworks.any { network ->

                    val capabilities =
                        cm.getNetworkCapabilities(
                            network
                        )

                    capabilities?.hasTransport(
                        NetworkCapabilities.TRANSPORT_VPN
                    ) == true
                }

            } catch (_: Exception) {
                false
            }

        val developerOptions =
            try {

                Settings.Global.getInt(
                    context.contentResolver,
                    Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
                    0
                ) == 1

            } catch (_: Exception) {
                false
            }

        val adbEnabled =
            try {

                Settings.Global.getInt(
                    context.contentResolver,
                    Settings.Global.ADB_ENABLED,
                    0
                ) == 1

            } catch (_: Exception) {
                false
            }

        val secureLock =
            try {

                val keyguard =
                    context.getSystemService(
                        KeyguardManager::class.java
                    )

                keyguard.isDeviceSecure

            } catch (_: Exception) {
                false
            }

        val unknownSources =
            if (Build.VERSION.SDK_INT >= 26) {
                try {
                    pm.canRequestPackageInstalls()
                } catch (_: Exception) {
                    false
                }
            } else {
                false
            }

        return Result(
            cameraApps = camera,
            microphoneApps = microphone,
            locationApps = location,
            contactsApps = contacts,
            smsApps = sms,
            callLogApps = callLog,
            overlayApps = overlay,
            installPackagesApps = installPackages,
            accessibilityEnabled = accessibility,
            deviceAdminEnabled = deviceAdmin,
            vpnActive = vpn,
            developerOptionsEnabled = developerOptions,
            adbEnabled = adbEnabled,
            secureLockEnabled = secureLock,
            unknownSourcesAllowed = unknownSources
        )
    }

    fun asText(
        result: Result
    ): String {

        return """
            PRIVACY / SECURITY AUDIT

            Apps requesting CAMERA:
            ${result.cameraApps}

            Apps requesting MICROPHONE:
            ${result.microphoneApps}

            Apps requesting LOCATION:
            ${result.locationApps}

            Apps requesting CONTACTS:
            ${result.contactsApps}

            Apps requesting SMS:
            ${result.smsApps}

            Apps requesting CALL LOG:
            ${result.callLogApps}

            Apps requesting OVERLAY:
            ${result.overlayApps}

            Apps requesting package installation:
            ${result.installPackagesApps}

            Accessibility service enabled:
            ${if (result.accessibilityEnabled) "YES ⚠️" else "NO"}

            Device administrator active:
            ${if (result.deviceAdminEnabled) "YES ⚠️" else "NO"}

            VPN currently active:
            ${if (result.vpnActive) "YES" else "NO"}

            Developer options:
            ${if (result.developerOptionsEnabled) "ON ⚠️" else "OFF"}

            ADB debugging:
            ${if (result.adbEnabled) "ON ⚠️" else "OFF"}

            Secure screen lock:
            ${if (result.secureLockEnabled) "YES ✅" else "NO ⚠️"}

            Unknown-source installation capability:
            ${if (result.unknownSourcesAllowed) "ALLOWED ⚠️" else "NOT ALLOWED"}

            IMPORTANT:
            These counts show requested permissions/capabilities,
            not proof that an application actually used a sensor.

            Android Privacy Dashboard and Android's own
            privacy indicators remain authoritative for actual
            recent camera, microphone and location access.
        """.trimIndent()
    }
}
