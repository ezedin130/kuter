package com.kuter.subscriptions;

import com.kuter.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * SubscriptionPlan entity defining tier limits, pricing, and features.
 */
@Entity
@Table(name = "subscription_plans")
@Getter
@Setter
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionPlan extends BaseEntity {

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "price", nullable = false, precision = 15, scale = 4)
    private BigDecimal price;

    @Column(name = "currency", nullable = false, length = 10)
    @Builder.Default
    private String currency = "ETB";

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_interval", nullable = false, length = 20)
    private BillingInterval billingInterval;

    @Column(name = "product_limit")
    private Integer productLimit;

    @Column(name = "user_limit")
    private Integer userLimit;

    @Column(name = "branch_limit")
    private Integer branchLimit;

    @Column(name = "trial_days")
    @Builder.Default
    private Integer trialDays = 14;

    @Column(name = "is_public", nullable = false)
    @Builder.Default
    private boolean isPublic = true;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "features_summary", columnDefinition = "TEXT")
    private String featuresSummary;
}
