package com.learningplatform.utils.exception

import com.learningplatform.domain.dto.ErrorDetail
import org.springframework.http.HttpStatus
import java.util.*

// Base exception for business logic errors
abstract class BusinessException(
    message: String,
    val errorCode: String,
    val httpStatus: HttpStatus = HttpStatus.BAD_REQUEST,
    val details: List<ErrorDetail>? = null,
    cause: Throwable? = null
) : RuntimeException(message, cause)

// Validation related exceptions
class ValidationException(
    message: String,
    val errors: List<ErrorDetail> = emptyList(),
    cause: Throwable? = null
) : BusinessException(
    message = message,
    errorCode = "VALIDATION_ERROR",
    httpStatus = HttpStatus.BAD_REQUEST,
    details = errors,
    cause = cause
)

// Resource not found exception
class ResourceNotFoundException(
    val resourceType: String,
    val resourceId: String,
    message: String? = null
) : BusinessException(
    message = message ?: "$resourceType with ID $resourceId not found",
    errorCode = "RESOURCE_NOT_FOUND",
    httpStatus = HttpStatus.NOT_FOUND
)

// Authentication and authorization exceptions
class UnauthorizedException(
    message: String = "Authentication required",
    cause: Throwable? = null
) : BusinessException(
    message = message,
    errorCode = "UNAUTHORIZED",
    httpStatus = HttpStatus.UNAUTHORIZED,
    cause = cause
)

class ForbiddenException(
    message: String = "Access denied",
    cause: Throwable? = null
) : BusinessException(
    message = message,
    errorCode = "FORBIDDEN",
    httpStatus = HttpStatus.FORBIDDEN,
    cause = cause
)

// Rate limiting exception
class RateLimitExceededException(
    message: String = "Rate limit exceeded",
    val retryAfterSeconds: Long? = null,
    cause: Throwable? = null
) : BusinessException(
    message = message,
    errorCode = "RATE_LIMIT_EXCEEDED",
    httpStatus = HttpStatus.TOO_MANY_REQUESTS,
    cause = cause
)

// Agent execution exceptions
class AgentExecutionException(
    val agentType: String,
    val sessionId: UUID,
    message: String,
    cause: Throwable? = null
) : BusinessException(
    message = "Agent execution failed: $message",
    errorCode = "AGENT_EXECUTION_FAILED",
    httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
    cause = cause
)

class AgentTimeoutException(
    val agentType: String,
    val sessionId: UUID,
    val timeoutMinutes: Long,
    cause: Throwable? = null
) : BusinessException(
    message = "Agent $agentType timed out after $timeoutMinutes minutes",
    errorCode = "AGENT_TIMEOUT",
    httpStatus = HttpStatus.REQUEST_TIMEOUT,
    cause = cause
)

class AgentConfigurationException(
    val agentType: String,
    message: String,
    cause: Throwable? = null
) : BusinessException(
    message = "Agent configuration error for $agentType: $message",
    errorCode = "AGENT_CONFIGURATION_ERROR",
    httpStatus = HttpStatus.BAD_REQUEST,
    cause = cause
)

// LLM provider exceptions
class LLMProviderException(
    val provider: String,
    message: String,
    cause: Throwable? = null
) : BusinessException(
    message = "LLM provider error ($provider): $message",
    errorCode = "LLM_PROVIDER_ERROR",
    httpStatus = HttpStatus.BAD_GATEWAY,
    cause = cause
)

class LLMQuotaExceededException(
    val provider: String,
    val userId: UUID,
    cause: Throwable? = null
) : BusinessException(
    message = "LLM quota exceeded for provider $provider",
    errorCode = "LLM_QUOTA_EXCEEDED",
    httpStatus = HttpStatus.PAYMENT_REQUIRED,
    cause = cause
)

class LLMApiKeyInvalidException(
    val provider: String,
    cause: Throwable? = null
) : BusinessException(
    message = "Invalid API key for provider $provider",
    errorCode = "LLM_API_KEY_INVALID",
    httpStatus = HttpStatus.UNAUTHORIZED,
    cause = cause
)

// Content related exceptions
class ContentGenerationException(
    val contentType: String,
    message: String,
    cause: Throwable? = null
) : BusinessException(
    message = "Content generation failed for $contentType: $message",
    errorCode = "CONTENT_GENERATION_FAILED",
    httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
    cause = cause
)

class ContentValidationException(
    val contentId: UUID,
    val validationErrors: List<String>,
    cause: Throwable? = null
) : BusinessException(
    message = "Content validation failed: ${validationErrors.joinToString(", ")}",
    errorCode = "CONTENT_VALIDATION_FAILED",
    httpStatus = HttpStatus.BAD_REQUEST,
    details = validationErrors.map { error ->
        ErrorDetail(
            field = "content",
            code = "VALIDATION_FAILED",
            message = error
        )
    },
    cause = cause
)

