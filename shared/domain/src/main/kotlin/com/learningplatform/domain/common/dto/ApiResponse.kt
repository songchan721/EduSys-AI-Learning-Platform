package com.learningplatform.domain.common.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import java.time.Instant
import java.util.*

/**
 * Standard API Response wrapper for all REST endpoints
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ErrorDetails? = null,
    val meta: ResponseMetadata = ResponseMetadata()
) {
    companion object {
        /**
         * Create a successful response with data
         */
        fun <T> success(data: T, meta: ResponseMetadata = ResponseMetadata()): ApiResponse<T> {
            return ApiResponse(
                success = true,
                data = data,
                meta = meta
            )
        }
        
        /**
         * Create a successful response without data
         */
        fun success(meta: ResponseMetadata = ResponseMetadata()): ApiResponse<Unit> {
            return ApiResponse(
                success = true,
                meta = meta
            )
        }
        
        /**
         * Create an error response
         */
        fun <T> error(
            code: String,
            message: String,
            details: List<ValidationError> = emptyList(),
            meta: ResponseMetadata = ResponseMetadata()
        ): ApiResponse<T> {
            return ApiResponse(
                success = false,
                error = ErrorDetails(
                    code = code,
                    message = message,
                    details = details
                ),
                meta = meta
            )
        }
        
        /**
         * Create a validation error response
         */
        fun <T> validationError(
            message: String = "Validation failed",
            details: List<ValidationError>,
            meta: ResponseMetadata = ResponseMetadata()
        ): ApiResponse<T> {
            return error(
                code = "VALIDATION_ERROR",
                message = message,
                details = details,
                meta = meta
            )
        }
        
        /**
         * Create a not found error response
         */
        fun <T> notFound(
            message: String = "Resource not found",
            meta: ResponseMetadata = ResponseMetadata()
        ): ApiResponse<T> {
            return error(
                code = "NOT_FOUND",
                message = message,
                meta = meta
            )
        }
    }
}

/**
 * Error details for API responses
 */
data class ErrorDetails(
    val code: String,
    val message: String,
    val details: List<ValidationError> = emptyList()
)

/**
 * Validation error details
 */
data class ValidationError(
    val field: String,
    val code: String,
    val message: String,
    val rejectedValue: Any? = null
)

/**
 * Response metadata
 */
data class ResponseMetadata(
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX")
    val timestamp: Instant = Instant.now(),
    val correlationId: UUID = UUID.randomUUID(),
    val version: String = "v1",
    val requestId: String? = null,
    val executionTimeMs: Long? = null
)

/**
 * Paginated response wrapper
 */
data class PagedResponse<T>(
    val content: List<T>,
    val page: PageInfo,
    val meta: ResponseMetadata = ResponseMetadata()
)

/**
 * Page information for paginated responses
 */
data class PageInfo(
    val number: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val first: Boolean,
    val last: Boolean,
    val hasNext: Boolean,
    val hasPrevious: Boolean,
    val numberOfElements: Int
) {
    companion object {
        fun of(
            pageNumber: Int,
            pageSize: Int,
            totalElements: Long,
            numberOfElements: Int
        ): PageInfo {
            val totalPages = if (pageSize == 0) 1 else ((totalElements + pageSize - 1) / pageSize).toInt()
            return PageInfo(
                number = pageNumber,
                size = pageSize,
                totalElements = totalElements,
                totalPages = totalPages,
                first = pageNumber == 0,
                last = pageNumber >= totalPages - 1,
                hasNext = pageNumber < totalPages - 1,
                hasPrevious = pageNumber > 0,
                numberOfElements = numberOfElements
            )
        }
    }
}

/**
 * Authentication response
 */
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long, // seconds
    val user: Any // UserDto will be used here
)

/**
 * Health check response
 */
data class HealthResponse(
    val status: HealthStatus,
    val components: Map<String, ComponentHealth> = emptyMap(),
    val version: String,
    val uptime: Long // milliseconds
)

/**
 * Component health information
 */
data class ComponentHealth(
    val status: HealthStatus,
    val details: Map<String, Any> = emptyMap()
)

/**
 * Health status enumeration
 */
enum class HealthStatus {
    UP, DOWN, OUT_OF_SERVICE, UNKNOWN
}