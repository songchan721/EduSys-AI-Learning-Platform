package com.learningplatform.domain.content

import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.Instant
import java.util.*

@Entity
@Table(name = "generated_content")
data class GeneratedContent(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),
    
    @Column(name = "session_id", nullable = false)
    val sessionId: UUID,
    
    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false)
    val contentType: ContentType,
    
    @Column(nullable = false, length = 500)
    @NotBlank(message = "Title is required")
    @Size(min = 1, max = 500, message = "Title must be between 1 and 500 characters")
    val title: String,
    
    @Column(name = "content_data", nullable = false, columnDefinition = "JSONB")
    val contentData: String,
    
    @Column(name = "file_path", length = 1000)
    val filePath: String? = null,
    
    @Column(nullable = false)
    val version: Int = 1,
    
    @Column(name = "quality_score", precision = 3, scale = 2)
    val qualityScore: BigDecimal? = null,
    
    @Column(name = "word_count")
    val wordCount: Int? = null,
    
    @Column(name = "estimated_reading_time_minutes")
    val estimatedReadingTimeMinutes: Int? = null,
    
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
    
    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now(),
    
    @OneToMany(mappedBy = "content", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val versions: MutableList<ContentVersion> = mutableListOf(),
    
    @OneToMany(mappedBy = "content", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val metadata: MutableSet<ContentMetadata> = mutableSetOf()
) {
    fun addVersion(contentData: String, changesDescription: String? = null): ContentVersion {
        val newVersion = ContentVersion(
            content = this,
            versionNumber = versions.size + 1,
            contentData = contentData,
            changesDescription = changesDescription
        )
        versions.add(newVersion)
        return newVersion
    }
    
    fun addMetadata(key: String, value: String): ContentMetadata {
        val existing = metadata.find { it.metadataKey == key }
        if (existing != null) {
            metadata.remove(existing)
        }
        
        val newMetadata = ContentMetadata(
            content = this,
            metadataKey = key,
            metadataValue = value
        )
        metadata.add(newMetadata)
        return newMetadata
    }
    
    fun getMetadata(key: String): String? {
        return metadata.find { it.metadataKey == key }?.metadataValue
    }
}

@Entity
@Table(name = "content_versions")
data class ContentVersion(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id", nullable = false)
    val content: GeneratedContent,
    
    @Column(name = "version_number", nullable = false)
    val versionNumber: Int,
    
    @Column(name = "content_data", nullable = false, columnDefinition = "JSONB")
    val contentData: String,
    
    @Column(name = "changes_description", columnDefinition = "TEXT")
    val changesDescription: String? = null,
    
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)

@Entity
@Table(name = "content_metadata")
data class ContentMetadata(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id", nullable = false)
    val content: GeneratedContent,
    
    @Column(name = "metadata_key", nullable = false, length = 100)
    val metadataKey: String,
    
    @Column(name = "metadata_value", nullable = false, columnDefinition = "TEXT")
    val metadataValue: String,
    
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)

enum class ContentType {
    TEXT,
    DIAGRAM,
    EXERCISE,
    QUIZ,
    ASSESSMENT,
    FLASHCARD,
    MIND_MAP,
    TUTORIAL,
    CODE_EXAMPLE,
    FORMULA
}