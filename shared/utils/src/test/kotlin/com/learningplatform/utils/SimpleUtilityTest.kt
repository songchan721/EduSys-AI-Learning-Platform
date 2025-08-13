package com.learningplatform.utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.learningplatform.utils.correlation.CorrelationIdService
import com.learningplatform.utils.json.SimpleJsonUtils
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.*

/**
 * Simple test to verify utility classes work
 */
class SimpleUtilityTest {

    private lateinit var objectMapper: ObjectMapper
    private lateinit var jsonUtils: SimpleJsonUtils
    private lateinit var correlationIdService: CorrelationIdService
    
    @BeforeEach
    fun setUp() {
        objectMapper = ObjectMapper()
        jsonUtils = SimpleJsonUtils(objectMapper)
        correlationIdService = CorrelationIdService()
    }
    
    @Test
    fun `json utils should convert object to JSON and back`() {
        // Given
        val testObject = TestData("test", 123)
        
        // When
        val json = jsonUtils.toJson(testObject)
        val result = jsonUtils.fromJson(json, TestData::class.java)
        
        // Then
        assertNotNull(json)
        assertNotNull(result)
        assertEquals("test", result!!.name)
        assertEquals(123, result.number)
    }
    
    @Test
    fun `correlation service should generate and manage correlation IDs`() {
        // When
        val correlationId = correlationIdService.generateAndSetCorrelationId()
        val retrieved = correlationIdService.getCorrelationId()
        
        // Then
        assertNotNull(correlationId)
        assertEquals(correlationId, retrieved)
    }
    
    @Test
    fun `correlation service should manage context`() {
        // Given
        val userId = "test-user"
        val sessionId = "test-session"
        
        // When
        correlationIdService.setUserId(userId)
        correlationIdService.setSessionId(sessionId)
        
        // Then
        assertEquals(userId, correlationIdService.getUserId())
        assertEquals(sessionId, correlationIdService.getSessionId())
    }
    
    @Test
    fun `json utils should validate JSON`() {
        // Given
        val validJson = """{"name":"test","number":123}"""
        val invalidJson = """{"name":"test","number":}"""
        
        // When & Then
        assertTrue(jsonUtils.isValidJson(validJson))
        assertFalse(jsonUtils.isValidJson(invalidJson))
    }
    
    data class TestData(
        val name: String,
        val number: Int
    )
}