package com.learningplatform.gateway.filter

import com.learningplatform.security.jwt.JwtTokenService
import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

/**
 * Authentication Filter
 * 
 * Validates JWT tokens for protected routes and adds user context to requests.
 * Integrates with the shared JWT token service for consistent authentication.
 */
@Component
class AuthenticationFilter(
    private val jwtTokenService: JwtTokenService
) : AbstractGatewayFilterFactory<AuthenticationFilter.Config>() {

    data class Config(
        val excludePaths: List<String> = listOf(
            "/auth/login",
            "/auth/register",
            "/auth/refresh",
            "/health",
            "/actuator"
        )
    )

    override fun apply(config: Config): GatewayFilter {
        return GatewayFilter { exchange, chain ->
            val request = exchange.request
            val path = request.path.value()

            // Skip authentication for excluded paths
            if (config.excludePaths.any { path.contains(it) }) {
                return@GatewayFilter chain.filter(exchange)
            }

            val authHeader = request.headers.getFirst(HttpHeaders.AUTHORIZATION)
            
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return@GatewayFilter handleUnauthorized(exchange)
            }

            val token = authHeader.substring(7)
            
            try {
                // Validate token using shared JWT service
                val claims = jwtTokenService.validateToken(token)
                if (claims == null) {
                    return@GatewayFilter handleUnauthorized(exchange)
                }

                // Extract user information from token
                val userId = jwtTokenService.getUserIdFromToken(token)
                val userRoles = jwtTokenService.getRolesFromToken(token)
                val userEmail = jwtTokenService.getEmailFromToken(token)

                // Add user context to request headers for downstream services
                val modifiedRequest = request.mutate()
                    .header("X-User-ID", userId?.toString() ?: "")
                    .header("X-User-Role", userRoles.joinToString(","))
                    .header("X-User-Email", userEmail ?: "")
                    .header("X-Authenticated", "true")
                    .build()

                val modifiedExchange = exchange.mutate()
                    .request(modifiedRequest)
                    .build()

                chain.filter(modifiedExchange)
                
            } catch (exception: Exception) {
                println("Authentication error: ${exception.message}")
                handleUnauthorized(exchange)
            }
        }
    }

    private fun handleUnauthorized(exchange: ServerWebExchange): Mono<Void> {
        val response = exchange.response
        response.statusCode = HttpStatus.UNAUTHORIZED
        response.headers.add("Content-Type", "application/json")
        
        val errorBody = """
            {
                "error": "Unauthorized",
                "message": "Invalid or missing authentication token",
                "timestamp": "${java.time.Instant.now()}",
                "path": "${exchange.request.path.value()}"
            }
        """.trimIndent()
        
        val buffer = response.bufferFactory().wrap(errorBody.toByteArray())
        return response.writeWith(Mono.just(buffer))
    }

    override fun getConfigClass(): Class<Config> = Config::class.java
}