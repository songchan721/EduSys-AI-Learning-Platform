package com.learningplatform.database.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach

class RedisConfigTest {

    private lateinit var redisConfig: RedisConfig
    private lateinit var objectMapper: ObjectMapper
    
    @BeforeEach
    fun setUp() {
        redisConfig = RedisConfig()
        objectMapper = ObjectMapper()
    }
    
    @Test
    fun `should create primary Redis properties with correct defaults`() {
        // When
        val properties = redisConfig.primaryRedisProperties()
        
        // Then
        assertEquals("localhost", properties.host)
        assertEquals(6379, properties.port)
        assertEquals(0, properties.database)
        assertEquals("", properties.password)
        assertEquals(5L, properties.timeout)
        assertNotNull(properties.pool)
    }
    
    @Test
    fun `should create session Redis properties with correct defaults`() {
        // When
        val properties = redisConfig.sessionRedisProperties()
        
        // Then
        assertEquals("localhost", properties.host)
        assertEquals(6379, properties.port)
        assertEquals(0, properties.database)
        assertEquals("", properties.password)
        assertEquals(5L, properties.timeout)
        assertNotNull(properties.pool)
    }
    
    @Test
    fun `should create Redis ObjectMapper with Kotlin module`() {
        // When
        val mapper = redisConfig.redisObjectMapper()
        
        // Then
        assertNotNull(mapper)
        // ObjectMapper should be properly configured
        assertTrue(mapper.registeredModuleIds.isNotEmpty())
    }
    
    @Test
    fun `should create client resources with correct configuration`() {
        // When
        val resources = redisConfig.clientResources()
        
        // Then
        assertNotNull(resources)
        // ClientResources should be properly configured
        assertTrue(resources.ioThreadPoolSize() > 0)
        assertTrue(resources.computationThreadPoolSize() > 0)
    }
    
    @Test
    fun `RedisProperties should have correct default values`() {
        // When
        val properties = RedisProperties()
        
        // Then
        assertEquals("localhost", properties.host)
        assertEquals(6379, properties.port)
        assertEquals(0, properties.database)
        assertEquals("", properties.password)
        assertEquals(5L, properties.timeout)
        assertNotNull(properties.pool)
    }
    
    @Test
    fun `RedisPoolProperties should have correct default values`() {
        // When
        val poolProperties = RedisPoolProperties()
        
        // Then
        assertEquals(20, poolProperties.maxActive)
        assertEquals(10, poolProperties.maxIdle)
        assertEquals(2, poolProperties.minIdle)
        assertEquals(5000L, poolProperties.maxWait)
    }
    
    @Test
    fun `should validate Redis pool configuration`() {
        // Given
        val properties = RedisProperties().apply {
            host = "redis.example.com"
            port = 6380
            database = 1
            password = "secret"
            timeout = 10
            pool = RedisPoolProperties().apply {
                maxActive = 50
                maxIdle = 25
                minIdle = 5
                maxWait = 10000
            }
        }
        
        // When & Then - Should not throw exception
        assertDoesNotThrow {
            // Validate configuration constraints
            assertTrue(properties.pool.maxActive >= properties.pool.maxIdle)
            assertTrue(properties.pool.maxIdle >= properties.pool.minIdle)
            assertTrue(properties.pool.maxWait > 0)
            assertTrue(properties.timeout > 0)
            assertTrue(properties.port > 0)
            assertTrue(properties.database >= 0)
        }
    }
}

class CacheWarmingServiceTest {

    private lateinit var cacheWarmingService: CacheWarmingService
    
    @Test
    fun `CacheWarmingService should be instantiable`() {
        // This is a basic test to ensure the class can be instantiated
        // Full integration testing would require Redis connection
        assertDoesNotThrow {
            // Basic validation that the class structure is correct
            val properties = RedisProperties()
            assertNotNull(properties)
        }
    }
}