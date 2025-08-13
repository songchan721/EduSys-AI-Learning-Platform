package com.learningplatform.domain.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import java.time.Instant
import java.util.*

// Common DTOs used across the application

data class ResponseMeta(
    val timestamp: Instant = Instant.now(),
    val correlationId: UUID = UUID.randomUUID(),
    val version: String = "v1"
)

data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ApiError? = null,
    val meta: ResponseMeta = ResponseMeta()
)

data class ApiError(
    val code: String,
    val message: String,
    val details: List<ErrorDetail>? = null
)

data class ErrorDetail(
    val field: String,
    val code: String,
    val message: String
)

// Pagination DTOs
data class PageRequest(
    @field:Min(value = 0, message = "Page number must be non-negative")
    val page: Int = 0,
    
    @field:Min(value = 1, message = "Page size must be at least 1")
    @field:Max(value = 100, message = "Page size must not exceed 100")
    val size: Int = 20,
    
    val sort: String? = null,
    val direction: String = "asc"
)

data class PageResponse<T>(
    val content: List<T>,
    val pagination: PaginationInfo
)

data class PaginationInfo(
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean
)

// Filter and Search DTOs
data class FilterCriteria(
    val field: String,
    val operator: FilterOperator,
    val value: Any
)

enum class FilterOperator {
    EQUALS,
    NOT_EQUALS,
    GREATER_THAN,
    GREATER_THAN_OR_EQUAL,
    LESS_THAN,
    LESS_THAN_OR_EQUAL,
    LIKE,
    IN,
    BETWEEN
}

data class SortCriteria(
    val field: String,
    val direction: SortDirection = SortDirection.ASC
)

enum class SortDirection {
    ASC, DESC
}

// Health Check DTOs
data class HealthStatus(
    val status: HealthStatusType,
    val components: Map<String, ComponentHealth>,
    val timestamp: Instant = Instant.now()
)

enum class HealthStatusType {
    UP, DOWN, DEGRADED
}

data class ComponentHealth(
    val status: HealthStatusType,
    val details: Map<String, Any>? = null
)

// Metrics DTOs
data class SystemMetrics(
    val timestamp: Instant,
    val cpu: CpuMetrics,
    val memory: MemoryMetrics,
    val disk: DiskMetrics,
    val network: NetworkMetrics
)

data class CpuMetrics(
    val usage: Double, // percentage
    val cores: Int
)

data class MemoryMetrics(
    val used: Long, // bytes
    val total: Long, // bytes
    val usage: Double // percentage
)

data class DiskMetrics(
    val used: Long, // bytes
    val total: Long, // bytes
    val usage: Double // percentage
)

data class NetworkMetrics(
    val bytesIn: Long,
    val bytesOut: Long
)

data class ApplicationMetrics(
    val timestamp: Instant,
    val activeUsers: Long,
    val activeSessions: Long,
    val completedSessions: Long,
    val failedSessions: Long,
    val averageSessionDuration: Double, // minutes
    val totalContentGenerated: Long,
    val averageQualityScore: Double
)

// Validation DTOs
data class ValidationResult(
    val valid: Boolean,
    val errors: List<ValidationError>
)

data class ValidationError(
    val field: String,
    val message: String,
    val code: String
)

// File Upload DTOs
data class FileUploadRequest(
    val fileName: String,
    val contentType: String,
    val size: Long
)

data class FileUploadResponse(
    val success: Boolean,
    val data: FileUploadData,
    val meta: ResponseMeta
)

data class FileUploadData(
    val fileId: UUID,
    val uploadUrl: String,
    val expiresAt: Instant
)

// Notification DTOs
data class NotificationDto(
    val id: UUID,
    val userId: UUID,
    val type: NotificationType,
    val title: String,
    val message: String,
    val data: Map<String, Any>? = null,
    val read: Boolean,
    val createdAt: Instant
)

enum class NotificationType {
    INFO,
    SUCCESS,
    WARNING,
    ERROR,
    SESSION_STARTED,
    SESSION_COMPLETED,
    SESSION_FAILED,
    AGENT_COMPLETED,
    CONTENT_GENERATED,
    SYSTEM_MAINTENANCE
}

data class CreateNotificationRequest(
    val userId: UUID,
    val type: NotificationType,
    val title: String,
    val message: String,
    val data: Map<String, Any>? = null
)

// Audit DTOs
data class AuditEventDto(
    val id: UUID,
    val userId: UUID?,
    val eventType: String,
    val resourceType: String,
    val resourceId: UUID?,
    val oldValues: Map<String, Any>? = null,
    val newValues: Map<String, Any>? = null,
    val timestamp: Instant,
    val ipAddress: String?,
    val userAgent: String?
)

// Configuration DTOs
data class SystemConfigurationDto(
    val key: String,
    val value: String,
    val description: String?,
    val category: String,
    val updatedAt: Instant,
    val updatedBy: UUID
)

data class UpdateSystemConfigurationRequest(
    val value: String,
    val description: String? = null
)

// Constants
object ApiConstants {
    const val API_VERSION = "v1"
    const val DEFAULT_PAGE_SIZE = 20
    const val MAX_PAGE_SIZE = 100
    const val DEFAULT_SORT_DIRECTION = "asc"
}

// Utility functions for DTOs
fun <T> createSuccessResponse(data: T, meta: ResponseMeta = ResponseMeta()): ApiResponse<T> {
    return ApiResponse(success = true, data = data, meta = meta)
}

fun <T> createErrorResponse(error: ApiError, meta: ResponseMeta = ResponseMeta()): ApiResponse<T> {
    return ApiResponse(success = false, error = error, meta = meta)
}

fun createPaginationInfo(
    page: Int,
    size: Int,
    totalElements: Long
): PaginationInfo {
    val totalPages = if (totalElements == 0L) 0 else ((totalElements - 1) / size + 1).toInt()
    return PaginationInfo(
        page = page,
        size = size,
        totalElements = totalElements,
        totalPages = totalPages,
        hasNext = page < totalPages - 1,
        hasPrevious = page > 0
    )
}