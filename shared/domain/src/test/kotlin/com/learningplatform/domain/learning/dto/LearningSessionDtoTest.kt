package com.learningplatform.domain.learning.dto

import com.learningplatform.domain.learning.AgentType
import com.learningplatform.domain.learning.ExecutionStatus
import com.learningplatform.domain.learning.SessionStatus
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.math.BigDecimal
import java.time.Instant
import java.util.*

class LearningSessionDtoTest {

    @Test
    fun `LearningSessionDto should be created with all fields`() {
        // Given
        val id = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val topic = "Machine Learning Basics"
        val status = SessionStatus.IN_PROGRESS
        val estimatedDurationMinutes = 120
        val actualDurationMinutes = 90
        val qualityScore = BigDecimal("0.85")
        val costUsd = BigDecimal("2.50")
        val createdAt = Instant.now()
        val completedAt = Instant.now()
        val agentExecutions = listOf(createTestAgentExecutionDto())
        val progressPercentage = 75
        val currentStage = 3
        
        // When
        val sessionDto = LearningSessionDto(
            id = id,
            userId = userId,
            topic = topic,
            status = status,
            estimatedDurationMinutes = estimatedDurationMinutes,
            actualDurationMinutes = actualDurationMinutes,
            qualityScore = qualityScore,
            costUsd = costUsd,
            createdAt = createdAt,
            completedAt = completedAt,
            agentExecutions = agentExecutions,
            progressPercentage = progressPercentage,
            currentStage = currentStage
        )
        
        // Then
        assertEquals(id, sessionDto.id)
        assertEquals(userId, sessionDto.userId)
        assertEquals(topic, sessionDto.topic)
        assertEquals(status, sessionDto.status)
        assertEquals(estimatedDurationMinutes, sessionDto.estimatedDurationMinutes)
        assertEquals(actualDurationMinutes, sessionDto.actualDurationMinutes)
        assertEquals(qualityScore, sessionDto.qualityScore)
        assertEquals(costUsd, sessionDto.costUsd)
        assertEquals(createdAt, sessionDto.createdAt)
        assertEquals(completedAt, sessionDto.completedAt)
        assertEquals(agentExecutions, sessionDto.agentExecutions)
        assertEquals(progressPercentage, sessionDto.progressPercentage)
        assertEquals(currentStage, sessionDto.currentStage)
    }
    
    @Test
    fun `isCompleted should return true when status is COMPLETED`() {
        // Given
        val sessionDto = createTestLearningSessionDto(status = SessionStatus.COMPLETED)
        
        // When & Then
        assertTrue(sessionDto.isCompleted())
        assertFalse(sessionDto.isInProgress())
        assertFalse(sessionDto.isFailed())
    }
    
    @Test
    fun `isInProgress should return true when status is IN_PROGRESS`() {
        // Given
        val sessionDto = createTestLearningSessionDto(status = SessionStatus.IN_PROGRESS)
        
        // When & Then
        assertTrue(sessionDto.isInProgress())
        assertFalse(sessionDto.isCompleted())
        assertFalse(sessionDto.isFailed())
    }
    
    @Test
    fun `isFailed should return true when status is FAILED`() {
        // Given
        val sessionDto = createTestLearningSessionDto(status = SessionStatus.FAILED)
        
        // When & Then
        assertTrue(sessionDto.isFailed())
        assertFalse(sessionDto.isCompleted())
        assertFalse(sessionDto.isInProgress())
    }
    
    @Test
    fun `getFormattedDuration should format minutes correctly`() {
        // Test minutes only
        val session1 = createTestLearningSessionDto(actualDurationMinutes = 45)
        assertEquals("45m", session1.getFormattedDuration())
        
        // Test hours and minutes
        val session2 = createTestLearningSessionDto(actualDurationMinutes = 125) // 2h 5m
        assertEquals("2h 5m", session2.getFormattedDuration())
        
        // Test days, hours
        val session3 = createTestLearningSessionDto(actualDurationMinutes = 1500) // 1d 1h
        assertEquals("1d 1h", session3.getFormattedDuration())
        
        // Test null duration
        val session4 = createTestLearningSessionDto(actualDurationMinutes = null)
        assertNull(session4.getFormattedDuration())
    }
    
