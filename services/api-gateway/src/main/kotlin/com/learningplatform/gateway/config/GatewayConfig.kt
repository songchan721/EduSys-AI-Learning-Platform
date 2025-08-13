package com.learningplatform.gateway.config

import com.learningplatform.gateway.filter.AuthenticationFilter
import com.learningplatform.gateway.filter.CircuitBreakerFilter
import com.learningplatform.gateway.filter.LoggingFilter
import com.learningplatform.gateway.filter.RateLimitingFilter
import com.learningplatform.gateway.filter.ValidationFilter
import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsWebFilter
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource

/**
 * Gateway Configuration
 * 
 * Configures routing rules, filters, and cross-cutting concerns for the API Gateway.
 * Routes requests to appropriate microservices based on URL patterns.
 */
@Configuration
class GatewayConfig(
    private val authenticationFilter: AuthenticationFilter,
    private val loggingFilter: LoggingFilter,
    private val rateLimitingFilter: RateLimitingFilter,
    private val validationFilter: ValidationFilter,
    private val circuitBreakerFilter: CircuitBreakerFilter
) {

    /**
     * Configure routes to all microservices
     */
    @Bean
    fun customRouteLocator(builder: RouteLocatorBuilder): RouteLocator {
        return builder.routes()
            // User Service Routes
            .route("user-service") { r ->
                r.path("/api/v1/auth/**", "/api/v1/users/**")
                    .filters { f ->
                        f.filter(validationFilter.apply(ValidationFilter.Config()))
                        f.filter(loggingFilter.apply(LoggingFilter.Config()))
                        f.filter(rateLimitingFilter.apply(RateLimitingFilter.Config()))
                        f.filter(circuitBreakerFilter.apply(CircuitBreakerFilter.Config()))
                        // Auth endpoints don't need authentication
                        if (!r.toString().contains("/auth/login") && !r.toString().contains("/auth/register")) {
                            f.filter(authenticationFilter.apply(AuthenticationFilter.Config()))
                        }
                        f.rewritePath("/api/v1/(?<segment>.*)", "/\${segment}")
                    }
                    .uri("lb://user-service")
            }
            
            // Agent Orchestrator Routes
            .route("agent-orchestrator") { r ->
                r.path("/api/v1/agents/**", "/api/v1/sessions/**")
                    .filters { f ->
                        f.filter(validationFilter.apply(ValidationFilter.Config()))
                        f.filter(loggingFilter.apply(LoggingFilter.Config()))
                        f.filter(rateLimitingFilter.apply(RateLimitingFilter.Config()))
                        f.filter(circuitBreakerFilter.apply(CircuitBreakerFilter.Config()))
                        f.filter(authenticationFilter.apply(AuthenticationFilter.Config()))
                        f.rewritePath("/api/v1/(?<segment>.*)", "/\${segment}")
                    }
                    .uri("lb://agent-orchestrator")
            }
            
            // Content Service Routes
            .route("content-service") { r ->
                r.path("/api/v1/content/**")
                    .filters { f ->
                        f.filter(validationFilter.apply(ValidationFilter.Config()))
                        f.filter(loggingFilter.apply(LoggingFilter.Config()))
                        f.filter(rateLimitingFilter.apply(RateLimitingFilter.Config()))
                        f.filter(circuitBreakerFilter.apply(CircuitBreakerFilter.Config()))
                        f.filter(authenticationFilter.apply(AuthenticationFilter.Config()))
                        f.rewritePath("/api/v1/(?<segment>.*)", "/\${segment}")
                    }
                    .uri("lb://content-service")
            }
            
            // Payment Service Routes
            .route("payment-service") { r ->
                r.path("/api/v1/payments/**", "/api/v1/subscriptions/**")
                    .filters { f ->
                        f.filter(validationFilter.apply(ValidationFilter.Config()))
                        f.filter(loggingFilter.apply(LoggingFilter.Config()))
                        f.filter(rateLimitingFilter.apply(RateLimitingFilter.Config()))
                        f.filter(circuitBreakerFilter.apply(CircuitBreakerFilter.Config()))
                        f.filter(authenticationFilter.apply(AuthenticationFilter.Config()))
                        f.rewritePath("/api/v1/(?<segment>.*)", "/\${segment}")
                    }
                    .uri("lb://payment-service")
            }
            
            // WebSocket Routes (for real-time messaging)
            .route("websocket") { r ->
                r.path("/ws/**")
                    .filters { f ->
                        f.filter(loggingFilter.apply(LoggingFilter.Config()))
                        // WebSocket connections need special handling
                        f.rewritePath("/ws/(?<segment>.*)", "/ws/\${segment}")
                    }
                    .uri("lb://messaging-service")
            }
            
            // Health Check Routes
            .route("health-check") { r ->
                r.path("/actuator/**")
                    .filters { f ->
                        f.filter(loggingFilter.apply(LoggingFilter.Config()))
                    }
                    .uri("http://localhost:8080")
            }
            
            .build()
    }

    /**
     * Configure CORS for cross-origin requests
     */
    @Bean
    fun corsWebFilter(): CorsWebFilter {
        val corsConfig = CorsConfiguration().apply {
            allowedOriginPatterns = listOf("*")
            maxAge = 3600L
            allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
            allowedHeaders = listOf("*")
            allowCredentials = true
            exposedHeaders = listOf(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "X-Correlation-ID",
                "X-Request-ID"
            )
        }

        val source = UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", corsConfig)
        }

        return CorsWebFilter(source)
    }
}