package com.learningplatform.domain.payment

import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import java.math.BigDecimal
import java.time.Instant
import java.util.*

@Entity
@Table(name = "subscriptions")
data class Subscription(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),
    
    @Column(name = "user_id", nullable = false)
    val userId: UUID,
    
    @Enumerated(EnumType.STRING)
    @Column(name = "plan_type", nullable = false)
    val planType: SubscriptionPlan,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: SubscriptionStatus,
    
    @Column(name = "stripe_subscription_id")
    val stripeSubscriptionId: String? = null,
    
    @Column(name = "current_period_start", nullable = false)
    val currentPeriodStart: Instant,
    
    @Column(name = "current_period_end", nullable = false)
    val currentPeriodEnd: Instant,
    
    @Column(name = "price_per_month", precision = 10, scale = 2)
    @Positive(message = "Price must be positive")
    val pricePerMonth: BigDecimal,
    
    @Column(name = "currency", length = 3, nullable = false)
    @NotBlank(message = "Currency is required")
    val currency: String = "USD",
    
    @Column(name = "trial_end")
    val trialEnd: Instant? = null,
    
    @Column(name = "cancelled_at")
    val cancelledAt: Instant? = null,
    
    @Column(name = "cancellation_reason")
    val cancellationReason: String? = null,
    
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
    
    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now(),
    
    @OneToMany(mappedBy = "subscription", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val paymentEvents: MutableList<PaymentEvent> = mutableListOf(),
    
    @OneToMany(mappedBy = "subscription", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val usageRecords: MutableList<UsageRecord> = mutableListOf()
) {
    fun isActive(): Boolean = status == SubscriptionStatus.ACTIVE
    
    fun isInTrial(): Boolean = trialEnd != null && Instant.now().isBefore(trialEnd)
    
    fun isCancelled(): Boolean = status == SubscriptionStatus.CANCELLED
    
    fun hasFeature(feature: SubscriptionFeature): Boolean {
        return planType.features.contains(feature)
    }
    
    fun getRemainingTrialDays(): Long? {
        return trialEnd?.let { 
            val now = Instant.now()
            if (now.isBefore(it)) {
                java.time.Duration.between(now, it).toDays()
            } else null
        }
    }
    
    fun addPaymentEvent(eventType: PaymentEventType, amount: BigDecimal, description: String? = null): PaymentEvent {
        val event = PaymentEvent(
            subscription = this,
            eventType = eventType,
            amount = amount,
            currency = currency,
            description = description
        )
        paymentEvents.add(event)
        return event
    }
}

@Entity
@Table(name = "payment_events")
data class PaymentEvent(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    val subscription: Subscription,
    
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    val eventType: PaymentEventType,
    
    @Column(precision = 10, scale = 2, nullable = false)
    @Positive(message = "Amount must be positive")
    val amount: BigDecimal,
    
    @Column(length = 3, nullable = false)
    val currency: String,
    
    @Column(name = "stripe_event_id")
    val stripeEventId: String? = null,
    
    @Column(columnDefinition = "TEXT")
    val description: String? = null,
    
    @Column(name = "processed_at")
    val processedAt: Instant? = null,
    
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)

@Entity
@Table(name = "usage_records")
data class UsageRecord(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    val subscription: Subscription,
    
    @Column(name = "usage_type", nullable = false)
    val usageType: String, // e.g., "learning_sessions", "llm_tokens", "storage_gb"
    
    @Column(nullable = false)
    @Positive(message = "Quantity must be positive")
    val quantity: Long,
    
    @Column(name = "unit_cost", precision = 10, scale = 6)
    val unitCost: BigDecimal? = null,
    
    @Column(name = "total_cost", precision = 10, scale = 4)
    val totalCost: BigDecimal? = null,
    
    @Column(name = "recorded_at", nullable = false)
    val recordedAt: Instant = Instant.now(),
    
    @Column(columnDefinition = "JSONB")
    val metadata: String? = null // JSON string for additional usage metadata
)

enum class SubscriptionPlan(val features: Set<SubscriptionFeature>, val maxSessions: Int?) {
    FREE(setOf(SubscriptionFeature.BASIC_LEARNING), 5),
    PRO(setOf(
        SubscriptionFeature.BASIC_LEARNING,
        SubscriptionFeature.ADVANCED_AGENTS,
        SubscriptionFeature.CUSTOM_LLM,
        SubscriptionFeature.PRIORITY_SUPPORT
    ), 50),
    ENTERPRISE(setOf(
        SubscriptionFeature.BASIC_LEARNING,
        SubscriptionFeature.ADVANCED_AGENTS,
        SubscriptionFeature.CUSTOM_LLM,
        SubscriptionFeature.PRIORITY_SUPPORT,
        SubscriptionFeature.TEAM_COLLABORATION,
        SubscriptionFeature.ADVANCED_ANALYTICS,
        SubscriptionFeature.API_ACCESS,
        SubscriptionFeature.WHITE_LABEL
    ), null) // Unlimited
}

enum class SubscriptionFeature {
    BASIC_LEARNING,
    ADVANCED_AGENTS,
    CUSTOM_LLM,
    PRIORITY_SUPPORT,
    TEAM_COLLABORATION,
    ADVANCED_ANALYTICS,
    API_ACCESS,
    WHITE_LABEL
}

enum class SubscriptionStatus {
    ACTIVE,
    CANCELLED,
    PAST_DUE,
    UNPAID,
    TRIALING,
    INCOMPLETE,
    INCOMPLETE_EXPIRED
}

enum class PaymentEventType {
    SUBSCRIPTION_CREATED,
    PAYMENT_SUCCEEDED,
    PAYMENT_FAILED,
    SUBSCRIPTION_UPDATED,
    SUBSCRIPTION_CANCELLED,
    INVOICE_CREATED,
    INVOICE_PAID,
    TRIAL_STARTED,
    TRIAL_ENDED
}