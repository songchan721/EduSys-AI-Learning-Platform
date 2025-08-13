package com.learningplatform.utils.json

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Simple JSON utility class for JSON operations
 */
@Component
class SimpleJsonUtils(
    private val objectMapper: ObjectMapper
) {
    
    private val logger = LoggerFactory.getLogger(SimpleJsonUtils::class.java)
    
    /**
     * Converts an object to JSON string
     */
    fun toJson(obj: Any?): String? {
        return try {
            if (obj == null) return null
            objectMapper.writeValueAsString(obj)
        } catch (e: JsonProcessingException) {
            logger.error("Failed to convert object to JSON: {}", obj?.javaClass?.simpleName, e)
            null
        }
    }
    
    /**
     * Converts an object to pretty-printed JSON string
     */
    fun toPrettyJson(obj: Any?): String? {
        return try {
            if (obj == null) return null
            objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj)
        } catch (e: JsonProcessingException) {
            logger.error("Failed to convert object to pretty JSON: {}", obj?.javaClass?.simpleName, e)
            null
        }
    }
    
    /**
     * Converts JSON string to object of specified type
     */
    fun <T> fromJson(json: String?, clazz: Class<T>): T? {
        return try {
            if (json.isNullOrBlank()) return null
            objectMapper.readValue(json, clazz)
        } catch (e: JsonProcessingException) {
            logger.error("Failed to parse JSON to {}: {}", clazz.simpleName, json, e)
            null
        }
    }
    
    /**
     * Converts JSON string to object using TypeReference
     */
    fun <T> fromJson(json: String?, typeReference: TypeReference<T>): T? {
        return try {
            if (json.isNullOrBlank()) return null
            objectMapper.readValue(json, typeReference)
        } catch (e: JsonProcessingException) {
            logger.error("Failed to parse JSON with TypeReference: {}", json, e)
            null
        }
    }
    
    /**
     * Converts JSON string to JsonNode for flexible manipulation
     */
    fun parseToNode(json: String?): JsonNode? {
        return try {
            if (json.isNullOrBlank()) return null
            objectMapper.readTree(json)
        } catch (e: JsonProcessingException) {
            logger.error("Failed to parse JSON to JsonNode: {}", json, e)
            null
        }
    }
    
    /**
     * Converts object to JsonNode
     */
    fun toNode(obj: Any?): JsonNode? {
        return try {
            if (obj == null) return null
            objectMapper.valueToTree(obj)
        } catch (e: Exception) {
            logger.error("Failed to convert object to JsonNode: {}", obj?.javaClass?.simpleName, e)
            null
        }
    }
    
    /**
     * Validates if a string is valid JSON
     */
    fun isValidJson(json: String?): Boolean {
        return try {
            if (json.isNullOrBlank()) return false
            objectMapper.readTree(json)
            true
        } catch (e: JsonProcessingException) {
            false
        }
    }
    
    /**
     * Converts a Map to JSON string
     */
    fun mapToJson(map: Map<String, Any?>): String? {
        return toJson(map)
    }
    
    /**
     * Converts JSON string to Map
     */
    fun jsonToMap(json: String?): Map<String, Any?>? {
        return fromJson(json, object : TypeReference<Map<String, Any?>>() {})
    }
    
    /**
     * Converts a List to JSON string
     */
    fun listToJson(list: List<Any?>): String? {
        return toJson(list)
    }
    
    /**
     * Converts JSON string to List
     */
    fun jsonToList(json: String?): List<Any?>? {
        return fromJson(json, object : TypeReference<List<Any?>>() {})
    }
}