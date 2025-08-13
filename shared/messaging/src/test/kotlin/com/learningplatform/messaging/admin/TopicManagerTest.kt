package com.learningplatform.messaging.admin

import org.apache.kafka.clients.admin.*
import org.apache.kafka.common.KafkaFuture
import org.apache.kafka.common.Node
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.test.util.ReflectionTestUtils
import java.util.concurrent.CompletableFuture

@ExtendWith(MockitoExtension::class)
class TopicManagerTest {

    @Mock
    private lateinit var adminClient: AdminClient

    @Mock
    private lateinit var listTopicsResult: ListTopicsResult

    @Mock
    private lateinit var createTopicsResult: CreateTopicsResult

    @Mock
    private lateinit var deleteTopicsResult: DeleteTopicsResult

    @Mock
    private lateinit var describeTopicsResult: DescribeTopicsResult

    @Mock
    private lateinit var describeClusterResult: DescribeClusterResult

    private lateinit var topicManager: TopicManager

    @BeforeEach
    fun setUp() {
        topicManager = TopicManager()
        
        // Set private fields using reflection
        ReflectionTestUtils.setField(topicManager, "bootstrapServers", "localhost:19092")
        ReflectionTestUtils.setField(topicManager, "defaultPartitions", 3)
        ReflectionTestUtils.setField(topicManager, "defaultReplicationFactor", 1.toShort())
        ReflectionTestUtils.setField(topicManager, "defaultRetentionMs", "604800000")
        ReflectionTestUtils.setField(topicManager, "defaultCleanupPolicy", "delete")
        ReflectionTestUtils.setField(topicManager, "defaultCompressionType", "snappy")
        ReflectionTestUtils.setField(topicManager, "adminClient", adminClient)
    }

    @Test
    fun `should create topics that don't exist`() {
        // Given
        val existingTopics = setOf("existing-topic")
        
        whenever(listTopicsResult.names()).thenReturn(KafkaFuture.completedFuture(existingTopics))
        whenever(adminClient.listTopics()).thenReturn(listTopicsResult)
        
        whenever(createTopicsResult.all()).thenReturn(KafkaFuture.completedFuture(null))
        whenever(adminClient.createTopics(any<Collection<NewTopic>>())).thenReturn(createTopicsResult)

        // When
        topicManager.createTopics()

        // Then
        verify(adminClient).listTopics()
        verify(adminClient).createTopics(any<Collection<NewTopic>>())
    }

    @Test
    fun `should not create topics that already exist`() {
        // Given
        val existingTopics = setOf(
            "user-events", "session-events", "agent-events", "content-events",
            "payment-events", "system-events", "analytics-events", "notification-events",
            "learning-platform-events", "user-events-dlq", "session-events-dlq", 
            "agent-events-dlq", "content-events-dlq", "payment-events-dlq", "system-events-dlq"
        )
        
        whenever(listTopicsResult.names()).thenReturn(KafkaFuture.completedFuture(existingTopics))
        whenever(adminClient.listTopics()).thenReturn(listTopicsResult)

        // When
        topicManager.createTopics()

        // Then
        verify(adminClient).listTopics()
        verify(adminClient, never()).createTopics(any<Collection<NewTopic>>())
    }

    @Test
    fun `should delete topic successfully`() {
        // Given
        val topicName = "test-topic"
        whenever(deleteTopicsResult.all()).thenReturn(KafkaFuture.completedFuture(null))
        whenever(adminClient.deleteTopics(listOf(topicName))).thenReturn(deleteTopicsResult)

        // When
        topicManager.deleteTopic(topicName)

        // Then
        verify(adminClient).deleteTopics(listOf(topicName))
    }

    @Test
    fun `should handle delete topic error`() {
        // Given
        val topicName = "test-topic"
        val exception = RuntimeException("Topic not found")
        whenever(deleteTopicsResult.all()).thenThrow(exception)
        whenever(adminClient.deleteTopics(listOf(topicName))).thenReturn(deleteTopicsResult)

        // When & Then
        assertThrows(TopicManagementException::class.java) {
            topicManager.deleteTopic(topicName)
        }
    }

    @Test
    fun `should list topics successfully`() {
        // Given
        val expectedTopics = setOf("user-events", "session-events", "agent-events")
        whenever(listTopicsResult.names()).thenReturn(KafkaFuture.completedFuture(expectedTopics))
        whenever(adminClient.listTopics()).thenReturn(listTopicsResult)

        // When
        val actualTopics = topicManager.listTopics()

        // Then
        assertEquals(expectedTopics, actualTopics)
        verify(adminClient).listTopics()
    }

    @Test
    fun `should handle list topics error`() {
        // Given
        val exception = RuntimeException("Connection failed")
        whenever(listTopicsResult.names()).thenThrow(exception)
        whenever(adminClient.listTopics()).thenReturn(listTopicsResult)

        // When
        val result = topicManager.listTopics()

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `should check if topic exists`() {
        // Given
        val existingTopics = setOf("user-events", "session-events")
        whenever(listTopicsResult.names()).thenReturn(KafkaFuture.completedFuture(existingTopics))
        whenever(adminClient.listTopics()).thenReturn(listTopicsResult)

        // When & Then
        assertTrue(topicManager.topicExists("user-events"))
        assertFalse(topicManager.topicExists("non-existent-topic"))
    }

    @Test
    fun `should get cluster info successfully`() {
        // Given
        val clusterId = "test-cluster"
        val controller = Node(1, "localhost", 9092)
        val nodes = listOf(Node(1, "localhost", 9092), Node(2, "localhost", 9093))
        
        whenever(describeClusterResult.clusterId()).thenReturn(KafkaFuture.completedFuture(clusterId))
        whenever(describeClusterResult.controller()).thenReturn(KafkaFuture.completedFuture(controller))
        whenever(describeClusterResult.nodes()).thenReturn(KafkaFuture.completedFuture(nodes))
        whenever(adminClient.describeCluster()).thenReturn(describeClusterResult)

        // When
        val clusterInfo = topicManager.getClusterInfo()

        // Then
        assertEquals(clusterId, clusterInfo["clusterId"])
        assertEquals(controller.toString(), clusterInfo["controller"])
        assertEquals(nodes.map { it.toString() }, clusterInfo["nodes"])
    }

    @Test
    fun `should handle cluster info error`() {
        // Given
        val exception = RuntimeException("Cluster unavailable")
        whenever(describeClusterResult.clusterId()).thenThrow(exception)
        whenever(adminClient.describeCluster()).thenReturn(describeClusterResult)

        // When
        val clusterInfo = topicManager.getClusterInfo()

        // Then
        assertEquals("Cluster unavailable", clusterInfo["error"])
    }

    @Test
    fun `should handle create topics error`() {
        // Given
        val existingTopics = setOf<String>()
        val exception = RuntimeException("Kafka cluster unavailable")
        
        whenever(listTopicsResult.names()).thenReturn(KafkaFuture.completedFuture(existingTopics))
        whenever(adminClient.listTopics()).thenReturn(listTopicsResult)
        
        whenever(createTopicsResult.all()).thenThrow(exception)
        whenever(adminClient.createTopics(any<Collection<NewTopic>>())).thenReturn(createTopicsResult)

        // When & Then
        assertThrows(TopicManagementException::class.java) {
            topicManager.createTopics()
        }
    }
}