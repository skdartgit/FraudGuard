package com.sanat.fraudguard

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.provider.Settings
import java.util.Locale

object PrivacyAudit {

    data class Result(
        val cameraApps: Int,
        val microphoneApps: Int,
        val locationApps: Int,
        val accessibilityEnabled: Boolean,
        val deviceAdminEnabled: Boolean,
        val vpnActive: Boolean
    )

    fun run(
        context: Context
    ): Result {

        val pm = context.packageManager

        var camera = 0
        var microphone = 0
        var location = 0

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

        return Result(
            cameraApps = camera,
            microphoneApps = microphone,
            locationApps = location,
            accessibilityEnabled = accessibility,
            deviceAdminEnabled = deviceAdmin,
            vpnActive = vpn
        )
    }

    fun asText(
        result: Result
    ): String {

        return """
            PRIVACY / SECURITY AUDIT

            Apps requesting camera permission:
            ${result.cameraApps}

            Apps requesting microphone permission:
            ${result.microphoneApps}

            Apps requesting location permission:
            ${result.locationApps}

            Accessibility service enabled:
            ${if (result.accessibilityEnabled) "YES ⚠️" else "NO"}

            Device administrator active:
            ${if (result.deviceAdminEnabled) "YES ⚠️" else "NO"}

            VPN currently active:
            ${if (result.vpnActive) "YES" else "NO"}

            IMPORTANT:
            Android does not expose unrestricted information
            about every camera/microphone access made by every
            application. Android's own privacy indicators and
            Privacy Dashboard remain authoritative for that.
        """.trimIndent()
    }
}
