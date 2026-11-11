package com.kuter.onboarding;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for TenantOnboardingRequest persistence.
 */
@Repository
public interface TenantOnboardingRequestRepository extends JpaRepository<TenantOnboardingRequest, UUID> {

    Optional<TenantOnboardingRequest> findByReferenceCode(String referenceCode);

    Optional<TenantOnboardingRequest> findByCreatedTenantId(UUID createdTenantId);

    List<TenantOnboardingRequest> findAllByStatus(OnboardingStatus status);

    long countByStatus(OnboardingStatus status);

    Page<TenantOnboardingRequest> findAllByStatus(OnboardingStatus status, Pageable pageable);

    boolean existsByOwnerEmailIgnoreCaseAndStatus(String ownerEmail, OnboardingStatus status);

    @Query("SELECT r FROM TenantOnboardingRequest r WHERE " +
           "(:status IS NULL OR r.status = :status) AND (" +
           ":search IS NULL OR LOWER(r.shopName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(r.ownerFullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(r.referenceCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(r.ownerEmail) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<TenantOnboardingRequest> searchRequests(@Param("status") OnboardingStatus status,
                                                @Param("search") String search,
                                                Pageable pageable);

    @Query("SELECT r FROM TenantOnboardingRequest r WHERE " +
           "LOWER(r.shopName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(r.ownerFullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(r.referenceCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(r.ownerEmail) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<TenantOnboardingRequest> searchRequests(@Param("search") String search,
                                                Pageable pageable);

    @Query("SELECT r FROM TenantOnboardingRequest r WHERE r.status = :status AND (" +
           "LOWER(r.shopName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(r.ownerFullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(r.referenceCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(r.ownerEmail) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<TenantOnboardingRequest> searchRequestsWithStatus(@Param("status") OnboardingStatus status,
                                                           @Param("search") String search,
                                                           Pageable pageable);
}
