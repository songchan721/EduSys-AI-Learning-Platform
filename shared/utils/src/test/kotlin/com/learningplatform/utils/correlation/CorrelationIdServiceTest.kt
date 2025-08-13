package com.learningplatform.utils.correlation

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.AfterEach
import java.util.*

class CorrelationIdServiceTest {

    private lateinit var correlationIdService: CorrelationIdService
    
    @BeforeEach
    fun setUp() {
        correlationIdService = CorrelationIdService()
        // Clear any existing context
        correlationIdService.clearContext()
    }
    
    @AfterEach
    fun tearDown() {
        // Clean up after each test
        correlationIdService.clearContext()
    }
    
    @Test
    fun `setCorrelationId should store correlation ID in thread local`() {
        // Given
        val correlationId = UUID.randomUUID().toString()
        
        // When
        correlationIdService.setCorrelationId(correlationId)
        
        // Then
        assertEquals(correlationId, correlationIdService.getCorrelationId())
    }
    
    @Test
    fun `getCorrelationId should return null when not set`() {
        // When
        val result = correlationIdService.getCorrelationId()
        
        // Then
        assertNull(result)
    }
    
    @Test
    fun `generateAndSetCorrelationId should create and set new correlation ID`() {
        // When
        val correlationId = correlationIdService.generateAndSetCorrelationId()
        
        // Then
        assertNotNull(correlationId)
        assertEquals(correlationId, correlationIdService.getCorrelationId())
        // Verify it's a valid UUID format
        assertDoesNotThrow { UUID.fromString(correlationId) }
    }
    
    @Test
    fun `setUserId should store user ID in thread local`() {
        // Given
        val userId = UUID.randomUUID().toString()
        
        // When
        correlationIdService.setUserId(userId)
        
        // Then
        assertEquals(userId, correlationIdService.getUserId())
    }
    
    @Test
    fun `getUserId should return null when not set`() {
        // When
        val result = correlationIdService.getUserId()
        
        // Then
        assertNull(result)
    }
    
    @Test
    fun `setRequestId should store request ID in thread local`() {
        // Given
        val requestId = "req-123"
        
        // When
        correlationIdService.setRequestId(requestId)
        
        // Then
        assertEquals(requestId, correlationIdService.getRequestId())
    }
    
    @Test
    fun `getRequestId should return null when not set`() {
        // When
        val result = correlationIdService.getRequestId()
        
        // Then
        assertNull(result)
    }
    
    @Test
    fun `startTiming should record start time for operation`() {
        // Given
        val operation = "test-operation"
        
        // When
        correlationIdService.startTiming(operation)
        
        // Then
        // Should not throw exception and timing should be recorded
        assertDoesNotThrow {
            correlationIdService.endTiming(operation)
        }
    }
    
    @Test
    fun `endTiming should return duration for started operation`() {
        // Given
        val operation = "test-operation"
        correlationIdService.startTiming(operation)
        
        // Add small delay to ensure measurable duration
        Thread.sleep(10)
        
        // When
        val duration = correlationIdService.endTiming(operation)
        
        // Then
        assertNotNull(duration)
        assertTrue(duration!! > 0)
    }
    
    @Test
    fun `endTiming should return null for non-started operation`() {
        // Given
        val operation = "non-started-operation"
        
        // When
        val duration = correlationIdService.endTiming(operation)
        
        // Then
        assertNull(duration)
    }
    
    @Test
    fun `clearContext should remove all thread local values`() {
        // Given
        correlationIdService.setCorrelationId("test-correlation")
        correlationIdService.setUserId("test-user")
        correlationIdService.setRequestId("test-request")
        correlationIdService.startTiming("test-operation")
        
        // When
        correlationIdService.clearContext()
        
        // Then
        assertNull(correlationIdService.getCorrelationId())
        assertNull(correlationIdService.getUserId())
        assertNull(correlationIdService.getRequestId())
        assertNull(correlationIdService.endTiming("test-operation"))
    }
    
    @Test
    fun `getCorrelationId should return existing correlation ID when set`() {
        // Given
        val existingId = "existing-correlation-id"
        correlationIdService.setCorrelationId(existingId)
        
        // When
        val result = correlationIdService.getCorrelationId()
        
        // Then
        assertEquals(existingId, result)
    }
    
    @Test
    fun `generateAndSetCorrelationId should generate new ID when none exists`() {
        // When
        val result = correlationIdService.generateAndSetCorrelationId()
        
        // Then
        assertNotNull(result)
        assertEquals(result, correlationIdService.getCorrelationId())
        // Verify it's a valid UUID format
        assertDoesNotThrow { UUID.fromString(result) }
    }
    
    @Test
    fun `thread isolation should work correctly`() {
        // Given
        val mainThreadId = "main-thread-correlation"
        correlationIdService.setCorrelationId(mainThreadId)
        
        var otherThreadId: String? = null
        var mainThreadIdFromOtherThread: String? = null
        
        // When
        val thread = Thread {
            // Other thread should not see main thread's correlation ID
            mainThreadIdFromOtherThread = correlationIdService.getCorrelationId()
            
            // Set different correlation ID in other thread
            correlationIdService.setCorrelationId("other-thread-correlation")
            otherThreadId = correlationIdService.getCorrelationId()
        }
        
        thread.start()
        thread.join()
        
        // Then
        assertEquals(mainThreadId, correlationIdService.getCorrelationId()) // Main thread unchanged
        assertNull(mainThreadIdFromOtherThread) // Other thread didn't see main thread's ID
        assertEquals("other-thread-correlation", otherThreadId) // Other thread had its own ID
    }
    
    @Test
    fun `multiple timing operations should work independently`() {
        // Given
        val operation1 = "operation-1"
        val operation2 = "operation-2"
        
        // When
        correlationIdService.startTiming(operation1)
        Thread.sleep(10)
        correlationIdService.startTiming(operation2)
        Thread.sleep(10)
        
        val duration1 = correlationIdService.endTiming(operation1)
        val duration2 = correlationIdService.endTiming(operation2)
        
        // Then
        assertNotNull(duration1)
        assertNotNull(duration2)
        assertTrue(duration1!! > duration2!!) // operation1 ran longer
    }
}