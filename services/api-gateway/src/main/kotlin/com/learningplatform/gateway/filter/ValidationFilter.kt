package com.learningplatform.gateway.filter

import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

/**
 * Request Validation Filter
 * 
 * Validates and sanitizes incoming requests to prevent malicious input.
 * Checks for common security threats and malformed requests.
 */
@Component
class ValidationFilter : AbstractGatewayFilterFactory<ValidationFilter.Config>() {

    data class Config(
        val maxRequestSize: Long = 10 * 1024 * 1024, // 10MB
        val allowedContentTypes: List<String> = listOf(
            "application/json",
            "application/x-www-form-urlencoded",
            "multipart/form-data",
            "text/plain"
        ),
        val blockedPatterns: List<String> = listOf(
            "(?i).*<script.*>.*</script>.*",
            "(?i).*javascript:.*",
            "(?i).*on\\w+\\s*=.*",
            "(?i).*union.*select.*",
            "(?i).*drop.*table.*",
            "(?i).*insert.*into.*",
            "(?i).*delete.*from.*"
        )
    )

    override fun apply(config: Config): GatewayFilter {
        return GatewayFilter { exchange, chain ->
            val request = exchange.request
            
            // Validate request size
            val contentLength = request.headers.contentLength
            if (contentLength > config.maxRequestSize) {
                return@GatewayFilter handleValidationError(
                    exchange, 
                    "Request too large", 
                    "Request size exceeds maximum allowed size of ${config.maxRequestSize} bytes"
                )
            }

            // Validate content type for POST/PUT requests
            val method = request.method?.toString()
            if (method in listOf("POST", "PUT", "PATCH")) {
                val contentType = request.headers.contentType?.toString()
                if (contentType != null && !config.allowedContentTypes.any { contentType.startsWith(it) }) {
                    return@GatewayFilter handleValidationError(
                        exchange,
                        "Invalid content type",
                        "Content type '$contentType' is not allowed"
                    )
                }
            }

            // Validate headers for malicious content
            for ((headerName, headerValues) in request.headers) {
                for (headerValue in headerValues) {
                    if (containsMaliciousContent(headerValue, config.blockedPatterns)) {
                        return@GatewayFilter handleValidationError(
                            exchange,
                            "Malicious content detected",
                            "Header '$headerName' contains potentially malicious content"
                        )
                    }
                }
            }

            // Validate query parameters
            for ((paramName, paramValues) in request.queryParams) {
                for (paramValue in paramValues) {
                    if (containsMaliciousContent(paramValue, config.blockedPatterns)) {
                        return@GatewayFilter handleValidationError(
                            exchange,
                            "Malicious content detected",
                            "Query parameter '$paramName' contains potentially malicious content"
                        )
                    }
                }
            }

            // Validate path for directory traversal and other attacks
            val path = request.path.value()
            if (path.contains("../") || path.contains("..\\") || containsMaliciousContent(path, config.blockedPatterns)) {
                return@GatewayFilter handleValidationError(
                    exchange,
                    "Invalid path",
                    "Request path contains potentially malicious content"
                )
            }

            // Add security headers to response
            exchange.response.headers.add("X-Content-Type-Options", "nosniff")
            exchange.response.headers.add("X-Frame-Options", "DENY")
            exchange.response.headers.add("X-XSS-Protection", "1; mode=block")
            exchange.response.headers.add("Strict-Transport-Security", "max-age=31536000; includeSubDomains")
            exchange.response.headers.add("Referrer-Policy", "strict-origin-when-cross-origin")

            chain.filter(exchange)
        }
    }

    private fun containsMaliciousContent(content: String, blockedPatterns: List<String>): Boolean {
        return blockedPatterns.any { pattern ->
            content.matches(Regex(pattern))
        }
    }

    private fun handleValidationError(
        exchange: ServerWebExchange,
        error: String,
        message: String
    ): Mono<Void> {
        val response = exchange.response
        response.statusCode = HttpStatus.BAD_REQUEST
        response.headers.add("Content-Type", "application/json")

        val errorBody = """
            {
                "error": "$error",
                "message": "$message",
                "timestamp": "${java.time.Instant.now()}",
                "path": "${exchange.request.path.value()}"
            }
        """.trimIndent()

        val buffer = response.bufferFactory().wrap(errorBody.toByteArray())
        return response.writeWith(Mono.just(buffer))
    }

    override fun getConfigClass(): Class<Config> = Config::class.java
}