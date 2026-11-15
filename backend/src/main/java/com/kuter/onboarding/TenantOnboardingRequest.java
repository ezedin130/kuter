package com.kuter.onboarding;

import com.kuter.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

/**
 * TenantOnboardingRequest entity capturing inbound registration requests
 * from building material shops on the public website.
 */
@Entity
@Table(name = "tenant_onboarding_requests")
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TenantOnboardingRequest extends BaseEntity {

    @Column(name = "reference_code", nullable = false, unique = true, length = 50)
    private String referenceCode;

    @Column(name = "shop_name", nullable = false, length = 255)
    private String shopName;

    @Column(name = "owner_full_name", nullable = false, length = 255)
    private String ownerFullName;

    @Column(name = "owner_email", nullable = false, length = 255)
    private String ownerEmail;

    @Column(name = "owner_phone", length = 50)
    private String ownerPhone;

    @Column(name = "requested_plan_code", nullable = false, length = 50)
    private String requestedPlanCode;

    @Column(name = "preferred_shop_code", length = 50)
    private String preferredShopCode;

    @Column(name = "business_address", length = 255)
    private String businessAddress;

    @Column(name = "currency", nullable = false, length = 10)
    @Builder.Default
    private String currency = "ETB";

    @Column(name = "applicant_notes", columnDefinition = "TEXT")
    private String applicantNotes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private OnboardingStatus status = OnboardingStatus.PENDING;

    @Column(name = "reviewer_id")
    private UUID reviewerId;

    @Column(name = "reviewer_notes", columnDefinition = "TEXT")
    private String reviewerNotes;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "created_tenant_id")
    private UUID createdTenantId;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "email_sent_at")
    private Instant emailSentAt;

    @Column(name = "approval_email_sent_at")
    private Instant approvalEmailSentAt;
}