// Payment and subscription exceptions
class SubscriptionException(
    val userId: UUID,
    message: String,
    cause: Throwable? = null
) : BusinessException(
    message = "Subscription error: $message",
    errorCode = "SUBSCRIPTION_ERROR",
    httpStatus = HttpStatus.PAYMENT_REQUIRED,
    cause = cause
)

class PaymentProcessingException(
    val userId: UUID,
    message: String,
    cause: Throwable? = null
) : BusinessException(
    message = "Payment processing failed: $message",
    errorCode = "PAYMENT_PROCESSING_FAILED",
    httpStatus = HttpStatus.PAYMENT_REQUIRED,
    cause = cause
)

// System and infrastructure exceptions
class ServiceUnavailableException(
    val serviceName: String,
    message: String? = null,
    cause: Throwable? = null
) : BusinessException(
    message = message ?: "Service $serviceName is currently unavailable",
    errorCode = "SERVICE_UNAVAILABLE",
    httpStatus = HttpStatus.SERVICE_UNAVAILABLE,
    cause = cause
)

class ConfigurationException(
    val configurationKey: String,
    message: String,
    cause: Throwable? = null
) : BusinessException(
    message = "Configuration error for $configurationKey: $message",
    errorCode = "CONFIGURATION_ERROR",
    httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
    cause = cause
)

class ExternalServiceException(
    val serviceName: String,
    message: String,
    cause: Throwable? = null
) : BusinessException(
    message = "External service error ($serviceName): $message",
    errorCode = "EXTERNAL_SERVICE_ERROR",
    httpStatus = HttpStatus.BAD_GATEWAY,
    cause = cause
)

// Data consistency exceptions
class ConcurrencyException(
    val resourceType: String,
    val resourceId: UUID,
    message: String? = null,
    cause: Throwable? = null
) : BusinessException(
    message = message ?: "Concurrent modification detected for $resourceType $resourceId",
    errorCode = "CONCURRENCY_ERROR",
    httpStatus = HttpStatus.CONFLICT,
    cause = cause
)

class DataConsistencyException(
    message: String,
    cause: Throwable? = null
) : BusinessException(
    message = "Data consistency error: $message",
    errorCode = "DATA_CONSISTENCY_ERROR",
    httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
    cause = cause
)

// Utility functions for creating exceptions
object ExceptionFactory {
    
    fun userNotFound(userId: UUID): ResourceNotFoundException {
        return ResourceNotFoundException("User", userId.toString())
    }
    
    fun sessionNotFound(sessionId: UUID): ResourceNotFoundException {
        return ResourceNotFoundException("LearningSession", sessionId.toString())
    }
    
    fun contentNotFound(contentId: UUID): ResourceNotFoundException {
        return ResourceNotFoundException("GeneratedContent", contentId.toString())
    }
    
    fun agentConfigurationNotFound(agentType: String, userId: UUID): ResourceNotFoundException {
        return ResourceNotFoundException("AgentConfiguration", "$agentType for user $userId")
    }
    
    fun subscriptionNotFound(userId: UUID): ResourceNotFoundException {
        return ResourceNotFoundException("Subscription", "for user $userId")
    }
    
    fun invalidCredentials(): UnauthorizedException {
        return UnauthorizedException("Invalid email or password")
    }
    
    fun tokenExpired(): UnauthorizedException {
        return UnauthorizedException("Token has expired")
    }
    
    fun insufficientPermissions(requiredRole: String): ForbiddenException {
        return ForbiddenException("Insufficient permissions. Required role: $requiredRole")
    }
    
    fun rateLimitExceeded(retryAfterSeconds: Long): RateLimitExceededException {
        return RateLimitExceededException("Rate limit exceeded. Try again in $retryAfterSeconds seconds", retryAfterSeconds)
    }
    
    fun agentExecutionFailed(agentType: String, sessionId: UUID, error: String): AgentExecutionException {
        return AgentExecutionException(agentType, sessionId, error)
    }
    
    fun agentTimeout(agentType: String, sessionId: UUID, timeoutMinutes: Long): AgentTimeoutException {
        return AgentTimeoutException(agentType, sessionId, timeoutMinutes)
    }
    
    fun llmProviderError(provider: String, error: String): LLMProviderException {
        return LLMProviderException(provider, error)
    }
    
    fun llmQuotaExceeded(provider: String, userId: UUID): LLMQuotaExceededException {
        return LLMQuotaExceededException(provider, userId)
    }
    
    fun contentGenerationFailed(contentType: String, error: String): ContentGenerationException {
        return ContentGenerationException(contentType, error)
    }
    
    fun subscriptionRequired(feature: String): SubscriptionException {
        return SubscriptionException(UUID.randomUUID(), "Feature '$feature' requires an active subscription")
    }
    
    fun serviceUnavailable(serviceName: String): ServiceUnavailableException {
        return ServiceUnavailableException(serviceName)
    }
}