package com.learningplatform.utils.messaging

import com.fasterxml.jackson.databind.ObjectMapper
import com.learningplatform.messaging.events.*
import com.learningplatform.utils.correlation.CorrelationIdService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import java.time.Instant
import java.util.*
import java.util.concurrent.CompletableFuture
import org.junit.jupiter.api.Assertions.*

@ExtendWith(MockitoExtension::class)
class SimpleMessagePublisherTest {

    @Mock
    private lateinit var kafkaTemplate: KafkaTemplate<String, String>
    
    @Mock
    private lateinit var correlationIdService: CorrelationIdService
    
    @Mock
    private lateinit var sendResult: SendResult<String, String>
    
    private lateinit var objectMapper: ObjectMapper
    private lateinit var messagePublisher: SimpleMessagePublisher
    
    @BeforeEach
    fun setUp() {
        objectMapper = ObjectMapper()
        messagePublisher = SimpleMessagePublisher(kafkaTemplate, objectMapper, correlationIdService)
        
        whenever(correlationIdService.startTiming(any())).thenReturn(Unit)
        whenever(correlationIdService.endTiming(any())).thenReturn(100L)
    }
    
    @Test
    fun `publishEvent should publish user event to correct topic`() {
        // Given
        val event = createTestEvent("user.registered.v1")
        val future = CompletableFuture.completedFuture(sendResult)
        
        whenever(kafkaTemplate.send(any<String>(), any<String>(), any<String>()))
            .thenReturn(future)
        
        // When
        val result = messagePublisher.publishEvent(event)
        
        // Then
        assertNotNull(result)
        verify(kafkaTemplate).send(
            eq(SimpleMessagePublisher.USER_EVENTS_TOPIC),
            eq(event.eventId.toString()),
            any<String>()
        )
    }
    
    @Test
    fun `publishEvent should publish session event to correct topic`() {
        // Given
        val event = createTestEvent("session.started.v1")
        val future = CompletableFuture.completedFuture(sendResult)
        
        whenever(kafkaTemplate.send(any<String>(), any<String>(), any<String>()))
            .thenReturn(future)
        
        // When
        val result = messagePublisher.publishEvent(event)
        
        // Then
        assertNotNull(result)
        verify(kafkaTemplate).send(
            eq(SimpleMessagePublisher.SESSION_EVENTS_TOPIC),
            eq(event.eventId.toString()),
            any<String>()
        )
    }
    
    @Test
    fun `publishEvent should publish agent event to correct topic`() {
        // Given
        val event = createTestEvent("agent.started.v1")
        val future = CompletableFuture.completedFuture(sendResult)
        
        whenever(kafkaTemplate.send(any<String>(), any<String>(), any<String>()))
            .thenReturn(future)
        
        // When
        val result = messagePublisher.publishEvent(event)
        
        // Then
        assertNotNull(result)
        verify(kafkaTemplate).send(
            eq(SimpleMessagePublisher.AGENT_EVENTS_TOPIC),
            eq(event.eventId.toString()),
            any<String>()
        )
    }
    
    @Test
    fun `publishEvent should publish content event to correct topic`() {
        // Given
        val event = createTestEvent("content.generated.v1")
        val future = CompletableFuture.completedFuture(sendResult)
        
        whenever(kafkaTemplate.send(any<String>(), any<String>(), any<String>()))
            .thenReturn(future)
        
        // When
        val result = messagePublisher.publishEvent(event)
        
        // Then
        assertNotNull(result)
        verify(kafkaTemplate).send(
            eq(SimpleMessagePublisher.CONTENT_EVENTS_TOPIC),
            eq(event.eventId.toString()),
            any<String>()
        )
    }
    
    @Test
    fun `publishEvent should publish payment event to correct topic`() {
        // Given
        val event = createTestEvent("payment.processed.v1")
        val future = CompletableFuture.completedFuture(sendResult)
        
        whenever(kafkaTemplate.send(any<String>(), any<String>(), any<String>()))
            .thenReturn(future)
        
        // When
        val result = messagePublisher.publishEvent(event)
        
        // Then
        assertNotNull(result)
        verify(kafkaTemplate).send(
            eq(SimpleMessagePublisher.PAYMENT_EVENTS_TOPIC),
            eq(event.eventId.toString()),
            any<String>()
        )
    }
    
