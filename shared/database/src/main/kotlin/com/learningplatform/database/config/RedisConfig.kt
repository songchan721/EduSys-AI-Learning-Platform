package com.learningplatform.database.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.type.CollectionType
import com.fasterxml.jackson.databind.type.MapType
import com.fasterxml.jackson.databind.type.TypeFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer
import io.lettuce.core.resource.ClientResources
import io.lettuce.core.resource.DefaultClientResources
import org.apache.commons.pool2.impl.GenericObjectPoolConfig
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.serializer.RedisSerializationContext
import java.time.Duration

/**
 * Redis configuration with connection pooling and caching
 * Supports multiple Redis configurations and cache management
 */
@Configuration
class RedisConfig {

    @Bean
    @Primary
    @ConfigurationProperties("app.redis.primary")
    fun primaryRedisProperties(): RedisProperties {
        return RedisProperties()
    }

    @Bean
    @ConfigurationProperties("app.redis.session")
    fun sessionRedisProperties(): RedisProperties {
        return RedisProperties()
    }

    @Bean
    fun redisObjectMapper(): ObjectMapper {
        return ObjectMapper().apply {
            registerModule(KotlinModule.Builder().build())
            findAndRegisterModules()
        }
    }

    @Bean
    fun clientResources(): ClientResources {
        return DefaultClientResources.builder()
            .ioThreadPoolSize(4)
            .computationThreadPoolSize(4)
            .build()
    }

    @Bean
    @Primary
    fun primaryRedisConnectionFactory(
        primaryRedisProperties: RedisProperties,
        clientResources: ClientResources
    ): RedisConnectionFactory {
        return createRedisConnectionFactory(primaryRedisProperties, clientResources, "primary")
    }

    @Bean
    fun sessionRedisConnectionFactory(
        sessionRedisProperties: RedisProperties,
        clientResources: ClientResources
    ): RedisConnectionFactory {
        return createRedisConnectionFactory(sessionRedisProperties, clientResources, "session")
    }

    @Bean
    @Primary
    fun redisTemplate(
        primaryRedisConnectionFactory: RedisConnectionFactory,
        redisObjectMapper: ObjectMapper
    ): RedisTemplate<String, String> {
        val template = RedisTemplate<String, String>()
        template.connectionFactory = primaryRedisConnectionFactory
        
        // Use String serializer for keys
        template.keySerializer = StringRedisSerializer()
        template.hashKeySerializer = StringRedisSerializer()
        
        // Use JSON serializer for values
        val jsonSerializer = GenericJackson2JsonRedisSerializer(redisObjectMapper)
        template.valueSerializer = jsonSerializer
        template.hashValueSerializer = jsonSerializer
        
        template.afterPropertiesSet()
        return template
    }

    @Bean
    fun sessionRedisTemplate(
        sessionRedisConnectionFactory: RedisConnectionFactory,
        redisObjectMapper: ObjectMapper
    ): RedisTemplate<String, String> {
        val template = RedisTemplate<String, String>()
        template.connectionFactory = sessionRedisConnectionFactory
        
        template.keySerializer = StringRedisSerializer()
        template.hashKeySerializer = StringRedisSerializer()
        
        val jsonSerializer = GenericJackson2JsonRedisSerializer(redisObjectMapper)
        template.valueSerializer = jsonSerializer
        template.hashValueSerializer = jsonSerializer
        
        template.afterPropertiesSet()
        return template
    }

    @Bean
    fun cacheManager(
        primaryRedisConnectionFactory: RedisConnectionFactory,
        redisObjectMapper: ObjectMapper
    ): RedisCacheManager {
        val jsonSerializer = GenericJackson2JsonRedisSerializer(redisObjectMapper)
        
        val cacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(1)) // Default TTL of 1 hour
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
            .disableCachingNullValues()

        // Configure specific cache TTLs
        val cacheConfigurations = mapOf(
            "users" to cacheConfiguration.entryTtl(Duration.ofMinutes(30)),
            "sessions" to cacheConfiguration.entryTtl(Duration.ofMinutes(15)),
            "content" to cacheConfiguration.entryTtl(Duration.ofHours(2)),
            "agents" to cacheConfiguration.entryTtl(Duration.ofMinutes(45)),
            "payments" to cacheConfiguration.entryTtl(Duration.ofMinutes(10)),
            "analytics" to cacheConfiguration.entryTtl(Duration.ofMinutes(5))
        )

