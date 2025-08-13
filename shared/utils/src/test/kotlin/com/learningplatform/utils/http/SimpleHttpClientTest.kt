package com.learningplatform.utils.http

import com.fasterxml.jackson.databind.ObjectMapper
import com.learningplatform.utils.correlation.CorrelationIdService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.http.*
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.junit.jupiter.api.Assertions.*

@ExtendWith(MockitoExtension::class)
class SimpleHttpClientTest {

    @Mock
    private lateinit var restTemplate: RestTemplate
    
    @Mock
    private lateinit var correlationIdService: CorrelationIdService
    
    private lateinit var objectMapper: ObjectMapper
    private lateinit var httpClient: SimpleHttpClient
    
    @BeforeEach
    fun setUp() {
        objectMapper = ObjectMapper()
        httpClient = SimpleHttpClient(restTemplate, objectMapper, correlationIdService)
        
        whenever(correlationIdService.getCorrelationId()).thenReturn("test-correlation-id")
        whenever(correlationIdService.getUserId()).thenReturn("test-user-id")
        whenever(correlationIdService.startTiming(any())).thenReturn(Unit)
        whenever(correlationIdService.endTiming(any())).thenReturn(100L)
    }
    
    @Test
    fun `get should make GET request successfully`() {
        // Given
        val url = "https://api.example.com/test"
        val responseBody = TestResponse("success", 200)
        val responseEntity = ResponseEntity.ok(responseBody)
        
        whenever(restTemplate.exchange(
            eq(url), 
            eq(HttpMethod.GET), 
            any<HttpEntity<String>>(), 
            eq(TestResponse::class.java)
        )).thenReturn(responseEntity)
        
        // When
        val result = httpClient.get(url, TestResponse::class.java)
        
        // Then
        assertTrue(result.isSuccess())
        assertEquals(200, result.statusCode)
        assertEquals(responseBody, result.body)
    }
    
    @Test
    fun `post should make POST request with body successfully`() {
        // Given
        val url = "https://api.example.com/test"
        val requestBody = TestRequest("test", 123)
        val responseBody = TestResponse("created", 201)
        val responseEntity = ResponseEntity.status(HttpStatus.CREATED).body(responseBody)
        
        whenever(restTemplate.exchange(
            eq(url), 
            eq(HttpMethod.POST), 
            any<HttpEntity<String>>(), 
            eq(TestResponse::class.java)
        )).thenReturn(responseEntity)
        
        // When
        val result = httpClient.post(url, requestBody, TestResponse::class.java)
        
        // Then
        assertTrue(result.isSuccess())
        assertEquals(201, result.statusCode)
        assertEquals(responseBody, result.body)
    }
    
    @Test
    fun `put should make PUT request successfully`() {
        // Given
        val url = "https://api.example.com/test/1"
        val requestBody = TestRequest("updated", 456)
        val responseBody = TestResponse("updated", 200)
        val responseEntity = ResponseEntity.ok(responseBody)
        
        whenever(restTemplate.exchange(
            eq(url), 
            eq(HttpMethod.PUT), 
            any<HttpEntity<String>>(), 
            eq(TestResponse::class.java)
        )).thenReturn(responseEntity)
        
        // When
        val result = httpClient.put(url, requestBody, TestResponse::class.java)
        
        // Then
        assertTrue(result.isSuccess())
        assertEquals(200, result.statusCode)
        assertEquals(responseBody, result.body)
    }
    
    @Test
    fun `delete should make DELETE request successfully`() {
        // Given
        val url = "https://api.example.com/test/1"
        val responseBody = TestResponse("deleted", 204)
        val responseEntity = ResponseEntity.status(HttpStatus.NO_CONTENT).body(responseBody)
        
        whenever(restTemplate.exchange(
            eq(url), 
            eq(HttpMethod.DELETE), 
            any<HttpEntity<String>>(), 
            eq(TestResponse::class.java)
        )).thenReturn(responseEntity)
        
        // When
        val result = httpClient.delete(url, TestResponse::class.java)
        
        // Then
        assertTrue(result.isSuccess())
        assertEquals(204, result.statusCode)
        assertEquals(responseBody, result.body)
    }
    
    @Test
    fun `makeRequest should add correlation headers`() {
        // Given
        val url = "https://api.example.com/test"
        val responseEntity = ResponseEntity.ok(TestResponse("success", 200))
        
        whenever(restTemplate.exchange(
            eq(url), 
            eq(HttpMethod.GET), 
            any<HttpEntity<String>>(), 
            eq(TestResponse::class.java)
        )).thenReturn(responseEntity)
        
        // When
        httpClient.makeRequest(HttpMethod.GET, url, null, TestResponse::class.java)
        
        // Then
        verify(restTemplate).exchange(
            eq(url), 
            eq(HttpMethod.GET), 
            argThat<HttpEntity<String>> { entity ->
                entity.headers.containsKey("X-Correlation-ID") &&
                entity.headers.containsKey("X-User-ID") &&
                entity.headers.containsKey("User-Agent")
            }, 
            eq(TestResponse::class.java)
        )
    }
    
    @Test
    fun `makeRequest should handle client errors gracefully`() {
        // Given
        val url = "https://api.example.com/test"
        val exception = HttpClientErrorException(HttpStatus.BAD_REQUEST, "Bad Request")
        
        whenever(restTemplate.exchange(
            eq(url), 
            eq(HttpMethod.GET), 
            any<HttpEntity<String>>(), 
            eq(TestResponse::class.java)
        )).thenThrow(exception)
        
        // When
        val result = httpClient.get(url, TestResponse::class.java)
        
        // Then
        assertFalse(result.isSuccess())
        assertEquals(400, result.statusCode)
        assertTrue(result.isClientError())
        assertNotNull(result.error)
    }
    
    @Test
    fun `makeRequest should handle server errors gracefully`() {
        // Given
        val url = "https://api.example.com/test"
        val exception = HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error")
        
        whenever(restTemplate.exchange(
            eq(url), 
            eq(HttpMethod.GET), 
            any<HttpEntity<String>>(), 
            eq(TestResponse::class.java)
        )).thenThrow(exception)
        
        // When
        val result = httpClient.get(url, TestResponse::class.java)
        
        // Then
        assertFalse(result.isSuccess())
        assertEquals(500, result.statusCode)
        assertTrue(result.isServerError())
        assertNotNull(result.error)
    }
    
    data class TestRequest(
        val name: String,
        val value: Int
    )
    
    data class TestResponse(
        val message: String,
        val code: Int
    )
}