    package com.learningplatform.security.jwt

import com.learningplatform.domain.user.Role
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.util.*

class JwtTokenServiceTest {

    private lateinit var jwtService: JwtTokenService
    
    @BeforeEach
    fun setUp() {
        jwtService = JwtTokenService(
            jwtSecret = "mySecretKey123456789012345678901234567890",
            accessTokenExpiration = 3600, // 1 hour
            refreshTokenExpiration = 86400, // 24 hours
            issuer = "test-issuer"
        )
    }
    
    @Test
    fun `generateAccessToken should create valid token`() {
        // Given
        val userId = UUID.randomUUID()
        val email = "test@example.com"
        val roles = setOf(Role.FREE_USER, Role.PRO_USER)
        
        // When
        val token = jwtService.generateAccessToken(userId, email, roles)
        
        // Then
        assertNotNull(token)
        assertTrue(token.split(".").size == 3) // JWT has 3 parts
        
        // Verify token can be validated
        val claims = jwtService.validateToken(token)
        assertNotNull(claims)
        assertEquals(userId.toString(), claims!!.subject)
        assertEquals(email, claims.email)
        assertEquals(roles, claims.roles)
        assertEquals("access", claims.type)
    }
    
    @Test
    fun `generateRefreshToken should create valid token`() {
        // Given
        val userId = UUID.randomUUID()
        
        // When
        val token = jwtService.generateRefreshToken(userId)
        
        // Then
        assertNotNull(token)
        assertTrue(token.split(".").size == 3)
        
        // Verify token can be validated
        val claims = jwtService.validateToken(token)
        assertNotNull(claims)
        assertEquals(userId.toString(), claims!!.subject)
        assertEquals("refresh", claims.type)
    }
    
    @Test
    fun `validateToken should return null for invalid token`() {
        // Given
        val invalidToken = "invalid.token.here"
        
        // When
        val claims = jwtService.validateToken(invalidToken)
        
        // Then
        assertNull(claims)
    }
    
    @Test
    fun `getUserIdFromToken should extract user ID`() {
        // Given
        val userId = UUID.randomUUID()
        val email = "test@example.com"
        val roles = setOf(Role.FREE_USER)
        val token = jwtService.generateAccessToken(userId, email, roles)
        
        // When
        val extractedUserId = jwtService.getUserIdFromToken(token)
        
        // Then
        assertEquals(userId, extractedUserId)
    }
    
    @Test
    fun `getEmailFromToken should extract email`() {
        // Given
        val userId = UUID.randomUUID()
        val email = "test@example.com"
        val roles = setOf(Role.FREE_USER)
        val token = jwtService.generateAccessToken(userId, email, roles)
        
        // When
        val extractedEmail = jwtService.getEmailFromToken(token)
        
        // Then
        assertEquals(email, extractedEmail)
    }
    
    @Test
    fun `getRolesFromToken should extract roles`() {
        // Given
        val userId = UUID.randomUUID()
        val email = "test@example.com"
        val roles = setOf(Role.FREE_USER, Role.PRO_USER)
        val token = jwtService.generateAccessToken(userId, email, roles)
        
        // When
        val extractedRoles = jwtService.getRolesFromToken(token)
        
        // Then
        assertEquals(roles, extractedRoles)
    }
    
    @Test
    fun `isAccessToken should identify access tokens`() {
        // Given
        val userId = UUID.randomUUID()
        val email = "test@example.com"
        val roles = setOf(Role.FREE_USER)
        val accessToken = jwtService.generateAccessToken(userId, email, roles)
        val refreshToken = jwtService.generateRefreshToken(userId)
        
        // When & Then
        assertTrue(jwtService.isAccessToken(accessToken))
        assertFalse(jwtService.isAccessToken(refreshToken))
    }
    
    @Test
    fun `isRefreshToken should identify refresh tokens`() {
        // Given
        val userId = UUID.randomUUID()
        val email = "test@example.com"
        val roles = setOf(Role.FREE_USER)
        val accessToken = jwtService.generateAccessToken(userId, email, roles)
        val refreshToken = jwtService.generateRefreshToken(userId)
        
        // When & Then
        assertTrue(jwtService.isRefreshToken(refreshToken))
        assertFalse(jwtService.isRefreshToken(accessToken))
    }
    
    @Test
    fun `generatePasswordResetToken should create valid token`() {
        // Given
        val userId = UUID.randomUUID()
        val email = "test@example.com"
        
        // When
        val token = jwtService.generatePasswordResetToken(userId, email)
        
        // Then
        assertNotNull(token)
        
        val claims = jwtService.validateToken(token)
        assertNotNull(claims)
        assertEquals(userId.toString(), claims!!.subject)
        assertEquals(email, claims.email)
        assertEquals("password-reset", claims.type)
    }
    
    @Test
    fun `generateEmailVerificationToken should create valid token`() {
        // Given
        val userId = UUID.randomUUID()
        val email = "test@example.com"
        
        // When
        val token = jwtService.generateEmailVerificationToken(userId, email)
        
        // Then
        assertNotNull(token)
        
        val claims = jwtService.validateToken(token)
        assertNotNull(claims)
        assertEquals(userId.toString(), claims!!.subject)
        assertEquals(email, claims.email)
        assertEquals("email-verification", claims.type)
    }
    
    @Test
    fun `token should be invalid with wrong signature`() {
        // Given
        val userId = UUID.randomUUID()
        val email = "test@example.com"
        val roles = setOf(Role.FREE_USER)
        val token = jwtService.generateAccessToken(userId, email, roles)
        
        // Tamper with the token
        val parts = token.split(".")
        val tamperedToken = "${parts[0]}.${parts[1]}.wrongsignature"
        
        // When
        val claims = jwtService.validateToken(tamperedToken)
        
        // Then
        assertNull(claims)
    }
}