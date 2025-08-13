package com.learningplatform.domain.dto

import com.learningplatform.domain.user.Role
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.*

// User DTOs for API communication
data class UserDto(
    val id: UUID,
    val email: String,
    val firstName: String,
    val lastName: String,
    val emailVerified: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
    val roles: List<UserRoleDto>,
    val preferences: Map<String, String>
)

data class UserRoleDto(
    val id: UUID,
    val role: Role,
    val grantedAt: Instant,
    val expiresAt: Instant? = null,
    val grantedBy: UUID? = null
)

data class CreateUserRequest(
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

data class UpdateUserRequest(
    @field:Size(min = 1, max = 100, message = "First name must be between 1 and 100 characters")
    val firstName: String? = null,
    
    @field:Size(min = 1, max = 100, message = "Last name must be between 1 and 100 characters")
    val lastName: String? = null,
    
    val preferences: Map<String, String>? = null
)

data class LoginRequest(
    @field:Email(message = "Email must be valid")
    @field:NotBlank(message = "Email is required")
    val email: String,
    
    @field:NotBlank(message = "Password is required")
    val password: String
)

data class LoginResponse(
    val success: Boolean,
    val data: LoginData,
    val meta: ResponseMeta
)

data class LoginData(
    val user: UserDto,
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long // seconds
)

data class UserRegistrationRequest(
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

data class UserRegistrationResponse(
    val success: Boolean,
    val data: UserRegistrationData,
    val meta: ResponseMeta
)

data class UserRegistrationData(
    val user: UserDto,
    val emailVerificationRequired: Boolean
)

data class PasswordResetRequest(
    @field:Email(message = "Email must be valid")
    @field:NotBlank(message = "Email is required")
    val email: String
)

data class PasswordResetConfirmRequest(
    @field:NotBlank(message = "Reset token is required")
    val resetToken: String,
    
    @field:NotBlank(message = "New password is required")
    @field:Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    val newPassword: String
)

data class ChangePasswordRequest(
    @field:NotBlank(message = "Current password is required")
    val currentPassword: String,
    
    @field:NotBlank(message = "New password is required")
    @field:Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    val newPassword: String
)

data class EmailVerificationRequest(
    @field:NotBlank(message = "Verification token is required")
    val verificationToken: String
)

data class RefreshTokenRequest(
    @field:NotBlank(message = "Refresh token is required")
    val refreshToken: String
)

data class RefreshTokenResponse(
    val success: Boolean,
    val data: RefreshTokenData,
    val meta: ResponseMeta
)

data class RefreshTokenData(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long // seconds
)