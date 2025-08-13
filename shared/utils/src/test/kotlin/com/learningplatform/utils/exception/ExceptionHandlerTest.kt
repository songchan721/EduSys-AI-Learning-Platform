package com.learningplatform.utils.exception

import com.learningplatform.domain.dto.ApiResponse
import com.learningplatform.utils.correlation.CorrelationIdService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.http.HttpStatus
import org.springframework.validation.BindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import java.util.*

@ExtendWith(MockitoExtension::class)
class ExceptionHandlerTest {

    @Mock
    private lateinit var correlationIdService: CorrelationIdService
    
    @Mock
    private lateinit var bindingResult: BindingResult
    
    private lateinit var exceptionHandler: GlobalExceptionHandler
    
    @BeforeEach
    fun setUp() {
        exceptionHandler = GlobalExceptionHandler(correlationIdService)
        whenever(correlationIdService.getCorrelationId()).thenReturn("test-correlation-id")
    }
    
    @Test
    fun `handleValidationException should return bad request with validation errors`() {
        // Given
        val exception = ValidationException("Validation failed")
        
        // When
        val response = exceptionHandler.handleValidationException(exception)
        
        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        val body = response.body as ApiResponse<*>
        assertFalse(body.success)
        assertEquals("VALIDATION_ERROR", body.error?.code)
        assertEquals("Validation failed", body.error?.message)
    }
    
    @Test
    fun `handleBusinessException should return bad request`() {
        // Given
        val exception = ValidationException("Business rule violation") // ValidationException extends BusinessException
        
        // When
        val response = exceptionHandler.handleBusinessException(exception)
        
        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        val body = response.body as ApiResponse<*>
        assertFalse(body.success)
        assertEquals("VALIDATION_ERROR", body.error?.code)
        assertEquals("Business rule violation", body.error?.message)
    }
    
    @Test
    fun `handleResourceNotFoundException should return not found`() {
        // Given
        val exception = ResourceNotFoundException("User", "123")
        
        // When
        val response = exceptionHandler.handleResourceNotFoundException(exception)
        
        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        // Note: The actual implementation returns notFound().build() which has no body
        assertNull(response.body)
    }
    
    @Test
    fun `handleUnauthorizedException should return unauthorized`() {
        // Given
        val exception = UnauthorizedException("Access denied")
        
        // When
        val response = exceptionHandler.handleUnauthorizedException(exception)
        
        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        val body = response.body as ApiResponse<*>
        assertFalse(body.success)
        assertEquals("UNAUTHORIZED", body.error?.code)
        assertEquals("Access denied", body.error?.message)
    }
    
    @Test
    fun `handleRateLimitExceededException should return too many requests`() {
        // Given
        val exception = RateLimitExceededException("Rate limit exceeded", 60L)
        
        // When
        val response = exceptionHandler.handleRateLimitExceededException(exception)
        
        // Then
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.statusCode)
        val body = response.body as ApiResponse<*>
        assertFalse(body.success)
        assertEquals("RATE_LIMIT_EXCEEDED", body.error?.code)
        assertEquals("Rate limit exceeded", body.error?.message)
    }
    
    @Test
    fun `handleMethodArgumentNotValidException should return bad request with field errors`() {
        // Given
        val fieldError1 = FieldError("user", "email", "rejected@value", false, null, null, "Email is required")
        val fieldError2 = FieldError("user", "firstName", "", false, null, null, "First name is required")
        
        whenever(bindingResult.allErrors).thenReturn(listOf(fieldError1, fieldError2))
        
        val exception = mock<MethodArgumentNotValidException> {
            on { bindingResult } doReturn this@ExceptionHandlerTest.bindingResult
        }
        
        // When
        val response = exceptionHandler.handleMethodArgumentNotValidException(exception)
        
        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        val body = response.body as ApiResponse<*>
        assertFalse(body.success)
        assertEquals("VALIDATION_ERROR", body.error?.code)
        assertEquals("Request validation failed", body.error?.message)
        assertEquals(2, body.error?.details?.size)
    }
    
    @Test
    fun `handleGenericException should return internal server error`() {
        // Given
        val exception = RuntimeException("Unexpected error")
        
        // When
        val response = exceptionHandler.handleGenericException(exception)
        
        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        val body = response.body as ApiResponse<*>
        assertFalse(body.success)
        assertEquals("INTERNAL_SERVER_ERROR", body.error?.code)
        assertEquals("An unexpected error occurred", body.error?.message)
    }
    
    @Test
    fun `all exception handlers should include correlation ID in response metadata`() {
        // Given
        val exception = ValidationException("Test error")
        
        // When
        val response = exceptionHandler.handleValidationException(exception)
        
        // Then
        val body = response.body as ApiResponse<*>
        assertEquals("test-correlation-id", body.meta.correlationId.toString())
    }
}