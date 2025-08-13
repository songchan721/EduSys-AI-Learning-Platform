package com.learningplatform.utils.exception

import com.learningplatform.domain.dto.ApiError
import com.learningplatform.domain.dto.ApiResponse
import com.learningplatform.domain.dto.ErrorDetail
import com.learningplatform.domain.dto.ResponseMeta
import com.learningplatform.utils.correlation.CorrelationIdService
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.AuthenticationException
import org.springframework.validation.FieldError
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.NoHandlerFoundException
import java.time.Instant
import java.util.*
import jakarta.validation.ConstraintViolationException

@RestControllerAdvice
class GlobalExceptionHandler(
    private val correlationIdService: CorrelationIdService
) {
    
    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
    
    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(ex: BusinessException): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn("Business exception: {}", ex.message, ex)
        
        val error = ApiError(
            code = ex.errorCode,
            message = ex.message ?: "Business rule violation",
            details = ex.details
        )
        
        val response = ApiResponse<Nothing>(
            success = false,
            error = error,
            meta = createResponseMeta()
        )
        
        return ResponseEntity.status(ex.httpStatus).body(response)
    }
    
    @ExceptionHandler(ValidationException::class)
    fun handleValidationException(ex: ValidationException): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn("Validation exception: {}", ex.message, ex)
        
        val error = ApiError(
            code = "VALIDATION_ERROR",
            message = ex.message ?: "Validation failed",
            details = ex.errors
        )
        
        val response = ApiResponse<Nothing>(
            success = false,
            error = error,
            meta = createResponseMeta()
        )
        
        return ResponseEntity.badRequest().body(response)
    }
    
    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleResourceNotFoundException(ex: ResourceNotFoundException): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn("Resource not found: {}", ex.message, ex)
        
        val error = ApiError(
            code = "RESOURCE_NOT_FOUND",
            message = ex.message ?: "Resource not found",
            details = listOf(
                ErrorDetail(
                    field = ex.resourceType,
                    code = "NOT_FOUND",
                    message = "Resource with ID ${ex.resourceId} not found"
                )
            )
        )
        
        val response = ApiResponse<Nothing>(
            success = false,
            error = error,
            meta = createResponseMeta()
        )
        
        return ResponseEntity.notFound().build()
    }
    
    @ExceptionHandler(UnauthorizedException::class)
    fun handleUnauthorizedException(ex: UnauthorizedException): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn("Unauthorized access: {}", ex.message, ex)
        
        val error = ApiError(
            code = "UNAUTHORIZED",
            message = ex.message ?: "Authentication required"
        )
        
        val response = ApiResponse<Nothing>(
            success = false,
            error = error,
            meta = createResponseMeta()
        )
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response)
    }
    
    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthenticationException(ex: AuthenticationException): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn("Authentication failed: {}", ex.message, ex)
        
        val error = ApiError(
            code = "AUTHENTICATION_FAILED",
            message = "Invalid credentials"
        )
        
        val response = ApiResponse<Nothing>(
            success = false,
            error = error,
            meta = createResponseMeta()
        )
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response)
    }
    
    @ExceptionHandler(BadCredentialsException::class)
    fun handleBadCredentialsException(ex: BadCredentialsException): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn("Bad credentials: {}", ex.message, ex)
        
        val error = ApiError(
            code = "INVALID_CREDENTIALS",
            message = "Invalid email or password"
        )
        
        val response = ApiResponse<Nothing>(
            success = false,
            error = error,
            meta = createResponseMeta()
        )
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response)
    }
    
    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDeniedException(ex: AccessDeniedException): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn("Access denied: {}", ex.message, ex)
        
        val error = ApiError(
            code = "ACCESS_DENIED",
            message = "Insufficient permissions"
        )
        
        val response = ApiResponse<Nothing>(
            success = false,
            error = error,
            meta = createResponseMeta()
        )
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response)
    }
    
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(ex: MethodArgumentNotValidException): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn("Validation failed: {}", ex.message, ex)
        
        val details = ex.bindingResult.allErrors.map { error ->
            when (error) {
                is FieldError -> ErrorDetail(
                    field = error.field,
                    code = error.code ?: "INVALID",
                    message = error.defaultMessage ?: "Invalid value"
                )
                else -> ErrorDetail(
                    field = error.objectName,
                    code = "INVALID",
                    message = error.defaultMessage ?: "Invalid value"
                )
            }
        }
        
        val error = ApiError(
            code = "VALIDATION_ERROR",
            message = "Request validation failed",
            details = details
        )
        
        val response = ApiResponse<Nothing>(
            success = false,
            error = error,
            meta = createResponseMeta()
        )
        
        return ResponseEntity.badRequest().body(response)
    }
    
    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolationException(ex: ConstraintViolationException): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn("Constraint violation: {}", ex.message, ex)
        
        val details = ex.constraintViolations.map { violation ->
            ErrorDetail(
                field = violation.propertyPath.toString(),
                code = "CONSTRAINT_VIOLATION",
                message = violation.message
            )
        }
        
        val error = ApiError(
            code = "VALIDATION_ERROR",
            message = "Constraint validation failed",
            details = details
        )
        
        val response = ApiResponse<Nothing>(
            success = false,
            error = error,
            meta = createResponseMeta()
        )
        
        return ResponseEntity.badRequest().body(response)
    }
    
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableException(ex: HttpMessageNotReadableException): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn("Invalid request body: {}", ex.message, ex)
        
        val error = ApiError(
            code = "INVALID_REQUEST_BODY",
            message = "Invalid request body format"
        )
        
        val response = ApiResponse<Nothing>(
            success = false,
            error = error,
            meta = createResponseMeta()
        )
        
        return ResponseEntity.badRequest().body(response)
    }
    
    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingServletRequestParameterException(ex: MissingServletRequestParameterException): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn("Missing request parameter: {}", ex.message, ex)
        
        val error = ApiError(
            code = "MISSING_PARAMETER",
            message = "Required parameter '${ex.parameterName}' is missing",
            details = listOf(
                ErrorDetail(
                    field = ex.parameterName,
                    code = "REQUIRED",
                    message = "This parameter is required"
                )
            )
        )
        
        val response = ApiResponse<Nothing>(
            success = false,
            error = error,
            meta = createResponseMeta()
        )
        
        return ResponseEntity.badRequest().body(response)
    }
    
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleMethodArgumentTypeMismatchException(ex: MethodArgumentTypeMismatchException): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn("Type mismatch: {}", ex.message, ex)
        
        val error = ApiError(
            code = "TYPE_MISMATCH",
            message = "Invalid parameter type for '${ex.name}'",
            details = listOf(
                ErrorDetail(
                    field = ex.name,
                    code = "INVALID_TYPE",
                    message = "Expected type: ${ex.requiredType?.simpleName}"
                )
            )
        )
        
        val response = ApiResponse<Nothing>(
            success = false,
            error = error,
            meta = createResponseMeta()
        )
        
        return ResponseEntity.badRequest().body(response)
    }
    
    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleHttpRequestMethodNotSupportedException(ex: HttpRequestMethodNotSupportedException): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn("Method not supported: {}", ex.message, ex)
        
        val error = ApiError(
            code = "METHOD_NOT_SUPPORTED",
            message = "HTTP method '${ex.method}' not supported for this endpoint"
        )
        
        val response = ApiResponse<Nothing>(
            success = false,
            error = error,
            meta = createResponseMeta()
        )
        
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response)
    }
    
    @ExceptionHandler(NoHandlerFoundException::class)
    fun handleNoHandlerFoundException(ex: NoHandlerFoundException): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn("No handler found: {}", ex.message, ex)
        
        val error = ApiError(
            code = "ENDPOINT_NOT_FOUND",
            message = "Endpoint not found: ${ex.httpMethod} ${ex.requestURL}"
        )
        
        val response = ApiResponse<Nothing>(
            success = false,
            error = error,
            meta = createResponseMeta()
        )
        
        return ResponseEntity.notFound().build()
    }
    
    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrityViolationException(ex: DataIntegrityViolationException): ResponseEntity<ApiResponse<Nothing>> {
        logger.error("Data integrity violation: {}", ex.message, ex)
        
        val error = ApiError(
            code = "DATA_INTEGRITY_VIOLATION",
            message = "Data integrity constraint violated"
        )
        
        val response = ApiResponse<Nothing>(
            success = false,
            error = error,
            meta = createResponseMeta()
        )
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response)
    }
    
    @ExceptionHandler(RateLimitExceededException::class)
    fun handleRateLimitExceededException(ex: RateLimitExceededException): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn("Rate limit exceeded: {}", ex.message, ex)
        
        val error = ApiError(
            code = "RATE_LIMIT_EXCEEDED",
            message = ex.message ?: "Rate limit exceeded"
        )
        
        val response = ApiResponse<Nothing>(
            success = false,
            error = error,
            meta = createResponseMeta()
        )
        
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response)
    }
    
    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ResponseEntity<ApiResponse<Nothing>> {
        logger.error("Unexpected error: {}", ex.message, ex)
        
        val error = ApiError(
            code = "INTERNAL_SERVER_ERROR",
            message = "An unexpected error occurred"
        )
        
        val response = ApiResponse<Nothing>(
            success = false,
            error = error,
            meta = createResponseMeta()
        )
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response)
    }
    
    private fun createResponseMeta(): ResponseMeta {
        return ResponseMeta(
            timestamp = Instant.now(),
            correlationId = UUID.fromString(correlationIdService.getCorrelationId() ?: UUID.randomUUID().toString()),
            version = "v1"
        )
    }
}