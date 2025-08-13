package com.learningplatform.domain.user

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.util.*

class UserTest {

    @Test
    fun `should create user with basic information`() {
        // Given
        val email = "test@example.com"
        val firstName = "John"
        val lastName = "Doe"
        val passwordHash = "hashedPassword123"

        // When
        val user = User(
            email = email,
            firstName = firstName,
            lastName = lastName,
            passwordHash = passwordHash
        )

        // Then
        assertNotNull(user.id)
        assertEquals(email, user.email)
        assertEquals(firstName, user.firstName)
        assertEquals(lastName, user.lastName)
        assertEquals(passwordHash, user.passwordHash)
        assertFalse(user.emailVerified)
        assertNotNull(user.createdAt)
        assertNotNull(user.updatedAt)
        assertTrue(user.roles.isEmpty())
        assertTrue(user.preferences.isEmpty())
    }

    @Test
    fun `should add and check user roles`() {
        // Given
        val user = User(
            email = "test@example.com",
            firstName = "John",
            lastName = "Doe",
            passwordHash = "hashedPassword123"
        )

        // When
        user.addRole(Role.FREE_USER)
        user.addRole(Role.PRO_USER, UUID.randomUUID())

        // Then
        assertTrue(user.hasRole(Role.FREE_USER))
        assertTrue(user.hasRole(Role.PRO_USER))
        assertFalse(user.hasRole(Role.ADMIN))
        assertEquals(2, user.roles.size)
    }

    @Test
    fun `should manage user preferences`() {
        // Given
        val user = User(
            email = "test@example.com",
            firstName = "John",
            lastName = "Doe",
            passwordHash = "hashedPassword123"
        )

        // When
        user.setPreference("theme", "dark")
        user.setPreference("language", "en")
        user.setPreference("theme", "light") // Update existing preference

        // Then
        assertEquals("light", user.getPreference("theme"))
        assertEquals("en", user.getPreference("language"))
        assertNull(user.getPreference("nonexistent"))
        assertEquals(2, user.preferences.size) // Should not duplicate theme preference
    }

    @Test
    fun `should remove user roles`() {
        // Given
        val user = User(
            email = "test@example.com",
            firstName = "John",
            lastName = "Doe",
            passwordHash = "hashedPassword123"
        )
        user.addRole(Role.FREE_USER)
        user.addRole(Role.PRO_USER)

        // When
        user.removeRole(Role.FREE_USER)

        // Then
        assertFalse(user.hasRole(Role.FREE_USER))
        assertTrue(user.hasRole(Role.PRO_USER))
        assertEquals(1, user.roles.size)
    }
}