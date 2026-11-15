package com.kuter.lending;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StockLoanRepository extends JpaRepository<StockLoan, UUID>, JpaSpecificationExecutor<StockLoan> {

    Optional<StockLoan> findByTenantIdAndId(UUID tenantId, UUID id);

    Optional<StockLoan> findByTenantIdAndLoanReference(UUID tenantId, String loanReference);

    Page<StockLoan> findAllByTenantIdAndDirection(UUID tenantId, LoanDirection direction, Pageable pageable);

    Page<StockLoan> findAllByTenantIdAndDirectionAndStatus(UUID tenantId, LoanDirection direction, LoanStatus status, Pageable pageable);

    List<StockLoan> findAllByTenantIdAndStatusIn(UUID tenantId, List<LoanStatus> statuses);

    default Page<StockLoan> searchLoans(UUID tenantId,
                                        LoanDirection direction,
                                        LoanStatus status,
                                        String search,
                                        Pageable pageable) {
        Specification<StockLoan> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("tenantId"), tenantId));

            if (direction != null) {
                predicates.add(cb.equal(root.get("direction"), direction));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate counterparty = cb.like(cb.lower(root.get("counterpartyName")), pattern);
                Predicate reference = cb.like(cb.lower(root.get("loanReference")), pattern);
                Predicate sku = cb.like(cb.lower(root.get("productSku")), pattern);
                Predicate name = cb.like(cb.lower(root.get("productName")), pattern);
                predicates.add(cb.or(counterparty, reference, sku, name));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return findAll(spec, pageable);
    }

    long countByTenantIdAndDirectionAndStatusIn(UUID tenantId, LoanDirection direction, List<LoanStatus> statuses);
}
