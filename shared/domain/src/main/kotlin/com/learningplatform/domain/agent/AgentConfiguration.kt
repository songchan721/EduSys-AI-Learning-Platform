package com.learningplatform.domain.agent

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.*

@Entity
@Table(name = "agent_configurations")
data class AgentConfiguration(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),
    
    @Column(name = "user_id", nullable = false)
    val userId: UUID,
    
    @Enumerated(EnumType.STRING)
    @Column(name = "agent_type", nullable = false)
    val agentType: AgentType,
    
    @Column(name = "llm_provider", nullable = false)
    @NotBlank(message = "LLM provider is required")
    @Size(max = 100, message = "LLM provider name must not exceed 100 characters")
    val llmProvider: String,
    
    @Column(name = "llm_model", nullable = false)
    @NotBlank(message = "LLM model is required")
    @Size(max = 100, message = "LLM model name must not exceed 100 characters")
    val llmModel: String,
    
    @JsonIgnore
    @Column(name = "encrypted_api_key", columnDefinition = "TEXT")
    val encryptedApiKey: String? = null,
    
    @Column(name = "config_data", columnDefinition = "JSONB")
    val configData: String? = null, // JSON string for additional configuration
    
    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true,
    
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
    
    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now()
) {
    fun isConfiguredForUser(userId: UUID): Boolean = this.userId == userId && isActive
    
    fun hasApiKey(): Boolean = !encryptedApiKey.isNullOrBlank()
}

@Entity
@Table(name = "llm_providers")
data class LLMProvider(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),
    
    @Column(name = "user_id", nullable = false)
    val userId: UUID,
    
    @Column(name = "provider_name", nullable = false)
    @NotBlank(message = "Provider name is required")
    @Size(max = 100, message = "Provider name must not exceed 100 characters")
    val providerName: String,
    
    @JsonIgnore
    @Column(name = "encrypted_api_key", nullable = false, columnDefinition = "TEXT")
    val encryptedApiKey: String,
    
    @Column(name = "config_data", columnDefinition = "JSONB")
    val configData: String? = null, // JSON string for provider-specific configuration
    
    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true,
    
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
    
    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now()
) {
    fun isOwnedBy(userId: UUID): Boolean = this.userId == userId
    
    fun isAvailable(): Boolean = isActive && encryptedApiKey.isNotBlank()
}

enum class AgentType {
    RESEARCH,
    VERIFICATION,
    DECOMPOSITION,
    STRUCTURING,
    CONTENT_GENERATION,
    VALIDATION,
    SYNTHESIS,
    LEARNING_EXPERIENCE
}

enum class LLMProviderType {
    OPENAI,
    ANTHROPIC,
    GOOGLE,
    AZURE,
    LOCAL,
    CUSTOM
}