package com.learningplatform.utils.http

import com.fasterxml.jackson.databind.ObjectMapper
import com.learningplatform.utils.correlation.CorrelationIdService
import org.slf4j.LoggerFactory
import org.springframework.http.*
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException

/**
 * Simple HTTP client utility for making REST API calls
 */
@Service
class SimpleHttpClient(
    private val restTemplate: RestTemplate,
    private val objectMapper: ObjectMapper,
    private val correlationIdService: CorrelationIdService
) {
    
    private val logger = LoggerFactory.getLogger(SimpleHttpClient::class.java)
    
    /**
     * Makes a GET request
     */
    fun <T> get(url: String, responseClass: Class<T>, headers: Map<String, String> = emptyMap()): HttpResponse<T> {
        return makeRequest(HttpMethod.GET, url, null, responseClass, headers)
    }
    
    /**
     * Makes a POST request
     */
    fun <T> post(url: String, body: Any?, responseClass: Class<T>, headers: Map<String, String> = emptyMap()): HttpResponse<T> {
        return makeRequest(HttpMethod.POST, url, body, responseClass, headers)
    }
    
    /**
     * Makes a PUT request
     */
    fun <T> put(url: String, body: Any?, responseClass: Class<T>, headers: Map<String, String> = emptyMap()): HttpResponse<T> {
        return makeRequest(HttpMethod.PUT, url, body, responseClass, headers)
    }
    
    /**
     * Makes a DELETE request
     */
    fun <T> delete(url: String, responseClass: Class<T>, headers: Map<String, String> = emptyMap()): HttpResponse<T> {
        return makeRequest(HttpMethod.DELETE, url, null, responseClass, headers)
    }
    
    /**
     * Makes a generic HTTP request
     */
    fun <T> makeRequest(
        method: HttpMethod,
        url: String,
        body: Any?,
        responseClass: Class<T>,
        headers: Map<String, String> = emptyMap()
    ): HttpResponse<T> {
        
        return try {
            correlationIdService.startTiming("http-request")
            
            // Build headers with correlation context
            val httpHeaders = buildHeaders(headers)
            
            // Prepare request body
            val requestBody = when (body) {
                null -> null
                is String -> body
                else -> objectMapper.writeValueAsString(body)
            }
            
            // Create HTTP entity
            val httpEntity = HttpEntity(requestBody, httpHeaders)
            
            logger.debug("Making {} request to {}", method, url)
            
            // Make the request
            val responseEntity = restTemplate.exchange(url, method, httpEntity, responseClass)
            
            val duration = correlationIdService.endTiming("http-request")
            logger.debug("HTTP {} {} completed in {}ms with status {}", 
                method, url, duration, responseEntity.statusCode)
            
            HttpResponse(
                statusCode = responseEntity.statusCode.value(),
                headers = responseEntity.headers.toSingleValueMap(),
                body = responseEntity.body,
                success = responseEntity.statusCode.is2xxSuccessful
            )
            
        } catch (e: HttpClientErrorException) {
            val duration = correlationIdService.endTiming("http-request")
            logger.warn("HTTP client error for {} {} after {}ms: {}", 
                method, url, duration, e.statusCode)
            
            HttpResponse<T>(
                statusCode = e.statusCode.value(),
                headers = e.responseHeaders?.toSingleValueMap() ?: emptyMap(),
                body = null,
                success = false,
                error = "Client error: ${e.statusCode}"
            )
            
        } catch (e: HttpServerErrorException) {
            val duration = correlationIdService.endTiming("http-request")
            logger.error("HTTP server error for {} {} after {}ms: {}", 
                method, url, duration, e.statusCode)
            
            HttpResponse<T>(
                statusCode = e.statusCode.value(),
                headers = e.responseHeaders?.toSingleValueMap() ?: emptyMap(),
                body = null,
                success = false,
                error = "Server error: ${e.statusCode}"
            )
            
        } catch (e: ResourceAccessException) {
            val duration = correlationIdService.endTiming("http-request")
            logger.error("HTTP resource access error for {} {} after {}ms", method, url, duration, e)
            
            HttpResponse<T>(
                statusCode = 503,
                headers = emptyMap(),
                body = null,
                success = false,
                error = "Resource access error: ${e.message}"
            )
            
        } catch (e: Exception) {
            val duration = correlationIdService.endTiming("http-request")
            logger.error("Unexpected HTTP error for {} {} after {}ms", method, url, duration, e)
            
            HttpResponse<T>(
                statusCode = 500,
                headers = emptyMap(),
                body = null,
                success = false,
                error = "Unexpected error: ${e.message}"
            )
        }
    }
    
    private fun buildHeaders(customHeaders: Map<String, String>): HttpHeaders {
        val headers = HttpHeaders()
        
        // Add correlation context headers
        correlationIdService.getCorrelationId()?.let { 
            headers.set("X-Correlation-ID", it) 
        }
        correlationIdService.getUserId()?.let { 
            headers.set("X-User-ID", it) 
        }
        
        // Add default headers
        headers.set("User-Agent", "LearningPlatform-HttpClient/1.0")
        headers.set("Accept", "application/json")
        headers.contentType = MediaType.APPLICATION_JSON
        
        // Add custom headers
        customHeaders.forEach { (key, value) ->
            headers.set(key, value)
        }
        
        return headers
    }
}

/**
 * HTTP response wrapper
 */
data class HttpResponse<T>(
    val statusCode: Int,
    val headers: Map<String, String>,
    val body: T?,
    val success: Boolean,
    val error: String? = null
) {
    fun isSuccess(): Boolean = success
    fun isClientError(): Boolean = statusCode in 400..499
    fun isServerError(): Boolean = statusCode in 500..599
}