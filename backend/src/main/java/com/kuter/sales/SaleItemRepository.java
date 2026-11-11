package com.kuter.sales;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for SaleItem line items on sales invoices.
 */
@Repository
public interface SaleItemRepository extends JpaRepository<SaleItem, UUID> {

    List<SaleItem> findAllByTenantIdAndSaleId(UUID tenantId, UUID saleId);

    List<SaleItem> findAllByTenantIdAndProductId(UUID tenantId, UUID productId);
}
