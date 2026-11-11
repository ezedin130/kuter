package com.kuter.inventory;

import com.kuter.common.ApiResponse;
import com.kuter.common.Constants;
import com.kuter.common.DateTimeUtils;
import com.kuter.common.PageResponse;
import com.kuter.identity.KuterUserPrincipal;
import com.kuter.inventory.dto.CreateStockAdjustmentDto;
import com.kuter.inventory.dto.CreateStockReceivingDto;
import com.kuter.inventory.dto.CurrentInventoryItemDto;
import com.kuter.inventory.dto.InventoryValuationReport;
import com.kuter.inventory.dto.StockAdjustmentResponse;
import com.kuter.inventory.dto.StockBalanceResponse;
import com.kuter.inventory.dto.StockMovementResponse;
import com.kuter.inventory.dto.StockReceivingResponse;
import java.time.Instant;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(Constants.API_BASE_PATH + "/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    // --- Inbound Stock Receiving ---

    @PostMapping("/receipts")
    @PreAuthorize("@tenantSecurity.canReceiveStock()")
    public ResponseEntity<ApiResponse<StockReceivingResponse>> receiveStock(
            @Valid @RequestBody CreateStockReceivingDto dto,
            @AuthenticationPrincipal KuterUserPrincipal principal,
            HttpServletRequest request) {
        StockReceivingResponse response = inventoryService.receiveStock(dto, principal, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Stock received and inventory balance updated successfully"));
    }

    @GetMapping("/receipts")
    @PreAuthorize("@tenantSecurity.canReadInventory()")
    public ResponseEntity<ApiResponse<PageResponse<StockReceivingResponse>>> listReceivings(
            @PageableDefault(size = 20, sort = "receivedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<StockReceivingResponse> response = inventoryService.listReceivings(pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "Stock receiving records retrieved successfully"));
    }

    @GetMapping("/receipts/{id}")
    @PreAuthorize("@tenantSecurity.canReadInventory()")
    public ResponseEntity<ApiResponse<StockReceivingResponse>> getReceiving(@PathVariable UUID id) {
        StockReceivingResponse response = inventoryService.getReceiving(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Stock receiving record retrieved successfully"));
    }

    // --- Stock Adjustments ---

    @PostMapping("/adjustments")
    @PreAuthorize("@tenantSecurity.canAdjustStock()")
    public ResponseEntity<ApiResponse<StockAdjustmentResponse>> adjustStock(
            @Valid @RequestBody CreateStockAdjustmentDto dto,
            @AuthenticationPrincipal KuterUserPrincipal principal,
            HttpServletRequest request) {
        StockAdjustmentResponse response = inventoryService.adjustStock(dto, principal, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Stock adjustment recorded successfully"));
    }

    @GetMapping("/adjustments")
    @PreAuthorize("@tenantSecurity.canReadInventory()")
    public ResponseEntity<ApiResponse<PageResponse<StockAdjustmentResponse>>> listAdjustments(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<StockAdjustmentResponse> response = inventoryService.listAdjustments(pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "Stock adjustment records retrieved successfully"));
    }

    @GetMapping("/adjustments/{id}")
    @PreAuthorize("@tenantSecurity.canReadInventory()")
    public ResponseEntity<ApiResponse<StockAdjustmentResponse>> getAdjustment(@PathVariable UUID id) {
        StockAdjustmentResponse response = inventoryService.getAdjustment(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Stock adjustment record retrieved successfully"));
    }

    // --- Inventory Balances ---

    @GetMapping("/balances")
    @PreAuthorize("@tenantSecurity.canReadInventory()")
    public ResponseEntity<ApiResponse<PageResponse<StockBalanceResponse>>> listBalances(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) Boolean lowStockOnly,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        PageResponse<StockBalanceResponse> response = inventoryService.listBalances(categoryId, lowStockOnly, search, pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "Inventory balances retrieved successfully"));
    }

    @GetMapping("/balances/{productId}")
    @PreAuthorize("@tenantSecurity.canReadInventory()")
    public ResponseEntity<ApiResponse<StockBalanceResponse>> getBalance(@PathVariable UUID productId) {
        StockBalanceResponse response = inventoryService.getBalance(productId);
        return ResponseEntity.ok(ApiResponse.success(response, "Stock balance retrieved successfully"));
    }

    // --- Movement History Ledger ---

    @GetMapping({"/movements", "/reports/movements"})
    @PreAuthorize("@tenantSecurity.canReadInventory()")
    public ResponseEntity<ApiResponse<PageResponse<StockMovementResponse>>> listMovements(
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) MovementType movementType,
            @RequestParam(required = false) String referenceType,
            @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Instant start = DateTimeUtils.parseIsoOrDate(startDate, true);
        Instant end = DateTimeUtils.parseIsoOrDate(endDate, false);
        DateTimeUtils.validateDateRange(start, end);

        PageResponse<StockMovementResponse> response = inventoryService.listMovements(
                productId, movementType, referenceType, actorId, start, end, search, pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "Stock movements retrieved successfully"));
    }

    // --- Reports ---

    @GetMapping("/reports/current")
    @PreAuthorize("@tenantSecurity.canReadInventory()")
    public ResponseEntity<ApiResponse<PageResponse<CurrentInventoryItemDto>>> getCurrentInventoryReport(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String stockStatus,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        PageResponse<CurrentInventoryItemDto> response = inventoryService.getCurrentInventoryReport(categoryId, search, stockStatus, pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "Current inventory report generated successfully"));
    }

    @GetMapping("/reports/valuation")
    @PreAuthorize("@tenantSecurity.canReadInventory()")
    public ResponseEntity<ApiResponse<InventoryValuationReport>> getValuationReport() {
        InventoryValuationReport report = inventoryService.getValuationReport();
        return ResponseEntity.ok(ApiResponse.success(report, "Inventory valuation report generated successfully"));
    }

    @GetMapping("/reports/valuation/items")
    @PreAuthorize("@tenantSecurity.canReadInventory()")
    public ResponseEntity<ApiResponse<PageResponse<CurrentInventoryItemDto>>> getValuationItemsReport(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "totalValuation", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<CurrentInventoryItemDto> response = inventoryService.getDetailedValuationReport(categoryId, search, pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "Inventory valuation items report generated successfully"));
    }

    @GetMapping("/reports/low-stock")
    @PreAuthorize("@tenantSecurity.canReadInventory()")
    public ResponseEntity<ApiResponse<PageResponse<StockBalanceResponse>>> getLowStockReport(
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        PageResponse<StockBalanceResponse> response = inventoryService.getLowStockReport(pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "Low-stock report retrieved successfully"));
    }
}
