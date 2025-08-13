package com.learningplatform.utils.exception

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.springframework.http.HttpStatus
import java.util.*

class CustomExceptionsTest {

    @Test
    fun `ValidationException should be created with message`() {
        // Given
        val message = "Validation failed"
        
        // When
        val exception = ValidationException(message)
        
        // Then
        assertEquals(message, exception.message)
        assertEquals("VALIDATION_ERROR", exception.errorCode)
        assertEquals(HttpStatus.BAD_REQUEST, exception.httpStatus)
        assertNull(exception.cause)
    }
    
    @Test
    fun `ValidationException should be created with message and cause`() {
        // Given
        val message = "Validation failed"
        val cause = IllegalArgumentException("Invalid argument")
        
        // When
        val exception = ValidationException(message, cause = cause)
        
        // Then
        assertEquals(message, exception.message)
        assertEquals(cause, exception.cause)
    }
    
    @Test
    fun `ResourceNotFoundException should be created with resource type and id`() {
        // Given
        val resourceType = "User"
        val resourceId = UUID.randomUUID().toString()
        
        // When
        val exception = ResourceNotFoundException(resourceType, resourceId)
        
        // Then
        assertEquals("$resourceType with ID $resourceId not found", exception.message)
        assertEquals("RESOURCE_NOT_FOUND", exception.errorCode)
        assertEquals(HttpStatus.NOT_FOUND, exception.httpStatus)
        assertEquals(resourceType, exception.resourceType)
        assertEquals(resourceId, exception.resourceId)
    }
    
    @Test
    fun `ResourceNotFoundException should be created with custom message`() {
        // Given
        val resourceType = "User"
        val resourceId = UUID.randomUUID().toString()
        val customMessage = "Custom not found message"
        
        // When
        val exception = ResourceNotFoundException(resourceType, resourceId, customMessage)
        
        // Then
        assertEquals(customMessage, exception.message)
        assertEquals(resourceType, exception.resourceType)
        assertEquals(resourceId, exception.resourceId)
    }
    
    @Test
    fun `UnauthorizedException should be created with default message`() {
        // When
        val exception = UnauthorizedException()
        
        // Then
        assertEquals("Authentication required", exception.message)
        assertEquals("UNAUTHORIZED", exception.errorCode)
        assertEquals(HttpStatus.UNAUTHORIZED, exception.httpStatus)
    }
    
    @Test
    fun `UnauthorizedException should be created with custom message`() {
        // Given
        val message = "Access denied for this resource"
        
        // When
        val exception = UnauthorizedException(message)
        
        // Then
        assertEquals(message, exception.message)
        assertEquals("UNAUTHORIZED", exception.errorCode)
    }
    
    @Test
    fun `ForbiddenException should be created with default message`() {
        // When
        val exception = ForbiddenException()
        
        // Then
        assertEquals("Access denied", exception.message)
        assertEquals("FORBIDDEN", exception.errorCode)
        assertEquals(HttpStatus.FORBIDDEN, exception.httpStatus)
    }
    
    @Test
    fun `ForbiddenException should be created with custom message`() {
        // Given
        val message = "Insufficient permissions"
        
        // When
        val exception = ForbiddenException(message)
        
        // Then
        assertEquals(message, exception.message)
        assertEquals("FORBIDDEN", exception.errorCode)
    }
    
    @Test
    fun `RateLimitExceededException should be created with default message`() {
        // When
        val exception = RateLimitExceededException()
        
        // Then
        assertEquals("Rate limit exceeded", exception.message)
        assertEquals("RATE_LIMIT_EXCEEDED", exception.errorCode)
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exception.httpStatus)
        assertNull(exception.retryAfterSeconds)
    }
    
    @Test
    fun `RateLimitExceededException should be created with retry after seconds`() {
        // Given
        val retryAfter = 60L
        
        // When
        val exception = RateLimitExceededException(retryAfterSeconds = retryAfter)
        
        // Then
        assertEquals("Rate limit exceeded", exception.message)
        assertEquals(retryAfter, exception.retryAfterSeconds)
    }
    
    @Test
    fun `RateLimitExceededException should be created with custom message and retry after`() {
        // Given
        val message = "API quota exceeded"
        val retryAfter = 120L
        
        // When
        val exception = RateLimitExceededException(message, retryAfter)
        
        // Then
        assertEquals(message, exception.message)
        assertEquals(retryAfter, exception.retryAfterSeconds)
    }
    
    @Test
    fun `AgentExecutionException should be created with agent details`() {
        // Given
        val agentType = "RESEARCH"
        val sessionId = UUID.randomUUID()
        val message = "Agent failed to process"
        
        // When
        val exception = AgentExecutionException(agentType, sessionId, message)
        
        // Then
        assertEquals("Agent execution failed: $message", exception.message)
        assertEquals("AGENT_EXECUTION_FAILED", exception.errorCode)
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.httpStatus)
        assertEquals(agentType, exception.agentType)
        assertEquals(sessionId, exception.sessionId)
    }
    
    @Test
    fun `LLMProviderException should be created with provider details`() {
        // Given
        val provider = "OpenAI"
        val message = "API quota exceeded"
        
        // When
        val exception = LLMProviderException(provider, message)
        
        // Then
        assertEquals("LLM provider error ($provider): $message", exception.message)
        assertEquals("LLM_PROVIDER_ERROR", exception.errorCode)
        assertEquals(HttpStatus.BAD_GATEWAY, exception.httpStatus)
        assertEquals(provider, exception.provider)
    }
    
    @Test
    fun `ServiceUnavailableException should be created with service name`() {
        // Given
        val serviceName = "PaymentService"
        
        // When
        val exception = ServiceUnavailableException(serviceName)
        
        // Then
        assertEquals("Service $serviceName is currently unavailable", exception.message)
        assertEquals("SERVICE_UNAVAILABLE", exception.errorCode)
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.httpStatus)
        assertEquals(serviceName, exception.serviceName)
    }
    
    @Test
    fun `ExternalServiceException should be created with service name and message`() {
        // Given
        val serviceName = "PaymentService"
        val message = "Payment processing failed"
        
        // When
        val exception = ExternalServiceException(serviceName, message)
        
        // Then
        assertEquals("External service error ($serviceName): $message", exception.message)
        assertEquals("EXTERNAL_SERVICE_ERROR", exception.errorCode)
        assertEquals(HttpStatus.BAD_GATEWAY, exception.httpStatus)
        assertEquals(serviceName, exception.serviceName)
    }
    
    @Test
    fun `ExceptionFactory should create user not found exception`() {
        // Given
        val userId = UUID.randomUUID()
        
        // When
        val exception = ExceptionFactory.userNotFound(userId)
        
        // Then
        assertEquals("User", exception.resourceType)
        assertEquals(userId.toString(), exception.resourceId)
        assertEquals("RESOURCE_NOT_FOUND", exception.errorCode)
    }
    
    @Test
    fun `ExceptionFactory should create invalid credentials exception`() {
        // When
        val exception = ExceptionFactory.invalidCredentials()
        
        // Then
        assertEquals("Invalid email or password", exception.message)
        assertEquals("UNAUTHORIZED", exception.errorCode)
    }
    
    @Test
    fun `ExceptionFactory should create rate limit exceeded exception`() {
        // Given
        val retryAfter = 60L
        
        // When
        val exception = ExceptionFactory.rateLimitExceeded(retryAfter)
        
        // Then
        assertTrue(exception.message!!.contains("60 seconds"))
        assertEquals(retryAfter, exception.retryAfterSeconds)
    }
}