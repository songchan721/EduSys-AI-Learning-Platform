package com.learningplatform.security.jwt

import com.learningplatform.domain.user.Role
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * JWT Token Service for authentication and authorization
 * Implements JWT token generation and validation without external dependencies
 */
@Service
class JwtTokenService(
    @Value("\${app.jwt.secret:mySecretKey123456789012345678901234567890}")
    private val jwtSecret: String,
    
    @Value("\${app.jwt.access-token-expiration:86400}") // 24 hours default
    private val accessTokenExpiration: Long,
    
    @Value("\${app.jwt.refresh-token-expiration:604800}") // 7 days default
    private val refreshTokenExpiration: Long,
    
    @Value("\${app.jwt.issuer:learning-platform}")
    private val issuer: String
) {
    
    private val algorithm = "HmacSHA256"
    
    /**
     * Generates an access token for the user
     */
    fun generateAccessToken(userId: UUID, email: String, roles: Set<Role>): String {
        val now = Instant.now()
        val expiration = now.plus(accessTokenExpiration, ChronoUnit.SECONDS)
        
        val header = createHeader()
        val payload = createPayload(userId, email, roles, now, expiration, "access")
        
        return createToken(header, payload)
    }
    
    /**
     * Generates a refresh token for the user
     */
    fun generateRefreshToken(userId: UUID): String {
        val now = Instant.now()
        val expiration = now.plus(refreshTokenExpiration, ChronoUnit.SECONDS)
        
        val header = createHeader()
        val payload = createRefreshPayload(userId, now, expiration)
        
        return createToken(header, payload)
    }
    
    /**
     * Validates a JWT token and returns the claims if valid
     */
    fun validateToken(token: String): TokenClaims? {
        return try {
            val parts = token.split(".")
            if (parts.size != 3) return null
            
            val header = parts[0]
            val payload = parts[1]
            val signature = parts[2]
            
            // Verify signature
            val expectedSignature = createSignature("$header.$payload")
            if (signature != expectedSignature) return null
            
            // Parse payload
            val payloadJson = String(Base64.getUrlDecoder().decode(payload))
            parseTokenClaims(payloadJson)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Extracts user ID from a valid token
     */
    fun getUserIdFromToken(token: String): UUID? {
        val claims = validateToken(token)
        return claims?.subject?.let { UUID.fromString(it) }
    }
    
    /**
     * Extracts email from a valid token
     */
    fun getEmailFromToken(token: String): String? {
        val claims = validateToken(token)
        return claims?.email
    }
    
    /**
     * Extracts roles from a valid token
     */
    fun getRolesFromToken(token: String): Set<Role> {
        val claims = validateToken(token)
        return claims?.roles ?: emptySet()
    }
    
    /**
     * Checks if a token is expired
     */
    fun isTokenExpired(token: String): Boolean {
        val claims = validateToken(token) ?: return true
        return claims.expiration.isBefore(Instant.now())
    }
    
    /**
     * Checks if a token is an access token
     */
    fun isAccessToken(token: String): Boolean {
        val claims = validateToken(token) ?: return false
        return claims.type == "access"
    }
    
    /**
     * Checks if a token is a refresh token
     */
    fun isRefreshToken(token: String): Boolean {
        val claims = validateToken(token) ?: return false
        return claims.type == "refresh"
    }
    
    /**
     * Generates a password reset token
     */
    fun generatePasswordResetToken(userId: UUID, email: String): String {
        val now = Instant.now()
        val expiration = now.plus(1, ChronoUnit.HOURS) // 1 hour expiration
        
        val header = createHeader()
        val payload = createSpecialPayload(userId, email, now, expiration, "password-reset")
        
        return createToken(header, payload)
    }
    
    /**
     * Generates an email verification token
     */
    fun generateEmailVerificationToken(userId: UUID, email: String): String {
        val now = Instant.now()
        val expiration = now.plus(24, ChronoUnit.HOURS) // 24 hour expiration
        
        val header = createHeader()
        val payload = createSpecialPayload(userId, email, now, expiration, "email-verification")
        
        return createToken(header, payload)
    }
    
    private fun createHeader(): String {
        val header = """{"alg":"HS256","typ":"JWT"}"""
        return Base64.getUrlEncoder().withoutPadding().encodeToString(header.toByteArray())
    }
    
    private fun createPayload(
        userId: UUID, 
        email: String, 
        roles: Set<Role>, 
        issuedAt: Instant, 
        expiration: Instant,
        type: String
    ): String {
        val roleNames = roles.map { it.name }.joinToString(",")
        val payload = """{
            "iss":"$issuer",
            "sub":"$userId",
            "email":"$email",
            "roles":"$roleNames",
            "type":"$type",
            "iat":${issuedAt.epochSecond},
            "exp":${expiration.epochSecond}
        }""".replace("\\s+".toRegex(), "")
        
        return Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray())
    }
    
    private fun createRefreshPayload(userId: UUID, issuedAt: Instant, expiration: Instant): String {
        val payload = """{
            "iss":"$issuer",
            "sub":"$userId",
            "type":"refresh",
            "iat":${issuedAt.epochSecond},
            "exp":${expiration.epochSecond}
        }""".replace("\\s+".toRegex(), "")
        
        return Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray())
    }
    
    private fun createSpecialPayload(
        userId: UUID, 
        email: String, 
        issuedAt: Instant, 
        expiration: Instant,
        type: String
    ): String {
        val payload = """{
            "iss":"$issuer",
            "sub":"$userId",
            "email":"$email",
            "type":"$type",
            "iat":${issuedAt.epochSecond},
            "exp":${expiration.epochSecond}
        }""".replace("\\s+".toRegex(), "")
        
        return Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray())
    }
    
    private fun createToken(header: String, payload: String): String {
        val signature = createSignature("$header.$payload")
        return "$header.$payload.$signature"
    }
    
    private fun createSignature(data: String): String {
        val mac = Mac.getInstance(algorithm)
        val secretKey = SecretKeySpec(jwtSecret.toByteArray(), algorithm)
        mac.init(secretKey)
        val signature = mac.doFinal(data.toByteArray())
        return Base64.getUrlEncoder().withoutPadding().encodeToString(signature)
    }
    
    private fun parseTokenClaims(payloadJson: String): TokenClaims? {
        return try {
            // Simple JSON parsing for JWT claims
            val issuer = extractJsonValue(payloadJson, "iss")
            val subject = extractJsonValue(payloadJson, "sub")
            val email = extractJsonValue(payloadJson, "email")
            val rolesStr = extractJsonValue(payloadJson, "roles")
            val type = extractJsonValue(payloadJson, "type")
            val iat = extractJsonValue(payloadJson, "iat")?.toLongOrNull()
            val exp = extractJsonValue(payloadJson, "exp")?.toLongOrNull()
            
            val roles = rolesStr?.split(",")?.mapNotNull { roleName ->
                try { Role.valueOf(roleName.trim()) } catch (e: Exception) { null }
            }?.toSet() ?: emptySet()
            
            TokenClaims(
                issuer = issuer ?: "",
                subject = subject ?: "",
                email = email,
                roles = roles,
                type = type ?: "",
                issuedAt = iat?.let { Instant.ofEpochSecond(it) } ?: Instant.now(),
                expiration = exp?.let { Instant.ofEpochSecond(it) } ?: Instant.now()
            )
        } catch (e: Exception) {
            null
        }
    }
    
    private fun extractJsonValue(json: String, key: String): String? {
        val pattern = """"$key":"([^"]+)"""".toRegex()
        return pattern.find(json)?.groupValues?.get(1)
    }
}

/**
 * Token claims data class
 */
data class TokenClaims(
    val issuer: String,
    val subject: String,
    val email: String?,
    val roles: Set<Role>,
    val type: String,
    val issuedAt: Instant,
    val expiration: Instant
)