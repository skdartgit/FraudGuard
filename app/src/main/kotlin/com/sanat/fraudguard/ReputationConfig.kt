package com.sanat.fraudguard

import android.content.Context
import android.util.Base64
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties

object ReputationConfig {

    private const val PREFS = "reputation_config"
    private const val ENABLED = "enabled"
    private const val WEB_RISK = "web_risk"
    private const val IPQS = "ipqs"

    private const val KEY_ALIAS =
        "FraudGuardReputationKey"

    data class Config(
        val enabled: Boolean,
        val webRiskKey: String,
        val ipqsKey: String
    )

    fun get(context: Context): Config {
        val prefs = context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        )

        return Config(
            enabled = prefs.getBoolean(
                ENABLED,
                false
            ),
            webRiskKey = decrypt(
                prefs.getString(
                    WEB_RISK,
                    ""
                ) ?: ""
            ),
            ipqsKey = decrypt(
                prefs.getString(
                    IPQS,
                    ""
                ) ?: ""
            )
        )
    }

    fun save(
        context: Context,
        enabled: Boolean,
        webRiskKey: String,
        ipqsKey: String
    ) {

        context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        ).edit()
            .putBoolean(
                ENABLED,
                enabled
            )
            .putString(
                WEB_RISK,
                encrypt(webRiskKey.trim())
            )
            .putString(
                IPQS,
                encrypt(ipqsKey.trim())
            )
            .apply()
    }

    private fun getKey(): SecretKey {

        val keyStore =
            java.security.KeyStore.getInstance(
                "AndroidKeyStore"
            )

        keyStore.load(null)

        if (
            keyStore.containsAlias(KEY_ALIAS)
        ) {
            return keyStore.getKey(
                KEY_ALIAS,
                null
            ) as SecretKey
        }

        val generator =
            KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                "AndroidKeyStore"
            )

        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or
                    KeyProperties.PURPOSE_DECRYPT
            )
                .setKeySize(256)
                .setBlockModes(
                    KeyProperties.BLOCK_MODE_GCM
                )
                .setEncryptionPaddings(
                    KeyProperties.ENCRYPTION_PADDING_NONE
                )
                .build()
        )

        return generator.generateKey()
    }

    private fun encrypt(
        value: String
    ): String {

        if (value.isEmpty()) {
            return ""
        }

        return try {

            val cipher =
                Cipher.getInstance(
                    "AES/GCM/NoPadding"
                )

            cipher.init(
                Cipher.ENCRYPT_MODE,
                getKey()
            )

            val iv =
                cipher.iv

            val encrypted =
                cipher.doFinal(
                    value.toByteArray(
                        StandardCharsets.UTF_8
                    )
                )

            Base64.encodeToString(
                iv,
                Base64.NO_WRAP
            ) +
                ":" +
                Base64.encodeToString(
                    encrypted,
                    Base64.NO_WRAP
                )

        } catch (_: Exception) {
            ""
        }
    }

    private fun decrypt(
        value: String
    ): String {

        if (value.isEmpty()) {
            return ""
        }

        return try {

            val parts =
                value.split(":")

            if (parts.size != 2) {
                return ""
            }

            val iv =
                Base64.decode(
                    parts[0],
                    Base64.NO_WRAP
                )

            val encrypted =
                Base64.decode(
                    parts[1],
                    Base64.NO_WRAP
                )

            val cipher =
                Cipher.getInstance(
                    "AES/GCM/NoPadding"
                )

            cipher.init(
                Cipher.DECRYPT_MODE,
                getKey(),
                GCMParameterSpec(
                    128,
                    iv
                )
            )

            String(
                cipher.doFinal(encrypted),
                StandardCharsets.UTF_8
            )

        } catch (_: Exception) {
            ""
        }
    }
}