    @Test
    fun `getFormattedCost should format cost as currency`() {
        // Test normal cost
        val session1 = createTestLearningSessionDto(costUsd = BigDecimal("2.50"))
        assertEquals("$2.50", session1.getFormattedCost())
        
        // Test high precision cost
        val session2 = createTestLearningSessionDto(costUsd = BigDecimal("0.123456"))
        assertEquals("$0.12", session2.getFormattedCost())
        
        // Test null cost
        val session3 = createTestLearningSessionDto(costUsd = null)
        assertNull(session3.getFormattedCost())
    }
    
    @Test
    fun `CreateLearningSessionDto should be created with topic and default priority`() {
        // Given
        val topic = "Advanced Python Programming"
        
        // When
        val createDto = CreateLearningSessionDto(topic = topic)
        
        // Then
        assertEquals(topic, createDto.topic)
        assertEquals(SessionPriority.NORMAL, createDto.priority)
    }
    
    @Test
    fun `CreateLearningSessionDto should be created with custom priority`() {
        // Given
        val topic = "Urgent Learning Topic"
        val priority = SessionPriority.HIGH
        
        // When
        val createDto = CreateLearningSessionDto(topic = topic, priority = priority)
        
        // Then
        assertEquals(topic, createDto.topic)
        assertEquals(priority, createDto.priority)
    }
    
    @Test
    fun `UpdateLearningSessionDto should handle optional fields`() {
        // Test with all fields
        val updateDto1 = UpdateLearningSessionDto(
            status = SessionStatus.COMPLETED,
            qualityScore = BigDecimal("0.9"),
            actualDurationMinutes = 120
        )
        assertEquals(SessionStatus.COMPLETED, updateDto1.status)
        assertEquals(BigDecimal("0.9"), updateDto1.qualityScore)
        assertEquals(120, updateDto1.actualDurationMinutes)
        
        // Test with partial fields
        val updateDto2 = UpdateLearningSessionDto(
            status = SessionStatus.FAILED,
            qualityScore = null,
            actualDurationMinutes = null
        )
        assertEquals(SessionStatus.FAILED, updateDto2.status)
        assertNull(updateDto2.qualityScore)
        assertNull(updateDto2.actualDurationMinutes)
    }
    
    @Test
    fun `AgentExecutionDto should be created with all fields`() {
        // Given
        val id = UUID.randomUUID()
        val sessionId = UUID.randomUUID()
        val agentType = AgentType.RESEARCH
        val stageNumber = 1
        val status = ExecutionStatus.COMPLETED
        val inputData = mapOf("query" to "machine learning")
        val outputData = mapOf("results" to "research data")
        val errorMessage = null
        val llmProvider = "OpenAI"
        val llmModel = "gpt-4"
        val tokensUsed = 1500
        val costUsd = BigDecimal("0.03")
        val startedAt = Instant.now().minusSeconds(300)
        val completedAt = Instant.now()
        val createdAt = Instant.now().minusSeconds(600)
        
        // When
        val executionDto = AgentExecutionDto(
            id = id,
            sessionId = sessionId,
            agentType = agentType,
            stageNumber = stageNumber,
            status = status,
            inputData = inputData,
            outputData = outputData,
            errorMessage = errorMessage,
            llmProvider = llmProvider,
            llmModel = llmModel,
            tokensUsed = tokensUsed,
            costUsd = costUsd,
            startedAt = startedAt,
            completedAt = completedAt,
            createdAt = createdAt
        )
        
        // Then
        assertEquals(id, executionDto.id)
        assertEquals(sessionId, executionDto.sessionId)
        assertEquals(agentType, executionDto.agentType)
        assertEquals(stageNumber, executionDto.stageNumber)
        assertEquals(status, executionDto.status)
        assertEquals(inputData, executionDto.inputData)
        assertEquals(outputData, executionDto.outputData)
        assertEquals(errorMessage, executionDto.errorMessage)
        assertEquals(llmProvider, executionDto.llmProvider)
        assertEquals(llmModel, executionDto.llmModel)
        assertEquals(tokensUsed, executionDto.tokensUsed)
        assertEquals(costUsd, executionDto.costUsd)
        assertEquals(startedAt, executionDto.startedAt)
        assertEquals(completedAt, executionDto.completedAt)
        assertEquals(createdAt, executionDto.createdAt)
    }
    
