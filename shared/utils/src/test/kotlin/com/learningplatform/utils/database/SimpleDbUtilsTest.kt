package com.learningplatform.utils.database

import com.learningplatform.utils.correlation.CorrelationIdService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import java.time.Instant
import java.util.*
import jakarta.persistence.EntityManager
import jakarta.persistence.Query
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Root
import jakarta.persistence.criteria.Predicate
import org.junit.jupiter.api.Assertions.*

@ExtendWith(MockitoExtension::class)
class SimpleDbUtilsTest {

    @Mock
    private lateinit var correlationIdService: CorrelationIdService
    
    @Mock
    private lateinit var entityManager: EntityManager
    
    @Mock
    private lateinit var query: Query
    
    @Mock
    private lateinit var criteriaBuilder: CriteriaBuilder
    
    @Mock
    private lateinit var criteriaQuery: CriteriaQuery<Any>
    
    @Mock
    private lateinit var root: Root<Any>
    
    @Mock
    private lateinit var predicate: Predicate
    
    private lateinit var dbUtils: SimpleDbUtils
    
    @BeforeEach
    fun setUp() {
        dbUtils = SimpleDbUtils(correlationIdService)
        
        // Use reflection to set the EntityManager
        val field = SimpleDbUtils::class.java.getDeclaredField("entityManager")
        field.isAccessible = true
        field.set(dbUtils, entityManager)
        
        whenever(correlationIdService.startTiming(any())).thenReturn(Unit)
        whenever(correlationIdService.endTiming(any())).thenReturn(100L)
    }
    
    @Test
    fun `createPageable should create valid pageable with defaults`() {
        // When
        val pageable = dbUtils.createPageable()
        
        // Then
        assertEquals(0, pageable.pageNumber)
        assertEquals(20, pageable.pageSize)
        assertEquals(Sort.by(Sort.Direction.DESC, "createdAt"), pageable.sort)
    }
    
    @Test
    fun `createPageable should create valid pageable with custom parameters`() {
        // When
        val pageable = dbUtils.createPageable(
            page = 2,
            size = 50,
            sortBy = "name",
            sortDirection = Sort.Direction.ASC
        )
        
        // Then
        assertEquals(2, pageable.pageNumber)
        assertEquals(50, pageable.pageSize)
        assertEquals(Sort.by(Sort.Direction.ASC, "name"), pageable.sort)
    }
    
    @Test
    fun `createPageable should validate page and size parameters`() {
        // When
        val pageable = dbUtils.createPageable(
            page = -1,
            size = 200
        )
        
        // Then
        assertEquals(0, pageable.pageNumber) // Negative page corrected to 0
        assertEquals(100, pageable.pageSize) // Size capped at 100
    }
    
    @Test
    fun `userIdEquals should create specification for user ID filtering`() {
        // Given
        val userId = UUID.randomUUID()
        
        // When
        val spec = dbUtils.userIdEquals<Any>(userId)
        
        // Then
        assertNotNull(spec)
        // Verify the specification can be created (actual JPA testing would require integration test)
    }
    
    @Test
    fun `dateRange should create specification for date filtering`() {
        // Given
        val startDate = Instant.now().minusSeconds(3600)
        val endDate = Instant.now()
        
        // When
        val spec = dbUtils.dateRange<Any>("createdAt", startDate, endDate)
        
        // Then
        assertNotNull(spec)
    }
    
    @Test
    fun `dateRange should handle null dates`() {
        // When
        val spec = dbUtils.dateRange<Any>("createdAt", null, null)
        
        // Then
        assertNotNull(spec)
    }
    
    @Test
    fun `textContains should create specification for text search`() {
        // Given
        val searchText = "test"
        
        // When
        val spec = dbUtils.textContains<Any>("name", searchText)
        
        // Then
        assertNotNull(spec)
    }
    
    @Test
    fun `textContains should handle null search text`() {
        // When
        val spec = dbUtils.textContains<Any>("name", null)
        
        // Then
        assertNotNull(spec)
    }
    
    @Test
    fun `statusEquals should create specification for status filtering`() {
        // Given - using a simple enum for testing
        val status = TestStatus.ACTIVE
        
        // When
        val spec = dbUtils.statusEquals<Any, TestStatus>(status)
        
        // Then
        assertNotNull(spec)
    }
    
    enum class TestStatus { ACTIVE, INACTIVE }
    
    @Test
    fun `and should combine specifications with AND logic`() {
        // Given
        val spec1 = dbUtils.textContains<Any>("name", "test")
        val spec2 = dbUtils.userIdEquals<Any>(UUID.randomUUID())
        
        // When
        val combinedSpec = dbUtils.and(spec1, spec2)
        
        // Then
        assertNotNull(combinedSpec)
    }
    
    @Test
    fun `or should combine specifications with OR logic`() {
        // Given
        val spec1 = dbUtils.textContains<Any>("name", "test")
        val spec2 = dbUtils.textContains<Any>("description", "test")
        
        // When
        val combinedSpec = dbUtils.or(spec1, spec2)
        
        // Then
        assertNotNull(combinedSpec)
    }
    
    @Test
    fun `executeNativeQuery should execute query with parameters`() {
        // Given
        val sql = "SELECT * FROM users WHERE id = :userId"
        val parameters = mapOf("userId" to UUID.randomUUID())
        val expectedResults = listOf("result1", "result2")
        
        whenever(entityManager.createNativeQuery(sql)).thenReturn(query)
        whenever(query.resultList).thenReturn(expectedResults)
        
        // When
        val results = dbUtils.executeNativeQuery(sql, parameters)
        
        // Then
        assertEquals(expectedResults, results)
        verify(entityManager).createNativeQuery(sql)
        verify(query).setParameter("userId", parameters["userId"])
    }
    
    @Test
    fun `executeNativeUpdate should execute update query`() {
        // Given
        val sql = "UPDATE users SET updated_at = NOW() WHERE id = :userId"
        val parameters = mapOf("userId" to UUID.randomUUID())
        val expectedAffectedRows = 1
        
        whenever(entityManager.createNativeQuery(sql)).thenReturn(query)
        whenever(query.executeUpdate()).thenReturn(expectedAffectedRows)
        
        // When
        val affectedRows = dbUtils.executeNativeUpdate(sql, parameters)
        
        // Then
        assertEquals(expectedAffectedRows, affectedRows)
        verify(entityManager).createNativeQuery(sql)
        verify(query).setParameter("userId", parameters["userId"])
        verify(query).executeUpdate()
    }
    
    @Test
    fun `bulkInsert should process entities in batches`() {
        // Given
        val entities = listOf("entity1", "entity2", "entity3")
        val batchSize = 2
        
        // When
        val result = dbUtils.bulkInsert(entities, batchSize)
        
        // Then
        assertEquals(entities, result)
        verify(entityManager, times(3)).persist(any())
        verify(entityManager, times(2)).flush() // 2 batches
        verify(entityManager, times(2)).clear()
    }
    
    @Test
    fun `bulkInsert should handle empty list`() {
        // Given
        val entities = emptyList<String>()
        
        // When
        val result = dbUtils.bulkInsert(entities)
        
        // Then
        assertTrue(result.isEmpty())
        verify(entityManager, never()).persist(any())
    }
    
    @Test
    fun `database operations should handle exceptions gracefully`() {
        // Given
        val sql = "INVALID SQL"
        
        whenever(entityManager.createNativeQuery(sql))
            .thenThrow(RuntimeException("SQL error"))
        
        // When & Then
        assertThrows(RuntimeException::class.java) {
            dbUtils.executeNativeQuery(sql)
        }
    }
}