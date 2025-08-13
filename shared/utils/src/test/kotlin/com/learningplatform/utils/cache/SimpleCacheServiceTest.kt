package com.learningplatform.utils.cache

import com.fasterxml.jackson.databind.ObjectMapper
import com.learningplatform.utils.correlation.CorrelationIdService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.time.Duration
import java.util.concurrent.TimeUnit
import org.junit.jupiter.api.Assertions.*

@ExtendWith(MockitoExtension::class)
class SimpleCacheServiceTest {

    @Mock
    private lateinit var redisTemplate: RedisTemplate<String, String>
    
    @Mock
    private lateinit var valueOperations: ValueOperations<String, String>
    
    @Mock
    private lateinit var correlationIdService: CorrelationIdService
    
    private lateinit var objectMapper: ObjectMapper
    private lateinit var cacheService: SimpleCacheService
    
    @BeforeEach
    fun setUp() {
        objectMapper = ObjectMapper()
        cacheService = SimpleCacheService(redisTemplate, objectMapper, correlationIdService)
        
        whenever(redisTemplate.opsForValue()).thenReturn(valueOperations)
        whenever(correlationIdService.getCorrelationId()).thenReturn("test-correlation-id")
    }
    
    @Test
    fun `put should store value in cache with TTL`() {
        // Given
        val key = "test-key"
        val value = TestData("test", 123)
        val ttlMinutes = 30L
        
        // When
        cacheService.put(key, value, ttlMinutes)
        
        // Then
        val expectedKey = "learning-platform:$key:test-correlation-id"
        val expectedJson = objectMapper.writeValueAsString(value)
        verify(valueOperations).set(expectedKey, expectedJson, ttlMinutes, TimeUnit.MINUTES)
    }
    
    @Test
    fun `put with Duration should store value in cache`() {
        // Given
        val key = "test-key"
        val value = TestData("test", 123)
        val ttl = Duration.ofHours(2)
        
        // When
        cacheService.put(key, value, ttl)
        
        // Then
        val expectedKey = "learning-platform:$key:test-correlation-id"
        val expectedJson = objectMapper.writeValueAsString(value)
        verify(valueOperations).set(expectedKey, expectedJson, 120L, TimeUnit.MINUTES)
    }
    
    @Test
    fun `get should retrieve value from cache`() {
        // Given
        val key = "test-key"
        val value = TestData("test", 123)
        val expectedKey = "learning-platform:$key:test-correlation-id"
        val jsonValue = objectMapper.writeValueAsString(value)
        
        whenever(valueOperations.get(expectedKey)).thenReturn(jsonValue)
        
        // When
        val result = cacheService.get(key, TestData::class.java)
        
        // Then
        assertNotNull(result)
        assertEquals(value.name, result!!.name)
        assertEquals(value.number, result.number)
    }
    
    @Test
    fun `get should return null when key not found`() {
        // Given
        val key = "non-existent-key"
        val expectedKey = "learning-platform:$key:test-correlation-id"
        
        whenever(valueOperations.get(expectedKey)).thenReturn(null)
        
        // When
        val result = cacheService.get(key, TestData::class.java)
        
        // Then
        assertNull(result)
    }
    
    @Test
    fun `getOrCompute should return cached value when present`() {
        // Given
        val key = "test-key"
        val cachedValue = TestData("cached", 456)
        val expectedKey = "learning-platform:$key:test-correlation-id"
        val jsonValue = objectMapper.writeValueAsString(cachedValue)
        
        whenever(valueOperations.get(expectedKey)).thenReturn(jsonValue)
        
        // When
        val result = cacheService.getOrCompute(key, TestData::class.java) { TestData("computed", 789) }
        
        // Then
        assertEquals(cachedValue.name, result.name)
        assertEquals(cachedValue.number, result.number)
        verify(valueOperations, never()).set(any(), any(), any(), any<TimeUnit>())
    }
    
    @Test
    fun `getOrCompute should compute and cache value when not present`() {
        // Given
        val key = "test-key"
        val computedValue = TestData("computed", 789)
        val expectedKey = "learning-platform:$key:test-correlation-id"
        
        whenever(valueOperations.get(expectedKey)).thenReturn(null)
        
        // When
        val result = cacheService.getOrCompute(key, TestData::class.java) { computedValue }
        
        // Then
        assertEquals(computedValue.name, result.name)
        assertEquals(computedValue.number, result.number)
        
        val expectedJson = objectMapper.writeValueAsString(computedValue)
        verify(valueOperations).set(expectedKey, expectedJson, 60L, TimeUnit.MINUTES)
    }
    
    @Test
    fun `evict should remove key from cache`() {
        // Given
        val key = "test-key"
        val expectedKey = "learning-platform:$key:test-correlation-id"
        
        whenever(redisTemplate.delete(expectedKey)).thenReturn(true)
        
        // When
        val result = cacheService.evict(key)
        
        // Then
        assertTrue(result)
        verify(redisTemplate).delete(expectedKey)
    }
    
    @Test
    fun `exists should check if key exists in cache`() {
        // Given
        val key = "test-key"
        val expectedKey = "learning-platform:$key:test-correlation-id"
        
        whenever(redisTemplate.hasKey(expectedKey)).thenReturn(true)
        
        // When
        val result = cacheService.exists(key)
        
        // Then
        assertTrue(result)
        verify(redisTemplate).hasKey(expectedKey)
    }
    
    @Test
    fun `getTtl should return TTL of cached key`() {
        // Given
        val key = "test-key"
        val expectedKey = "learning-platform:$key:test-correlation-id"
        val ttlSeconds = 3600L
        
        whenever(redisTemplate.getExpire(expectedKey, TimeUnit.SECONDS)).thenReturn(ttlSeconds)
        
        // When
        val result = cacheService.getTtl(key)
        
        // Then
        assertEquals(ttlSeconds, result)
    }
    
    @Test
    fun `increment should increment numeric value`() {
        // Given
        val key = "counter-key"
        val expectedKey = "learning-platform:$key:test-correlation-id"
        val delta = 5L
        
        whenever(valueOperations.increment(expectedKey, delta)).thenReturn(15L)
        
        // When
        val result = cacheService.increment(key, delta)
        
        // Then
        assertEquals(15L, result)
    }
    
    @Test
    fun `cache operations should handle exceptions gracefully`() {
        // Given
        val key = "error-key"
        val value = TestData("error-test", 123)
        
        whenever(valueOperations.set(any(), any(), any(), any<TimeUnit>()))
            .thenThrow(RuntimeException("Redis error"))
        
        // When & Then - should not throw exception
        assertDoesNotThrow {
            cacheService.put(key, value)
        }
        
        // Verify error was handled
        verify(valueOperations).set(any(), any(), any(), any<TimeUnit>())
    }
    
    data class TestData(
        val name: String,
        val number: Int
    )
}