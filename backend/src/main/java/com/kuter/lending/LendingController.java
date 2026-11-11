package com.kuter.lending;

import com.kuter.common.ApiResponse;
import com.kuter.common.Constants;
import com.kuter.common.PageResponse;
import com.kuter.identity.KuterUserPrincipal;
import com.kuter.lending.dto.CancelLoanDto;
import com.kuter.lending.dto.CreateBorrowingRequestDto;
import com.kuter.lending.dto.CreateLoanRequestDto;
import com.kuter.lending.dto.LendingObligationsSummaryDto;
import com.kuter.lending.dto.RecordLoanReturnDto;
import com.kuter.lending.dto.StockLoanResponseDto;
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
public class LendingController {

    private final LendingService lendingService;

    public LendingController(LendingService lendingService) {
        this.lendingService = lendingService;
    }

    @PostMapping("/borrowings")
    @PreAuthorize("@tenantSecurity.hasPermission('LENDING_REQUEST')")
    public ResponseEntity<ApiResponse<StockLoanResponseDto>> recordBorrowing(
            @Valid @RequestBody CreateBorrowingRequestDto dto,
            @AuthenticationPrincipal KuterUserPrincipal principal,
            HttpServletRequest request) {
        StockLoanResponseDto response = lendingService.recordBorrowing(dto, principal, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Borrowing recorded successfully"));
    }

    @GetMapping("/borrowings")
    @PreAuthorize("@tenantSecurity.hasPermission('LENDING_VIEW')")
    public ResponseEntity<ApiResponse<PageResponse<StockLoanResponseDto>>> listBorrowings(
            @RequestParam(required = false) LoanStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<StockLoanResponseDto> response = lendingService.listLoans(LoanDirection.BORROW, status, search, pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "Borrowings retrieved successfully"));
    }

    @PostMapping("/loans")
    @PreAuthorize("@tenantSecurity.hasPermission('LENDING_REQUEST')")
    public ResponseEntity<ApiResponse<StockLoanResponseDto>> recordLoan(
            @Valid @RequestBody CreateLoanRequestDto dto,
            @AuthenticationPrincipal KuterUserPrincipal principal,
            HttpServletRequest request) {
        StockLoanResponseDto response = lendingService.recordLoan(dto, principal, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Loan recorded and stock dispatched successfully"));
    }

    @GetMapping("/loans")
    @PreAuthorize("@tenantSecurity.hasPermission('LENDING_VIEW')")
    public ResponseEntity<ApiResponse<PageResponse<StockLoanResponseDto>>> listLoans(
            @RequestParam(required = false) LoanStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<StockLoanResponseDto> response = lendingService.listLoans(LoanDirection.LOAN, status, search, pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "Loans retrieved successfully"));
    }

    @GetMapping("/lending/obligations")
    @PreAuthorize("@tenantSecurity.hasPermission('LENDING_VIEW')")
    public ResponseEntity<ApiResponse<LendingObligationsSummaryDto>> getObligationsSummary() {
        LendingObligationsSummaryDto response = lendingService.getObligationsSummary();
        return ResponseEntity.ok(ApiResponse.success(response, "Lending obligations summary retrieved successfully"));
    }

    @GetMapping("/lending/{id}")
    @PreAuthorize("@tenantSecurity.hasPermission('LENDING_VIEW')")
    public ResponseEntity<ApiResponse<StockLoanResponseDto>> getLoan(@PathVariable UUID id) {
        StockLoanResponseDto response = lendingService.getLoan(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Loan details retrieved successfully"));
    }

    @PostMapping("/lending/{id}/returns")
    @PreAuthorize("@tenantSecurity.hasPermission('LENDING_MANAGE')")
    public ResponseEntity<ApiResponse<StockLoanResponseDto>> recordReturn(
            @PathVariable UUID id,
            @Valid @RequestBody RecordLoanReturnDto dto,
            @AuthenticationPrincipal KuterUserPrincipal principal,
            HttpServletRequest request) {
        StockLoanResponseDto response = lendingService.recordReturn(id, dto, principal, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Loan return recorded successfully"));
    }

    @PostMapping("/lending/{id}/cancel")
    @PreAuthorize("@tenantSecurity.hasPermission('LENDING_MANAGE')")
    public ResponseEntity<ApiResponse<StockLoanResponseDto>> cancelLoan(
            @PathVariable UUID id,
            @Valid @RequestBody CancelLoanDto dto,
            @AuthenticationPrincipal KuterUserPrincipal principal,
            HttpServletRequest request) {
        StockLoanResponseDto response = lendingService.cancelLoan(id, dto, principal, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Loan cancelled successfully"));
    }
}
