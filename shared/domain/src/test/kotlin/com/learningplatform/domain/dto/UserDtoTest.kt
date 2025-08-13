package com.learningplatform.domain.dto

import jakarta.validation.Validation
import jakarta.validation.Validator
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class UserDtoTest {

    private lateinit var validator: Validator

    @BeforeEach
    fun setUp() {
        val factory = Validation.buildDefaultValidatorFactory()
        validator = factory.validator
    }

    @Test
    fun `should validate CreateUserRequest with valid data`() {
        // Given
        val request = CreateUserRequest(
            email = "test@example.com",
            password = "SecurePass123!",
            firstName = "John",
            lastName = "Doe"
        )

        // When
        val violations = validator.validate(request)

        // Then
        assertTrue(violations.isEmpty())
    }

    @Test
    fun `should reject CreateUserRequest with invalid email`() {
        // Given
        val request = CreateUserRequest(
            email = "invalid-email",
            password = "SecurePass123!",
            firstName = "John",
            lastName = "Doe"
        )

        // When
        val violations = validator.validate(request)

        // Then
        assertFalse(violations.isEmpty())
        assertTrue(violations.any { it.propertyPath.toString() == "email" })
    }

    @Test
    fun `should reject CreateUserRequest with short password`() {
        // Given
        val request = CreateUserRequest(
            email = "test@example.com",
            password = "short",
            firstName = "John",
            lastName = "Doe"
        )

        // When
        val violations = validator.validate(request)

        // Then
        assertFalse(violations.isEmpty())
        assertTrue(violations.any { it.propertyPath.toString() == "password" })
    }

    @Test
    fun `should reject CreateUserRequest with blank first name`() {
        // Given
        val request = CreateUserRequest(
            email = "test@example.com",
            password = "SecurePass123!",
            firstName = "",
            lastName = "Doe"
        )

        // When
        val violations = validator.validate(request)

        // Then
        assertFalse(violations.isEmpty())
        assertTrue(violations.any { it.propertyPath.toString() == "firstName" })
    }

    @Test
    fun `should reject CreateUserRequest with blank last name`() {
        // Given
        val request = CreateUserRequest(
            email = "test@example.com",
            password = "SecurePass123!",
            firstName = "John",
            lastName = ""
        )

        // When
        val violations = validator.validate(request)

        // Then
        assertFalse(violations.isEmpty())
        assertTrue(violations.any { it.propertyPath.toString() == "lastName" })
    }

    @Test
    fun `should validate LoginRequest with valid data`() {
        // Given
        val request = LoginRequest(
            email = "test@example.com",
            password = "password123"
        )

        // When
        val violations = validator.validate(request)

        // Then
        assertTrue(violations.isEmpty())
    }

    @Test
    fun `should reject LoginRequest with invalid email`() {
        // Given
        val request = LoginRequest(
            email = "not-an-email",
            password = "password123"
        )

        // When
        val violations = validator.validate(request)

        // Then
        assertFalse(violations.isEmpty())
        assertTrue(violations.any { it.propertyPath.toString() == "email" })
    }

    @Test
    fun `should validate UpdateUserRequest with partial data`() {
        // Given
        val request = UpdateUserRequest(
            firstName = "UpdatedName",
            lastName = null,
            preferences = mapOf("theme" to "dark")
        )

        // When
        val violations = validator.validate(request)

        // Then
        assertTrue(violations.isEmpty())
    }

    @Test
    fun `should validate UpdateUserRequest with empty preferences`() {
        // Given
        val request = UpdateUserRequest(
            firstName = "UpdatedName",
            lastName = "UpdatedLastName",
            preferences = emptyMap()
        )

        // When
        val violations = validator.validate(request)

        // Then
        assertTrue(violations.isEmpty())
    }

    @Test
    fun `should reject UpdateUserRequest with too long first name`() {
        // Given
        val longName = "a".repeat(101) // 101 characters
        val request = UpdateUserRequest(
            firstName = longName,
            lastName = "Doe"
        )

        // When
        val violations = validator.validate(request)

        // Then
        assertFalse(violations.isEmpty())
        assertTrue(violations.any { it.propertyPath.toString() == "firstName" })
    }

    @Test
    fun `should validate PasswordResetRequest with valid email`() {
        // Given
        val request = PasswordResetRequest(
            email = "test@example.com"
        )

        // When
        val violations = validator.validate(request)

        // Then
        assertTrue(violations.isEmpty())
    }

    @Test
    fun `should validate ChangePasswordRequest with valid passwords`() {
        // Given
        val request = ChangePasswordRequest(
            currentPassword = "oldPassword123",
            newPassword = "NewSecurePass456!"
        )

        // When
        val violations = validator.validate(request)

        // Then
        assertTrue(violations.isEmpty())
    }

    @Test
    fun `should reject ChangePasswordRequest with short new password`() {
        // Given
        val request = ChangePasswordRequest(
            currentPassword = "oldPassword123",
            newPassword = "short"
        )

        // When
        val violations = validator.validate(request)

        // Then
        assertFalse(violations.isEmpty())
        assertTrue(violations.any { it.propertyPath.toString() == "newPassword" })
    }
}