package com.learningplatform.messaging.publisher

import com.learningplatform.messaging.events.BaseEvent
import com.learningplatform.messaging.events.UserRegisteredEvent
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.TopicPartition
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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull

@ExtendWith(MockitoExtension::class)
class EventPublisherTest {

    @Mock
    private lateinit var kafkaTemplate: KafkaTemplate<String, Any>

    @Mock
    private lateinit var stringKafkaTemplate: KafkaTemplate<String, String>

    private lateinit var eventPublisher: EventPublisher

    @BeforeEach
    fun setUp() {
        eventPublisher = EventPublisher(kafkaTemplate, stringKafkaTemplate)
    }

    @Test
    fun `should publish user event to correct topic`() {
        // Given
        val userId = UUID.randomUUID()
        val event = UserRegisteredEvent(
            userId = userId,
            email = "test@example.com",
            firstName = "John",
            lastName = "Doe",
            role = "USER"
        )

        val recordMetadata = RecordMetadata(
            TopicPartition("user-events", 0),
            0L,
            0L,
            System.currentTimeMillis(),
            0L,
            0,
            0
        )
        val producerRecord = ProducerRecord<String, Any>("user-events", userId.toString(), event)
        val sendResult = SendResult(producerRecord, recordMetadata)
        val future = CompletableFuture.completedFuture(sendResult)

        whenever(kafkaTemplate.send(eq("user-events"), eq(userId.toString()), any<BaseEvent>()))
            .thenReturn(future)

        // When
        val result = eventPublisher.publishEvent(event)

        // Then
        assertNotNull(result)
        verify(kafkaTemplate).send(eq("user-events"), eq(userId.toString()), any<BaseEvent>())
    }

    @Test
    fun `should publish string message to specific topic`() {
        // Given
        val topic = "test-topic"
        val key = "test-key"
        val message = "test message"

        val recordMetadata = RecordMetadata(
            TopicPartition(topic, 0),
            0L,
            0L,
            System.currentTimeMillis(),
            0L,
            0,
            0
        )
        val producerRecord = ProducerRecord<String, String>(topic, key, message)
        val sendResult = SendResult(producerRecord, recordMetadata)
        val future = CompletableFuture.completedFuture(sendResult)

        whenever(stringKafkaTemplate.send(topic, key, message)).thenReturn(future)

        // When
        val result = eventPublisher.publishMessage(topic, key, message)

        // Then
        assertNotNull(result)
        verify(stringKafkaTemplate).send(topic, key, message)
    }

    @Test
    fun `should publish multiple events in batch`() {
        // Given
        val userId1 = UUID.randomUUID()
        val userId2 = UUID.randomUUID()
        val events = listOf(
            UserRegisteredEvent(
                userId = userId1,
                email = "test1@example.com",
                firstName = "John",
                lastName = "Doe",
                role = "USER"
            ),
            UserRegisteredEvent(
                userId = userId2,
                email = "test2@example.com",
                firstName = "Jane",
                lastName = "Smith",
                role = "USER"
            )
        )

        val recordMetadata = RecordMetadata(
            TopicPartition("user-events", 0),
            0L,
            0L,
            System.currentTimeMillis(),
            0L,
            0,
            0
        )
        val producerRecord = ProducerRecord<String, Any>("user-events", userId1.toString(), events[0])
        val sendResult = SendResult(producerRecord, recordMetadata)
        val future = CompletableFuture.completedFuture(sendResult)

        whenever(kafkaTemplate.send(any<String>(), any<String>(), any<BaseEvent>()))
            .thenReturn(future)

        // When
        val results = eventPublisher.publishEvents(events)

        // Then
        assertEquals(2, results.size)
        verify(kafkaTemplate, times(2)).send(any<String>(), any<String>(), any<BaseEvent>())
    }

    @Test
    fun `should handle publish error and send to DLQ`() {
        // Given
        val userId = UUID.randomUUID()
        val event = UserRegisteredEvent(
            userId = userId,
            email = "test@example.com",
            firstName = "John",
            lastName = "Doe",
            role = "USER"
        )

        val exception = RuntimeException("Kafka is down")
        val future = CompletableFuture<SendResult<String, Any>>()
        future.completeExceptionally(exception)

        whenever(kafkaTemplate.send(eq("user-events"), eq(userId.toString()), any<BaseEvent>()))
            .thenReturn(future)

        // Mock DLQ send
        val dlqFuture = CompletableFuture<SendResult<String, Any>>()
        dlqFuture.complete(mock())
        whenever(kafkaTemplate.send(eq("user-events-dlq"), eq(userId.toString()), any()))
            .thenReturn(dlqFuture)

        // When & Then
        val result = eventPublisher.publishEvent(event)
        
        // Verify DLQ was called
        verify(kafkaTemplate, timeout(1000)).send(eq("user-events-dlq"), eq(userId.toString()), any())
    }
}