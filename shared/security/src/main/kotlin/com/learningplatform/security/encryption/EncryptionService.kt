package com.learningplatform.security.encryption

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@Service
class EncryptionService(
    @Value("\${app.encryption.master-key:#{null}}")
    private val masterKeyBase64: String?
) {
    
    private val algorithm = "AES"
    private val transformation = "AES/GCM/NoPadding"
    private val gcmIvLength = 12
    private val gcmTagLength = 16
    private val secureRandom = SecureRandom()
    
    private val masterKey: SecretKey by lazy {
        if (masterKeyBase64 != null) {
            val keyBytes = Base64.getDecoder().decode(masterKeyBase64)
            SecretKeySpec(keyBytes, algorithm)
        } else {
            // Generate a new key for development/testing
            generateKey()
        }
    }
    
    /**
     * Encrypts sensitive data using AES-GCM encryption
     * @param plaintext The data to encrypt
     * @return Base64-encoded encrypted data with IV prepended
     */
    fun encrypt(plaintext: String): String {
        if (plaintext.isBlank()) {
            throw IllegalArgumentException("Plaintext cannot be blank")
        }
        
        val cipher = Cipher.getInstance(transformation)
        
        // Generate random IV
        val iv = ByteArray(gcmIvLength)
        secureRandom.nextBytes(iv)
        
        val gcmParameterSpec = GCMParameterSpec(gcmTagLength * 8, iv)
        cipher.init(Cipher.ENCRYPT_MODE, masterKey, gcmParameterSpec)
        
        val encryptedData = cipher.doFinal(plaintext.toByteArray(StandardCharsets.UTF_8))
        
        // Prepend IV to encrypted data
        val encryptedWithIv = ByteArray(iv.size + encryptedData.size)
        System.arraycopy(iv, 0, encryptedWithIv, 0, iv.size)
        System.arraycopy(encryptedData, 0, encryptedWithIv, iv.size, encryptedData.size)
        
        return Base64.getEncoder().encodeToString(encryptedWithIv)
    }
    
    /**
     * Decrypts data that was encrypted with encrypt()
     * @param encryptedData Base64-encoded encrypted data with IV prepended
     * @return The original plaintext
     */
    fun decrypt(encryptedData: String): String {
        if (encryptedData.isBlank()) {
            throw IllegalArgumentException("Encrypted data cannot be blank")
        }
        
        val encryptedWithIv = Base64.getDecoder().decode(encryptedData)
        
        if (encryptedWithIv.size < gcmIvLength + gcmTagLength) {
            throw IllegalArgumentException("Invalid encrypted data format")
        }
        
        // Extract IV and encrypted data
        val iv = ByteArray(gcmIvLength)
        val encrypted = ByteArray(encryptedWithIv.size - gcmIvLength)
        
        System.arraycopy(encryptedWithIv, 0, iv, 0, gcmIvLength)
        System.arraycopy(encryptedWithIv, gcmIvLength, encrypted, 0, encrypted.size)
        
        val cipher = Cipher.getInstance(transformation)
        val gcmParameterSpec = GCMParameterSpec(gcmTagLength * 8, iv)
        cipher.init(Cipher.DECRYPT_MODE, masterKey, gcmParameterSpec)
        
        val decryptedData = cipher.doFinal(encrypted)
        return String(decryptedData, StandardCharsets.UTF_8)
    }
    
    /**
     * Encrypts API keys and other sensitive configuration data
     * Uses envelope encryption for additional security
     */
    fun encryptApiKey(apiKey: String, userId: String): String {
        if (apiKey.isBlank()) {
            throw IllegalArgumentException("API key cannot be blank")
        }
        
        // Generate a data encryption key (DEK) for this specific API key
        val dek = generateKey()
        
        // Encrypt the API key with the DEK
        val cipher = Cipher.getInstance(transformation)
        val iv = ByteArray(gcmIvLength)
        secureRandom.nextBytes(iv)
        
        val gcmParameterSpec = GCMParameterSpec(gcmTagLength * 8, iv)
        cipher.init(Cipher.ENCRYPT_MODE, dek, gcmParameterSpec)
        
        // Add user ID as additional authenticated data
        cipher.updateAAD(userId.toByteArray(StandardCharsets.UTF_8))
        
        val encryptedApiKey = cipher.doFinal(apiKey.toByteArray(StandardCharsets.UTF_8))
        
        // Encrypt the DEK with the master key
        val encryptedDek = encryptKey(dek)
        
        // Combine IV + encrypted API key + encrypted DEK
        val result = ByteArray(iv.size + encryptedApiKey.size + encryptedDek.size)
        System.arraycopy(iv, 0, result, 0, iv.size)
        System.arraycopy(encryptedApiKey, 0, result, iv.size, encryptedApiKey.size)
        System.arraycopy(encryptedDek, 0, result, iv.size + encryptedApiKey.size, encryptedDek.size)
        
        return Base64.getEncoder().encodeToString(result)
    }
    
    /**
     * Decrypts API keys encrypted with encryptApiKey()
     */
    fun decryptApiKey(encryptedData: String, userId: String): String {
        if (encryptedData.isBlank()) {
            throw IllegalArgumentException("Encrypted data cannot be blank")
        }
        
        val data = Base64.getDecoder().decode(encryptedData)
        
        // Extract components
        val iv = ByteArray(gcmIvLength)
        val encryptedDekSize = 32 + gcmTagLength // AES-256 key + GCM tag
        val encryptedApiKeySize = data.size - gcmIvLength - encryptedDekSize
        
        if (encryptedApiKeySize <= 0) {
            throw IllegalArgumentException("Invalid encrypted data format")
        }
        
        val encryptedApiKey = ByteArray(encryptedApiKeySize)
        val encryptedDek = ByteArray(encryptedDekSize)
        
        System.arraycopy(data, 0, iv, 0, gcmIvLength)
        System.arraycopy(data, gcmIvLength, encryptedApiKey, 0, encryptedApiKeySize)
        System.arraycopy(data, gcmIvLength + encryptedApiKeySize, encryptedDek, 0, encryptedDekSize)
        
        // Decrypt the DEK
        val dek = decryptKey(encryptedDek)
        
        // Decrypt the API key
        val cipher = Cipher.getInstance(transformation)
        val gcmParameterSpec = GCMParameterSpec(gcmTagLength * 8, iv)
        cipher.init(Cipher.DECRYPT_MODE, dek, gcmParameterSpec)
        
        // Add user ID as additional authenticated data
        cipher.updateAAD(userId.toByteArray(StandardCharsets.UTF_8))
        
        val decryptedApiKey = cipher.doFinal(encryptedApiKey)
        return String(decryptedApiKey, StandardCharsets.UTF_8)
    }
    
    /**
     * Generates a new AES-256 key
     */
    fun generateKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(algorithm)
        keyGenerator.init(256)
        return keyGenerator.generateKey()
    }
    
    /**
     * Generates a secure random string for tokens, passwords, etc.
     */
    fun generateSecureToken(length: Int = 32): String {
        val bytes = ByteArray(length)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
    
    /**
     * Generates a cryptographically secure random UUID
     */
    fun generateSecureUUID(): UUID {
        val randomBytes = ByteArray(16)
        secureRandom.nextBytes(randomBytes)
        
        // Set version (4) and variant bits according to RFC 4122
        randomBytes[6] = (randomBytes[6].toInt() and 0x0f or 0x40).toByte()
        randomBytes[8] = (randomBytes[8].toInt() and 0x3f or 0x80).toByte()
        
        val mostSigBits = bytesToLong(randomBytes, 0)
        val leastSigBits = bytesToLong(randomBytes, 8)
        
        return UUID(mostSigBits, leastSigBits)
    }
    
    private fun encryptKey(key: SecretKey): ByteArray {
        val cipher = Cipher.getInstance(transformation)
        val iv = ByteArray(gcmIvLength)
        secureRandom.nextBytes(iv)
        
        val gcmParameterSpec = GCMParameterSpec(gcmTagLength * 8, iv)
        cipher.init(Cipher.ENCRYPT_MODE, masterKey, gcmParameterSpec)
        
        val encryptedKey = cipher.doFinal(key.encoded)
        
        val result = ByteArray(iv.size + encryptedKey.size)
        System.arraycopy(iv, 0, result, 0, iv.size)
        System.arraycopy(encryptedKey, 0, result, iv.size, encryptedKey.size)
        
        return result
    }
    
    private fun decryptKey(encryptedData: ByteArray): SecretKey {
        val iv = ByteArray(gcmIvLength)
        val encrypted = ByteArray(encryptedData.size - gcmIvLength)
        
        System.arraycopy(encryptedData, 0, iv, 0, gcmIvLength)
        System.arraycopy(encryptedData, gcmIvLength, encrypted, 0, encrypted.size)
        
        val cipher = Cipher.getInstance(transformation)
        val gcmParameterSpec = GCMParameterSpec(gcmTagLength * 8, iv)
        cipher.init(Cipher.DECRYPT_MODE, masterKey, gcmParameterSpec)
        
        val keyBytes = cipher.doFinal(encrypted)
        return SecretKeySpec(keyBytes, algorithm)
    }
    
    private fun bytesToLong(bytes: ByteArray, offset: Int): Long {
        var result = 0L
        for (i in 0..7) {
            result = result shl 8 or (bytes[offset + i].toInt() and 0xFF).toLong()
        }
        return result
    }
}