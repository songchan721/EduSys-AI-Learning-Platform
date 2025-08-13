package com.learningplatform.messaging.admin

import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.admin.NewTopic
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy

/**
 * Topic Manager Service
 * Manages Kafka topics creation, configuration, and lifecycle
 * Ensures all required topics exist with proper configuration
 */
@Service
class TopicManager {

    @Value("\${spring.kafka.bootstrap-servers:localhost:19092}")
    private lateinit var bootstrapServers: String

    @Value("\${spring.kafka.topics.partitions:3}")
    private var defaultPartitions: Int = 3

    @Value("\${spring.kafka.topics.replication-factor:1}")
    private var defaultReplicationFactor: Short = 1

    @Value("\${spring.kafka.topics.retention-ms:604800000}")
    private var defaultRetentionMs: String = "604800000" // 7 days

    @Value("\${spring.kafka.topics.cleanup-policy:delete}")
    private var defaultCleanupPolicy: String = "delete"

    @Value("\${spring.kafka.topics.compression-type:snappy}")
    private var defaultCompressionType: String = "snappy"

    private lateinit var adminClient: AdminClient

    /**
     * Topic configurations for different event types
     */
    private val topicConfigurations = mapOf(
        // Main event topics
        "user-events" to TopicConfig(
            partitions = 3,
            retentionMs = "2592000000", // 30 days
            cleanupPolicy = "delete"
        ),
        "session-events" to TopicConfig(
            partitions = 5,
            retentionMs = "7776000000", // 90 days
            cleanupPolicy = "delete"
        ),
        "agent-events" to TopicConfig(
            partitions = 8,
            retentionMs = "1209600000", // 14 days
            cleanupPolicy = "delete"
        ),
        "content-events" to TopicConfig(
            partitions = 3,
            retentionMs = "5184000000", // 60 days
            cleanupPolicy = "delete"
        ),
        "payment-events" to TopicConfig(
            partitions = 2,
            retentionMs = "31536000000", // 365 days
            cleanupPolicy = "delete"
        ),
        "system-events" to TopicConfig(
            partitions = 1,
            retentionMs = "604800000", // 7 days
            cleanupPolicy = "delete"
        ),
        "analytics-events" to TopicConfig(
            partitions = 3,
            retentionMs = "15552000000", // 180 days
            cleanupPolicy = "delete"
        ),
        "notification-events" to TopicConfig(
            partitions = 2,
            retentionMs = "2592000000", // 30 days
            cleanupPolicy = "delete"
        ),
        
        // Dead letter queue topics
        "user-events-dlq" to TopicConfig(
            partitions = 1,
            retentionMs = "2592000000", // 30 days
            cleanupPolicy = "delete"
        ),
        "session-events-dlq" to TopicConfig(
            partitions = 1,
            retentionMs = "2592000000",
            cleanupPolicy = "delete"
        ),
        "agent-events-dlq" to TopicConfig(
            partitions = 1,
            retentionMs = "2592000000",
            cleanupPolicy = "delete"
        ),
        "content-events-dlq" to TopicConfig(
            partitions = 1,
            retentionMs = "2592000000",
            cleanupPolicy = "delete"
        ),
        "payment-events-dlq" to TopicConfig(
            partitions = 1,
            retentionMs = "31536000000", // 365 days for payment DLQ
            cleanupPolicy = "delete"
        ),
        "system-events-dlq" to TopicConfig(
            partitions = 1,
            retentionMs = "2592000000",
            cleanupPolicy = "delete"
        ),
        
        // Default topic
        "learning-platform-events" to TopicConfig(
            partitions = 5,
            retentionMs = "2592000000",
            cleanupPolicy = "delete"
        )
    )

    @PostConstruct
    fun initialize() {
        if (!::adminClient.isInitialized) {
            createAdminClient()
        }
        createTopics()
    }

    @PreDestroy
    fun cleanup() {
        adminClient.close()
    }

    /**
     * Create admin client for topic management
     */
    private fun createAdminClient() {
        val configs = mapOf(
            AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            AdminClientConfig.CLIENT_ID_CONFIG to "topic-manager",
            AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG to "30000",
            AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG to "60000"
        )
        
        adminClient = AdminClient.create(configs)
    }

