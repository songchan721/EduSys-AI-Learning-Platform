package com.learningplatform.utils.cache

import com.fasterxml.jackson.databind.ObjectMapper
import com.learningplatform.utils.correlation.CorrelationIdService
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * Simple, working cache service for Redis operations
 * Provides basic caching functionality with proper error handling
 */
@Service
class SimpleCacheService(
    private val redisTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
    private val correlationIdService: CorrelationIdService
) {
    
    private val logger = LoggerFactory.getLogger(SimpleCacheService::class.java)
    
    companion object {
        private const val DEFAULT_TTL_MINUTES = 60L
        private const val KEY_PREFIX = "learning-platform:"
    }
    
    /**
     * Stores a value in cache with TTL
     */
    fun <T> put(key: String, value: T, ttlMinutes: Long = DEFAULT_TTL_MINUTES) {
        try {
            val fullKey = buildKey(key)
            val jsonValue = objectMapper.writeValueAsString(value)
            redisTemplate.opsForValue().set(fullKey, jsonValue, ttlMinutes, TimeUnit.MINUTES)
            logger.debug("Cached value for key: {} with TTL: {} minutes", fullKey, ttlMinutes)
        } catch (e: Exception) {
            logger.error("Failed to cache value for key: {}", key, e)
        }
    }
    
    /**
     * Stores a value in cache with Duration TTL
     */
    fun <T> put(key: String, value: T, ttl: Duration) {
        put(key, value, ttl.toMinutes())
    }
    
    /**
     * Retrieves a value from cache
     */
    fun <T> get(key: String, clazz: Class<T>): T? {
        return try {
            val fullKey = buildKey(key)
            val jsonValue = redisTemplate.opsForValue().get(fullKey)
            
            if (jsonValue != null) {
                logger.debug("Cache hit for key: {}", fullKey)
                objectMapper.readValue(jsonValue, clazz)
            } else {
                logger.debug("Cache miss for key: {}", fullKey)
                null
            }
        } catch (e: Exception) {
            logger.error("Failed to retrieve cached value for key: {}", key, e)
            null
        }
    }
    
    /**
     * Retrieves a value from cache or computes it if not present
     */
    fun <T> getOrCompute(key: String, clazz: Class<T>, ttlMinutes: Long = DEFAULT_TTL_MINUTES, compute: () -> T): T {
        val cached = get(key, clazz)
        if (cached != null) {
            return cached
        }
        
        val computed = compute()
        put(key, computed, ttlMinutes)
        return computed
    }
    
    /**
     * Removes a value from cache
     */
    fun evict(key: String): Boolean {
        return try {
            val fullKey = buildKey(key)
            val deleted = redisTemplate.delete(fullKey)
            logger.debug("Evicted cache key: {} (existed: {})", fullKey, deleted)
            deleted
        } catch (e: Exception) {
            logger.error("Failed to evict cache key: {}", key, e)
            false
        }
    }
    
    /**
     * Checks if a key exists in cache
     */
    fun exists(key: String): Boolean {
        return try {
            val fullKey = buildKey(key)
            redisTemplate.hasKey(fullKey)
        } catch (e: Exception) {
            logger.error("Failed to check cache key existence: {}", key, e)
            false
        }
    }
    
    /**
     * Gets the TTL of a cached key in seconds
     */
    fun getTtl(key: String): Long {
        return try {
            val fullKey = buildKey(key)
            redisTemplate.getExpire(fullKey, TimeUnit.SECONDS)
        } catch (e: Exception) {
            logger.error("Failed to get TTL for cache key: {}", key, e)
            -1L
        }
    }
    
    /**
     * Increments a numeric value in cache
     */
    fun increment(key: String, delta: Long = 1L): Long {
        return try {
            val fullKey = buildKey(key)
            redisTemplate.opsForValue().increment(fullKey, delta) ?: 0L
        } catch (e: Exception) {
            logger.error("Failed to increment cache key: {}", key, e)
            0L
        }
    }
    
    private fun buildKey(key: String): String {
        val correlationId = correlationIdService.getCorrelationId()
        return if (correlationId != null) {
            "$KEY_PREFIX$key:$correlationId"
        } else {
            "$KEY_PREFIX$key"
        }
    }
}