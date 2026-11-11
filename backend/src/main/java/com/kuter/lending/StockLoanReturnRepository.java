package com.kuter.lending;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StockLoanReturnRepository extends JpaRepository<StockLoanReturn, UUID> {

    List<StockLoanReturn> findAllByTenantIdAndLoanIdOrderByReturnDateDesc(UUID tenantId, UUID loanId);

    List<StockLoanReturn> findAllByLoanIdOrderByReturnDateDesc(UUID loanId);
}
