package com.example.util

import java.security.SecureRandom

object SecurityHelper {

    private val secureRandom = SecureRandom()

    private const val ALPHANUMERIC_SPECIAL = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*-_=+"
    private const val NUMERIC = "0123456789"
    private const val CLOCK_ID_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    /**
     * Generate 50-character random cryptographic unlock token
     */
    fun generateSecretToken50(): String {
        val sb = StringBuilder(50)
        for (i in 0 until 50) {
            val randomIndex = secureRandom.nextInt(ALPHANUMERIC_SPECIAL.length)
            sb.append(ALPHANUMERIC_SPECIAL[randomIndex])
        }
        return sb.toString()
    }

    /**
     * Generate 8-digit emergency PIN
     */
    fun generateEmergencyPin8(): String {
        val sb = StringBuilder(8)
        for (i in 0 until 8) {
            val randomIndex = secureRandom.nextInt(NUMERIC.length)
            sb.append(NUMERIC[randomIndex])
        }
        return sb.toString()
    }

    /**
     * Generate 6-character Clock ID (e.g., "ND-9482" or "K8X2M9")
     */
    fun generateClockId(): String {
        val sb = StringBuilder(6)
        sb.append("ND-")
        for (i in 0 until 3) {
            val randomIndex = secureRandom.nextInt(CLOCK_ID_CHARS.length)
            sb.append(CLOCK_ID_CHARS[randomIndex])
        }
        return sb.toString()
    }

    /**
     * Timing-safe token verification to prevent timing attacks
     */
    fun verifyToken(inputToken: String, actualToken: String): Boolean {
        if (inputToken.length != actualToken.length) return false
        var result = 0
        for (i in inputToken.indices) {
            result = result or (inputToken[i].code xor actualToken[i].code)
        }
        return result == 0
    }

    /**
     * Verify 8-digit emergency PIN
     */
    fun verifyPin(inputPin: String, actualPin: String): Boolean {
        if (inputPin.trim().length != actualPin.trim().length) return false
        return inputPin.trim() == actualPin.trim()
    }
}
