package com.learningplatform.domain.user.dto

import com.learningplatform.domain.user.Role
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.time.Instant
import java.util.*

class UserDtoTest {

    @Test
    fun `UserDto should be created with all required fields`() {
        // Given
        val id = UUID.randomUUID()
        val email = "test@example.com"
        val firstName = "John"
        val lastName = "Doe"
        val emailVerified = true
        val createdAt = Instant.now()
        val updatedAt = Instant.now()
        val roles = setOf(Role.FREE_USER)
        val preferences = mapOf("theme" to "dark", "language" to "en")
        
        // When
        val userDto = UserDto(
            id = id,
            email = email,
            firstName = firstName,
            lastName = lastName,
            emailVerified = emailVerified,
            createdAt = createdAt,
            updatedAt = updatedAt,
            roles = roles,
            preferences = preferences
        )
        
        // Then
        assertEquals(id, userDto.id)
        assertEquals(email, userDto.email)
        assertEquals(firstName, userDto.firstName)
        assertEquals(lastName, userDto.lastName)
        assertEquals(emailVerified, userDto.emailVerified)
        assertEquals(createdAt, userDto.createdAt)
        assertEquals(updatedAt, userDto.updatedAt)
        assertEquals(roles, userDto.roles)
        assertEquals(preferences, userDto.preferences)
    }
    
    @Test
    fun `getDisplayName should return formatted full name`() {
        // Given
        val userDto = createTestUserDto(firstName = "John", lastName = "Doe")
        
        // When
        val displayName = userDto.getDisplayName()
        
        // Then
        assertEquals("John Doe", displayName)
    }
    
    @Test
    fun `hasRole should return true when user has the role`() {
        // Given
        val userDto = createTestUserDto(roles = setOf(Role.PRO_USER, Role.FREE_USER))
        
        // When & Then
        assertTrue(userDto.hasRole(Role.PRO_USER))
        assertTrue(userDto.hasRole(Role.FREE_USER))
        assertFalse(userDto.hasRole(Role.ADMIN))
    }
    
    @Test
    fun `getPrimaryRole should return highest role`() {
        // Test SUPER_ADMIN as highest
        val superAdminUser = createTestUserDto(roles = setOf(Role.SUPER_ADMIN, Role.ADMIN, Role.PRO_USER))
        assertEquals(Role.SUPER_ADMIN, superAdminUser.getPrimaryRole())
        
        // Test ADMIN as highest
        val adminUser = createTestUserDto(roles = setOf(Role.ADMIN, Role.PRO_USER, Role.FREE_USER))
        assertEquals(Role.ADMIN, adminUser.getPrimaryRole())
        
        // Test ENTERPRISE_USER as highest
        val enterpriseUser = createTestUserDto(roles = setOf(Role.ENTERPRISE_USER, Role.PRO_USER))
        assertEquals(Role.ENTERPRISE_USER, enterpriseUser.getPrimaryRole())
        
        // Test PRO_USER as highest
        val proUser = createTestUserDto(roles = setOf(Role.PRO_USER, Role.FREE_USER))
        assertEquals(Role.PRO_USER, proUser.getPrimaryRole())
        
        // Test FREE_USER as default
        val freeUser = createTestUserDto(roles = setOf(Role.FREE_USER))
        assertEquals(Role.FREE_USER, freeUser.getPrimaryRole())
    }
    
    @Test
    fun `UserRegistrationDto should be created with required fields`() {
        // Given
        val email = "newuser@example.com"
        val password = "securePassword123"
        val firstName = "Jane"
        val lastName = "Smith"
        
        // When
        val registrationDto = UserRegistrationDto(
            email = email,
            password = password,
            firstName = firstName,
            lastName = lastName
        )
        
        // Then
        assertEquals(email, registrationDto.email)
        assertEquals(password, registrationDto.password)
        assertEquals(firstName, registrationDto.firstName)
        assertEquals(lastName, registrationDto.lastName)
    }
    
    @Test
    fun `UserLoginDto should be created with email and password`() {
        // Given
        val email = "user@example.com"
        val password = "password123"
        
        // When
        val loginDto = UserLoginDto(email = email, password = password)
        
        // Then
        assertEquals(email, loginDto.email)
        assertEquals(password, loginDto.password)
    }
    
    @Test
    fun `UserUpdateDto should handle optional fields`() {
        // Test with both fields
        val updateDto1 = UserUpdateDto(firstName = "NewFirst", lastName = "NewLast")
        assertEquals("NewFirst", updateDto1.firstName)
        assertEquals("NewLast", updateDto1.lastName)
        
        // Test with only firstName
        val updateDto2 = UserUpdateDto(firstName = "NewFirst", lastName = null)
        assertEquals("NewFirst", updateDto2.firstName)
        assertNull(updateDto2.lastName)
        
        // Test with only lastName
        val updateDto3 = UserUpdateDto(firstName = null, lastName = "NewLast")
        assertNull(updateDto3.firstName)
        assertEquals("NewLast", updateDto3.lastName)
    }
    
    @Test
    fun `UserPreferencesDto should store preferences map`() {
        // Given
        val preferences = mapOf(
            "theme" to "dark",
            "language" to "en",
            "notifications" to "enabled"
        )
        
        // When
        val preferencesDto = UserPreferencesDto(preferences)
        
        // Then
        assertEquals(preferences, preferencesDto.preferences)
        assertEquals("dark", preferencesDto.preferences["theme"])
        assertEquals("en", preferencesDto.preferences["language"])
        assertEquals("enabled", preferencesDto.preferences["notifications"])
    }
    
    @Test
    fun `PasswordChangeDto should be created with current and new passwords`() {
        // Given
        val currentPassword = "oldPassword123"
        val newPassword = "newSecurePassword456"
        
        // When
        val passwordChangeDto = PasswordChangeDto(
            currentPassword = currentPassword,
            newPassword = newPassword
        )
        
        // Then
        assertEquals(currentPassword, passwordChangeDto.currentPassword)
        assertEquals(newPassword, passwordChangeDto.newPassword)
    }
    
    @Test
    fun `PasswordResetRequestDto should be created with email`() {
        // Given
        val email = "reset@example.com"
        
        // When
        val resetRequestDto = PasswordResetRequestDto(email = email)
        
        // Then
        assertEquals(email, resetRequestDto.email)
    }
    
    @Test
    fun `PasswordResetDto should be created with token and new password`() {
        // Given
        val resetToken = "reset-token-123"
        val newPassword = "newPassword789"
        
        // When
        val passwordResetDto = PasswordResetDto(
            resetToken = resetToken,
            newPassword = newPassword
        )
        
        // Then
        assertEquals(resetToken, passwordResetDto.resetToken)
        assertEquals(newPassword, passwordResetDto.newPassword)
    }
    
    private fun createTestUserDto(
        id: UUID = UUID.randomUUID(),
        email: String = "test@example.com",
        firstName: String = "Test",
        lastName: String = "User",
        emailVerified: Boolean = true,
        createdAt: Instant = Instant.now(),
        updatedAt: Instant = Instant.now(),
        roles: Set<Role> = setOf(Role.FREE_USER),
        preferences: Map<String, String> = emptyMap()
    ): UserDto {
        return UserDto(
            id = id,
            email = email,
            firstName = firstName,
            lastName = lastName,
            emailVerified = emailVerified,
            createdAt = createdAt,
            updatedAt = updatedAt,
            roles = roles,
            preferences = preferences
        )
    }
}