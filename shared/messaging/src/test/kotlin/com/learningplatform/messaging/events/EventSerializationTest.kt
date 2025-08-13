package com.learningplatform.messaging.events

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.time.Instant
import java.util.*

class EventSerializationTest {

    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        objectMapper = ObjectMapper()
            .registerModule(KotlinModule.Builder().build())
            .registerModule(JavaTimeModule())
    }

    @Test
    fun `should serialize and deserialize UserRegisteredEvent`() {
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
        val json = objectMapper.writeValueAsString(event)
        val deserializedEvent = objectMapper.readValue<UserRegisteredEvent>(json)

        // Then
        assertEquals(event.userId, deserializedEvent.userId)
        assertEquals(event.email, deserializedEvent.email)
        assertEquals(event.firstName, deserializedEvent.firstName)
        assertEquals(event.lastName, deserializedEvent.lastName)
        assertEquals(event.role, deserializedEvent.role)
        assertEquals(event.eventType, deserializedEvent.eventType)
    }

    @Test
    fun `should serialize and deserialize SessionStartedEvent`() {
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
        val json = objectMapper.writeValueAsString(event)
        val deserializedEvent = objectMapper.readValue<SessionStartedEvent>(json)

        // Then
        assertEquals(event.sessionId, deserializedEvent.sessionId)
        assertEquals(event.userId, deserializedEvent.userId)
        assertEquals(event.topic, deserializedEvent.topic)
        assertEquals(event.estimatedDurationMinutes, deserializedEvent.estimatedDurationMinutes)
        assertEquals(event.eventType, deserializedEvent.eventType)
    }

    @Test
    fun `should serialize and deserialize AgentCompletedEvent`() {
        // Given
        val sessionId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val event = AgentCompletedEvent(
            sessionId = sessionId,
            userId = userId,
            agentType = "ContentGenerator",
            stageNumber = 2,
            durationMinutes = 15L,
            executionId = UUID.randomUUID(),
            tokensUsed = 1500,
            costUsd = 3.75
        )

        // When
        val json = objectMapper.writeValueAsString(event)
        val deserializedEvent = objectMapper.readValue<AgentCompletedEvent>(json)

        // Then
        assertEquals(event.sessionId, deserializedEvent.sessionId)
        assertEquals(event.userId, deserializedEvent.userId)
        assertEquals(event.agentType, deserializedEvent.agentType)
        assertEquals(event.stageNumber, deserializedEvent.stageNumber)
        assertEquals(event.durationMinutes, deserializedEvent.durationMinutes)
        assertEquals(event.costUsd, deserializedEvent.costUsd)
        assertEquals(event.tokensUsed, deserializedEvent.tokensUsed)
        assertEquals(event.eventType, deserializedEvent.eventType)
    }

    @Test
    fun `should serialize and deserialize ContentGeneratedEvent`() {
        // Given
        val contentId = UUID.randomUUID()
        val sessionId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val event = ContentGeneratedEvent(
            contentId = contentId,
            sessionId = sessionId,
            userId = userId,
            contentType = "lesson",
            title = "Introduction to Neural Networks",
            wordCount = 2500,
            qualityScore = 0.88
        )

        // When
        val json = objectMapper.writeValueAsString(event)
        val deserializedEvent = objectMapper.readValue<ContentGeneratedEvent>(json)

        // Then
        assertEquals(event.contentId, deserializedEvent.contentId)
        assertEquals(event.sessionId, deserializedEvent.sessionId)
        assertEquals(event.userId, deserializedEvent.userId)
        assertEquals(event.contentType, deserializedEvent.contentType)
        assertEquals(event.title, deserializedEvent.title)
        assertEquals(event.wordCount, deserializedEvent.wordCount)
        assertEquals(event.qualityScore, deserializedEvent.qualityScore)
        assertEquals(event.eventType, deserializedEvent.eventType)
    }

    @Test
    fun `should serialize and deserialize PaymentSubscriptionActivatedEvent`() {
        // Given
        val userId = UUID.randomUUID()
        val subscriptionId = UUID.randomUUID()
        val event = PaymentSubscriptionActivatedEvent(
            userId = userId,
            subscriptionId = subscriptionId,
            planType = "PREMIUM",
            features = listOf("unlimited_sessions", "priority_support", "advanced_analytics")
        )

        // When
        val json = objectMapper.writeValueAsString(event)
        val deserializedEvent = objectMapper.readValue<PaymentSubscriptionActivatedEvent>(json)

        // Then
        assertEquals(event.userId, deserializedEvent.userId)
        assertEquals(event.subscriptionId, deserializedEvent.subscriptionId)
        assertEquals(event.planType, deserializedEvent.planType)
        assertEquals(event.features, deserializedEvent.features)
        assertEquals(event.eventType, deserializedEvent.eventType)
    }

    @Test
    fun `should serialize and deserialize SystemMaintenanceStartedEvent`() {
        // Given
        val event = SystemMaintenanceStartedEvent(
            maintenanceType = "DATABASE_UPGRADE",
            estimatedDurationMinutes = 120,
            affectedServices = listOf("user-service", "content-service", "agent-orchestrator")
        )

        // When
        val json = objectMapper.writeValueAsString(event)
        val deserializedEvent = objectMapper.readValue<SystemMaintenanceStartedEvent>(json)

        // Then
        assertEquals(event.maintenanceType, deserializedEvent.maintenanceType)
        assertEquals(event.estimatedDurationMinutes, deserializedEvent.estimatedDurationMinutes)
        assertEquals(event.affectedServices, deserializedEvent.affectedServices)
        assertEquals(event.eventType, deserializedEvent.eventType)
    }

    @Test
    fun `should handle polymorphic deserialization with BaseEvent`() {
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
        val json = objectMapper.writeValueAsString(event)
        val deserializedEvent = objectMapper.readValue<BaseEvent>(json)

        // Then
        assertTrue(deserializedEvent is UserRegisteredEvent)
        val userEvent = deserializedEvent as UserRegisteredEvent
        assertEquals(event.userId, userEvent.userId)
        assertEquals(event.email, userEvent.email)
        assertEquals(event.eventType, userEvent.eventType)
    }

    @Test
    fun `should preserve correlation ID and timestamp during serialization`() {
        // Given
        val userId = UUID.randomUUID()
        val correlationId = UUID.randomUUID()
        val timestamp = Instant.now()
        val event = UserRegisteredEvent(
            userId = userId,
            email = "test@example.com",
            firstName = "John",
            lastName = "Doe",
            role = "USER",
            correlationId = correlationId,
            timestamp = timestamp
        )

        // When
        val json = objectMapper.writeValueAsString(event)
        val deserializedEvent = objectMapper.readValue<UserRegisteredEvent>(json)

        // Then
        assertEquals(correlationId, deserializedEvent.correlationId)
        assertEquals(timestamp, deserializedEvent.timestamp)
    }

    @Test
    fun `should serialize event with null userId`() {
        // Given
        val event = SystemMaintenanceStartedEvent(
            maintenanceType = "SCHEDULED_MAINTENANCE",
            estimatedDurationMinutes = 30,
            affectedServices = listOf("all-services")
        )

        // When
        val json = objectMapper.writeValueAsString(event)
        val deserializedEvent = objectMapper.readValue<SystemMaintenanceStartedEvent>(json)

        // Then
        assertEquals(event.maintenanceType, deserializedEvent.maintenanceType)
        assertEquals(event.estimatedDurationMinutes, deserializedEvent.estimatedDurationMinutes)
        assertEquals(event.affectedServices, deserializedEvent.affectedServices)
        assertEquals(null, deserializedEvent.userId)
    }

    @Test
    fun `should include eventType in JSON for proper deserialization`() {
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
        val json = objectMapper.writeValueAsString(event)

        // Then
        assertTrue(json.contains("\"eventType\":\"user.registered.v1\""))
        assertNotNull(json)
    }
}