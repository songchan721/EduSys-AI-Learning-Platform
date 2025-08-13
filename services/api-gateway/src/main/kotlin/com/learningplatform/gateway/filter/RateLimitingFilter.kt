package com.learningplatform.gateway.filter

import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.Instant

/**
 * Rate Limiting Filter
 * 
 * Implements rate limiting using Redis as the backend store.
 * Supports different rate limits based on user roles and endpoints.
 */
@Component
class RateLimitingFilter(
    private val redisTemplate: ReactiveRedisTemplate<String, String>
) : AbstractGatewayFilterFactory<RateLimitingFilter.Config>() {

    data class Config(
        val requestsPerMinute: Int = 60,
        val burstCapacity: Int = 100,
        val keyResolver: String = "user" // user, ip, or endpoint
    )

    override fun apply(config: Config): GatewayFilter {
        return GatewayFilter { exchange, chain ->
            val key = resolveKey(exchange, config.keyResolver)
            val now = Instant.now()
            val windowStart = now.minusSeconds(60) // 1-minute window

            checkRateLimit(key, windowStart, now, config)
                .flatMap { allowed ->
                    if (allowed) {
                        // Add rate limit headers
                        addRateLimitHeaders(exchange, key, config)
                            .then(chain.filter(exchange))
                    } else {
                        handleRateLimitExceeded(exchange)
                    }
                }
        }
    }

    private fun resolveKey(exchange: ServerWebExchange, keyResolver: String): String {
        return when (keyResolver) {
            "user" -> {
                val userId = exchange.request.headers.getFirst("X-User-ID")
                if (userId != null) "rate_limit:user:$userId" else "rate_limit:anonymous:${getClientIP(exchange)}"
            }
            "ip" -> "rate_limit:ip:${getClientIP(exchange)}"
            "endpoint" -> "rate_limit:endpoint:${exchange.request.path.value()}"
            else -> "rate_limit:default:${getClientIP(exchange)}"
        }
    }

    private fun getClientIP(exchange: ServerWebExchange): String {
        val xForwardedFor = exchange.request.headers.getFirst("X-Forwarded-For")
        val xRealIP = exchange.request.headers.getFirst("X-Real-IP")
        
        return when {
            !xForwardedFor.isNullOrBlank() -> xForwardedFor.split(",")[0].trim()
            !xRealIP.isNullOrBlank() -> xRealIP
            else -> exchange.request.remoteAddress?.address?.hostAddress ?: "unknown"
        }
    }

    private fun checkRateLimit(
        key: String,
        windowStart: Instant,
        now: Instant,
        config: Config
    ): Mono<Boolean> {
        return redisTemplate.opsForZSet()
            .removeRangeByScore(key, org.springframework.data.domain.Range.closed(0.0, windowStart.epochSecond.toDouble()))
            .then(redisTemplate.opsForZSet().count(key, org.springframework.data.domain.Range.closed(windowStart.epochSecond.toDouble(), now.epochSecond.toDouble())))
            .flatMap { currentCount ->
                if (currentCount < config.requestsPerMinute) {
                    // Add current request to the window
                    redisTemplate.opsForZSet()
                        .add(key, now.toString(), now.epochSecond.toDouble())
                        .then(redisTemplate.expire(key, Duration.ofMinutes(2))) // Cleanup after 2 minutes
                        .thenReturn(true)
                } else {
                    Mono.just(false)
                }
            }
    }

    private fun addRateLimitHeaders(
        exchange: ServerWebExchange,
        key: String,
        config: Config
    ): Mono<Void> {
        val now = Instant.now()
        val windowStart = now.minusSeconds(60)

        return redisTemplate.opsForZSet()
            .count(key, org.springframework.data.domain.Range.closed(windowStart.epochSecond.toDouble(), now.epochSecond.toDouble()))
            .doOnNext { currentCount ->
                val response = exchange.response
                response.headers.add("X-RateLimit-Limit", config.requestsPerMinute.toString())
                response.headers.add("X-RateLimit-Remaining", (config.requestsPerMinute - currentCount).toString())
                response.headers.add("X-RateLimit-Reset", (now.epochSecond + 60).toString())
            }
            .then()
    }

    private fun handleRateLimitExceeded(exchange: ServerWebExchange): Mono<Void> {
        val response = exchange.response
        response.statusCode = HttpStatus.TOO_MANY_REQUESTS
        response.headers.add("Content-Type", "application/json")
        response.headers.add("Retry-After", "60")

        val errorBody = """
            {
                "error": "Rate Limit Exceeded",
                "message": "Too many requests. Please try again later.",
                "timestamp": "${Instant.now()}",
                "path": "${exchange.request.path.value()}"
            }
        """.trimIndent()

        val buffer = response.bufferFactory().wrap(errorBody.toByteArray())
        return response.writeWith(Mono.just(buffer))
    }

    override fun getConfigClass(): Class<Config> = Config::class.java
}