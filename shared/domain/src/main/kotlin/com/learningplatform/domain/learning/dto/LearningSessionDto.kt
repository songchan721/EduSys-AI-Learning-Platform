package com.learningplatform.domain.learning.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.learningplatform.domain.learning.AgentType
import com.learningplatform.domain.learning.ExecutionStatus
import com.learningplatform.domain.learning.SessionStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.Instant
import java.util.*

/**
 * DTO for Learning Session
 */
data class LearningSessionDto(
    val id: UUID,
    val userId: UUID,
    val topic: String,
    val status: SessionStatus,
    val estimatedDurationMinutes: Int?,
    val actualDurationMinutes: Int?,
    val qualityScore: BigDecimal?,
    val costUsd: BigDecimal?,
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX")
    val createdAt: Instant,
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX")
    val completedAt: Instant?,
    
    val agentExecutions: List<AgentExecutionDto> = emptyList(),
    val progressPercentage: Int = 0,
    val currentStage: Int = 0
) {
    /**
     * Check if session is completed
     */
    fun isCompleted(): Boolean = status == SessionStatus.COMPLETED
    
    /**
     * Check if session is in progress
     */
    fun isInProgress(): Boolean = status == SessionStatus.IN_PROGRESS
    
    /**
     * Check if session has failed
     */
    fun isFailed(): Boolean = status == SessionStatus.FAILED
    
    /**
     * Get the duration in a human-readable format
     */
    fun getFormattedDuration(): String? {
        return actualDurationMinutes?.let { minutes ->
            when {
                minutes < 60 -> "${minutes}m"
                minutes < 1440 -> "${minutes / 60}h ${minutes % 60}m"
                else -> "${minutes / 1440}d ${(minutes % 1440) / 60}h"
            }
        }
    }
    
    /**
     * Get the estimated cost formatted as currency
     */
    fun getFormattedCost(): String? {
        return costUsd?.let { cost ->
            "$${String.format("%.2f", cost)}"
        }
    }
}

/**
 * DTO for creating a new learning session
 */
data class CreateLearningSessionDto(
    @field:NotBlank(message = "Topic is required")
    @field:Size(min = 3, max = 1000, message = "Topic must be between 3 and 1000 characters")
    val topic: String,
    
    val priority: SessionPriority = SessionPriority.NORMAL
)

/**
 * DTO for updating a learning session
 */
data class UpdateLearningSessionDto(
    val status: SessionStatus?,
    val qualityScore: BigDecimal?,
    val actualDurationMinutes: Int?
)

/**
 * DTO for Agent Execution
 */
data class AgentExecutionDto(
    val id: UUID,
    val sessionId: UUID,
    val agentType: AgentType,
    val stageNumber: Int,
    val status: ExecutionStatus,
    val inputData: Map<String, Any>?,
    val outputData: Map<String, Any>?,
    val errorMessage: String?,
    val llmProvider: String?,
    val llmModel: String?,
    val tokensUsed: Int?,
    val costUsd: BigDecimal?,
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX")
    val startedAt: Instant?,
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX")
    val completedAt: Instant?,
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX")
    val createdAt: Instant
) {
    /**
     * Get execution duration in minutes
     */
    fun getDurationMinutes(): Long? {
        return if (startedAt != null && completedAt != null) {
            java.time.Duration.between(startedAt, completedAt).toMinutes()
        } else null
    }
    
    /**
     * Get formatted execution time
     */
    fun getFormattedDuration(): String? {
        return getDurationMinutes()?.let { minutes ->
            when {
                minutes < 1 -> "<1m"
                minutes < 60 -> "${minutes}m"
                else -> "${minutes / 60}h ${minutes % 60}m"
            }
        }
    }
    
    /**
     * Check if execution is completed
     */
    fun isCompleted(): Boolean = status == ExecutionStatus.COMPLETED
    
    /**
     * Check if execution is running
     */
    fun isRunning(): Boolean = status == ExecutionStatus.RUNNING
    
    /**
     * Check if execution has failed
     */
    fun isFailed(): Boolean = status == ExecutionStatus.FAILED
}

/**
 * Enums for configuration options
 */
enum class SessionPriority {
    LOW, NORMAL, HIGH
}