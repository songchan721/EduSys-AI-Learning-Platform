package com.learningplatform.domain.dto

import com.learningplatform.domain.learning.AgentType
import com.learningplatform.domain.learning.ExecutionStatus
import com.learningplatform.domain.learning.SessionStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.Instant
import java.util.*

// Learning Session DTOs
data class LearningSessionDto(
    val id: UUID,
    val userId: UUID,
    val topic: String,
    val status: SessionStatus,
    val estimatedDurationMinutes: Int? = null,
    val actualDurationMinutes: Int? = null,
    val qualityScore: BigDecimal? = null,
    val costUsd: BigDecimal? = null,
    val createdAt: Instant,
    val completedAt: Instant? = null,
    val agentExecutions: List<AgentExecutionDto>,
    val progressPercentage: Int,
    val currentStage: Int
)

data class AgentExecutionDto(
    val id: UUID,
    val sessionId: UUID,
    val agentType: AgentType,
    val stageNumber: Int,
    val status: ExecutionStatus,
    val inputData: Any? = null,
    val outputData: Any? = null,
    val errorMessage: String? = null,
    val llmProvider: String? = null,
    val llmModel: String? = null,
    val tokensUsed: Int? = null,
    val costUsd: BigDecimal? = null,
    val startedAt: Instant? = null,
    val completedAt: Instant? = null,
    val createdAt: Instant,
    val durationMinutes: Long? = null
)

data class AgentCheckpointDto(
    val id: UUID,
    val executionId: UUID,
    val checkpointData: Any,
    val createdAt: Instant
)

// Request DTOs
data class CreateLearningSessionRequest(
    @field:NotBlank(message = "Topic is required")
    @field:Size(min = 3, max = 1000, message = "Topic must be between 3 and 1000 characters")
    val topic: String,
    
    val agentConfiguration: AgentConfigurationRequest? = null,
    val priority: SessionPriority = SessionPriority.NORMAL
)

data class AgentConfigurationRequest(
    val llmProvider: String? = null,
    val llmModel: String? = null,
    val customParameters: Map<String, Any>? = null
)

enum class SessionPriority {
    LOW, NORMAL, HIGH
}

// Response DTOs
data class CreateLearningSessionResponse(
    val success: Boolean,
    val data: CreateLearningSessionData,
    val meta: ResponseMeta
)

data class CreateLearningSessionData(
    val session: LearningSessionDto,
    val websocketUrl: String,
    val estimatedCompletionTime: Instant
)

data class SessionListRequest(
    val status: SessionStatus? = null,
    val page: Int = 0,
    val size: Int = 20,
    val sortBy: String = "createdAt",
    val sortDirection: String = "desc"
)

data class SessionListResponse(
    val success: Boolean,
    val data: SessionListData,
    val meta: ResponseMeta
)

data class SessionListData(
    val sessions: List<LearningSessionDto>,
    val pagination: PaginationInfo
)

// Real-time update DTOs
data class SessionProgressUpdate(
    val sessionId: UUID,
    val currentStage: Int,
    val totalStages: Int,
    val progressPercentage: Int,
    val currentAgentType: AgentType,
    val estimatedTimeRemaining: Int? = null, // minutes
    val message: String? = null,
    val timestamp: Instant
)

data class AgentProgressUpdate(
    val sessionId: UUID,
    val executionId: UUID,
    val agentType: AgentType,
    val stageNumber: Int,
    val status: ExecutionStatus,
    val progressPercentage: Int,
    val message: String? = null,
    val intermediateResults: Any? = null,
    val timestamp: Instant
)

// Agent configuration DTOs
data class AgentConfigurationDto(
    val id: UUID,
    val userId: UUID,
    val agentType: AgentType,
    val llmProvider: String,
    val llmModel: String,
    val hasApiKey: Boolean,
    val configData: Map<String, Any>? = null,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class CreateAgentConfigurationRequest(
    val agentType: AgentType,
    
    @field:NotBlank(message = "LLM provider is required")
    val llmProvider: String,
    
    @field:NotBlank(message = "LLM model is required")
    val llmModel: String,
    
    val apiKey: String? = null, // Will be encrypted
    val configData: Map<String, Any>? = null
)

data class UpdateAgentConfigurationRequest(
    val llmProvider: String? = null,
    val llmModel: String? = null,
    val apiKey: String? = null, // Will be encrypted
    val configData: Map<String, Any>? = null,
    val isActive: Boolean? = null
)

// LLM Provider DTOs
data class LLMProviderDto(
    val id: UUID,
    val userId: UUID,
    val providerName: String,
    val hasApiKey: Boolean,
    val configData: Map<String, Any>? = null,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class CreateLLMProviderRequest(
    @field:NotBlank(message = "Provider name is required")
    val providerName: String,
    
    @field:NotBlank(message = "API key is required")
    val apiKey: String, // Will be encrypted
    
    val configData: Map<String, Any>? = null
)

data class UpdateLLMProviderRequest(
    val apiKey: String? = null, // Will be encrypted
    val configData: Map<String, Any>? = null,
    val isActive: Boolean? = null
)

// Session analytics DTOs
data class SessionAnalyticsDto(
    val totalSessions: Long,
    val completedSessions: Long,
    val failedSessions: Long,
    val averageDurationMinutes: Double,
    val averageQualityScore: Double,
    val totalCostUsd: BigDecimal,
    val averageCostPerSession: BigDecimal,
    val sessionsByStatus: Map<SessionStatus, Long>,
    val sessionsByAgentType: Map<AgentType, Long>,
    val period: AnalyticsPeriod
)

data class AnalyticsPeriod(
    val startDate: Instant,
    val endDate: Instant,
    val periodType: String // "day", "week", "month", "year"
)