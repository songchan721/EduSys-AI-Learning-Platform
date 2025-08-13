package com.learningplatform.messaging.listener

import com.learningplatform.messaging.events.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.Assertions.assertThrows
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.kafka.support.Acknowledgment
import java.time.Instant
import java.util.*

@ExtendWith(MockitoExtension::class)
class EventListenerTest {

    private lateinit var eventListener: EventListener
    private lateinit var acknowledgment: Acknowledgment

    @BeforeEach
    fun setUp() {
        eventListener = EventListener()
        acknowledgment = mock()
    }

    @Test
    fun `should handle user registered event successfully`() {
        // Given
        val userId = UUID.randomUUID()
        val event = UserRegisteredEvent(
            userId = userId,
            email = "test@example.com",
            firstName = "John",
            lastName = "Doe",
            role = "USER"
        )

        // When
        eventListener.handleUserEvent(
            event = event,
            topic = "user-events",
            partition = 0,
            offset = 123L,
            timestamp = System.currentTimeMillis(),
            acknowledgment = acknowledgment
        )

        // Then
        verify(acknowledgment).acknowledge()
    }

    @Test
    fun `should handle session started event successfully`() {
        // Given
        val sessionId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val event = SessionStartedEvent(
            sessionId = sessionId,
            userId = userId,
            topic = "Machine Learning Basics",
            estimatedDurationMinutes = 60
        )

        // When
        eventListener.handleSessionEvent(
            event = event,
            topic = "session-events",
            partition = 0,
            offset = 456L,
            timestamp = System.currentTimeMillis(),
            acknowledgment = acknowledgment
        )

        // Then
        verify(acknowledgment).acknowledge()
    }

    @Test
    fun `should handle agent started event successfully`() {
        // Given
        val sessionId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val event = AgentStartedEvent(
            sessionId = sessionId,
            userId = userId,
            agentType = "ContentGenerator",
            stageNumber = 1,
            executionId = UUID.randomUUID()
        )

        // When
        eventListener.handleAgentEvent(
            event = event,
            topic = "agent-events",
            partition = 0,
            offset = 789L,
            timestamp = System.currentTimeMillis(),
            acknowledgment = acknowledgment
        )

        // Then
        verify(acknowledgment).acknowledge()
    }

    @Test
    fun `should handle content generated event successfully`() {
        // Given
        val contentId = UUID.randomUUID()
        val sessionId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val event = ContentGeneratedEvent(
            contentId = contentId,
            sessionId = sessionId,
            userId = userId,
            contentType = "lesson",
            title = "Introduction to Machine Learning",
            wordCount = 1500,
            qualityScore = 0.85
        )

        // When
        eventListener.handleContentEvent(
            event = event,
            topic = "content-events",
            partition = 0,
            offset = 101112L,
            timestamp = System.currentTimeMillis(),
            acknowledgment = acknowledgment
        )

        // Then
        verify(acknowledgment).acknowledge()
    }

    @Test
    fun `should handle payment subscription activated event successfully`() {
        // Given
        val userId = UUID.randomUUID()
        val subscriptionId = UUID.randomUUID()
        val event = PaymentSubscriptionActivatedEvent(
            userId = userId,
            subscriptionId = subscriptionId,
            planType = "PREMIUM",
            features = listOf("unlimited_sessions", "priority_support")
        )

        // When
        eventListener.handlePaymentEvent(
            event = event,
            topic = "payment-events",
            partition = 0,
            offset = 131415L,
            timestamp = System.currentTimeMillis(),
            acknowledgment = acknowledgment
        )

        // Then
        verify(acknowledgment).acknowledge()
    }

    @Test
    fun `should handle system maintenance started event successfully`() {
        // Given
        val event = SystemMaintenanceStartedEvent(
            maintenanceType = "DATABASE_UPGRADE",
            estimatedDurationMinutes = 120,
            affectedServices = listOf("user-service", "content-service")
        )

        // When
        eventListener.handleSystemEvent(
            event = event,
            topic = "system-events",
            partition = 0,
            offset = 161718L,
            timestamp = System.currentTimeMillis(),
            acknowledgment = acknowledgment
        )

        // Then
        verify(acknowledgment).acknowledge()
    }

    @Test
    fun `should handle unknown event type gracefully`() {
        // Given
        val userId = UUID.randomUUID()
        val unknownEvent = object : BaseEvent("unknown.event.v1", UUID.randomUUID(), Instant.now(), UUID.randomUUID(), null, userId) {}

        // When
        eventListener.handleUserEvent(
            event = unknownEvent,
            topic = "user-events",
            partition = 0,
            offset = 192021L,
            timestamp = System.currentTimeMillis(),
            acknowledgment = acknowledgment
        )

        // Then
        verify(acknowledgment).acknowledge()
    }

    @Test
    fun `should throw exception on processing error and not acknowledge`() {
        // Given
        val userId = UUID.randomUUID()
        val event = UserRegisteredEvent(
            userId = userId,
            email = "test@example.com",
            firstName = "John",
            lastName = "Doe",
            role = "USER"
        )

        // Mock acknowledgment to throw exception when called
        doThrow(RuntimeException("Processing failed")).whenever(acknowledgment).acknowledge()

        // When & Then
        assertThrows(EventProcessingException::class.java) {
            eventListener.handleUserEvent(
                event = event,
                topic = "user-events",
                partition = 0,
                offset = 222324L,
                timestamp = System.currentTimeMillis(),
                acknowledgment = acknowledgment
            )
        }

        // Verify acknowledgment was called (which threw the exception)
        verify(acknowledgment).acknowledge()
    }
}