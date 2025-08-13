package com.learningplatform.messaging.websocket

import com.learningplatform.messaging.events.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.messaging.simp.SimpMessagingTemplate
import java.time.Instant
import java.util.*

@ExtendWith(MockitoExtension::class)
class WebSocketEventBridgeTest {

    @Mock
    private lateinit var messagingTemplate: SimpMessagingTemplate

    private lateinit var webSocketEventBridge: WebSocketEventBridge

    @BeforeEach
    fun setUp() {
        webSocketEventBridge = WebSocketEventBridge(messagingTemplate)
    }

    @Test
    fun `should forward agent started event to WebSocket`() {
        // Given
        val sessionId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val event = AgentStartedEvent(
            sessionId = sessionId,
            userId = userId,
            agentType = "ResearchAgent",
            stageNumber = 1,
            executionId = UUID.randomUUID()
        )

        // When
        webSocketEventBridge.handleAgentEventForWebSocket(event)

        // Then
        val expectedDestination = "/topic/user/$userId/session/$sessionId"
        verify(messagingTemplate).convertAndSend(eq(expectedDestination), any<Map<String, Any?>>())
    }

    @Test
    fun `should forward agent completed event to WebSocket`() {
        // Given
        val sessionId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val event = AgentCompletedEvent(
            sessionId = sessionId,
            userId = userId,
            agentType = "ContentGenerator",
            stageNumber = 2,
            durationMinutes = 15L,
            executionId = UUID.randomUUID(),
            tokensUsed = 1500,
            costUsd = 3.75
        )

        // When
        webSocketEventBridge.handleAgentEventForWebSocket(event)

        // Then
        val expectedDestination = "/topic/user/$userId/session/$sessionId"
        verify(messagingTemplate).convertAndSend(eq(expectedDestination), any<Map<String, Any?>>())
    }

    @Test
    fun `should forward session started event to WebSocket`() {
        // Given
        val sessionId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val event = SessionStartedEvent(
            sessionId = sessionId,
            userId = userId,
            topic = "Machine Learning Fundamentals",
            estimatedDurationMinutes = 120
        )

        // When
        webSocketEventBridge.handleSessionEventForWebSocket(event)

        // Then
        val expectedDestination = "/topic/user/$userId/session/$sessionId"
        verify(messagingTemplate).convertAndSend(eq(expectedDestination), any<Map<String, Any?>>())
    }

    @Test
    fun `should forward session completed event to WebSocket`() {
        // Given
        val sessionId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val event = SessionCompletedEvent(
            sessionId = sessionId,
            userId = userId,
            actualDurationMinutes = 135,
            qualityScore = 8.5,
            totalCostUsd = 12.50
        )

        // When
        webSocketEventBridge.handleSessionEventForWebSocket(event)

        // Then
        val expectedDestination = "/topic/user/$userId/session/$sessionId"
        verify(messagingTemplate).convertAndSend(eq(expectedDestination), any<Map<String, Any?>>())
    }

    @Test
    fun `should forward content generated event to WebSocket`() {
        // Given
        val sessionId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val contentId = UUID.randomUUID()
        val event = ContentGeneratedEvent(
            contentId = contentId,
            sessionId = sessionId,
            userId = userId,
            contentType = "lesson",
            title = "Introduction to Neural Networks",
            wordCount = 2500,
            qualityScore = 9.2
        )

        // When
        webSocketEventBridge.handleContentEventForWebSocket(event)

        // Then
        val expectedDestination = "/topic/user/$userId/session/$sessionId"
        verify(messagingTemplate).convertAndSend(eq(expectedDestination), any<Map<String, Any?>>())
    }

    @Test
    fun `should forward system maintenance event to all users`() {
        // Given
        val event = SystemMaintenanceStartedEvent(
            maintenanceType = "scheduled",
            estimatedDurationMinutes = 30,
            affectedServices = listOf("agent-orchestrator", "content-service")
        )

        // When
        webSocketEventBridge.handleSystemEventForWebSocket(event)

        // Then
        val expectedDestination = "/topic/system"
        verify(messagingTemplate).convertAndSend(eq(expectedDestination), any<Map<String, Any?>>())
    }

    @Test
    fun `should forward system alert event to all users`() {
        // Given
        val event = SystemAlertTriggeredEvent(
            alertType = "high_load",
            severity = "warning",
            message = "System experiencing high load",
            affectedServices = listOf("user-service")
        )

        // When
        webSocketEventBridge.handleSystemEventForWebSocket(event)

        // Then
        val expectedDestination = "/topic/system"
        verify(messagingTemplate).convertAndSend(eq(expectedDestination), any<Map<String, Any?>>())
    }

    @Test
    fun `should handle agent failed event and send error message`() {
        // Given
        val sessionId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val event = AgentFailedEvent(
            sessionId = sessionId,
            userId = userId,
            agentType = "ResearchAgent",
            stageNumber = 1,
            executionId = UUID.randomUUID(),
            errorMessage = "API rate limit exceeded"
        )

        // When
        webSocketEventBridge.handleAgentEventForWebSocket(event)

        // Then
        val expectedDestination = "/topic/user/$userId/session/$sessionId"
        verify(messagingTemplate).convertAndSend(eq(expectedDestination), any<Map<String, Any?>>())
    }

    @Test
    fun `should handle session failed event and send error message`() {
        // Given
        val sessionId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val event = SessionFailedEvent(
            sessionId = sessionId,
            userId = userId,
            errorMessage = "Insufficient credits",
            failedAtStage = "content-generation"
        )

        // When
        webSocketEventBridge.handleSessionEventForWebSocket(event)

        // Then
        val expectedDestination = "/topic/user/$userId/session/$sessionId"
        verify(messagingTemplate).convertAndSend(eq(expectedDestination), any<Map<String, Any?>>())
    }

    @Test
    fun `should handle content updated event`() {
        // Given
        val userId = UUID.randomUUID()
        val contentId = UUID.randomUUID()
        val event = ContentUpdatedEvent(
            contentId = contentId,
            userId = userId,
            newVersion = 2,
            changes = "Added interactive examples"
        )

        // When
        webSocketEventBridge.handleContentEventForWebSocket(event)

        // Then - ContentUpdatedEvent doesn't have sessionId, but it still sends a message with "unknown" sessionId
        val expectedDestination = "/topic/user/$userId/session/unknown"
        verify(messagingTemplate).convertAndSend(eq(expectedDestination), any<Map<String, Any?>>())
    }
}