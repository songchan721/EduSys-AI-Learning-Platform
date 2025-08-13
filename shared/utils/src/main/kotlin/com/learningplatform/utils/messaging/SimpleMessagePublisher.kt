package com.learningplatform.utils.messaging

import com.fasterxml.jackson.databind.ObjectMapper
import com.learningplatform.messaging.events.BaseEvent
import com.learningplatform.utils.correlation.CorrelationIdService
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*
import java.util.concurrent.CompletableFuture

/**
 * Simple message publisher for Kafka/Redpanda
 */
@Service
class SimpleMessagePublisher(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper,
    private val correlationIdService: CorrelationIdService
) {
    
    private val logger = LoggerFactory.getLogger(SimpleMessagePublisher::class.java)
    
    companion object {
        // Topic naming conventions
        const val USER_EVENTS_TOPIC = "user-events"
        const val SESSION_EVENTS_TOPIC = "session-events"
        const val AGENT_EVENTS_TOPIC = "agent-events"
        const val CONTENT_EVENTS_TOPIC = "content-events"
        const val PAYMENT_EVENTS_TOPIC = "payment-events"
        const val SYSTEM_EVENTS_TOPIC = "system-events"
        const val DEAD_LETTER_TOPIC = "dead-letter-events"
    }
    
    /**
     * Publishes an event to the appropriate topic
     */
    fun publishEvent(event: BaseEvent): CompletableFuture<SendResult<String, String>> {
        return try {
            correlationIdService.startTiming("publish-event")
            
            // Determine topic based on event type
            val topic = getTopicForEvent(event)
            
            // Serialize event to JSON
            val eventJson = objectMapper.writeValueAsString(event)
            
            // Use event ID as message key for partitioning
            val messageKey = event.eventId.toString()
            
            logger.debug("Publishing event {} to topic {}", event.eventId, topic)
            
            // Send message asynchronously
            val future = kafkaTemplate.send(topic, messageKey, eventJson)
            
            // Add success/failure callbacks
            future.whenComplete { result, throwable ->
                val duration = correlationIdService.endTiming("publish-event")
                
                if (throwable != null) {
                    logger.error("Failed to publish event {} to topic {} after {}ms", 
                        event.eventId, topic, duration, throwable)
                } else {
                    logger.debug("Successfully published event {} to topic {} in {}ms", 
                        event.eventId, topic, duration)
                }
            }
            
            future
        } catch (e: Exception) {
            logger.error("Failed to publish event {}", event.eventId, e)
            CompletableFuture.failedFuture(e)
        }
    }
    
    /**
     * Publishes a message to a specific topic
     */
    fun publishToTopic(topic: String, key: String, message: Any): CompletableFuture<SendResult<String, String>> {
        return try {
            correlationIdService.startTiming("publish-to-topic")
            
            val messageJson = when (message) {
                is String -> message
                else -> objectMapper.writeValueAsString(message)
            }
            
            logger.debug("Publishing message to topic {} with key {}", topic, key)
            
            val future = kafkaTemplate.send(topic, key, messageJson)
            
            future.whenComplete { result, throwable ->
                val duration = correlationIdService.endTiming("publish-to-topic")
                
                if (throwable != null) {
                    logger.error("Failed to publish message to topic {} after {}ms", topic, duration, throwable)
                } else {
                    logger.debug("Successfully published message to topic {} in {}ms", topic, duration)
                }
            }
            
            future
        } catch (e: Exception) {
            logger.error("Failed to publish message to topic {}", topic, e)
            CompletableFuture.failedFuture(e)
        }
    }
    
    /**
     * Publishes an event synchronously (blocks until completion)
     */
    fun publishEventSync(event: BaseEvent): SendResult<String, String> {
        return try {
            val future = publishEvent(event)
            future.get() // Block until completion
        } catch (e: Exception) {
            logger.error("Failed to publish event synchronously {}", event.eventId, e)
            throw e
        }
    }
    
    private fun getTopicForEvent(event: BaseEvent): String {
        return when {
            event.eventType.startsWith("user.") -> USER_EVENTS_TOPIC
            event.eventType.startsWith("session.") -> SESSION_EVENTS_TOPIC
            event.eventType.startsWith("agent.") -> AGENT_EVENTS_TOPIC
            event.eventType.startsWith("content.") -> CONTENT_EVENTS_TOPIC
            event.eventType.startsWith("payment.") -> PAYMENT_EVENTS_TOPIC
            else -> {
                logger.warn("Unknown event type {}, using system events topic", event.eventType)
                SYSTEM_EVENTS_TOPIC
            }
        }
    }
}