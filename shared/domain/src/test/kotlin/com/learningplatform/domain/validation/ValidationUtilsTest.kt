package com.learningplatform.domain.validation

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.util.*

class ValidationUtilsTest {

    @Test
    fun `should validate correct UUID format`() {
        // Given
        val validUuid = UUID.randomUUID().toString()
        val invalidUuid = "not-a-uuid"

        // When & Then
        assertTrue(ValidationUtils.isValidUUID(validUuid))
        assertFalse(ValidationUtils.isValidUUID(invalidUuid))
        assertFalse(ValidationUtils.isValidUUID(null))
    }

    @Test
    fun `should validate email addresses`() {
        // Given
        val validEmails = listOf(
            "test@example.com",
            "user.name@domain.co.uk",
            "user+tag@example.org"
        )
        
        val invalidEmails = listOf(
            "not-an-email",
            "@example.com",
            "user@",
            "user@domain",
            null
        )

        // When & Then
        validEmails.forEach { email ->
            assertTrue(ValidationUtils.isValidEmail(email), "Should validate: $email")
        }
        
        invalidEmails.forEach { email ->
            assertFalse(ValidationUtils.isValidEmail(email), "Should reject: $email")
        }
    }

    @Test
    fun `should validate strong passwords`() {
        // Given
        val strongPasswords = listOf(
            "SecurePass123!",
            "MyP@ssw0rd",
            "Complex1ty!"
        )
        
        val weakPasswords = listOf(
            "password", // No uppercase, digits, or special chars
            "PASSWORD", // No lowercase, digits, or special chars
            "Password", // No digits or special chars
            "Password1", // No special chars
            "Pass1!", // Too short
            null
        )

        // When & Then
        strongPasswords.forEach { password ->
            assertTrue(ValidationUtils.isStrongPassword(password), "Should validate: $password")
        }
        
        weakPasswords.forEach { password ->
            assertFalse(ValidationUtils.isStrongPassword(password), "Should reject: $password")
        }
    }

    @Test
    fun `should sanitize input correctly`() {
        // Given
        val inputs = mapOf(
            "  normal text  " to "normal text",
            "<script>alert('xss')</script>" to "scriptalert('xss')/script",
            "text with \"quotes\" and 'apostrophes'" to "text with quotes and apostrophes",
            null to null,
            "" to "",
            "   " to ""
        )

        // When & Then
        inputs.forEach { (input, expected) ->
            assertEquals(expected, ValidationUtils.sanitizeInput(input))
        }
    }

    @Test
    fun `should validate JSON strings`() {
        // Given
        val validJson = listOf(
            """{"key": "value"}""",
            """[1, 2, 3]""",
            """"simple string"""",
            "123",
            "true",
            null,
            ""
        )
        
        val invalidJson = listOf(
            """{"key": value}""", // Missing quotes around value
            """[1, 2, 3""", // Missing closing bracket
            """{"key":}""" // Missing value
        )

        // When & Then
        validJson.forEach { json ->
            assertTrue(ValidationUtils.isValidJson(json), "Should validate: $json")
        }
        
        invalidJson.forEach { json ->
            assertFalse(ValidationUtils.isValidJson(json), "Should reject: $json")
        }
    }

    @Test
    fun `should validate page request parameters`() {
        // Given & When & Then
        assertEquals(Pair(0, 20), ValidationUtils.validatePageRequest(null, null))
        assertEquals(Pair(0, 20), ValidationUtils.validatePageRequest(-1, null))
        assertEquals(Pair(5, 50), ValidationUtils.validatePageRequest(5, 50))
        assertEquals(Pair(0, 100), ValidationUtils.validatePageRequest(0, 150)) // Capped at 100
        assertEquals(Pair(0, 1), ValidationUtils.validatePageRequest(0, 0)) // Min 1
    }

