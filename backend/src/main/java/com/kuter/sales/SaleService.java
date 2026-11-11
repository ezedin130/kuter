package com.kuter.sales;

import com.kuter.sales.dto.CreateSaleRequestDto;
import com.kuter.sales.dto.ProductSalesReportDto;
import com.kuter.sales.dto.SaleReceiptResponseDto;
import com.kuter.sales.dto.SaleResponseDto;
import com.kuter.sales.dto.SalesSummaryReportDto;
import com.kuter.sales.dto.VoidSaleRequestDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.UUID;

/**
 * Service interface for counter sales processing, stock deduction,
 * voiding with compensating movements, and operational sales reporting.
 */
public interface SaleService {

    /**
     * Atomically creates and finalizes a counter sale, validates units,
     * checks stock availability, deducts inventory under pessimistic write lock,
     * captures WAC cost snapshot, logs stock movements, and records audit logs.
     */
    SaleResponseDto createSale(CreateSaleRequestDto request);

    /**
     * Retrieves an individual sale by ID within the active tenant.
     */
    SaleResponseDto getSaleById(UUID saleId);

    /**
     * Lists sales with pagination and optional filtering by date range, status, cashier, product, or invoice query.
     */
    Page<SaleResponseDto> listSales(Instant startDate, Instant endDate, SaleStatus status,
                                    String search, UUID cashierId, UUID productId, Pageable pageable);

    default Page<SaleResponseDto> listSales(Instant startDate, Instant endDate, SaleStatus status,
                                            String search, UUID cashierId, Pageable pageable) {
        return listSales(startDate, endDate, status, search, cashierId, null, pageable);
    }

    /**
     * Voids an existing completed sale, releasing stock back into inventory,
     * creating compensating movements, restoring weighted-average valuation,
     * and recording an immutable audit event.
     */
    SaleResponseDto voidSale(UUID saleId, VoidSaleRequestDto request);

    /**
     * Retrieves operational receipt data suitable for client-side rendering or printing.
     * Contains no payment or financial cost fields.
     */
    SaleReceiptResponseDto getSaleReceipt(UUID saleId);

    /**
     * Generates an operational sales summary report for a date range with daily breakdowns.
     */
    SalesSummaryReportDto getSalesSummaryReport(Instant startDate, Instant endDate);

    /**
     * Retrieves daily sales breakdown items for charting and interval analysis.
     */
    java.util.List<com.kuter.sales.dto.DailySalesSummaryDto> getDailySalesReports(Instant startDate, Instant endDate);

    /**
     * Generates product sales rankings (quantities and revenue) for a date range.
     */
    ProductSalesReportDto getProductSalesReport(Instant startDate, Instant endDate, int limit);
}
