package com.learningplatform.messaging.events

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.Instant
import java.util.*

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "eventType"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = UserRegisteredEvent::class, name = "user.registered.v1"),
    JsonSubTypes.Type(value = UserUpdatedEvent::class, name = "user.updated.v1"),
    JsonSubTypes.Type(value = UserRoleChangedEvent::class, name = "user.role-changed.v1"),
    JsonSubTypes.Type(value = SessionStartedEvent::class, name = "session.started.v1"),
    JsonSubTypes.Type(value = SessionCompletedEvent::class, name = "session.completed.v1"),
    JsonSubTypes.Type(value = SessionFailedEvent::class, name = "session.failed.v1"),
    JsonSubTypes.Type(value = AgentStartedEvent::class, name = "agent.started.v1"),
    JsonSubTypes.Type(value = AgentCompletedEvent::class, name = "agent.completed.v1"),
    JsonSubTypes.Type(value = AgentFailedEvent::class, name = "agent.failed.v1"),
    JsonSubTypes.Type(value = ContentGeneratedEvent::class, name = "content.generated.v1"),
    JsonSubTypes.Type(value = ContentUpdatedEvent::class, name = "content.updated.v1"),
    JsonSubTypes.Type(value = PaymentSubscriptionActivatedEvent::class, name = "payment.subscription-activated.v1"),
    JsonSubTypes.Type(value = PaymentSubscriptionCancelledEvent::class, name = "payment.subscription-cancelled.v1"),
    JsonSubTypes.Type(value = SystemMaintenanceStartedEvent::class, name = "system.maintenance-started.v1"),
    JsonSubTypes.Type(value = SystemAlertTriggeredEvent::class, name = "system.alert-triggered.v1")
)
abstract class BaseEvent(
    open val eventType: String,
    open val eventId: UUID = UUID.randomUUID(),
    open val timestamp: Instant = Instant.now(),
    open val correlationId: UUID = UUID.randomUUID(),
    open val causationId: UUID? = null,
    open val userId: UUID? = null,
    open val metadata: EventMetadata = EventMetadata()
)

data class EventMetadata(
    val source: String = "unknown",
    val version: String = "1.0.0",
    val environment: String = "development",
    val region: String = "us-east-1",
    val schemaVersion: String = "1.0"
)

// User Events
data class UserRegisteredEvent(
    override val eventType: String = "user.registered.v1",
    override val eventId: UUID = UUID.randomUUID(),
    override val timestamp: Instant = Instant.now(),
    override val correlationId: UUID = UUID.randomUUID(),
    override val causationId: UUID? = null,
    override val userId: UUID,
    override val metadata: EventMetadata = EventMetadata(),
    val email: String,
    val firstName: String,
    val lastName: String,
    val role: String
) : BaseEvent(eventType, eventId, timestamp, correlationId, causationId, userId, metadata)

data class UserUpdatedEvent(
    override val eventType: String = "user.updated.v1",
    override val eventId: UUID = UUID.randomUUID(),
    override val timestamp: Instant = Instant.now(),
    override val correlationId: UUID = UUID.randomUUID(),
    override val causationId: UUID? = null,
    override val userId: UUID,
    override val metadata: EventMetadata = EventMetadata(),
    val changes: Map<String, Any>
) : BaseEvent(eventType, eventId, timestamp, correlationId, causationId, userId, metadata)

data class UserRoleChangedEvent(
    override val eventType: String = "user.role-changed.v1",
    override val eventId: UUID = UUID.randomUUID(),
    override val timestamp: Instant = Instant.now(),
    override val correlationId: UUID = UUID.randomUUID(),
    override val causationId: UUID? = null,
    override val userId: UUID,
    override val metadata: EventMetadata = EventMetadata(),
    val oldRole: String,
    val newRole: String,
    val changedBy: UUID
) : BaseEvent(eventType, eventId, timestamp, correlationId, causationId, userId, metadata)

// Session Events
data class SessionStartedEvent(
    override val eventType: String = "session.started.v1",
    override val eventId: UUID = UUID.randomUUID(),
    override val timestamp: Instant = Instant.now(),
    override val correlationId: UUID = UUID.randomUUID(),
    override val causationId: UUID? = null,
    override val userId: UUID,
    override val metadata: EventMetadata = EventMetadata(),
    val sessionId: UUID,
    val topic: String,
    val estimatedDurationMinutes: Int?
) : BaseEvent(eventType, eventId, timestamp, correlationId, causationId, userId, metadata)

data class SessionCompletedEvent(
    override val eventType: String = "session.completed.v1",
    override val eventId: UUID = UUID.randomUUID(),
    override val timestamp: Instant = Instant.now(),
    override val correlationId: UUID = UUID.randomUUID(),
    override val causationId: UUID? = null,
    override val userId: UUID,
    override val metadata: EventMetadata = EventMetadata(),
    val sessionId: UUID,
    val actualDurationMinutes: Int,
    val qualityScore: Double?,
    val totalCostUsd: Double?
) : BaseEvent(eventType, eventId, timestamp, correlationId, causationId, userId, metadata)

data class SessionFailedEvent(
    override val eventType: String = "session.failed.v1",
    override val eventId: UUID = UUID.randomUUID(),
    override val timestamp: Instant = Instant.now(),
    override val correlationId: UUID = UUID.randomUUID(),
    override val causationId: UUID? = null,
    override val userId: UUID,
    override val metadata: EventMetadata = EventMetadata(),
    val sessionId: UUID,
    val errorMessage: String,
    val failedAtStage: String
) : BaseEvent(eventType, eventId, timestamp, correlationId, causationId, userId, metadata)