    @Test
    fun `AgentExecutionDto getDurationMinutes should calculate correctly`() {
        // Test with both start and end times
        val startTime = Instant.now().minusSeconds(300) // 5 minutes ago
        val endTime = Instant.now()
        val executionDto = createTestAgentExecutionDto(startedAt = startTime, completedAt = endTime)
        
        val duration = executionDto.getDurationMinutes()
        assertNotNull(duration)
        assertEquals(5L, duration)
        
        // Test with missing times
        val executionDto2 = createTestAgentExecutionDto(startedAt = null, completedAt = null)
        assertNull(executionDto2.getDurationMinutes())
    }
    
    @Test
    fun `AgentExecutionDto getFormattedDuration should format correctly`() {
        // Test less than 1 minute
        val execution1 = createTestAgentExecutionDto(
            startedAt = Instant.now().minusSeconds(30),
            completedAt = Instant.now()
        )
        assertEquals("<1m", execution1.getFormattedDuration())
        
        // Test minutes only
        val execution2 = createTestAgentExecutionDto(
            startedAt = Instant.now().minusSeconds(300), // 5 minutes
            completedAt = Instant.now()
        )
        assertEquals("5m", execution2.getFormattedDuration())
        
        // Test hours and minutes
        val execution3 = createTestAgentExecutionDto(
            startedAt = Instant.now().minusSeconds(3900), // 65 minutes = 1h 5m
            completedAt = Instant.now()
        )
        assertEquals("1h 5m", execution3.getFormattedDuration())
    }
    
    @Test
    fun `AgentExecutionDto status check methods should work correctly`() {
        val completedExecution = createTestAgentExecutionDto(status = ExecutionStatus.COMPLETED)
        assertTrue(completedExecution.isCompleted())
        assertFalse(completedExecution.isRunning())
        assertFalse(completedExecution.isFailed())
        
        val runningExecution = createTestAgentExecutionDto(status = ExecutionStatus.RUNNING)
        assertFalse(runningExecution.isCompleted())
        assertTrue(runningExecution.isRunning())
        assertFalse(runningExecution.isFailed())
        
        val failedExecution = createTestAgentExecutionDto(status = ExecutionStatus.FAILED)
        assertFalse(failedExecution.isCompleted())
        assertFalse(failedExecution.isRunning())
        assertTrue(failedExecution.isFailed())
    }
    
    private fun createTestLearningSessionDto(
        id: UUID = UUID.randomUUID(),
        userId: UUID = UUID.randomUUID(),
        topic: String = "Test Topic",
        status: SessionStatus = SessionStatus.PENDING,
        estimatedDurationMinutes: Int? = null,
        actualDurationMinutes: Int? = null,
        qualityScore: BigDecimal? = null,
        costUsd: BigDecimal? = null,
        createdAt: Instant = Instant.now(),
        completedAt: Instant? = null,
        agentExecutions: List<AgentExecutionDto> = emptyList(),
        progressPercentage: Int = 0,
        currentStage: Int = 0
    ): LearningSessionDto {
        return LearningSessionDto(
            id = id,
            userId = userId,
            topic = topic,
            status = status,
            estimatedDurationMinutes = estimatedDurationMinutes,
            actualDurationMinutes = actualDurationMinutes,
            qualityScore = qualityScore,
            costUsd = costUsd,
            createdAt = createdAt,
            completedAt = completedAt,
            agentExecutions = agentExecutions,
            progressPercentage = progressPercentage,
            currentStage = currentStage
        )
    }
    
    private fun createTestAgentExecutionDto(
        id: UUID = UUID.randomUUID(),
        sessionId: UUID = UUID.randomUUID(),
        agentType: AgentType = AgentType.RESEARCH,
        stageNumber: Int = 1,
        status: ExecutionStatus = ExecutionStatus.PENDING,
        inputData: Map<String, Any>? = null,
        outputData: Map<String, Any>? = null,
        errorMessage: String? = null,
        llmProvider: String? = null,
        llmModel: String? = null,
        tokensUsed: Int? = null,
        costUsd: BigDecimal? = null,
        startedAt: Instant? = null,
        completedAt: Instant? = null,
        createdAt: Instant = Instant.now()
    ): AgentExecutionDto {
        return AgentExecutionDto(
            id = id,
            sessionId = sessionId,
            agentType = agentType,
            stageNumber = stageNumber,
            status = status,
            inputData = inputData,
            outputData = outputData,
            errorMessage = errorMessage,
            llmProvider = llmProvider,
            llmModel = llmModel,
            tokensUsed = tokensUsed,
            costUsd = costUsd,
            startedAt = startedAt,
            completedAt = completedAt,
            createdAt = createdAt
        )
    }
}