    @Test
    fun `should validate sort direction`() {
        // Given & When & Then
        assertEquals("asc", ValidationUtils.validateSortDirection(null))
        assertEquals("asc", ValidationUtils.validateSortDirection(""))
        assertEquals("asc", ValidationUtils.validateSortDirection("asc"))
        assertEquals("asc", ValidationUtils.validateSortDirection("ascending"))
        assertEquals("desc", ValidationUtils.validateSortDirection("desc"))
        assertEquals("desc", ValidationUtils.validateSortDirection("descending"))
        assertEquals("desc", ValidationUtils.validateSortDirection("DESC"))
        assertEquals("asc", ValidationUtils.validateSortDirection("invalid"))
    }

    @Test
    fun `should validate content types`() {
        // Given
        val validTypes = listOf(
            "TEXT", "DIAGRAM", "EXERCISE", "QUIZ", "ASSESSMENT",
            "FLASHCARD", "MIND_MAP", "TUTORIAL", "CODE_EXAMPLE", "FORMULA"
        )
        
        val invalidTypes = listOf(
            "INVALID", "text", null, "", "VIDEO"
        )

        // When & Then
        validTypes.forEach { type ->
            assertTrue(ValidationUtils.isValidContentType(type), "Should validate: $type")
        }
        
        invalidTypes.forEach { type ->
            assertFalse(ValidationUtils.isValidContentType(type), "Should reject: $type")
        }
    }

    @Test
    fun `should validate agent types`() {
        // Given
        val validTypes = listOf(
            "RESEARCH", "VERIFICATION", "DECOMPOSITION", "STRUCTURING",
            "CONTENT_GENERATION", "VALIDATION", "SYNTHESIS", "LEARNING_EXPERIENCE"
        )
        
        val invalidTypes = listOf(
            "INVALID", "research", null, "", "AI_AGENT"
        )

        // When & Then
        validTypes.forEach { type ->
            assertTrue(ValidationUtils.isValidAgentType(type), "Should validate: $type")
        }
        
        invalidTypes.forEach { type ->
            assertFalse(ValidationUtils.isValidAgentType(type), "Should reject: $type")
        }
    }

    @Test
    fun `should validate cost amounts`() {
        // Given & When & Then
        assertTrue(ValidationUtils.validateCostAmount(0.0))
        assertTrue(ValidationUtils.validateCostAmount(10.50))
        assertTrue(ValidationUtils.validateCostAmount(9999.99))
        assertFalse(ValidationUtils.validateCostAmount(-1.0))
        assertFalse(ValidationUtils.validateCostAmount(10001.0))
        assertFalse(ValidationUtils.validateCostAmount(null))
    }

    @Test
    fun `should validate quality scores`() {
        // Given & When & Then
        assertTrue(ValidationUtils.validateQualityScore(null)) // Null is allowed
        assertTrue(ValidationUtils.validateQualityScore(0.0))
        assertTrue(ValidationUtils.validateQualityScore(0.5))
        assertTrue(ValidationUtils.validateQualityScore(1.0))
        assertFalse(ValidationUtils.validateQualityScore(-0.1))
        assertFalse(ValidationUtils.validateQualityScore(1.1))
    }

    @Test
    fun `should validate token counts`() {
        // Given & When & Then
        assertTrue(ValidationUtils.validateTokenCount(null)) // Null is allowed
        assertTrue(ValidationUtils.validateTokenCount(0))
        assertTrue(ValidationUtils.validateTokenCount(1000))
        assertTrue(ValidationUtils.validateTokenCount(999999))
        assertFalse(ValidationUtils.validateTokenCount(-1))
        assertFalse(ValidationUtils.validateTokenCount(1000001))
    }

    @Test
    fun `should validate durations`() {
        // Given & When & Then
        assertTrue(ValidationUtils.validateDuration(null)) // Null is allowed
        assertTrue(ValidationUtils.validateDuration(0))
        assertTrue(ValidationUtils.validateDuration(60))
        assertTrue(ValidationUtils.validateDuration(1440)) // 24 hours
        assertFalse(ValidationUtils.validateDuration(-1))
        assertFalse(ValidationUtils.validateDuration(1441)) // More than 24 hours
    }
}