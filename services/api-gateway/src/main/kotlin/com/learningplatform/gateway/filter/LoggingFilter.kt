package com.learningplatform.gateway.filter

import com.learningplatform.utils.correlation.CorrelationIdService
import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.time.Instant

/**
 * Logging Filter
 * 
 * Logs all requests and responses passing through the gateway.
 * Adds correlation IDs for request tracing across services.
 */
@Component
class LoggingFilter(
    private val correlationIdService: CorrelationIdService
) : AbstractGatewayFilterFactory<LoggingFilter.Config>() {

    data class Config(
        val logRequestBody: Boolean = false,
        val logResponseBody: Boolean = false,
        val excludePaths: List<String> = listOf("/health", "/actuator")
    )

    override fun apply(config: Config): GatewayFilter {
        return GatewayFilter { exchange, chain ->
            val startTime = Instant.now()
            val request = exchange.request
            val path = request.path.value()

            // Skip logging for excluded paths
            if (config.excludePaths.any { path.contains(it) }) {
                return@GatewayFilter chain.filter(exchange)
            }

            // Generate or extract correlation ID
            val correlationId = request.headers.getFirst("X-Correlation-ID") 
                ?: correlationIdService.generateAndSetCorrelationId()

            // Add correlation ID to request
            val modifiedRequest = request.mutate()
                .header("X-Correlation-ID", correlationId)
                .header("X-Request-ID", java.util.UUID.randomUUID().toString())
                .build()

            val modifiedExchange = exchange.mutate()
                .request(modifiedRequest)
                .build()

            // Log request
            logRequest(modifiedRequest, correlationId)

            // Process request and log response
            chain.filter(modifiedExchange)
                .doOnSuccess {
                    val duration = java.time.Duration.between(startTime, Instant.now())
                    logResponse(modifiedExchange, correlationId, duration)
                }
                .doOnError { error ->
                    val duration = java.time.Duration.between(startTime, Instant.now())
                    logError(modifiedExchange, correlationId, duration, error)
                }
        }
    }

    private fun logRequest(request: org.springframework.http.server.reactive.ServerHttpRequest, correlationId: String) {
        println("=== GATEWAY REQUEST ===")
        println("Correlation ID: $correlationId")
        println("Method: ${request.method}")
        println("Path: ${request.path.value()}")
        println("Query: ${request.queryParams}")
        println("Headers: ${request.headers.toSingleValueMap()}")
        println("Remote Address: ${request.remoteAddress}")
        println("Timestamp: ${Instant.now()}")
        println("========================")
    }

    private fun logResponse(exchange: ServerWebExchange, correlationId: String, duration: java.time.Duration) {
        val response = exchange.response
        println("=== GATEWAY RESPONSE ===")
        println("Correlation ID: $correlationId")
        println("Status: ${response.statusCode}")
        println("Headers: ${response.headers.toSingleValueMap()}")
        println("Duration: ${duration.toMillis()}ms")
        println("Timestamp: ${Instant.now()}")
        println("=========================")
    }

    private fun logError(exchange: ServerWebExchange, correlationId: String, duration: java.time.Duration, error: Throwable) {
        println("=== GATEWAY ERROR ===")
        println("Correlation ID: $correlationId")
        println("Path: ${exchange.request.path.value()}")
        println("Error: ${error.message}")
        println("Duration: ${duration.toMillis()}ms")
        println("Timestamp: ${Instant.now()}")
        println("======================")
    }

    override fun getConfigClass(): Class<Config> = Config::class.java
}