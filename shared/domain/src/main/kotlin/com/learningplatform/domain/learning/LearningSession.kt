package com.learningplatform.domain.learning

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.Instant
import java.util.*

@Entity
@Table(name = "learning_sessions")
data class LearningSession(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),
    
    @Column(name = "user_id", nullable = false)
    val userId: UUID,
    
    @Column(nullable = false, columnDefinition = "TEXT")
    @NotBlank(message = "Topic is required")
    @Size(min = 3, max = 1000, message = "Topic must be between 3 and 1000 characters")
    val topic: String,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: SessionStatus = SessionStatus.PENDING,
    
    @Column(name = "estimated_duration_minutes")
    val estimatedDurationMinutes: Int? = null,
    
    @Column(name = "actual_duration_minutes")
    val actualDurationMinutes: Int? = null,
    
    @Column(name = "quality_score", precision = 3, scale = 2)
    val qualityScore: BigDecimal? = null,
    
    @Column(name = "cost_usd", precision = 10, scale = 4)
    val costUsd: BigDecimal? = null,
    
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
    
    @Column(name = "completed_at")
    val completedAt: Instant? = null,
    
    @OneToMany(mappedBy = "session", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val agentExecutions: MutableList<AgentExecution> = mutableListOf(),
    
    // Note: GeneratedContent relationship managed in content service
) {
    fun addAgentExecution(agentType: AgentType): AgentExecution {
        val execution = AgentExecution(
            session = this,
            agentType = agentType,
            stageNumber = agentExecutions.size + 1
        )
        agentExecutions.add(execution)
        return execution
    }
    
    fun getCurrentStage(): Int = agentExecutions.size
    
    fun getProgressPercentage(): Int {
        val totalStages = 8 // 8 agents in the pipeline
        val completedStages = agentExecutions.count { it.status == ExecutionStatus.COMPLETED }
        return (completedStages * 100) / totalStages
    }
    
    fun isCompleted(): Boolean = status == SessionStatus.COMPLETED
    
    fun isFailed(): Boolean = status == SessionStatus.FAILED
}

@Entity
@Table(name = "agent_executions")
data class AgentExecution(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    val session: LearningSession,
    
    @Enumerated(EnumType.STRING)
    @Column(name = "agent_type", nullable = false)
    val agentType: AgentType,
    
    @Column(name = "stage_number", nullable = false)
    val stageNumber: Int,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: ExecutionStatus = ExecutionStatus.PENDING,
    
    @Column(name = "input_data", columnDefinition = "JSONB")
    val inputData: String? = null,
    
    @Column(name = "output_data", columnDefinition = "JSONB")
    val outputData: String? = null,
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    val errorMessage: String? = null,
    
    @Column(name = "llm_provider")
    val llmProvider: String? = null,
    
    @Column(name = "llm_model")
    val llmModel: String? = null,
    
    @Column(name = "tokens_used")
    val tokensUsed: Int? = null,
    
    @Column(name = "cost_usd", precision = 10, scale = 6)
    val costUsd: BigDecimal? = null,
    
    @Column(name = "started_at")
    val startedAt: Instant? = null,
    
    @Column(name = "completed_at")
    val completedAt: Instant? = null,
    
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
    
    @OneToMany(mappedBy = "execution", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val checkpoints: MutableList<AgentCheckpoint> = mutableListOf()
) {
    fun addCheckpoint(checkpointData: String): AgentCheckpoint {
        val checkpoint = AgentCheckpoint(
            execution = this,
            checkpointData = checkpointData
        )
        checkpoints.add(checkpoint)
        return checkpoint
    }
    
    fun getDurationMinutes(): Long? {
        return if (startedAt != null && completedAt != null) {
            java.time.Duration.between(startedAt, completedAt).toMinutes()
        } else null
    }
}

@Entity
@Table(name = "agent_checkpoints")
data class AgentCheckpoint(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "execution_id", nullable = false)
    val execution: AgentExecution,
    
    @Column(name = "checkpoint_data", nullable = false, columnDefinition = "JSONB")
    val checkpointData: String,
    
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)

enum class SessionStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    CANCELLED
}

enum class ExecutionStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED
}

enum class AgentType {
    RESEARCH,
    VERIFICATION,
    DECOMPOSITION,
    STRUCTURING,
    CONTENT_GENERATION,
    VALIDATION,
    SYNTHESIS,
    LEARNING_EXPERIENCE
}