package com.kuter.sales;

import com.kuter.common.ApiResponse;
import com.kuter.common.BadRequestException;
import com.kuter.common.DateTimeUtils;
import com.kuter.sales.dto.CreateSaleRequestDto;
import com.kuter.sales.dto.DailySalesSummaryDto;
import com.kuter.sales.dto.ProductSalesReportDto;
import com.kuter.sales.dto.SaleReceiptResponseDto;
import com.kuter.sales.dto.SaleResponseDto;
import com.kuter.sales.dto.SalesSummaryReportDto;
import com.kuter.sales.dto.VoidSaleRequestDto;
import java.util.List;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * REST controller for retail counter sales, multi-product checkout,
 * stock deduction, sale voiding, printable receipts, and operational sales reports.
 */
@RestController
@RequestMapping("/api/v1/sales")
@RequiredArgsConstructor
public class SaleController {

    private final SaleService saleService;

    @PostMapping
    @PreAuthorize("@tenantSecurity.canCreateSale()")
    public ResponseEntity<ApiResponse<SaleResponseDto>> createSale(@Valid @RequestBody CreateSaleRequestDto request) {
        SaleResponseDto response = saleService.createSale(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Sale completed and stock deducted successfully"));
    }

    @GetMapping
    @PreAuthorize("@tenantSecurity.canReadSales()")
    public ResponseEntity<ApiResponse<Page<SaleResponseDto>>> listSales(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) SaleStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID cashierId,
            @RequestParam(required = false) UUID productId,
            @PageableDefault(size = 20, sort = "soldAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Instant start = DateTimeUtils.parseIsoOrDate(startDate, true);
        Instant end = DateTimeUtils.parseIsoOrDate(endDate, false);
        DateTimeUtils.validateDateRange(start, end);

        Page<SaleResponseDto> response = saleService.listSales(start, end, status, search, cashierId, productId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "Sales retrieved successfully"));
    }

    @GetMapping("/{saleId}")
    @PreAuthorize("@tenantSecurity.canReadSales()")
    public ResponseEntity<ApiResponse<SaleResponseDto>> getSaleById(@PathVariable UUID saleId) {
        SaleResponseDto response = saleService.getSaleById(saleId);
        return ResponseEntity.ok(ApiResponse.success(response, "Sale retrieved successfully"));
    }

    @PostMapping("/{saleId}/void")
    @PreAuthorize("@tenantSecurity.canVoidSale()")
    public ResponseEntity<ApiResponse<SaleResponseDto>> voidSale(
            @PathVariable UUID saleId,
            @Valid @RequestBody VoidSaleRequestDto request) {

        SaleResponseDto response = saleService.voidSale(saleId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Sale voided and stock restored successfully"));
    }

    @GetMapping("/{saleId}/receipt")
    @PreAuthorize("@tenantSecurity.canReadSales()")
    public ResponseEntity<ApiResponse<SaleReceiptResponseDto>> getSaleReceipt(@PathVariable UUID saleId) {
        SaleReceiptResponseDto response = saleService.getSaleReceipt(saleId);
        return ResponseEntity.ok(ApiResponse.success(response, "Sale receipt retrieved successfully"));
    }

    @GetMapping("/reports/summary")
    @PreAuthorize("@tenantSecurity.canReadSales()")
    public ResponseEntity<ApiResponse<SalesSummaryReportDto>> getSalesSummaryReport(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        Instant start = DateTimeUtils.parseIsoOrDate(startDate, true);
        Instant end = DateTimeUtils.parseIsoOrDate(endDate, false);
        DateTimeUtils.validateDateRange(start, end);

        SalesSummaryReportDto response = saleService.getSalesSummaryReport(start, end);
        return ResponseEntity.ok(ApiResponse.success(response, "Sales summary report generated successfully"));
    }

    @GetMapping("/reports/daily")
    @PreAuthorize("@tenantSecurity.canReadSales()")
    public ResponseEntity<ApiResponse<List<DailySalesSummaryDto>>> getDailySalesReports(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        Instant start = DateTimeUtils.parseIsoOrDate(startDate, true);
        Instant end = DateTimeUtils.parseIsoOrDate(endDate, false);
        DateTimeUtils.validateDateRange(start, end);

        List<DailySalesSummaryDto> response = saleService.getDailySalesReports(start, end);
        return ResponseEntity.ok(ApiResponse.success(response, "Daily sales reports generated successfully"));
    }

    @GetMapping("/reports/products")
    @PreAuthorize("@tenantSecurity.canReadSales()")
    public ResponseEntity<ApiResponse<ProductSalesReportDto>> getProductSalesReport(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "20") int limit) {

        Instant start = DateTimeUtils.parseIsoOrDate(startDate, true);
        Instant end = DateTimeUtils.parseIsoOrDate(endDate, false);
        DateTimeUtils.validateDateRange(start, end);

        ProductSalesReportDto response = saleService.getProductSalesReport(start, end, limit);
        return ResponseEntity.ok(ApiResponse.success(response, "Product sales report generated successfully"));
    }
}
