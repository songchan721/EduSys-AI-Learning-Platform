package com.learningplatform.domain.user

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.*

@Entity
@Table(name = "users")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),
    
    @Column(unique = true, nullable = false)
    @Email(message = "Email must be valid")
    @NotBlank(message = "Email is required")
    val email: String,
    
    @JsonIgnore
    @Column(name = "password_hash", nullable = false)
    val passwordHash: String,
    
    @Column(name = "first_name", nullable = false)
    @NotBlank(message = "First name is required")
    @Size(min = 1, max = 100, message = "First name must be between 1 and 100 characters")
    val firstName: String,
    
    @Column(name = "last_name", nullable = false)
    @NotBlank(message = "Last name is required")
    @Size(min = 1, max = 100, message = "Last name must be between 1 and 100 characters")
    val lastName: String,
    
    @Column(name = "email_verified", nullable = false)
    val emailVerified: Boolean = false,
    
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
    
    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now(),
    
    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val roles: MutableSet<UserRole> = mutableSetOf(),
    
    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val preferences: MutableSet<UserPreference> = mutableSetOf()
) {
    fun hasRole(role: Role): Boolean = roles.any { it.role == role }
    
    fun addRole(role: Role, grantedBy: UUID? = null): UserRole {
        val userRole = UserRole(
            user = this,
            role = role,
            grantedBy = grantedBy
        )
        roles.add(userRole)
        return userRole
    }
    
    fun removeRole(role: Role) {
        roles.removeIf { it.role == role }
    }
    
    fun getPreference(key: String): String? {
        return preferences.find { it.preferenceKey == key }?.preferenceValue
    }
    
    fun setPreference(key: String, value: String) {
        val existing = preferences.find { it.preferenceKey == key }
        if (existing != null) {
            preferences.remove(existing)
        }
        preferences.add(UserPreference(
            user = this,
            preferenceKey = key,
            preferenceValue = value
        ))
    }
}

@Entity
@Table(name = "user_roles")
data class UserRole(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val role: Role,
    
    @Column(name = "granted_at", nullable = false)
    val grantedAt: Instant = Instant.now(),
    
    @Column(name = "expires_at")
    val expiresAt: Instant? = null,
    
    @Column(name = "granted_by")
    val grantedBy: UUID? = null
)

@Entity
@Table(name = "user_preferences")
data class UserPreference(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,
    
    @Column(name = "preference_key", nullable = false)
    val preferenceKey: String,
    
    @Column(name = "preference_value", nullable = false, columnDefinition = "TEXT")
    val preferenceValue: String,
    
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
    
    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now()
)

enum class Role {
    FREE_USER,
    PRO_USER,
    ENTERPRISE_USER,
    ADMIN,
    SUPER_ADMIN
}