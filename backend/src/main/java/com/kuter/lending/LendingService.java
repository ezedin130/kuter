package com.kuter.lending;

import com.kuter.common.PageResponse;
import com.kuter.identity.KuterUserPrincipal;
import com.kuter.lending.dto.CancelLoanDto;
import com.kuter.lending.dto.CreateBorrowingRequestDto;
import com.kuter.lending.dto.CreateLoanRequestDto;
import com.kuter.lending.dto.LendingObligationsSummaryDto;
import com.kuter.lending.dto.RecordLoanReturnDto;
import com.kuter.lending.dto.StockLoanResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface LendingService {

    StockLoanResponseDto recordBorrowing(CreateBorrowingRequestDto dto, KuterUserPrincipal principal, HttpServletRequest request);

    StockLoanResponseDto recordLoan(CreateLoanRequestDto dto, KuterUserPrincipal principal, HttpServletRequest request);

    StockLoanResponseDto recordReturn(UUID loanId, RecordLoanReturnDto dto, KuterUserPrincipal principal, HttpServletRequest request);

    StockLoanResponseDto cancelLoan(UUID loanId, CancelLoanDto dto, KuterUserPrincipal principal, HttpServletRequest request);

    PageResponse<StockLoanResponseDto> listLoans(LoanDirection direction, LoanStatus status, String search, Pageable pageable);

    StockLoanResponseDto getLoan(UUID loanId);

    LendingObligationsSummaryDto getObligationsSummary();
}
