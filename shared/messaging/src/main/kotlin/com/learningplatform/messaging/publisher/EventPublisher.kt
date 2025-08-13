package com.learningplatform.messaging.publisher

import com.learningplatform.messaging.events.BaseEvent
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*
import java.util.concurrent.CompletableFuture

/**
 * Event Publisher Service
 * Handles publishing events to Redpanda message queue with proper routing,
 * correlation tracking, and error handling
 */
@Service
class EventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, Any>,
    private val stringKafkaTemplate: KafkaTemplate<String, String>
) {

    companion object {
        // Topic routing configuration
        private val TOPIC_ROUTING = mapOf(
            "user" to "user-events",
            "learning" to "learning-events",
            "session" to "session-events",
            "agent" to "agent-events",
            "content" to "content-events",
            "payment" to "payment-events",
            "system" to "system-events",
            "analytics" to "analytics-events",
            "notification" to "notification-events"
        )
        
        private const val DEFAULT_TOPIC = "learning-platform-events"
        private const val DLQ_TOPIC_SUFFIX = "-dlq"
    }

    /**
     * Publish a domain event to the appropriate topic
     * Automatically routes events based on event type and adds correlation tracking
     */
    fun publishEvent(event: BaseEvent): CompletableFuture<SendResult<String, Any>> {
        try {
            // Enrich event with correlation data
            val enrichedEvent = enrichEvent(event)
            
            // Determine topic based on event type
            val topic = determineTopicForEvent(enrichedEvent)
            
            // Create partition key for ordered processing
            val partitionKey = createPartitionKey(enrichedEvent)
            
            // Publish event
            val future = kafkaTemplate.send(topic, partitionKey, enrichedEvent)
            
            // Add success/failure callbacks
            future.whenComplete { result, exception ->
                if (exception != null) {
                    handlePublishError(enrichedEvent, topic, exception)
                } else {
                    handlePublishSuccess(enrichedEvent, topic, result)
                }
            }
            
            return future
        } catch (exception: Exception) {
            throw EventPublishException("Failed to publish event: ${event.eventType}", exception)
        }
    }

    /**
     * Publish multiple events in batch for better performance
     */
    fun publishEvents(events: List<BaseEvent>): List<CompletableFuture<SendResult<String, Any>>> {
        return events.map { event -> publishEvent(event) }
    }

    /**
     * Publish a simple string message to a specific topic
     */
    fun publishMessage(topic: String, key: String, message: String): CompletableFuture<SendResult<String, String>> {
        return stringKafkaTemplate.send(topic, key, message)
    }

    /**
     * Publish event to specific topic (override automatic routing)
     */
    fun publishEventToTopic(event: BaseEvent, topic: String): CompletableFuture<SendResult<String, Any>> {
        val enrichedEvent = enrichEvent(event)
        val partitionKey = createPartitionKey(enrichedEvent)
        
        return kafkaTemplate.send(topic, partitionKey, enrichedEvent)
    }

    /**
     * Publish event with custom partition key
     */
    fun publishEventWithKey(event: BaseEvent, partitionKey: String): CompletableFuture<SendResult<String, Any>> {
        val enrichedEvent = enrichEvent(event)
        val topic = determineTopicForEvent(enrichedEvent)
        
        return kafkaTemplate.send(topic, partitionKey, enrichedEvent)
    }

    /**
     * Enrich event with correlation and tracking data
     */
    private fun enrichEvent(event: BaseEvent): BaseEvent {
        // Return event with updated timestamp if needed
        return if (event.timestamp.isBefore(Instant.now().minusSeconds(60))) {
            // Create a new event with updated timestamp
            when (event) {
                is com.learningplatform.messaging.events.UserRegisteredEvent -> 
                    event.copy(timestamp = Instant.now())
                is com.learningplatform.messaging.events.SessionStartedEvent -> 
                    event.copy(timestamp = Instant.now())
                is com.learningplatform.messaging.events.AgentStartedEvent -> 
                    event.copy(timestamp = Instant.now())
                else -> event
            }
        } else {
            event
        }
    }

    /**
     * Determine the appropriate topic for an event
     */
    private fun determineTopicForEvent(event: BaseEvent): String {
        val eventCategory = event.eventType.split(".").firstOrNull()?.lowercase()
        return TOPIC_ROUTING[eventCategory] ?: DEFAULT_TOPIC
    }

    /**
     * Create partition key for ordered processing
     * Events with the same key will be processed in order
     */
    private fun createPartitionKey(event: BaseEvent): String {
        return when {
            event.userId != null -> event.userId.toString()
            else -> event.eventType
        }
    }

    /**
     * Handle successful event publication
     */
    private fun handlePublishSuccess(
        event: BaseEvent,
        topic: String,
        result: SendResult<String, Any>
    ) {
        val metadata = result.recordMetadata
        println("Event published successfully:")
        println("  Event Type: ${event.eventType}")
        println("  Topic: $topic")
        println("  Partition: ${metadata.partition()}")
        println("  Offset: ${metadata.offset()}")
        println("  Timestamp: ${metadata.timestamp()}")
        println("  Correlation ID: ${event.correlationId}")
    }

    /**
     * Handle event publication errors
     */
    private fun handlePublishError(
        event: BaseEvent,
        topic: String,
        exception: Throwable
    ) {
        println("Failed to publish event:")
        println("  Event Type: ${event.eventType}")
        println("  Topic: $topic")
        println("  Error: ${exception.message}")
        println("  Correlation ID: ${event.correlationId}")
        
        // Attempt to send to dead letter queue
        try {
            val dlqTopic = topic + DLQ_TOPIC_SUFFIX
            val dlqMessage = mapOf(
                "originalEvent" to event,
                "error" to exception.message,
                "failedTopic" to topic,
                "failureTimestamp" to Instant.now().toString()
            )
            
            kafkaTemplate.send(dlqTopic, event.userId?.toString() ?: "unknown", dlqMessage)
            println("Event sent to DLQ: $dlqTopic")
        } catch (dlqException: Exception) {
            println("Failed to send event to DLQ: ${dlqException.message}")
        }
    }
}

/**
 * Exception thrown when event publication fails
 */
class EventPublishException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)