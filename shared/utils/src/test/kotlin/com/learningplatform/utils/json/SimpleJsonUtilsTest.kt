package com.learningplatform.utils.json

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class SimpleJsonUtilsTest {

    private lateinit var objectMapper: ObjectMapper
    private lateinit var jsonUtils: SimpleJsonUtils
    
    @BeforeEach
    fun setUp() {
        objectMapper = ObjectMapper()
        jsonUtils = SimpleJsonUtils(objectMapper)
    }
    
    @Test
    fun `toJson should convert object to JSON string`() {
        // Given
        val testObject = TestData("test", 123, true)
        
        // When
        val json = jsonUtils.toJson(testObject)
        
        // Then
        assertNotNull(json)
        assertTrue(json!!.contains("\"name\":\"test\""))
        assertTrue(json.contains("\"number\":123"))
        assertTrue(json.contains("\"active\":true"))
    }
    
    @Test
    fun `toJson should return null for null input`() {
        // When
        val json = jsonUtils.toJson(null)
        
        // Then
        assertNull(json)
    }
    
    @Test
    fun `toPrettyJson should convert object to pretty JSON string`() {
        // Given
        val testObject = TestData("test", 123, true)
        
        // When
        val json = jsonUtils.toPrettyJson(testObject)
        
        // Then
        assertNotNull(json)
        assertTrue(json!!.contains("\n"))
        assertTrue(json.contains("  "))
    }
    
    @Test
    fun `fromJson should convert JSON string to object`() {
        // Given
        val json = """{"name":"test","number":123,"active":true}"""
        
        // When
        val result = jsonUtils.fromJson(json, TestData::class.java)
        
        // Then
        assertNotNull(result)
        assertEquals("test", result!!.name)
        assertEquals(123, result.number)
        assertEquals(true, result.active)
    }
    
    @Test
    fun `fromJson should return null for invalid JSON`() {
        // Given
        val invalidJson = """{"name":"test","number":}"""
        
        // When
        val result = jsonUtils.fromJson(invalidJson, TestData::class.java)
        
        // Then
        assertNull(result)
    }
    
    @Test
    fun `fromJson with TypeReference should convert JSON to Map`() {
        // Given
        val json = """{"key1":"value1","key2":"value2"}"""
        val typeRef = object : TypeReference<Map<String, String>>() {}
        
        // When
        val result = jsonUtils.fromJson(json, typeRef)
        
        // Then
        assertNotNull(result)
        assertEquals("value1", result!!["key1"])
        assertEquals("value2", result["key2"])
    }
    
    @Test
    fun `parseToNode should convert JSON string to JsonNode`() {
        // Given
        val json = """{"name":"test","number":123}"""
        
        // When
        val node = jsonUtils.parseToNode(json)
        
        // Then
        assertNotNull(node)
        assertEquals("test", node!!.get("name").asText())
        assertEquals(123, node.get("number").asInt())
    }
    
    @Test
    fun `toNode should convert object to JsonNode`() {
        // Given
        val testObject = TestData("test", 123, true)
        
        // When
        val node = jsonUtils.toNode(testObject)
        
        // Then
        assertNotNull(node)
        assertEquals("test", node!!.get("name").asText())
        assertEquals(123, node.get("number").asInt())
        assertEquals(true, node.get("active").asBoolean())
    }
    
    @Test
    fun `isValidJson should return true for valid JSON`() {
        // Given
        val validJson = """{"name":"test","number":123}"""
        
        // When
        val isValid = jsonUtils.isValidJson(validJson)
        
        // Then
        assertTrue(isValid)
    }
    
    @Test
    fun `isValidJson should return false for invalid JSON`() {
        // Given
        val invalidJson = """{"name":"test","number":}"""
        
        // When
        val isValid = jsonUtils.isValidJson(invalidJson)
        
        // Then
        assertFalse(isValid)
    }
    
    @Test
    fun `mapToJson should convert Map to JSON string`() {
        // Given
        val map = mapOf("key1" to "value1", "key2" to 123)
        
        // When
        val json = jsonUtils.mapToJson(map)
        
        // Then
        assertNotNull(json)
        assertTrue(json!!.contains("\"key1\":\"value1\""))
        assertTrue(json.contains("\"key2\":123"))
    }
    
    @Test
    fun `jsonToMap should convert JSON string to Map`() {
        // Given
        val json = """{"key1":"value1","key2":123}"""
        
        // When
        val map = jsonUtils.jsonToMap(json)
        
        // Then
        assertNotNull(map)
        assertEquals("value1", map!!["key1"])
        assertEquals(123, map["key2"])
    }
    
    @Test
    fun `listToJson should convert List to JSON string`() {
        // Given
        val list = listOf("item1", "item2", 123)
        
        // When
        val json = jsonUtils.listToJson(list)
        
        // Then
        assertNotNull(json)
        assertTrue(json!!.contains("\"item1\""))
        assertTrue(json.contains("\"item2\""))
        assertTrue(json.contains("123"))
    }
    
    @Test
    fun `jsonToList should convert JSON string to List`() {
        // Given
        val json = """["item1","item2",123]"""
        
        // When
        val list = jsonUtils.jsonToList(json)
        
        // Then
        assertNotNull(list)
        assertEquals(3, list!!.size)
        assertEquals("item1", list[0])
        assertEquals("item2", list[1])
        assertEquals(123, list[2])
    }
    
    data class TestData(
        val name: String,
        val number: Int,
        val active: Boolean
    )
}