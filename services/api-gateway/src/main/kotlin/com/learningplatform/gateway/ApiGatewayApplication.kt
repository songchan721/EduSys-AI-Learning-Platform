package com.learningplatform.gateway

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.gateway.config.GatewayAutoConfiguration
import org.springframework.context.annotation.ComponentScan

/**
 * API Gateway Application
 * 
 * Central entry point for all client requests to the learning platform.
 * Provides routing, authentication, rate limiting, and cross-cutting concerns.
 */
@SpringBootApplication
@ComponentScan(basePackages = [
    "com.learningplatform.gateway",
    "com.learningplatform.security",
    "com.learningplatform.utils"
])
class ApiGatewayApplication

fun main(args: Array<String>) {
    runApplication<ApiGatewayApplication>(*args)
}