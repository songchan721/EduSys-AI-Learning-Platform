package com.learningplatform.domain.user.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.learningplatform.domain.user.Role
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.*

/**
 * Data Transfer Object for User entity
 * Used for API communication between frontend and backend
 */
data class UserDto(
    val id: UUID,
    
    @field:Email(message = "Email must be valid")
    @field:NotBlank(message = "Email is required")
    val email: String,
    
    @field:NotBlank(message = "First name is required")
    @field:Size(min = 1, max = 100, message = "First name must be between 1 and 100 characters")
    val firstName: String,
    
    @field:NotBlank(message = "Last name is required")
    @field:Size(min = 1, max = 100, message = "Last name must be between 1 and 100 characters")
    val lastName: String,
    
    val emailVerified: Boolean,
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX")
    val createdAt: Instant,
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX")
    val updatedAt: Instant,
    
    val roles: Set<Role>,
    val preferences: Map<String, String>
) {
    /**
     * Get the user's display name
     */
    fun getDisplayName(): String = "$firstName $lastName"
    
    /**
     * Check if user has a specific role
     */
    fun hasRole(role: Role): Boolean = roles.contains(role)
    
    /**
     * Get the highest role (for UI display purposes)
     */
    fun getPrimaryRole(): Role {
        return when {
            roles.contains(Role.SUPER_ADMIN) -> Role.SUPER_ADMIN
            roles.contains(Role.ADMIN) -> Role.ADMIN
            roles.contains(Role.ENTERPRISE_USER) -> Role.ENTERPRISE_USER
            roles.contains(Role.PRO_USER) -> Role.PRO_USER
            else -> Role.FREE_USER
        }
    }
}

/**
 * DTO for user registration
 */
data class UserRegistrationDto(
    @field:Email(message = "Email must be valid")
    @field:NotBlank(message = "Email is required")
    val email: String,
    
    @field:NotBlank(message = "Password is required")
    @field:Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    val password: String,
    
    @field:NotBlank(message = "First name is required")
    @field:Size(min = 1, max = 100, message = "First name must be between 1 and 100 characters")
    val firstName: String,
    
    @field:NotBlank(message = "Last name is required")
    @field:Size(min = 1, max = 100, message = "Last name must be between 1 and 100 characters")
    val lastName: String
)

/**
 * DTO for user login
 */
data class UserLoginDto(
    @field:Email(message = "Email must be valid")
    @field:NotBlank(message = "Email is required")
    val email: String,
    
    @field:NotBlank(message = "Password is required")
    val password: String
)

/**
 * DTO for user profile updates
 */
data class UserUpdateDto(
    @field:Size(min = 1, max = 100, message = "First name must be between 1 and 100 characters")
    val firstName: String?,
    
    @field:Size(min = 1, max = 100, message = "Last name must be between 1 and 100 characters")
    val lastName: String?
)

/**
 * DTO for user preferences
 */
data class UserPreferencesDto(
    val preferences: Map<String, String>
)

/**
 * DTO for password change
 */
data class PasswordChangeDto(
    @field:NotBlank(message = "Current password is required")
    val currentPassword: String,
    
    @field:NotBlank(message = "New password is required")
    @field:Size(min = 8, max = 128, message = "New password must be between 8 and 128 characters")
    val newPassword: String
)

/**
 * DTO for password reset request
 */
data class PasswordResetRequestDto(
    @field:Email(message = "Email must be valid")
    @field:NotBlank(message = "Email is required")
    val email: String
)

/**
 * DTO for password reset
 */
data class PasswordResetDto(
    @field:NotBlank(message = "Reset token is required")
    val resetToken: String,
    
    @field:NotBlank(message = "New password is required")
    @field:Size(min = 8, max = 128, message = "New password must be between 8 and 128 characters")
    val newPassword: String
)