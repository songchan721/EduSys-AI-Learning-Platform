package com.learningplatform.messaging.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

/**
 * WebSocket Configuration
 * Configures WebSocket endpoints and message broker for real-time communication
 */
@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig : WebSocketMessageBrokerConfigurer {

    @Value("\${websocket.allowed-origins:http://localhost:3000,http://localhost:5173}")
    private lateinit var allowedOrigins: String

    override fun configureMessageBroker(config: MessageBrokerRegistry) {
        // Enable simple broker for topics
        config.enableSimpleBroker("/topic", "/queue")
        
        // Set application destination prefix
        config.setApplicationDestinationPrefixes("/app")
        
        // Set user destination prefix
        config.setUserDestinationPrefix("/user")
    }

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        // Register WebSocket endpoint
        registry.addEndpoint("/ws")
            .setAllowedOrigins(*allowedOrigins.split(",").toTypedArray())
            .withSockJS()
    }
}