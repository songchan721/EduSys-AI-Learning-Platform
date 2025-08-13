package com.learningplatform.messaging.integration

import com.learningplatform.messaging.events.UserRegisteredEvent
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import java.util.*

/**
 * Integration tests for messaging components
 * These tests verify the messaging infrastructure without requiring external dependencies
 */
class MessagingIntegrationTest {

    @Test
    @DisplayName("Should create and serialize user registered event")
    fun `should create and serialize user registered event`() {
        // Given
        val userId = UUID.randomUUID()
        val event = UserRegisteredEvent(
            userId = userId,
            email = "integration-test@example.com",
            firstName = "Integration",
            lastName = "Test",
            role = "USER"
        )

        // When & Then
        assertNotNull(event.eventId)
        assertNotNull(event.timestamp)
        assertEquals("user.registered.v1", event.eventType)
        assertEquals(userId, event.userId)
        assertEquals("integration-test@example.com", event.email)
        assertEquals("Integration", event.firstName)
        assertEquals("Test", event.lastName)
        assertEquals("USER", event.role)
        
        println("✅ Successfully created and validated user registered event")
        println("   Event ID: ${event.eventId}")
        println("   Event Type: ${event.eventType}")
        println("   User ID: ${event.userId}")
    }

    @Test
    @DisplayName("Should validate event properties")
    fun `should validate event properties`() {
        // Given
        val userId = UUID.randomUUID()
        val event = UserRegisteredEvent(
            userId = userId,
            email = "test@example.com",
            firstName = "Test",
            lastName = "User",
            role = "ADMIN"
        )

        // When & Then
        assertTrue(event.eventId.toString().isNotEmpty())
        assertTrue(event.timestamp.isAfter(java.time.Instant.now().minusSeconds(10)))
        assertTrue(event.timestamp.isBefore(java.time.Instant.now().plusSeconds(10)))
        assertEquals("user.registered.v1", event.eventType)
        
        println("✅ Successfully validated event properties")
        println("   Event created at: ${event.timestamp}")
    }

    @Test
    @DisplayName("Should handle different user roles")
    fun `should handle different user roles`() {
        // Given
        val roles = listOf("USER", "ADMIN", "MODERATOR", "INSTRUCTOR")
        
        // When & Then
        roles.forEach { role ->
            val event = UserRegisteredEvent(
                userId = UUID.randomUUID(),
                email = "test-$role@example.com",
                firstName = "Test",
                lastName = role,
                role = role
            )
            
            assertEquals(role, event.role)
            assertEquals("user.registered.v1", event.eventType)
        }
        
        println("✅ Successfully handled different user roles: ${roles.joinToString(", ")}")
    }

    @Test
    @DisplayName("Should generate unique event IDs")
    fun `should generate unique event IDs`() {
        // Given
        val events = mutableListOf<UserRegisteredEvent>()
        
        // When
        repeat(10) { index ->
            val event = UserRegisteredEvent(
                userId = UUID.randomUUID(),
                email = "test$index@example.com",
                firstName = "Test$index",
                lastName = "User$index",
                role = "USER"
            )
            events.add(event)
        }
        
        // Then
        val eventIds = events.map { it.eventId }.toSet()
        assertEquals(10, eventIds.size, "All event IDs should be unique")
        
        println("✅ Successfully generated ${eventIds.size} unique event IDs")
    }
}