// Agent Events
data class AgentStartedEvent(
    override val eventType: String = "agent.started.v1",
    override val eventId: UUID = UUID.randomUUID(),
    override val timestamp: Instant = Instant.now(),
    override val correlationId: UUID = UUID.randomUUID(),
    override val causationId: UUID? = null,
    override val userId: UUID,
    override val metadata: EventMetadata = EventMetadata(),
    val sessionId: UUID,
    val executionId: UUID,
    val agentType: String,
    val stageNumber: Int
) : BaseEvent(eventType, eventId, timestamp, correlationId, causationId, userId, metadata)

data class AgentCompletedEvent(
    override val eventType: String = "agent.completed.v1",
    override val eventId: UUID = UUID.randomUUID(),
    override val timestamp: Instant = Instant.now(),
    override val correlationId: UUID = UUID.randomUUID(),
    override val causationId: UUID? = null,
    override val userId: UUID,
    override val metadata: EventMetadata = EventMetadata(),
    val sessionId: UUID,
    val executionId: UUID,
    val agentType: String,
    val stageNumber: Int,
    val durationMinutes: Long,
    val tokensUsed: Int?,
    val costUsd: Double?
) : BaseEvent(eventType, eventId, timestamp, correlationId, causationId, userId, metadata)

data class AgentFailedEvent(
    override val eventType: String = "agent.failed.v1",
    override val eventId: UUID = UUID.randomUUID(),
    override val timestamp: Instant = Instant.now(),
    override val correlationId: UUID = UUID.randomUUID(),
    override val causationId: UUID? = null,
    override val userId: UUID,
    override val metadata: EventMetadata = EventMetadata(),
    val sessionId: UUID,
    val executionId: UUID,
    val agentType: String,
    val stageNumber: Int,
    val errorMessage: String
) : BaseEvent(eventType, eventId, timestamp, correlationId, causationId, userId, metadata)

// Content Events
data class ContentGeneratedEvent(
    override val eventType: String = "content.generated.v1",
    override val eventId: UUID = UUID.randomUUID(),
    override val timestamp: Instant = Instant.now(),
    override val correlationId: UUID = UUID.randomUUID(),
    override val causationId: UUID? = null,
    override val userId: UUID,
    override val metadata: EventMetadata = EventMetadata(),
    val contentId: UUID,
    val sessionId: UUID,
    val contentType: String,
    val title: String,
    val wordCount: Int?,
    val qualityScore: Double?
) : BaseEvent(eventType, eventId, timestamp, correlationId, causationId, userId, metadata)

data class ContentUpdatedEvent(
    override val eventType: String = "content.updated.v1",
    override val eventId: UUID = UUID.randomUUID(),
    override val timestamp: Instant = Instant.now(),
    override val correlationId: UUID = UUID.randomUUID(),
    override val causationId: UUID? = null,
    override val userId: UUID,
    override val metadata: EventMetadata = EventMetadata(),
    val contentId: UUID,
    val newVersion: Int,
    val changes: String?
) : BaseEvent(eventType, eventId, timestamp, correlationId, causationId, userId, metadata)

// Payment Events
data class PaymentSubscriptionActivatedEvent(
    override val eventType: String = "payment.subscription-activated.v1",
    override val eventId: UUID = UUID.randomUUID(),
    override val timestamp: Instant = Instant.now(),
    override val correlationId: UUID = UUID.randomUUID(),
    override val causationId: UUID? = null,
    override val userId: UUID,
    override val metadata: EventMetadata = EventMetadata(),
    val subscriptionId: UUID,
    val planType: String,
    val features: List<String>
) : BaseEvent(eventType, eventId, timestamp, correlationId, causationId, userId, metadata)

data class PaymentSubscriptionCancelledEvent(
    override val eventType: String = "payment.subscription-cancelled.v1",
    override val eventId: UUID = UUID.randomUUID(),
    override val timestamp: Instant = Instant.now(),
    override val correlationId: UUID = UUID.randomUUID(),
    override val causationId: UUID? = null,
    override val userId: UUID,
    override val metadata: EventMetadata = EventMetadata(),
    val subscriptionId: UUID,
    val reason: String?
) : BaseEvent(eventType, eventId, timestamp, correlationId, causationId, userId, metadata)

// System Events
data class SystemMaintenanceStartedEvent(
    override val eventType: String = "system.maintenance-started.v1",
    override val eventId: UUID = UUID.randomUUID(),
    override val timestamp: Instant = Instant.now(),
    override val correlationId: UUID = UUID.randomUUID(),
    override val causationId: UUID? = null,
    override val userId: UUID? = null,
    override val metadata: EventMetadata = EventMetadata(),
    val maintenanceType: String,
    val estimatedDurationMinutes: Int,
    val affectedServices: List<String>
) : BaseEvent(eventType, eventId, timestamp, correlationId, causationId, userId, metadata)

data class SystemAlertTriggeredEvent(
    override val eventType: String = "system.alert-triggered.v1",
    override val eventId: UUID = UUID.randomUUID(),
    override val timestamp: Instant = Instant.now(),
    override val correlationId: UUID = UUID.randomUUID(),
    override val causationId: UUID? = null,
    override val userId: UUID? = null,
    override val metadata: EventMetadata = EventMetadata(),
    val alertType: String,
    val severity: String,
    val message: String,
    val affectedServices: List<String>
) : BaseEvent(eventType, eventId, timestamp, correlationId, causationId, userId, metadata)