    /**
     * Create all required topics if they don't exist
     */
    fun createTopics() {
        try {
            // Get existing topics
            val existingTopics = adminClient.listTopics().names().get()
            
            // Create topics that don't exist
            val topicsToCreate = topicConfigurations
                .filterKeys { topicName -> !existingTopics.contains(topicName) }
                .map { (topicName, config) -> createNewTopic(topicName, config) }
            
            if (topicsToCreate.isNotEmpty()) {
                println("Creating ${topicsToCreate.size} topics...")
                
                val result = adminClient.createTopics(topicsToCreate)
                result.all().get()
                
                println("Successfully created topics: ${topicsToCreate.map { it.name() }}")
            } else {
                println("All required topics already exist")
            }
            
        } catch (exception: Exception) {
            println("Error creating topics: ${exception.message}")
            throw TopicManagementException("Failed to create topics", exception)
        }
    }

    /**
     * Create a new topic with configuration
     */
    private fun createNewTopic(topicName: String, config: TopicConfig): NewTopic {
        val topic = NewTopic(
            topicName,
            config.partitions,
            defaultReplicationFactor
        )
        
        // Set topic configurations
        val topicConfigs = mutableMapOf<String, String>()
        topicConfigs["retention.ms"] = config.retentionMs
        topicConfigs["cleanup.policy"] = config.cleanupPolicy
        topicConfigs["compression.type"] = defaultCompressionType
        topicConfigs["min.insync.replicas"] = "1"
        topicConfigs["unclean.leader.election.enable"] = "false"
        
        // Additional configurations based on topic type
        when {
            topicName.contains("payment") -> {
                topicConfigs["min.insync.replicas"] = "1" // Ensure durability for payments
            }
            topicName.contains("dlq") -> {
                topicConfigs["cleanup.policy"] = "delete"
                topicConfigs["retention.ms"] = config.retentionMs
            }
            topicName.contains("analytics") -> {
                topicConfigs["compression.type"] = "lz4" // Better for analytics data
            }
        }
        
        topic.configs(topicConfigs)
        return topic
    }

    /**
     * Delete a topic (use with caution)
     */
    fun deleteTopic(topicName: String) {
        try {
            adminClient.deleteTopics(listOf(topicName)).all().get()
            println("Successfully deleted topic: $topicName")
        } catch (exception: Exception) {
            println("Error deleting topic $topicName: ${exception.message}")
            throw TopicManagementException("Failed to delete topic: $topicName", exception)
        }
    }

    /**
     * List all topics
     */
    fun listTopics(): Set<String> {
        return try {
            adminClient.listTopics().names().get()
        } catch (exception: Exception) {
            println("Error listing topics: ${exception.message}")
            emptySet()
        }
    }

    /**
     * Get topic information
     */
    fun getTopicInfo(topicName: String): Map<String, Any>? {
        return try {
            val topicDescription = adminClient.describeTopics(listOf(topicName))
                .values()[topicName]?.get()
            
            if (topicDescription != null) {
                mapOf(
                    "name" to topicDescription.name(),
                    "partitions" to topicDescription.partitions().size,
                    "replicationFactor" to (topicDescription.partitions().firstOrNull()?.replicas()?.size ?: 0),
                    "internal" to topicDescription.isInternal
                )
            } else {
                null
            }
        } catch (exception: Exception) {
            println("Error getting topic info for $topicName: ${exception.message}")
            null
        }
    }

    /**
     * Check if topic exists
     */
    fun topicExists(topicName: String): Boolean {
        return try {
            val existingTopics = adminClient.listTopics().names().get()
            existingTopics.contains(topicName)
        } catch (exception: Exception) {
            println("Error checking if topic exists: ${exception.message}")
            false
        }
    }

    /**
     * Get cluster information
     */
    fun getClusterInfo(): Map<String, Any> {
        return try {
            val clusterMetadata = adminClient.describeCluster()
            mapOf(
                "clusterId" to (clusterMetadata.clusterId().get() ?: "unknown"),
                "controller" to clusterMetadata.controller().get().toString(),
                "nodes" to clusterMetadata.nodes().get().map { it.toString() }
            )
        } catch (exception: Exception) {
            println("Error getting cluster info: ${exception.message}")
            mapOf("error" to (exception.message ?: "Unknown error"))
        }
    }
}

/**
 * Topic configuration data class
 */
data class TopicConfig(
    val partitions: Int,
    val retentionMs: String,
    val cleanupPolicy: String
)

/**
 * Exception thrown when topic management operations fail
 */
class TopicManagementException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)