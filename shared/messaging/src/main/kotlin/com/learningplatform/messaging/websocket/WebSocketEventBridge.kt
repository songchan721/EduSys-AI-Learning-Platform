package com.learningplatform.messaging.websocket

import com.learningplatform.messaging.events.BaseEvent
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service

/**
 * WebSocket Event Bridge
 * Bridges Kafka events to WebSocket connections for real-time frontend updates
 * Enables frontend to receive real-time notifications about backend events
 */
@Service
class WebSocketEventBridge(
    private val messagingTemplate: SimpMessagingTemplate
) {

    /**
     * Listen to agent events and forward to WebSocket subscribers
     * Enables real-time agent progress updates in the frontend
     */
    @KafkaListener(
        topics = ["agent-events"],
        groupId = "websocket-bridge-agent",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun handleAgentEventForWebSocket(event: BaseEvent) {
        try {
            when (event.eventType) {
                "agent.started.v1" -> {
                    val message = createWebSocketMessage(
                        type = "agent.progress",
                        sessionId = extractSessionId(event),
                        data = mapOf(
                            "stage" to extractAgentStage(event),
                            "status" to "started",
                            "message" to "Agent execution started",
                            "timestamp" to event.timestamp
                        )
                    )
                    sendToUserSession(event.userId.toString(), extractSessionId(event), message)
                }
                
                "agent.completed.v1" -> {
                    val message = createWebSocketMessage(
                        type = "agent.progress",
                        sessionId = extractSessionId(event),
                        data = mapOf(
                            "stage" to extractAgentStage(event),
                            "status" to "completed",
                            "message" to "Agent execution completed",
                            "timestamp" to event.timestamp,
                            "duration" to extractDuration(event),
                            "cost" to extractCost(event)
                        )
                    )
                    sendToUserSession(event.userId.toString(), extractSessionId(event), message)
                }
                
                "agent.failed.v1" -> {
                    val message = createWebSocketMessage(
                        type = "agent.error",
                        sessionId = extractSessionId(event),
                        data = mapOf(
                            "stage" to extractAgentStage(event),
                            "status" to "failed",
                            "message" to extractErrorMessage(event),
                            "timestamp" to event.timestamp
                        )
                    )
                    sendToUserSession(event.userId.toString(), extractSessionId(event), message)
                }
            }
        } catch (exception: Exception) {
            println("Error forwarding agent event to WebSocket: ${exception.message}")
        }
    }

    /**
     * Listen to session events and forward to WebSocket subscribers
     * Enables real-time session status updates in the frontend
     */
    @KafkaListener(
        topics = ["session-events"],
        groupId = "websocket-bridge-session",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun handleSessionEventForWebSocket(event: BaseEvent) {
        try {
            when (event.eventType) {
                "session.started.v1" -> {
                    val message = createWebSocketMessage(
                        type = "session.started",
                        sessionId = extractSessionId(event),
                        data = mapOf(
                            "topic" to extractTopic(event),
                            "estimatedDuration" to extractEstimatedDuration(event),
                            "timestamp" to event.timestamp
                        )
                    )
                    sendToUserSession(event.userId.toString(), extractSessionId(event), message)
                }
                
                "session.completed.v1" -> {
                    val message = createWebSocketMessage(
                        type = "session.completed",
                        sessionId = extractSessionId(event),
                        data = mapOf(
                            "actualDuration" to extractActualDuration(event),
                            "qualityScore" to extractQualityScore(event),
                            "totalCost" to extractTotalCost(event),
                            "timestamp" to event.timestamp
                        )
                    )
                    sendToUserSession(event.userId.toString(), extractSessionId(event), message)
                }
                
                "session.failed.v1" -> {
                    val message = createWebSocketMessage(
                        type = "session.error",
                        sessionId = extractSessionId(event),
                        data = mapOf(
                            "errorMessage" to extractErrorMessage(event),
                            "failedAtStage" to extractFailedStage(event),
                            "timestamp" to event.timestamp
                        )
                    )
                    sendToUserSession(event.userId.toString(), extractSessionId(event), message)
                }
            }
        } catch (exception: Exception) {
            println("Error forwarding session event to WebSocket: ${exception.message}")
        }
    }

    /**
     * Listen to content events and forward to WebSocket subscribers
     * Enables real-time content generation updates in the frontend
     */
    @KafkaListener(
        topics = ["content-events"],
        groupId = "websocket-bridge-content",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun handleContentEventForWebSocket(event: BaseEvent) {
        try {
            when (event.eventType) {
                "content.generated.v1" -> {
                    val message = createWebSocketMessage(
                        type = "content.generated",
                        sessionId = extractSessionId(event),
                        data = mapOf(
                            "contentId" to extractContentId(event),
                            "contentType" to extractContentType(event),
                            "title" to extractTitle(event),
                            "wordCount" to extractWordCount(event),
                            "qualityScore" to extractQualityScore(event),
                            "timestamp" to event.timestamp
                        )
                    )
                    sendToUserSession(event.userId.toString(), extractSessionId(event), message)
                }
                
                "content.updated.v1" -> {
                    val message = createWebSocketMessage(
                        type = "content.updated",
                        sessionId = extractSessionId(event),
                        data = mapOf(
                            "contentId" to extractContentId(event),
                            "newVersion" to extractNewVersion(event),
                            "changes" to extractChanges(event),
                            "timestamp" to event.timestamp
                        )
                    )
                    sendToUserSession(event.userId.toString(), extractSessionId(event), message)
                }
            }
        } catch (exception: Exception) {
            println("Error forwarding content event to WebSocket: ${exception.message}")
        }
    }

    /**
     * Listen to system events and forward to all connected users
     * Enables real-time system notifications in the frontend
     */
    @KafkaListener(
        topics = ["system-events"],
        groupId = "websocket-bridge-system",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun handleSystemEventForWebSocket(event: BaseEvent) {
        try {
            when (event.eventType) {
                "system.maintenance-started.v1" -> {
                    val message = createWebSocketMessage(
                        type = "system.notification",
                        sessionId = null,
                        data = mapOf(
                            "notificationType" to "maintenance",
                            "maintenanceType" to extractMaintenanceType(event),
                            "estimatedDuration" to extractEstimatedDuration(event),
                            "affectedServices" to extractAffectedServices(event),
                            "timestamp" to event.timestamp
                        )
                    )
                    sendToAllUsers(message)
                }
                
                "system.alert-triggered.v1" -> {
                    val message = createWebSocketMessage(
                        type = "system.alert",
                        sessionId = null,
                        data = mapOf(
                            "alertType" to extractAlertType(event),
                            "severity" to extractSeverity(event),
                            "message" to extractMessage(event),
                            "affectedServices" to extractAffectedServices(event),
                            "timestamp" to event.timestamp
                        )
                    )
                    sendToAllUsers(message)
                }
            }
        } catch (exception: Exception) {
            println("Error forwarding system event to WebSocket: ${exception.message}")
        }
    }

    /**
     * Create standardized WebSocket message format
     */
    private fun createWebSocketMessage(
        type: String,
        sessionId: String?,
        data: Map<String, Any?>
    ): Map<String, Any?> {
        return mapOf(
            "type" to type,
            "sessionId" to sessionId,
            "timestamp" to System.currentTimeMillis(),
            "data" to data
        )
    }

    /**
     * Send message to specific user session
     */
    private fun sendToUserSession(userId: String, sessionId: String, message: Map<String, Any?>) {
        val destination = "/topic/user/$userId/session/$sessionId"
        messagingTemplate.convertAndSend(destination, message)
        println("Sent WebSocket message to $destination: ${message["type"]}")
    }

    /**
     * Send message to all connected users
     */
    private fun sendToAllUsers(message: Map<String, Any?>) {
        val destination = "/topic/system"
        messagingTemplate.convertAndSend(destination, message)
        println("Sent WebSocket message to all users: ${message["type"]}")
    }

    // Event field extraction methods
    private fun extractSessionId(event: BaseEvent): String {
        return when (event) {
            is com.learningplatform.messaging.events.SessionStartedEvent -> event.sessionId.toString()
            is com.learningplatform.messaging.events.SessionCompletedEvent -> event.sessionId.toString()
            is com.learningplatform.messaging.events.SessionFailedEvent -> event.sessionId.toString()
            is com.learningplatform.messaging.events.AgentStartedEvent -> event.sessionId.toString()
            is com.learningplatform.messaging.events.AgentCompletedEvent -> event.sessionId.toString()
            is com.learningplatform.messaging.events.AgentFailedEvent -> event.sessionId.toString()
            is com.learningplatform.messaging.events.ContentGeneratedEvent -> event.sessionId.toString()
            else -> "unknown"
        }
    }

    private fun extractAgentStage(event: BaseEvent): Int {
        return when (event) {
            is com.learningplatform.messaging.events.AgentStartedEvent -> event.stageNumber
            is com.learningplatform.messaging.events.AgentCompletedEvent -> event.stageNumber
            is com.learningplatform.messaging.events.AgentFailedEvent -> event.stageNumber
            else -> 0
        }
    }

    private fun extractDuration(event: BaseEvent): Long? {
        return when (event) {
            is com.learningplatform.messaging.events.AgentCompletedEvent -> event.durationMinutes
            else -> null
        }
    }

    private fun extractCost(event: BaseEvent): Double? {
        return when (event) {
            is com.learningplatform.messaging.events.AgentCompletedEvent -> event.costUsd
            else -> null
        }
    }

    private fun extractErrorMessage(event: BaseEvent): String {
        return when (event) {
            is com.learningplatform.messaging.events.AgentFailedEvent -> event.errorMessage
            is com.learningplatform.messaging.events.SessionFailedEvent -> event.errorMessage
            else -> "Unknown error"
        }
    }

    private fun extractTopic(event: BaseEvent): String {
        return when (event) {
            is com.learningplatform.messaging.events.SessionStartedEvent -> event.topic
            else -> "Unknown topic"
        }
    }

    private fun extractEstimatedDuration(event: BaseEvent): Int? {
        return when (event) {
            is com.learningplatform.messaging.events.SessionStartedEvent -> event.estimatedDurationMinutes
            is com.learningplatform.messaging.events.SystemMaintenanceStartedEvent -> event.estimatedDurationMinutes
            else -> null
        }
    }

    private fun extractActualDuration(event: BaseEvent): Int? {
        return when (event) {
            is com.learningplatform.messaging.events.SessionCompletedEvent -> event.actualDurationMinutes
            else -> null
        }
    }

    private fun extractQualityScore(event: BaseEvent): Double? {
        return when (event) {
            is com.learningplatform.messaging.events.SessionCompletedEvent -> event.qualityScore
            is com.learningplatform.messaging.events.ContentGeneratedEvent -> event.qualityScore
            else -> null
        }
    }

    private fun extractTotalCost(event: BaseEvent): Double? {
        return when (event) {
            is com.learningplatform.messaging.events.SessionCompletedEvent -> event.totalCostUsd
            else -> null
        }
    }

    private fun extractFailedStage(event: BaseEvent): String {
        return when (event) {
            is com.learningplatform.messaging.events.SessionFailedEvent -> event.failedAtStage
            else -> "Unknown stage"
        }
    }

    private fun extractContentId(event: BaseEvent): String {
        return when (event) {
            is com.learningplatform.messaging.events.ContentGeneratedEvent -> event.contentId.toString()
            is com.learningplatform.messaging.events.ContentUpdatedEvent -> event.contentId.toString()
            else -> "unknown"
        }
    }

    private fun extractContentType(event: BaseEvent): String {
        return when (event) {
            is com.learningplatform.messaging.events.ContentGeneratedEvent -> event.contentType
            else -> "unknown"
        }
    }

    private fun extractTitle(event: BaseEvent): String {
        return when (event) {
            is com.learningplatform.messaging.events.ContentGeneratedEvent -> event.title
            else -> "Unknown title"
        }
    }

    private fun extractWordCount(event: BaseEvent): Int? {
        return when (event) {
            is com.learningplatform.messaging.events.ContentGeneratedEvent -> event.wordCount
            else -> null
        }
    }

    private fun extractNewVersion(event: BaseEvent): Int? {
        return when (event) {
            is com.learningplatform.messaging.events.ContentUpdatedEvent -> event.newVersion
            else -> null
        }
    }

    private fun extractChanges(event: BaseEvent): String? {
        return when (event) {
            is com.learningplatform.messaging.events.ContentUpdatedEvent -> event.changes
            else -> null
        }
    }

    private fun extractMaintenanceType(event: BaseEvent): String {
        return when (event) {
            is com.learningplatform.messaging.events.SystemMaintenanceStartedEvent -> event.maintenanceType
            else -> "Unknown maintenance"
        }
    }

    private fun extractAffectedServices(event: BaseEvent): List<String> {
        return when (event) {
            is com.learningplatform.messaging.events.SystemMaintenanceStartedEvent -> event.affectedServices
            is com.learningplatform.messaging.events.SystemAlertTriggeredEvent -> event.affectedServices
            else -> emptyList()
        }
    }

    private fun extractAlertType(event: BaseEvent): String {
        return when (event) {
            is com.learningplatform.messaging.events.SystemAlertTriggeredEvent -> event.alertType
            else -> "Unknown alert"
        }
    }

    private fun extractSeverity(event: BaseEvent): String {
        return when (event) {
            is com.learningplatform.messaging.events.SystemAlertTriggeredEvent -> event.severity
            else -> "Unknown severity"
        }
    }

    private fun extractMessage(event: BaseEvent): String {
        return when (event) {
            is com.learningplatform.messaging.events.SystemAlertTriggeredEvent -> event.message
            else -> "Unknown message"
        }
    }
}