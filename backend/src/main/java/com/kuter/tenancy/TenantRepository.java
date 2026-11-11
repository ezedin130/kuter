package com.kuter.tenancy;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Tenant management.
 */
@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Tenant t WHERE t.id = :id")
    Optional<Tenant> findByIdForUpdate(@Param("id") UUID id);

    Optional<Tenant> findByCode(String code);

    boolean existsByCode(String code);

    long countByStatus(TenantStatus status);

    Page<Tenant> findAllByStatus(TenantStatus status, Pageable pageable);

    @Query("SELECT t FROM Tenant t WHERE " +
           "(:status IS NULL OR t.status = :status) AND (" +
           ":search IS NULL OR LOWER(t.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(t.code) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Tenant> searchTenants(@Param("status") TenantStatus status,
                               @Param("search") String search,
                               Pageable pageable);

    @Query("SELECT t FROM Tenant t WHERE " +
           "LOWER(t.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(t.code) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Tenant> searchTenants(@Param("search") String search,
                               Pageable pageable);

    @Query("SELECT t FROM Tenant t WHERE t.status = :status AND (" +
           "LOWER(t.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(t.code) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Tenant> searchTenantsWithStatus(@Param("status") TenantStatus status,
                                         @Param("search") String search,
                                         Pageable pageable);
}
