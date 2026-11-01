package com.example.util

import java.security.MessageDigest

object CryptoUtils {
    /**
     * Mã hóa chuỗi đầu vào sang định dạng băm SHA-256 hex string
     */
    fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Xác minh mã PIN nhập vào so với chuỗi băm SHA-256 đã lưu
     */
    fun verifyPin(inputPin: String, storedHash: String): Boolean {
        if (inputPin.isBlank() || storedHash.isBlank()) return false
        val computedHash = sha256(inputPin.trim())
        return computedHash.equals(storedHash.trim(), ignoreCase = true)
    }
}
