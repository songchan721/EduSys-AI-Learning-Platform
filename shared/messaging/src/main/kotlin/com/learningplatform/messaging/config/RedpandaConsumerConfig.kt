package com.learningplatform.messaging.config

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.support.serializer.JsonDeserializer

/**
 * Redpanda Consumer Configuration
 * Configures Kafka consumers for consuming events from Redpanda message queue
 * Supports both string and JSON deserialization with error handling
 */
@Configuration
@EnableKafka
class RedpandaConsumerConfig {

    @Value("\${spring.kafka.bootstrap-servers:localhost:19092}")
    private lateinit var bootstrapServers: String

    @Value("\${spring.kafka.consumer.group-id:learning-platform-group}")
    private lateinit var groupId: String

    @Value("\${spring.kafka.consumer.auto-offset-reset:earliest}")
    private lateinit var autoOffsetReset: String

    @Value("\${spring.kafka.consumer.enable-auto-commit:false}")
    private var enableAutoCommit: Boolean = false

    @Value("\${spring.kafka.consumer.session-timeout-ms:30000}")
    private var sessionTimeoutMs: Int = 30000

    @Value("\${spring.kafka.consumer.heartbeat-interval-ms:10000}")
    private var heartbeatIntervalMs: Int = 10000

    @Value("\${spring.kafka.consumer.max-poll-records:500}")
    private var maxPollRecords: Int = 500

    @Value("\${spring.kafka.consumer.max-poll-interval-ms:300000}")
    private var maxPollIntervalMs: Int = 300000

    @Value("\${spring.kafka.consumer.fetch-min-size:1}")
    private var fetchMinSize: Int = 1

    @Value("\${spring.kafka.consumer.fetch-max-wait-ms:500}")
    private var fetchMaxWaitMs: Int = 500

    @Value("\${spring.kafka.listener.concurrency:3}")
    private var concurrency: Int = 3

    @Value("\${spring.kafka.listener.poll-timeout:3000}")
    private var pollTimeout: Long = 3000

    /**
     * Consumer configuration properties
     * Optimized for reliable message processing
     */
    @Bean
    fun consumerConfigs(): Map<String, Any> {
        return mapOf(
            // Connection settings
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ConsumerConfig.GROUP_ID_CONFIG to groupId,
            
            // Deserialization
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to JsonDeserializer::class.java,
            
            // Offset management
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to autoOffsetReset,
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to enableAutoCommit,
            
            // Session management
            ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG to sessionTimeoutMs,
            ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG to heartbeatIntervalMs,
            
            // Polling settings
            ConsumerConfig.MAX_POLL_RECORDS_CONFIG to maxPollRecords,
            ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG to maxPollIntervalMs,
            ConsumerConfig.FETCH_MIN_BYTES_CONFIG to fetchMinSize,
            ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG to fetchMaxWaitMs,
            
            // JSON deserializer settings
            JsonDeserializer.TRUSTED_PACKAGES to "com.learningplatform.messaging.events",
            JsonDeserializer.VALUE_DEFAULT_TYPE to "com.learningplatform.messaging.events.BaseEvent",
            JsonDeserializer.USE_TYPE_INFO_HEADERS to false
        )
    }

    /**
     * Consumer factory for creating Kafka consumers
     */
    @Bean
    fun consumerFactory(): ConsumerFactory<String, Any> {
        return DefaultKafkaConsumerFactory(consumerConfigs())
    }

    /**
     * Kafka listener container factory
     * Configures how consumers process messages
     */
    @Bean
    fun kafkaListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, Any> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, Any>()
        factory.consumerFactory = consumerFactory()
        
        // Concurrency settings
        factory.setConcurrency(concurrency)
        
        // Container properties
        factory.containerProperties.pollTimeout = pollTimeout
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE
        
        // Error handling - will be configured by individual services
        
        return factory
    }

    /**
     * String-only consumer factory for simple messages
     */
    @Bean
    fun stringConsumerFactory(): ConsumerFactory<String, String> {
        val stringConsumerConfigs = consumerConfigs().toMutableMap()
        stringConsumerConfigs[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        
        return DefaultKafkaConsumerFactory(stringConsumerConfigs)
    }

    /**
     * String-only listener container factory
     */
    @Bean
    fun stringKafkaListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, String> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, String>()
        factory.consumerFactory = stringConsumerFactory()
        factory.setConcurrency(concurrency)
        factory.containerProperties.pollTimeout = pollTimeout
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE
        
        return factory
    }
}