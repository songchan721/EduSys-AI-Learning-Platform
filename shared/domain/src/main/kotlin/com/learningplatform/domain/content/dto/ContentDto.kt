package com.learningplatform.domain.content.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.learningplatform.domain.content.ContentType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.Instant
import java.util.*

/**
 * DTO for Generated Content
 */
data class GeneratedContentDto(
    val id: UUID,
    val sessionId: UUID,
    val contentType: ContentType,
    val title: String,
    val contentData: Map<String, Any>,
    val filePath: String?,
    val version: Int,
    val qualityScore: BigDecimal?,
    val wordCount: Int?,
    val estimatedReadingTimeMinutes: Int?,
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX")
    val createdAt: Instant,
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX")
    val updatedAt: Instant,
    
    val metadata: Map<String, String> = emptyMap()
) {
    /**
     * Get formatted reading time
     */
    fun getFormattedReadingTime(): String? {
        return estimatedReadingTimeMinutes?.let { minutes ->
            when {
                minutes < 1 -> "<1 min read"
                minutes == 1 -> "1 min read"
                minutes < 60 -> "$minutes min read"
                else -> "${minutes / 60}h ${minutes % 60}m read"
            }
        }
    }
    
    /**
     * Get formatted word count
     */
    fun getFormattedWordCount(): String? {
        return wordCount?.let { count ->
            when {
                count < 1000 -> "$count words"
                count < 1000000 -> "${String.format("%.1f", count / 1000.0)}K words"
                else -> "${String.format("%.1f", count / 1000000.0)}M words"
            }
        }
    }
    
    /**
     * Get quality score as percentage
     */
    fun getQualityPercentage(): Int? {
        return qualityScore?.let { score ->
            (score * BigDecimal(100)).toInt()
        }
    }
    
    /**
     * Check if content has high quality
     */
    fun isHighQuality(): Boolean {
        return qualityScore?.let { it >= BigDecimal("0.8") } ?: false
    }
}

/**
 * DTO for creating new content
 */
data class CreateContentDto(
    val sessionId: UUID,
    val contentType: ContentType,
    
    @field:NotBlank(message = "Title is required")
    @field:Size(min = 1, max = 500, message = "Title must be between 1 and 500 characters")
    val title: String,
    
    val contentData: Map<String, Any>,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * DTO for updating content
 */
data class UpdateContentDto(
    @field:Size(min = 1, max = 500, message = "Title must be between 1 and 500 characters")
    val title: String?,
    
    val contentData: Map<String, Any>?,
    val qualityScore: BigDecimal?,
    val metadata: Map<String, String>?
)

/**
 * DTO for content search
 */
data class ContentSearchDto(
    val query: String?,
    val contentTypes: Set<ContentType>?,
    val sessionId: UUID?,
    val minQualityScore: BigDecimal?,
    val fromDate: Instant?,
    val toDate: Instant?,
    val tags: Set<String>?,
    val sortBy: ContentSortBy = ContentSortBy.CREATED_AT,
    val sortDirection: SortDirection = SortDirection.DESC,
    val page: Int = 0,
    val size: Int = 20
)

/**
 * DTO for content search results
 */
data class ContentSearchResultDto(
    val content: List<GeneratedContentDto>,
    val totalElements: Long,
    val totalPages: Int,
    val currentPage: Int,
    val pageSize: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean
)

/**
 * Enums for content operations
 */
enum class ContentSortBy {
    CREATED_AT, UPDATED_AT, TITLE, QUALITY_SCORE, WORD_COUNT, READING_TIME
}

enum class SortDirection {
    ASC, DESC
}