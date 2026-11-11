package com.kuter.lending;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StockLoanRepository extends JpaRepository<StockLoan, UUID> {

    Optional<StockLoan> findByTenantIdAndId(UUID tenantId, UUID id);

    Optional<StockLoan> findByTenantIdAndLoanReference(UUID tenantId, String loanReference);

    Page<StockLoan> findAllByTenantIdAndDirection(UUID tenantId, LoanDirection direction, Pageable pageable);

    Page<StockLoan> findAllByTenantIdAndDirectionAndStatus(UUID tenantId, LoanDirection direction, LoanStatus status, Pageable pageable);

    List<StockLoan> findAllByTenantIdAndStatusIn(UUID tenantId, List<LoanStatus> statuses);

    @Query("SELECT sl FROM StockLoan sl WHERE sl.tenantId = :tenantId " +
           "AND (:direction IS NULL OR sl.direction = :direction) " +
           "AND (:status IS NULL OR sl.status = :status) " +
           "AND (:search IS NULL OR " +
           "     LOWER(sl.counterpartyName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "     LOWER(sl.loanReference) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "     LOWER(sl.productSku) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "     LOWER(sl.productName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<StockLoan> searchLoans(@Param("tenantId") UUID tenantId,
                                @Param("direction") LoanDirection direction,
                                @Param("status") LoanStatus status,
                                @Param("search") String search,
                                Pageable pageable);

    long countByTenantIdAndDirectionAndStatusIn(UUID tenantId, LoanDirection direction, List<LoanStatus> statuses);
}
