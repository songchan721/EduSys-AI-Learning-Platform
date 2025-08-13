package com.learningplatform.gateway.filter

import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Circuit Breaker Filter
 * 
 * Implements circuit breaker pattern to prevent cascading failures.
 * Monitors service health and fails fast when services are unavailable.
 */
@Component
class CircuitBreakerFilter : AbstractGatewayFilterFactory<CircuitBreakerFilter.Config>() {

    data class Config(
        val failureThreshold: Int = 5,
        val recoveryTimeoutSeconds: Long = 30,
        val monitoringWindowSeconds: Long = 60
    )

    private val circuitStates = ConcurrentHashMap<String, CircuitState>()

    data class CircuitState(
        var state: State = State.CLOSED,
        val failureCount: AtomicInteger = AtomicInteger(0),
        var lastFailureTime: Instant? = null,
        var lastSuccessTime: Instant? = null
    )

    enum class State {
        CLOSED,    // Normal operation
        OPEN,      // Circuit is open, failing fast
        HALF_OPEN  // Testing if service is back
    }

    override fun apply(config: Config): GatewayFilter {
        return GatewayFilter { exchange, chain ->
            val serviceName = extractServiceName(exchange)
            val circuitState = circuitStates.computeIfAbsent(serviceName) { CircuitState() }

            when (circuitState.state) {
                State.OPEN -> {
                    if (shouldAttemptReset(circuitState, config)) {
                        circuitState.state = State.HALF_OPEN
                        attemptRequest(exchange, chain, serviceName, circuitState, config)
                    } else {
                        handleCircuitOpen(exchange, serviceName)
                    }
                }
                State.HALF_OPEN -> {
                    attemptRequest(exchange, chain, serviceName, circuitState, config)
                }
                State.CLOSED -> {
                    attemptRequest(exchange, chain, serviceName, circuitState, config)
                }
            }
        }
    }

    private fun extractServiceName(exchange: ServerWebExchange): String {
        val path = exchange.request.path.value()
        return when {
            path.startsWith("/api/v1/auth") || path.startsWith("/api/v1/users") -> "user-service"
            path.startsWith("/api/v1/agents") || path.startsWith("/api/v1/sessions") -> "agent-orchestrator"
            path.startsWith("/api/v1/content") -> "content-service"
            path.startsWith("/api/v1/payments") -> "payment-service"
            else -> "unknown-service"
        }
    }

    private fun shouldAttemptReset(circuitState: CircuitState, config: Config): Boolean {
        val lastFailure = circuitState.lastFailureTime ?: return true
        val recoveryTimeout = Duration.ofSeconds(config.recoveryTimeoutSeconds)
        return Instant.now().isAfter(lastFailure.plus(recoveryTimeout))
    }

    private fun attemptRequest(
        exchange: ServerWebExchange,
        chain: org.springframework.cloud.gateway.filter.GatewayFilterChain,
        serviceName: String,
        circuitState: CircuitState,
        config: Config
    ): Mono<Void> {
        return chain.filter(exchange)
            .doOnSuccess {
                handleSuccess(circuitState)
            }
            .doOnError { error ->
                handleFailure(circuitState, config, error)
            }
            .onErrorResume { error ->
                if (circuitState.state == State.OPEN) {
                    handleCircuitOpen(exchange, serviceName)
                } else {
                    handleServiceError(exchange, serviceName, error)
                }
            }
    }

    private fun handleSuccess(circuitState: CircuitState) {
        circuitState.lastSuccessTime = Instant.now()
        circuitState.failureCount.set(0)
        circuitState.state = State.CLOSED
        println("Circuit breaker: Service recovered, circuit closed")
    }

    private fun handleFailure(circuitState: CircuitState, config: Config, error: Throwable) {
        circuitState.lastFailureTime = Instant.now()
        val failures = circuitState.failureCount.incrementAndGet()
        
        if (failures >= config.failureThreshold) {
            circuitState.state = State.OPEN
            println("Circuit breaker: Failure threshold reached ($failures), circuit opened")
        }
        
        println("Circuit breaker: Service failure recorded ($failures/${config.failureThreshold}): ${error.message}")
    }

    private fun handleCircuitOpen(exchange: ServerWebExchange, serviceName: String): Mono<Void> {
        val response = exchange.response
        response.statusCode = HttpStatus.SERVICE_UNAVAILABLE
        response.headers.add("Content-Type", "application/json")

        val errorBody = """
            {
                "error": "Service Unavailable",
                "message": "Circuit breaker is open for $serviceName. Service is temporarily unavailable.",
                "timestamp": "${Instant.now()}",
                "path": "${exchange.request.path.value()}",
                "service": "$serviceName"
            }
        """.trimIndent()

        val buffer = response.bufferFactory().wrap(errorBody.toByteArray())
        return response.writeWith(Mono.just(buffer))
    }

    private fun handleServiceError(exchange: ServerWebExchange, serviceName: String, error: Throwable): Mono<Void> {
        val response = exchange.response
        response.statusCode = HttpStatus.BAD_GATEWAY
        response.headers.add("Content-Type", "application/json")

        val errorBody = """
            {
                "error": "Service Error",
                "message": "Error communicating with $serviceName: ${error.message}",
                "timestamp": "${Instant.now()}",
                "path": "${exchange.request.path.value()}",
                "service": "$serviceName"
            }
        """.trimIndent()

        val buffer = response.bufferFactory().wrap(errorBody.toByteArray())
        return response.writeWith(Mono.just(buffer))
    }

    override fun getConfigClass(): Class<Config> = Config::class.java
}