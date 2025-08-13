package com.learningplatform.messaging.config

import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.support.serializer.JsonSerializer

/**
 * Redpanda Producer Configuration
 * Configures Kafka producers for publishing events to Redpanda message queue
 * Supports both string and JSON serialization for different event types
 */
@Configuration
class RedpandaProducerConfig {

    @Value("\${spring.kafka.bootstrap-servers:localhost:19092}")
    private lateinit var bootstrapServers: String

    @Value("\${spring.kafka.producer.client-id:learning-platform-producer}")
    private lateinit var clientId: String

    @Value("\${spring.kafka.producer.acks:all}")
    private lateinit var acks: String

    @Value("\${spring.kafka.producer.retries:3}")
    private var retries: Int = 3

    @Value("\${spring.kafka.producer.batch-size:16384}")
    private var batchSize: Int = 16384

    @Value("\${spring.kafka.producer.linger-ms:5}")
    private var lingerMs: Int = 5

    @Value("\${spring.kafka.producer.buffer-memory:33554432}")
    private var bufferMemory: Long = 33554432

    @Value("\${spring.kafka.producer.compression-type:snappy}")
    private lateinit var compressionType: String

    @Value("\${spring.kafka.producer.max-in-flight-requests:5}")
    private var maxInFlightRequests: Int = 5

    @Value("\${spring.kafka.producer.request-timeout-ms:30000}")
    private var requestTimeoutMs: Int = 30000

    @Value("\${spring.kafka.producer.delivery-timeout-ms:120000}")
    private var deliveryTimeoutMs: Int = 120000

    /**
     * Producer configuration properties
     * Optimized for high throughput and reliability
     */
    @Bean
    fun producerConfigs(): Map<String, Any> {
        return mapOf(
            // Connection settings
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ProducerConfig.CLIENT_ID_CONFIG to clientId,
            
            // Serialization
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to JsonSerializer::class.java,
            
            // Reliability settings
            ProducerConfig.ACKS_CONFIG to acks,
            ProducerConfig.RETRIES_CONFIG to retries,
            ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG to true,
            ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION to maxInFlightRequests,
            
            // Performance settings
            ProducerConfig.BATCH_SIZE_CONFIG to batchSize,
            ProducerConfig.LINGER_MS_CONFIG to lingerMs,
            ProducerConfig.BUFFER_MEMORY_CONFIG to bufferMemory,
            ProducerConfig.COMPRESSION_TYPE_CONFIG to compressionType,
            
            // Timeout settings
            ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG to requestTimeoutMs,
            ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG to deliveryTimeoutMs,
            
            // JSON serializer settings
            JsonSerializer.ADD_TYPE_INFO_HEADERS to false,
            JsonSerializer.TYPE_MAPPINGS to "event:com.learningplatform.messaging.events.BaseEvent"
        )
    }

    /**
     * Producer factory for creating Kafka producers
     */
    @Bean
    fun producerFactory(): ProducerFactory<String, Any> {
        return DefaultKafkaProducerFactory(producerConfigs())
    }

    /**
     * Kafka template for sending messages
     * Primary interface for publishing events to Redpanda
     */
    @Bean
    fun kafkaTemplate(): KafkaTemplate<String, Any> {
        val template = KafkaTemplate(producerFactory())
        
        // Set default topic for events
        template.defaultTopic = "learning-platform-events"
        
        return template
    }

    /**
     * String-only Kafka template for simple messages
     */
    @Bean
    fun stringKafkaTemplate(): KafkaTemplate<String, String> {
        val stringProducerConfigs = producerConfigs().toMutableMap()
        stringProducerConfigs[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        
        val stringProducerFactory = DefaultKafkaProducerFactory<String, String>(stringProducerConfigs)
        return KafkaTemplate(stringProducerFactory)
    }
}