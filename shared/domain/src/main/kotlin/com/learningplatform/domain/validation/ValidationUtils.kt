package com.learningplatform.domain.validation

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Constraint
import jakarta.validation.Payload
import java.util.*
import kotlin.reflect.KClass

// Custom validation annotations

@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [ValidUUIDValidator::class])
@MustBeDocumented
annotation class ValidUUID(
    val message: String = "Invalid UUID format",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

class ValidUUIDValidator : ConstraintValidator<ValidUUID, String> {
    override fun isValid(value: String?, context: ConstraintValidatorContext?): Boolean {
        if (value == null) return true // Let @NotNull handle null validation
        
        return try {
            UUID.fromString(value)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }
}

@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [ValidTopicValidator::class])
@MustBeDocumented
annotation class ValidTopic(
    val message: String = "Topic must be between 3 and 1000 characters and contain meaningful content",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

class ValidTopicValidator : ConstraintValidator<ValidTopic, String> {
    override fun isValid(value: String?, context: ConstraintValidatorContext?): Boolean {
        if (value == null) return true // Let @NotNull handle null validation
        
        val trimmed = value.trim()
        
        // Check length
        if (trimmed.length < 3 || trimmed.length > 1000) return false
        
        // Check for meaningful content (not just whitespace or special characters)
        val meaningfulChars = trimmed.count { it.isLetterOrDigit() }
        if (meaningfulChars < 3) return false
        
        // Check for common spam patterns
        val spamPatterns = listOf(
            "test", "asdf", "qwerty", "123456", "aaaaaa"
        )
        
        return !spamPatterns.any { trimmed.lowercase().contains(it) && trimmed.length < 20 }
    }
}

@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [ValidPasswordValidator::class])
@MustBeDocumented
annotation class ValidPassword(
    val message: String = "Password must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, one digit, and one special character",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

class ValidPasswordValidator : ConstraintValidator<ValidPassword, String> {
    override fun isValid(value: String?, context: ConstraintValidatorContext?): Boolean {
        if (value == null) return true // Let @NotNull handle null validation
        
        // Minimum length
        if (value.length < 8) return false
        
        // Check for required character types
        val hasUppercase = value.any { it.isUpperCase() }
        val hasLowercase = value.any { it.isLowerCase() }
        val hasDigit = value.any { it.isDigit() }
        val hasSpecialChar = value.any { !it.isLetterOrDigit() }
        
        return hasUppercase && hasLowercase && hasDigit && hasSpecialChar
    }
}

@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [ValidJsonValidator::class])
@MustBeDocumented
annotation class ValidJson(
    val message: String = "Invalid JSON format",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

class ValidJsonValidator : ConstraintValidator<ValidJson, String> {
    override fun isValid(value: String?, context: ConstraintValidatorContext?): Boolean {
        if (value == null || value.isBlank()) return true // Allow null/empty
        
        return try {
            com.fasterxml.jackson.module.kotlin.jacksonObjectMapper().readTree(value)
            true
        } catch (e: Exception) {
            false
        }
    }
}

@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [ValidLLMProviderValidator::class])
@MustBeDocumented
annotation class ValidLLMProvider(
    val message: String = "Invalid LLM provider",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

class ValidLLMProviderValidator : ConstraintValidator<ValidLLMProvider, String> {
    private val validProviders = setOf(
        "openai", "anthropic", "google", "azure", "local", "custom",
        "OpenAI", "Anthropic", "Google", "Azure", "Local", "Custom"
    )
    
    override fun isValid(value: String?, context: ConstraintValidatorContext?): Boolean {
        if (value == null) return true // Let @NotNull handle null validation
        
        return validProviders.contains(value)
    }
}

// Utility functions for validation
object ValidationUtils {
    
    fun isValidUUID(value: String?): Boolean {
        if (value == null) return false
        return try {
            UUID.fromString(value)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }
    
    fun isValidEmail(email: String?): Boolean {
        if (email == null) return false
        val emailRegex = "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$".toRegex()
        return emailRegex.matches(email)
    }
    
    fun isStrongPassword(password: String?): Boolean {
        if (password == null || password.length < 8) return false
        
        val hasUppercase = password.any { it.isUpperCase() }
        val hasLowercase = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecialChar = password.any { !it.isLetterOrDigit() }
        
        return hasUppercase && hasLowercase && hasDigit && hasSpecialChar
    }
    
    fun sanitizeInput(input: String?): String? {
        if (input == null) return null
        
        return input.trim()
            .replace(Regex("[<>\"'&]"), "") // Remove potentially dangerous characters
            .take(10000) // Limit length to prevent DoS
    }
    
    fun isValidJson(json: String?): Boolean {
        if (json == null || json.isBlank()) return true
        
        return try {
            com.fasterxml.jackson.module.kotlin.jacksonObjectMapper().readTree(json)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    fun validatePageRequest(page: Int?, size: Int?): Pair<Int, Int> {
        val validPage = (page ?: 0).coerceAtLeast(0)
        val validSize = (size ?: 20).coerceIn(1, 100)
        return Pair(validPage, validSize)
    }
    
    fun validateSortDirection(direction: String?): String {
        return when (direction?.lowercase()) {
            "desc", "descending" -> "desc"
            else -> "asc"
        }
    }
    
    fun isValidContentType(contentType: String?): Boolean {
        if (contentType == null) return false
        
        val validTypes = setOf(
            "TEXT", "DIAGRAM", "EXERCISE", "QUIZ", "ASSESSMENT",
            "FLASHCARD", "MIND_MAP", "TUTORIAL", "CODE_EXAMPLE", "FORMULA"
        )
        
        return validTypes.contains(contentType.uppercase())
    }
    
    fun isValidAgentType(agentType: String?): Boolean {
        if (agentType == null) return false
        
        val validTypes = setOf(
            "RESEARCH", "VERIFICATION", "DECOMPOSITION", "STRUCTURING",
            "CONTENT_GENERATION", "VALIDATION", "SYNTHESIS", "LEARNING_EXPERIENCE"
        )
        
        return validTypes.contains(agentType.uppercase())
    }
    
    fun validateCostAmount(amount: Double?): Boolean {
        return amount != null && amount >= 0.0 && amount <= 10000.0 // Max $10,000 per session
    }
    
    fun validateQualityScore(score: Double?): Boolean {
        return score == null || (score >= 0.0 && score <= 1.0)
    }
    
    fun validateTokenCount(tokens: Int?): Boolean {
        return tokens == null || (tokens >= 0 && tokens <= 1_000_000) // Max 1M tokens
    }
    
    fun validateDuration(minutes: Int?): Boolean {
        return minutes == null || (minutes >= 0 && minutes <= 1440) // Max 24 hours
    }
}