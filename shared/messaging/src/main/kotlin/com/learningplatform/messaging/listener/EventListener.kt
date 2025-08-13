package com.learningplatform.messaging.listener

import com.learningplatform.messaging.events.BaseEvent
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service
import java.time.Instant

/**
 * Event Listener Service
 * Base service for consuming events from Redpanda message queue
 * Provides common functionality for all event consumers
 */
@Service
class EventListener {

    /**
     * Listen to user events
     * Handles user registration, login, profile updates, etc.
     */
    @KafkaListener(
        topics = ["user-events"],
        groupId = "user-event-processor",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun handleUserEvent(
        @Payload event: BaseEvent,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long,
        @Header(KafkaHeaders.RECEIVED_TIMESTAMP) timestamp: Long,
        acknowledgment: Acknowledgment
    ) {
        processEvent("USER", event, topic, partition, offset, timestamp, acknowledgment) {
            when (event.eventType) {
                "user.registered.v1" -> handleUserRegistered(event)
                "user.updated.v1" -> handleUserUpdated(event)
                "user.role-changed.v1" -> handleUserRoleChanged(event)
                else -> println("Unknown user event type: ${event.eventType}")
            }
        }
    }

    /**
     * Listen to session events
     * Handles learning session events, progress updates, etc.
     */
    @KafkaListener(
        topics = ["session-events"],
        groupId = "session-event-processor",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun handleSessionEvent(
        @Payload event: BaseEvent,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long,
        @Header(KafkaHeaders.RECEIVED_TIMESTAMP) timestamp: Long,
        acknowledgment: Acknowledgment
    ) {
        processEvent("SESSION", event, topic, partition, offset, timestamp, acknowledgment) {
            when (event.eventType) {
                "session.started.v1" -> handleSessionStarted(event)
                "session.completed.v1" -> handleSessionCompleted(event)
                "session.failed.v1" -> handleSessionFailed(event)
                else -> println("Unknown session event type: ${event.eventType}")
            }
        }
    }

    /**
     * Listen to agent events
     * Handles agent execution events, performance metrics, etc.
     */
    @KafkaListener(
        topics = ["agent-events"],
        groupId = "agent-event-processor",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun handleAgentEvent(
        @Payload event: BaseEvent,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long,
        @Header(KafkaHeaders.RECEIVED_TIMESTAMP) timestamp: Long,
        acknowledgment: Acknowledgment
    ) {
        processEvent("AGENT", event, topic, partition, offset, timestamp, acknowledgment) {
            when (event.eventType) {
                "agent.started.v1" -> handleAgentStarted(event)
                "agent.completed.v1" -> handleAgentCompleted(event)
                "agent.failed.v1" -> handleAgentFailed(event)
                else -> println("Unknown agent event type: ${event.eventType}")
            }
        }
    }

    /**
     * Listen to content events
     * Handles content generation, updates, feedback, etc.
     */
    @KafkaListener(
        topics = ["content-events"],
        groupId = "content-event-processor",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun handleContentEvent(
        @Payload event: BaseEvent,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long,
        @Header(KafkaHeaders.RECEIVED_TIMESTAMP) timestamp: Long,
        acknowledgment: Acknowledgment
    ) {
        processEvent("CONTENT", event, topic, partition, offset, timestamp, acknowledgment) {
            when (event.eventType) {
                "content.generated.v1" -> handleContentGenerated(event)
                "content.updated.v1" -> handleContentUpdated(event)
                else -> println("Unknown content event type: ${event.eventType}")
            }
        }
    }

    /**
     * Listen to payment events
     * Handles subscription changes, payment processing, etc.
     */
    @KafkaListener(
        topics = ["payment-events"],
        groupId = "payment-event-processor",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun handlePaymentEvent(
        @Payload event: BaseEvent,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long,
        @Header(KafkaHeaders.RECEIVED_TIMESTAMP) timestamp: Long,
        acknowledgment: Acknowledgment
    ) {
        processEvent("PAYMENT", event, topic, partition, offset, timestamp, acknowledgment) {
            when (event.eventType) {
                "payment.subscription-activated.v1" -> handleSubscriptionActivated(event)
                "payment.subscription-cancelled.v1" -> handleSubscriptionCancelled(event)
                else -> println("Unknown payment event type: ${event.eventType}")
            }
        }
    }

    /**
     * Listen to system events
     * Handles system-wide events, health checks, etc.
     */
    @KafkaListener(
        topics = ["system-events"],
        groupId = "system-event-processor",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun handleSystemEvent(
        @Payload event: BaseEvent,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long,
        @Header(KafkaHeaders.RECEIVED_TIMESTAMP) timestamp: Long,
        acknowledgment: Acknowledgment
    ) {
        processEvent("SYSTEM", event, topic, partition, offset, timestamp, acknowledgment) {
            when (event.eventType) {
                "system.maintenance-started.v1" -> handleSystemMaintenanceStarted(event)
                "system.alert-triggered.v1" -> handleSystemAlertTriggered(event)
                else -> println("Unknown system event type: ${event.eventType}")
            }
        }
    }

    /**
     * Common event processing logic
     * Handles correlation tracking, error handling, and acknowledgment
     */
    private fun processEvent(
        category: String,
        event: BaseEvent,
        topic: String,
        partition: Int,
        offset: Long,
        timestamp: Long,
        acknowledgment: Acknowledgment,
        processor: () -> Unit
    ) {
        val processingStart = Instant.now()
        
        try {
            println("Processing $category event:")
            println("  Event Type: ${event.eventType}")
            println("  Topic: $topic")
            println("  Partition: $partition")
            println("  Offset: $offset")
            println("  Timestamp: $timestamp")
            println("  Correlation ID: ${event.correlationId}")
            println("  User ID: ${event.userId}")
            
            // Process the event
            processor()
            
            // Acknowledge successful processing
            acknowledgment.acknowledge()
            
            val processingTime = java.time.Duration.between(processingStart, Instant.now())
            println("Event processed successfully in ${processingTime.toMillis()}ms")
            
        } catch (exception: Exception) {
            println("Error processing $category event: ${exception.message}")
            println("Event will be retried or sent to DLQ")
            
            // Don't acknowledge - let Kafka retry
            throw EventProcessingException("Failed to process event: ${event.eventType}", exception)
            
        }
    }

    // Event-specific handlers (to be implemented by services)
    private fun handleUserRegistered(event: BaseEvent) {
        println("Handling user registration: ${event}")
        // Implementation will be added by specific services
    }

    private fun handleUserUpdated(event: BaseEvent) {
        println("Handling user update: ${event}")
    }

    private fun handleUserRoleChanged(event: BaseEvent) {
        println("Handling user role change: ${event}")
    }

    private fun handleSessionStarted(event: BaseEvent) {
        println("Handling session start: ${event}")
    }

    private fun handleSessionCompleted(event: BaseEvent) {
        println("Handling session completion: ${event}")
    }

    private fun handleSessionFailed(event: BaseEvent) {
        println("Handling session failure: ${event}")
    }

    private fun handleAgentStarted(event: BaseEvent) {
        println("Handling agent start: ${event}")
    }

    private fun handleAgentCompleted(event: BaseEvent) {
        println("Handling agent completion: ${event}")
    }

    private fun handleAgentFailed(event: BaseEvent) {
        println("Handling agent failure: ${event}")
    }

    private fun handleContentGenerated(event: BaseEvent) {
        println("Handling content generation: ${event}")
    }

    private fun handleContentUpdated(event: BaseEvent) {
        println("Handling content update: ${event}")
    }

    private fun handleSubscriptionActivated(event: BaseEvent) {
        println("Handling subscription activation: ${event}")
    }

    private fun handleSubscriptionCancelled(event: BaseEvent) {
        println("Handling subscription cancellation: ${event}")
    }

    private fun handleSystemMaintenanceStarted(event: BaseEvent) {
        println("Handling system maintenance start: ${event}")
    }

    private fun handleSystemAlertTriggered(event: BaseEvent) {
        println("Handling system alert: ${event}")
    }
}

/**
 * Exception thrown when event processing fails
 */
class EventProcessingException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)