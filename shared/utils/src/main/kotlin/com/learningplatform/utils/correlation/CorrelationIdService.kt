package com.learningplatform.utils.correlation

import org.slf4j.MDC
import org.springframework.stereotype.Service
import java.util.*

@Service
class CorrelationIdService {
    
    companion object {
        const val CORRELATION_ID_KEY = "correlationId"
        const val USER_ID_KEY = "userId"
        const val SESSION_ID_KEY = "sessionId"
        const val REQUEST_ID_KEY = "requestId"
        const val TRACE_ID_KEY = "traceId"
    }
    
    /**
     * Generates a new correlation ID and sets it in the MDC
     */
    fun generateAndSetCorrelationId(): String {
        val correlationId = UUID.randomUUID().toString()
        setCorrelationId(correlationId)
        return correlationId
    }
    
    /**
     * Sets the correlation ID in the MDC for logging
     */
    fun setCorrelationId(correlationId: String) {
        MDC.put(CORRELATION_ID_KEY, correlationId)
    }
    
    /**
     * Gets the current correlation ID from MDC
     */
    fun getCorrelationId(): String? {
        return MDC.get(CORRELATION_ID_KEY)
    }
    
    /**
     * Sets the user ID in the MDC for logging
     */
    fun setUserId(userId: String) {
        MDC.put(USER_ID_KEY, userId)
    }
    
    /**
     * Gets the current user ID from MDC
     */
    fun getUserId(): String? {
        return MDC.get(USER_ID_KEY)
    }
    
    /**
     * Sets the session ID in the MDC for logging
     */
    fun setSessionId(sessionId: String) {
        MDC.put(SESSION_ID_KEY, sessionId)
    }
    
    /**
     * Gets the current session ID from MDC
     */
    fun getSessionId(): String? {
        return MDC.get(SESSION_ID_KEY)
    }
    
    /**
     * Sets the request ID in the MDC for logging
     */
    fun setRequestId(requestId: String) {
        MDC.put(REQUEST_ID_KEY, requestId)
    }
    
    /**
     * Gets the current request ID from MDC
     */
    fun getRequestId(): String? {
        return MDC.get(REQUEST_ID_KEY)
    }
    
    /**
     * Sets the trace ID in the MDC for distributed tracing
     */
    fun setTraceId(traceId: String) {
        MDC.put(TRACE_ID_KEY, traceId)
    }
    
    /**
     * Gets the current trace ID from MDC
     */
    fun getTraceId(): String? {
        return MDC.get(TRACE_ID_KEY)
    }
    
    /**
     * Sets multiple context values at once
     */
    fun setContext(
        correlationId: String? = null,
        userId: String? = null,
        sessionId: String? = null,
        requestId: String? = null,
        traceId: String? = null
    ) {
        correlationId?.let { setCorrelationId(it) }
        userId?.let { setUserId(it) }
        sessionId?.let { setSessionId(it) }
        requestId?.let { setRequestId(it) }
        traceId?.let { setTraceId(it) }
    }
    
    /**
     * Gets all current context values
     */
    fun getContext(): Map<String, String> {
        val context = mutableMapOf<String, String>()
        
        getCorrelationId()?.let { context[CORRELATION_ID_KEY] = it }
        getUserId()?.let { context[USER_ID_KEY] = it }
        getSessionId()?.let { context[SESSION_ID_KEY] = it }
        getRequestId()?.let { context[REQUEST_ID_KEY] = it }
        getTraceId()?.let { context[TRACE_ID_KEY] = it }
        
        return context
    }
    
    /**
     * Clears all context from MDC
     */
    fun clearContext() {
        MDC.clear()
    }
    
    /**
     * Clears specific context key from MDC
     */
    fun clearContextKey(key: String) {
        MDC.remove(key)
    }
    
    /**
     * Executes a block of code with a specific correlation context
     */
    fun <T> withContext(
        correlationId: String? = null,
        userId: String? = null,
        sessionId: String? = null,
        requestId: String? = null,
        traceId: String? = null,
        block: () -> T
    ): T {
        val originalContext = getContext()
        
        try {
            setContext(correlationId, userId, sessionId, requestId, traceId)
            return block()
        } finally {
            // Restore original context
            clearContext()
            originalContext.forEach { (key, value) ->
                MDC.put(key, value)
            }
        }
    }
    
    /**
     * Executes a block of code with a new correlation ID
     */
    fun <T> withNewCorrelationId(block: () -> T): T {
        val newCorrelationId = UUID.randomUUID().toString()
        return withContext(correlationId = newCorrelationId, block = block)
    }
    
    /**
     * Propagates correlation context to a new thread
     */
    fun propagateToNewThread(): Map<String, String> {
        return getContext()
    }
    
    /**
     * Restores correlation context in a new thread
     */
    fun restoreFromPropagation(context: Map<String, String>) {
        clearContext()
        context.forEach { (key, value) ->
            MDC.put(key, value)
        }
    }
    
    /**
     * Creates a correlation context for agent execution
     */
    fun createAgentContext(
        sessionId: String,
        agentType: String,
        executionId: String
    ): String {
        val correlationId = UUID.randomUUID().toString()
        setContext(
            correlationId = correlationId,
            sessionId = sessionId,
            requestId = executionId
        )
        
        // Add agent-specific context
        MDC.put("agentType", agentType)
        MDC.put("executionId", executionId)
        
        return correlationId
    }
    
    /**
     * Creates a correlation context for API requests
     */
    fun createApiContext(
        userId: String? = null,
        endpoint: String? = null,
        method: String? = null
    ): String {
        val correlationId = UUID.randomUUID().toString()
        val requestId = UUID.randomUUID().toString()
        
        setContext(
            correlationId = correlationId,
            userId = userId,
            requestId = requestId
        )
        
        endpoint?.let { MDC.put("endpoint", it) }
        method?.let { MDC.put("method", it) }
        
        return correlationId
    }
    
    /**
     * Adds timing information to the context
     */
    fun startTiming(operation: String) {
        MDC.put("${operation}_start", System.currentTimeMillis().toString())
    }
    
    /**
     * Calculates and logs timing information
     */
    fun endTiming(operation: String): Long? {
        val startTime = MDC.get("${operation}_start")?.toLongOrNull()
        return if (startTime != null) {
            val duration = System.currentTimeMillis() - startTime
            MDC.put("${operation}_duration", duration.toString())
            MDC.remove("${operation}_start")
            duration
        } else {
            null
        }
    }
}