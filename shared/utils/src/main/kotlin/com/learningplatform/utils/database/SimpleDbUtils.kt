package com.learningplatform.utils.database

import com.learningplatform.utils.correlation.CorrelationIdService
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.criteria.Predicate

/**
 * Simple database utility class for common operations
 */
@Component
class SimpleDbUtils(
    private val correlationIdService: CorrelationIdService
) {
    
    private val logger = LoggerFactory.getLogger(SimpleDbUtils::class.java)
    
    @PersistenceContext
    private lateinit var entityManager: EntityManager
    
    /**
     * Creates a pageable object with default sorting
     */
    fun createPageable(
        page: Int = 0,
        size: Int = 20,
        sortBy: String = "createdAt",
        sortDirection: Sort.Direction = Sort.Direction.DESC
    ): Pageable {
        val validatedPage = maxOf(0, page)
        val validatedSize = when {
            size <= 0 -> 20
            size > 100 -> 100
            else -> size
        }
        
        return PageRequest.of(validatedPage, validatedSize, Sort.by(sortDirection, sortBy))
    }
    
    /**
     * Creates a specification for filtering by user ID
     */
    fun <T> userIdEquals(userId: UUID): Specification<T> {
        return Specification { root, _, criteriaBuilder ->
            criteriaBuilder.equal(root.get<UUID>("userId"), userId)
        }
    }
    
    /**
     * Creates a specification for filtering by date range
     */
    fun <T> dateRange(
        fieldName: String,
        startDate: Instant?,
        endDate: Instant?
    ): Specification<T> {
        return Specification { root, _, criteriaBuilder ->
            val predicates = mutableListOf<Predicate>()
            
            startDate?.let { start ->
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get(fieldName), start))
            }
            
            endDate?.let { end ->
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get(fieldName), end))
            }
            
            if (predicates.isEmpty()) {
                criteriaBuilder.conjunction()
            } else {
                criteriaBuilder.and(*predicates.toTypedArray())
            }
        }
    }
    
    /**
     * Creates a specification for text search (case-insensitive)
     */
    fun <T> textContains(fieldName: String, searchText: String?): Specification<T> {
        return Specification { root, _, criteriaBuilder ->
            if (searchText.isNullOrBlank()) {
                criteriaBuilder.conjunction()
            } else {
                criteriaBuilder.like(
                    criteriaBuilder.lower(root.get(fieldName)),
                    "%${searchText.lowercase()}%"
                )
            }
        }
    }
    
    /**
     * Creates a specification for filtering by status
     */
    fun <T, E : Enum<E>> statusEquals(status: E?): Specification<T> {
        return Specification { root, _, criteriaBuilder ->
            if (status == null) {
                criteriaBuilder.conjunction()
            } else {
                criteriaBuilder.equal(root.get<E>("status"), status)
            }
        }
    }
    
    /**
     * Combines multiple specifications with AND logic
     */
    fun <T> and(vararg specifications: Specification<T>): Specification<T> {
        return specifications.reduce { acc, spec -> acc.and(spec) }
    }
    
    /**
     * Combines multiple specifications with OR logic
     */
    fun <T> or(vararg specifications: Specification<T>): Specification<T> {
        return specifications.reduce { acc, spec -> acc.or(spec) }
    }
    
    /**
     * Executes a native query with parameters
     */
    @Transactional(readOnly = true)
    fun executeNativeQuery(query: String, parameters: Map<String, Any> = emptyMap()): List<*> {
        return try {
            correlationIdService.startTiming("native-query")
            
            val nativeQuery = entityManager.createNativeQuery(query)
            parameters.forEach { (key, value) ->
                nativeQuery.setParameter(key, value)
            }
            
            val results = nativeQuery.resultList
            
            val duration = correlationIdService.endTiming("native-query")
            logger.debug("Executed native query in {}ms: {}", duration, query)
            
            results
        } catch (e: Exception) {
            logger.error("Failed to execute native query: {}", query, e)
            throw e
        }
    }
    
    /**
     * Executes a native update/delete query
     */
    @Transactional
    fun executeNativeUpdate(query: String, parameters: Map<String, Any> = emptyMap()): Int {
        return try {
            correlationIdService.startTiming("native-update")
            
            val nativeQuery = entityManager.createNativeQuery(query)
            parameters.forEach { (key, value) ->
                nativeQuery.setParameter(key, value)
            }
            
            val affectedRows = nativeQuery.executeUpdate()
            
            val duration = correlationIdService.endTiming("native-update")
            logger.debug("Executed native update in {}ms, affected {} rows: {}", duration, affectedRows, query)
            
            affectedRows
        } catch (e: Exception) {
            logger.error("Failed to execute native update: {}", query, e)
            throw e
        }
    }
    
    /**
     * Bulk insert entities using batch processing
     */
    @Transactional
    fun <T> bulkInsert(entities: List<T>, batchSize: Int = 50): List<T> {
        if (entities.isEmpty()) return emptyList()
        
        return try {
            correlationIdService.startTiming("bulk-insert")
            
            val savedEntities = mutableListOf<T>()
            
            entities.chunked(batchSize).forEach { batch ->
                batch.forEach { entity ->
                    entityManager.persist(entity)
                    savedEntities.add(entity)
                }
                entityManager.flush()
                entityManager.clear()
            }
            
            val duration = correlationIdService.endTiming("bulk-insert")
            logger.debug("Bulk inserted {} entities in {}ms", entities.size, duration)
            
            savedEntities
        } catch (e: Exception) {
            logger.error("Failed to bulk insert {} entities", entities.size, e)
            throw e
        }
    }
}