    @Test
    fun `publishEvent should publish unknown event to system topic`() {
        // Given
        val event = createTestEvent("unknown.event.v1")
        val future = CompletableFuture.completedFuture(sendResult)
        
        whenever(kafkaTemplate.send(any<String>(), any<String>(), any<String>()))
            .thenReturn(future)
        
        // When
        val result = messagePublisher.publishEvent(event)
        
        // Then
        assertNotNull(result)
        verify(kafkaTemplate).send(
            eq(SimpleMessagePublisher.SYSTEM_EVENTS_TOPIC),
            eq(event.eventId.toString()),
            any<String>()
        )
    }
    
    @Test
    fun `publishToTopic should publish message to specified topic`() {
        // Given
        val topic = "custom-topic"
        val key = "test-key"
        val message = mapOf("test" to "data")
        val future = CompletableFuture.completedFuture(sendResult)
        
        whenever(kafkaTemplate.send(any<String>(), any<String>(), any<String>()))
            .thenReturn(future)
        
        // When
        val result = messagePublisher.publishToTopic(topic, key, message)
        
        // Then
        assertNotNull(result)
        verify(kafkaTemplate).send(
            eq(topic),
            eq(key),
            any<String>()
        )
    }
    
    @Test
    fun `publishEventSync should block until completion`() {
        // Given
        val event = createTestEvent("user.registered.v1")
        val future = CompletableFuture.completedFuture(sendResult)
        
        whenever(kafkaTemplate.send(any<String>(), any<String>(), any<String>()))
            .thenReturn(future)
        
        // When
        val result = messagePublisher.publishEventSync(event)
        
        // Then
        assertNotNull(result)
        assertEquals(sendResult, result)
    }
    
    @Test
    fun `publishEvent should handle exceptions gracefully`() {
        // Given
        val event = createTestEvent("user.registered.v1")
        
        whenever(kafkaTemplate.send(any<String>(), any<String>(), any<String>()))
            .thenThrow(RuntimeException("Kafka error"))
        
        // When
        val result = messagePublisher.publishEvent(event)
        
        // Then
        assertNotNull(result)
        assertTrue(result.isCompletedExceptionally)
    }
    
    private fun createTestEvent(eventType: String): BaseEvent {
        return when {
            eventType.startsWith("user.") -> UserRegisteredEvent(
                eventType = eventType,
                userId = UUID.randomUUID(),
                email = "test@example.com",
                firstName = "Test",
                lastName = "User",
                role = "FREE_USER"
            )
            eventType.startsWith("session.") -> SessionStartedEvent(
                eventType = eventType,
                userId = UUID.randomUUID(),
                sessionId = UUID.randomUUID(),
                topic = "Test Topic",
                estimatedDurationMinutes = 60
            )
            eventType.startsWith("agent.") -> AgentStartedEvent(
                eventType = eventType,
                userId = UUID.randomUUID(),
                sessionId = UUID.randomUUID(),
                executionId = UUID.randomUUID(),
                agentType = "RESEARCH_AGENT",
                stageNumber = 1
            )
            eventType.startsWith("content.") -> ContentGeneratedEvent(
                eventType = eventType,
                userId = UUID.randomUUID(),
                contentId = UUID.randomUUID(),
                sessionId = UUID.randomUUID(),
                contentType = "TEXT",
                title = "Test Content",
                wordCount = 100,
                qualityScore = 0.8
            )
            eventType.startsWith("payment.") -> PaymentSubscriptionActivatedEvent(
                eventType = eventType,
                userId = UUID.randomUUID(),
                subscriptionId = UUID.randomUUID(),
                planType = "PRO",
                features = listOf("feature1", "feature2")
            )
            else -> SystemAlertTriggeredEvent(
                eventType = eventType,
                alertType = "TEST_ALERT",
                severity = "INFO",
                message = "Test alert message",
                affectedServices = listOf("test-service")
            )
        }
    }
}