        return RedisCacheManager.builder(primaryRedisConnectionFactory)
            .cacheDefaults(cacheConfiguration)
            .withInitialCacheConfigurations(cacheConfigurations)
            .transactionAware()
            .build()
    }

    private fun createRedisConnectionFactory(
        properties: RedisProperties,
        clientResources: ClientResources,
        poolName: String
    ): LettuceConnectionFactory {
        // Redis standalone configuration
        val redisConfig = RedisStandaloneConfiguration().apply {
            hostName = properties.host
            port = properties.port
            database = properties.database
            if (properties.password.isNotEmpty()) {
                setPassword(properties.password)
            }
        }

        // Connection pool configuration
        val poolConfig = GenericObjectPoolConfig<Any>().apply {
            maxTotal = properties.pool.maxActive
            maxIdle = properties.pool.maxIdle
            minIdle = properties.pool.minIdle
            setMaxWait(Duration.ofMillis(properties.pool.maxWait))
            testOnBorrow = true
            testOnReturn = true
            testWhileIdle = true
            setTimeBetweenEvictionRuns(Duration.ofSeconds(30))
            setMinEvictableIdleTime(Duration.ofMinutes(1))
            numTestsPerEvictionRun = 3
        }

        // Lettuce client configuration
        val clientConfig = LettucePoolingClientConfiguration.builder()
            .poolConfig(poolConfig)
            .clientResources(clientResources)
            .commandTimeout(Duration.ofSeconds(properties.timeout))
            .shutdownTimeout(Duration.ofSeconds(5))
            .build()

        return LettuceConnectionFactory(redisConfig, clientConfig)
    }
}

/**
 * Redis connection properties
 */
data class RedisProperties(
    var host: String = "localhost",
    var port: Int = 6379,
    var database: Int = 0,
    var password: String = "",
    var timeout: Long = 5, // seconds
    var pool: RedisPoolProperties = RedisPoolProperties()
)

/**
 * Redis connection pool properties
 */
data class RedisPoolProperties(
    var maxActive: Int = 20,
    var maxIdle: Int = 10,
    var minIdle: Int = 2,
    var maxWait: Long = 5000 // milliseconds
)

/**
 * Cache warming service for preloading frequently accessed data
 */
@Configuration
class CacheWarmingConfig {

    @Bean
    fun cacheWarmingService(
        redisTemplate: RedisTemplate<String, String>,
        cacheManager: RedisCacheManager
    ): CacheWarmingService {
        return CacheWarmingService(redisTemplate, cacheManager)
    }
}

/**
 * Service for warming up caches with frequently accessed data
 */
class CacheWarmingService(
    private val redisTemplate: RedisTemplate<String, String>,
    private val cacheManager: RedisCacheManager
) {

    /**
     * Warm up user-related caches
     */
    fun warmUserCaches(userIds: List<String>) {
        val userCache = cacheManager.getCache("users")
        userIds.forEach { userId ->
            // Pre-populate user cache with placeholder or fetch from database
            userCache?.put("user:$userId", "warming")
        }
    }

    /**
     * Warm up content caches
     */
    fun warmContentCaches(contentIds: List<String>) {
        val contentCache = cacheManager.getCache("content")
        contentIds.forEach { contentId ->
            contentCache?.put("content:$contentId", "warming")
        }
    }

    /**
     * Warm up session caches
     */
    fun warmSessionCaches(sessionIds: List<String>) {
        val sessionCache = cacheManager.getCache("sessions")
        sessionIds.forEach { sessionId ->
            sessionCache?.put("session:$sessionId", "warming")
        }
    }

    /**
     * Clear all caches
     */
    fun clearAllCaches() {
        cacheManager.cacheNames.forEach { cacheName ->
            cacheManager.getCache(cacheName)?.clear()
        }
    }

    /**
     * Clear specific cache
     */
    fun clearCache(cacheName: String) {
        cacheManager.getCache(cacheName)?.clear()
    }

    /**
     * Get cache statistics
     */
    fun getCacheStats(): Map<String, Any> {
        val stats = mutableMapOf<String, Any>()
        
        cacheManager.cacheNames.forEach { cacheName ->
            val cache = cacheManager.getCache(cacheName)
            if (cache != null) {
                stats[cacheName] = mapOf(
                    "name" to cacheName,
                    "nativeCache" to cache.nativeCache.javaClass.simpleName
                )
            }
        }
        
        return stats
    }

    /**
     * Preload frequently accessed data patterns
     */
    fun preloadFrequentPatterns() {
        // This would typically be called during application startup
        // to preload commonly accessed data patterns
        
        // Example: Preload active user sessions
        warmUserCaches(listOf("active-users"))
        
        // Example: Preload popular content
        warmContentCaches(listOf("popular-content"))
        
        // Example: Preload recent sessions
        warmSessionCaches(listOf("recent-sessions"))
    }
}