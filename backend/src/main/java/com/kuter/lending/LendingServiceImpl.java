package com.kuter.lending;

import com.kuter.audit.AuditService;
import com.kuter.catalog.Product;
import com.kuter.catalog.ProductRepository;
import com.kuter.catalog.ProductStatus;
import com.kuter.catalog.ProductUnitConversion;
import com.kuter.catalog.ProductUnitConversionRepository;
import com.kuter.catalog.UnitDefinition;
import com.kuter.catalog.UnitDefinitionRepository;
import com.kuter.common.BadRequestException;
import com.kuter.common.PageResponse;
import com.kuter.common.ResourceNotFoundException;
import com.kuter.identity.KuterUserPrincipal;
import com.kuter.inventory.MovementType;
import com.kuter.inventory.StockBalance;
import com.kuter.inventory.StockBalanceRepository;
import com.kuter.inventory.StockMovement;
import com.kuter.inventory.StockMovementRepository;
import com.kuter.lending.dto.CancelLoanDto;
import com.kuter.lending.dto.CreateBorrowingRequestDto;
import com.kuter.lending.dto.CreateLoanRequestDto;
import com.kuter.lending.dto.LendingObligationsSummaryDto;
import com.kuter.lending.dto.ProductObligationDto;
import com.kuter.lending.dto.RecordLoanReturnDto;
import com.kuter.lending.dto.StockLoanResponseDto;
import com.kuter.lending.dto.StockLoanReturnResponseDto;
import com.kuter.tenancy.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.Year;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class LendingServiceImpl implements LendingService {

    private static final Logger log = LoggerFactory.getLogger(LendingServiceImpl.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StockLoanRepository loanRepository;
    private final StockLoanReturnRepository returnRepository;
    private final ProductRepository productRepository;
    private final UnitDefinitionRepository unitRepository;
    private final ProductUnitConversionRepository conversionRepository;
    private final StockBalanceRepository balanceRepository;
    private final StockMovementRepository movementRepository;
    private final AuditService auditService;

    public LendingServiceImpl(StockLoanRepository loanRepository,
                              StockLoanReturnRepository returnRepository,
                              ProductRepository productRepository,
                              UnitDefinitionRepository unitRepository,
                              ProductUnitConversionRepository conversionRepository,
                              StockBalanceRepository balanceRepository,
                              StockMovementRepository movementRepository,
                              AuditService auditService) {
        this.loanRepository = loanRepository;
        this.returnRepository = returnRepository;
        this.productRepository = productRepository;
        this.unitRepository = unitRepository;
        this.conversionRepository = conversionRepository;
        this.balanceRepository = balanceRepository;
        this.movementRepository = movementRepository;
        this.auditService = auditService;
    }

    @Override
    @Transactional
    public StockLoanResponseDto recordBorrowing(CreateBorrowingRequestDto dto, KuterUserPrincipal principal, HttpServletRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        Product product = productRepository.findByTenantIdAndId(tenantId, dto.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + dto.getProductId()));

        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new BadRequestException("Cannot borrow inactive product: " + product.getSku());
        }

        UnitDefinition unit = unitRepository.findByTenantIdAndId(tenantId, dto.getUnitDefinitionId())
                .orElseThrow(() -> new BadRequestException("Unit definition not found with ID: " + dto.getUnitDefinitionId()));

        BigDecimal conversionFactor = resolveConversionFactor(tenantId, product, unit);
        BigDecimal baseQty = dto.getQuantity().multiply(conversionFactor).setScale(4, RoundingMode.HALF_UP);

        // Lock stock balance for this product
        StockBalance balance = balanceRepository.findByTenantIdAndProductIdForUpdate(tenantId, product.getId())
                .orElseGet(() -> balanceRepository.save(StockBalance.builder()
                        .tenantId(tenantId)
                        .productId(product.getId())
                        .quantityOnHand(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                        .averageCost(product.getCostPrice() != null ? product.getCostPrice() : BigDecimal.ZERO)
                        .build()));

        // Increase physical stock for incoming borrowed inventory
        balance.setQuantityOnHand(balance.getQuantityOnHand().add(baseQty).setScale(4, RoundingMode.HALF_UP));
        balanceRepository.save(balance);

        String reference = "BRW-" + Year.now().getValue() + "-" + String.format("%05d", RANDOM.nextInt(100000));
        Instant txDate = dto.getTransactionDate() != null ? dto.getTransactionDate() : Instant.now();

        StockLoan loan = StockLoan.builder()
                .tenantId(tenantId)
                .loanReference(reference)
                .direction(LoanDirection.BORROW)
                .status(LoanStatus.ACTIVE)
                .counterpartyName(dto.getCounterpartyName().trim())
                .counterpartyContactPerson(StringUtils.hasText(dto.getCounterpartyContactPerson()) ? dto.getCounterpartyContactPerson().trim() : null)
                .counterpartyPhone(StringUtils.hasText(dto.getCounterpartyPhone()) ? dto.getCounterpartyPhone().trim() : null)
                .counterpartyAddress(StringUtils.hasText(dto.getCounterpartyAddress()) ? dto.getCounterpartyAddress().trim() : null)
                .productId(product.getId())
                .productSku(product.getSku())
                .productName(product.getName())
                .unitId(unit.getId())
                .unitCode(unit.getCode())
                .conversionFactor(conversionFactor)
                .initialQuantity(dto.getQuantity().setScale(4, RoundingMode.HALF_UP))
                .initialBaseQuantity(baseQty)
                .returnedQuantity(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                .returnedBaseQuantity(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                .outstandingBaseQuantity(baseQty)
                .unitCostSnapshot(balance.getAverageCost())
                .transactionDate(txDate)
                .expectedReturnDate(dto.getExpectedReturnDate())
                .notes(StringUtils.hasText(dto.getNotes()) ? dto.getNotes().trim() : null)
                .createdByUserId(principal != null ? principal.getUserId() : null)
                .build();

        StockLoan savedLoan = loanRepository.save(loan);

        // Record stock movement ledger entry
        StockMovement movement = StockMovement.builder()
                .tenantId(tenantId)
                .productId(product.getId())
                .movementType(MovementType.BORROW_RECEIPT)
                .quantityChange(baseQty)
                .balanceAfter(balance.getQuantityOnHand())
                .unitCost(balance.getAverageCost())
                .referenceId(savedLoan.getId())
                .referenceType("STOCK_LOAN")
                .notes("Borrowed " + dto.getQuantity() + " " + unit.getCode() + " from " + dto.getCounterpartyName())
                .build();
        movementRepository.save(movement);

        logAudit("STOCK_BORROWED", savedLoan.getId(),
                "Recorded borrowing of " + dto.getQuantity() + " " + unit.getCode() + " (" + baseQty + " base units) from " + dto.getCounterpartyName(),
                principal, request);
        log.info("Recorded borrowing {} for tenant {}: {} {} of SKU {}", reference, tenantId, dto.getQuantity(), unit.getCode(), product.getSku());

        return mapToResponseDto(savedLoan, List.of());
    }

    @Override
    @Transactional
    public StockLoanResponseDto recordLoan(CreateLoanRequestDto dto, KuterUserPrincipal principal, HttpServletRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        Product product = productRepository.findByTenantIdAndId(tenantId, dto.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + dto.getProductId()));

        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new BadRequestException("Cannot loan inactive product: " + product.getSku());
        }

        UnitDefinition unit = unitRepository.findByTenantIdAndId(tenantId, dto.getUnitDefinitionId())
                .orElseThrow(() -> new BadRequestException("Unit definition not found with ID: " + dto.getUnitDefinitionId()));

        BigDecimal conversionFactor = resolveConversionFactor(tenantId, product, unit);
        BigDecimal baseQty = dto.getQuantity().multiply(conversionFactor).setScale(4, RoundingMode.HALF_UP);

        // Lock stock balance for this product
        StockBalance balance = balanceRepository.findByTenantIdAndProductIdForUpdate(tenantId, product.getId())
                .orElseThrow(() -> new BadRequestException("Product '" + product.getSku() + "' has no stock balance record. Cannot loan out stock."));

        if (balance.getQuantityOnHand().compareTo(baseQty) < 0) {
            throw new BadRequestException("Insufficient available stock to loan product '" + product.getSku() +
                    "'. Available on hand: " + balance.getQuantityOnHand() + ", requested to loan: " + baseQty);
        }

        // Deduct physical stock for outgoing loan
        balance.setQuantityOnHand(balance.getQuantityOnHand().subtract(baseQty).setScale(4, RoundingMode.HALF_UP));
        balanceRepository.save(balance);

        String reference = "LN-" + Year.now().getValue() + "-" + String.format("%05d", RANDOM.nextInt(100000));
        Instant txDate = dto.getTransactionDate() != null ? dto.getTransactionDate() : Instant.now();

        StockLoan loan = StockLoan.builder()
                .tenantId(tenantId)
                .loanReference(reference)
                .direction(LoanDirection.LOAN)
                .status(LoanStatus.ACTIVE)
                .counterpartyName(dto.getCounterpartyName().trim())
                .counterpartyContactPerson(StringUtils.hasText(dto.getCounterpartyContactPerson()) ? dto.getCounterpartyContactPerson().trim() : null)
                .counterpartyPhone(StringUtils.hasText(dto.getCounterpartyPhone()) ? dto.getCounterpartyPhone().trim() : null)
                .counterpartyAddress(StringUtils.hasText(dto.getCounterpartyAddress()) ? dto.getCounterpartyAddress().trim() : null)
                .productId(product.getId())
                .productSku(product.getSku())
                .productName(product.getName())
                .unitId(unit.getId())
                .unitCode(unit.getCode())
                .conversionFactor(conversionFactor)
                .initialQuantity(dto.getQuantity().setScale(4, RoundingMode.HALF_UP))
                .initialBaseQuantity(baseQty)
                .returnedQuantity(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                .returnedBaseQuantity(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                .outstandingBaseQuantity(baseQty)
                .unitCostSnapshot(balance.getAverageCost())
                .transactionDate(txDate)
                .expectedReturnDate(dto.getExpectedReturnDate())
                .notes(StringUtils.hasText(dto.getNotes()) ? dto.getNotes().trim() : null)
                .createdByUserId(principal != null ? principal.getUserId() : null)
                .build();

        StockLoan savedLoan = loanRepository.save(loan);

        // Record stock movement ledger entry
        StockMovement movement = StockMovement.builder()
                .tenantId(tenantId)
                .productId(product.getId())
                .movementType(MovementType.LOAN_DISPATCH)
                .quantityChange(baseQty.negate())
                .balanceAfter(balance.getQuantityOnHand())
                .unitCost(balance.getAverageCost())
                .referenceId(savedLoan.getId())
                .referenceType("STOCK_LOAN")
                .notes("Loaned " + dto.getQuantity() + " " + unit.getCode() + " to " + dto.getCounterpartyName())
                .build();
        movementRepository.save(movement);

        logAudit("STOCK_LOANED", savedLoan.getId(),
                "Recorded outgoing loan of " + dto.getQuantity() + " " + unit.getCode() + " (" + baseQty + " base units) to " + dto.getCounterpartyName(),
                principal, request);
        log.info("Recorded outgoing loan {} for tenant {}: {} {} of SKU {}", reference, tenantId, dto.getQuantity(), unit.getCode(), product.getSku());

        return mapToResponseDto(savedLoan, List.of());
    }

    @Override
    @Transactional
    public StockLoanResponseDto recordReturn(UUID loanId, RecordLoanReturnDto dto, KuterUserPrincipal principal, HttpServletRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        StockLoan loan = loanRepository.findByTenantIdAndId(tenantId, loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Stock loan record not found with ID: " + loanId));

        if (loan.getStatus() == LoanStatus.RETURNED || loan.getStatus() == LoanStatus.CANCELLED) {
            throw new BadRequestException("Cannot return stock for a loan with status: " + loan.getStatus());
        }

        BigDecimal conversionFactor = loan.getConversionFactor();
        if (dto.getUnitDefinitionId() != null && !dto.getUnitDefinitionId().equals(loan.getUnitId())) {
            UnitDefinition unit = unitRepository.findByTenantIdAndId(tenantId, dto.getUnitDefinitionId())
                    .orElseThrow(() -> new BadRequestException("Unit definition not found with ID: " + dto.getUnitDefinitionId()));
            Product product = productRepository.findByTenantIdAndId(tenantId, loan.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + loan.getProductId()));
            conversionFactor = resolveConversionFactor(tenantId, product, unit);
        }

        BigDecimal returnBaseQty = dto.getQuantity().multiply(conversionFactor).setScale(4, RoundingMode.HALF_UP);

        if (returnBaseQty.compareTo(loan.getOutstandingBaseQuantity()) > 0) {
            throw new BadRequestException("Return quantity (" + returnBaseQty + " base units) exceeds outstanding obligation of " +
                    loan.getOutstandingBaseQuantity() + " base units");
        }

        // Lock product stock balance
        StockBalance balance = balanceRepository.findByTenantIdAndProductIdForUpdate(tenantId, loan.getProductId())
                .orElseThrow(() -> new BadRequestException("Product stock balance not found for product: " + loan.getProductSku()));

        MovementType movementType;
        BigDecimal movementQtyChange;

        if (loan.getDirection() == LoanDirection.BORROW) {
            // Local shop is returning borrowed goods to external supplier -> physical stock decreases
            if (balance.getQuantityOnHand().compareTo(returnBaseQty) < 0) {
                throw new BadRequestException("Cannot return " + returnBaseQty + " base units of '" + loan.getProductSku() +
                        "'. Only " + balance.getQuantityOnHand() + " base units physically available on hand. Fresh stock must be received before returning.");
            }
            balance.setQuantityOnHand(balance.getQuantityOnHand().subtract(returnBaseQty).setScale(4, RoundingMode.HALF_UP));
            movementType = MovementType.BORROW_RETURN_DISPATCH;
            movementQtyChange = returnBaseQty.negate();
        } else {
            // External borrower is returning goods to local shop -> physical stock increases
            balance.setQuantityOnHand(balance.getQuantityOnHand().add(returnBaseQty).setScale(4, RoundingMode.HALF_UP));
            movementType = MovementType.LOAN_RETURN_RECEIPT;
            movementQtyChange = returnBaseQty;
        }

        balanceRepository.save(balance);

        String returnRef = "RET-" + Year.now().getValue() + "-" + String.format("%05d", RANDOM.nextInt(100000));
        Instant returnDate = dto.getReturnDate() != null ? dto.getReturnDate() : Instant.now();

        StockLoanReturn returnRecord = StockLoanReturn.builder()
                .loanId(loan.getId())
                .tenantId(tenantId)
                .returnReference(returnRef)
                .returnQuantity(dto.getQuantity().setScale(4, RoundingMode.HALF_UP))
                .returnBaseQuantity(returnBaseQty)
                .unitId(dto.getUnitDefinitionId() != null ? dto.getUnitDefinitionId() : loan.getUnitId())
                .unitCode(loan.getUnitCode())
                .returnDate(returnDate)
                .notes(StringUtils.hasText(dto.getNotes()) ? dto.getNotes().trim() : null)
                .receivedByUserId(principal != null ? principal.getUserId() : null)
                .build();

        returnRepository.save(returnRecord);

        // Update loan obligation
        loan.setReturnedBaseQuantity(loan.getReturnedBaseQuantity().add(returnBaseQty).setScale(4, RoundingMode.HALF_UP));
        loan.setReturnedQuantity(loan.getReturnedQuantity().add(dto.getQuantity()).setScale(4, RoundingMode.HALF_UP));
        loan.setOutstandingBaseQuantity(loan.getInitialBaseQuantity().subtract(loan.getReturnedBaseQuantity()).setScale(4, RoundingMode.HALF_UP));

        if (loan.getOutstandingBaseQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            loan.setStatus(LoanStatus.RETURNED);
        } else {
            loan.setStatus(LoanStatus.PARTIALLY_RETURNED);
        }

        StockLoan updatedLoan = loanRepository.save(loan);

        // Record stock movement
        StockMovement movement = StockMovement.builder()
                .tenantId(tenantId)
                .productId(loan.getProductId())
                .movementType(movementType)
                .quantityChange(movementQtyChange)
                .balanceAfter(balance.getQuantityOnHand())
                .unitCost(loan.getUnitCostSnapshot())
                .referenceId(updatedLoan.getId())
                .referenceType("STOCK_LOAN_RETURN")
                .notes("Return record " + returnRef + " for loan " + loan.getLoanReference() + " (" + dto.getQuantity() + " " + loan.getUnitCode() + ")")
                .build();
        movementRepository.save(movement);

        logAudit("LOAN_RETURN_RECORDED", updatedLoan.getId(),
                "Recorded return of " + dto.getQuantity() + " " + loan.getUnitCode() + " for loan " + loan.getLoanReference() +
                        ", remaining obligation: " + updatedLoan.getOutstandingBaseQuantity(),
                principal, request);
        log.info("Recorded return for loan {} in tenant {}: {} units (remaining: {})",
                loan.getLoanReference(), tenantId, returnBaseQty, updatedLoan.getOutstandingBaseQuantity());

        List<StockLoanReturn> returns = returnRepository.findAllByTenantIdAndLoanIdOrderByReturnDateDesc(tenantId, loan.getId());
        return mapToResponseDto(updatedLoan, returns);
    }

    @Override
    @Transactional
    public StockLoanResponseDto cancelLoan(UUID loanId, CancelLoanDto dto, KuterUserPrincipal principal, HttpServletRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        StockLoan loan = loanRepository.findByTenantIdAndId(tenantId, loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Stock loan record not found with ID: " + loanId));

        if (loan.getStatus() != LoanStatus.ACTIVE) {
            throw new BadRequestException("Only ACTIVE loans with no return activity can be cancelled. Current status: " + loan.getStatus());
        }

        if (loan.getReturnedBaseQuantity().compareTo(BigDecimal.ZERO) > 0) {
            throw new BadRequestException("Cannot cancel loan with existing return records.");
        }

        StockBalance balance = balanceRepository.findByTenantIdAndProductIdForUpdate(tenantId, loan.getProductId())
                .orElseThrow(() -> new BadRequestException("Stock balance not found for product: " + loan.getProductSku()));

        if (loan.getDirection() == LoanDirection.LOAN) {
            // Restore physical stock that was loaned out
            balance.setQuantityOnHand(balance.getQuantityOnHand().add(loan.getInitialBaseQuantity()).setScale(4, RoundingMode.HALF_UP));
            balanceRepository.save(balance);

            StockMovement movement = StockMovement.builder()
                    .tenantId(tenantId)
                    .productId(loan.getProductId())
                    .movementType(MovementType.LOAN_RETURN_RECEIPT)
                    .quantityChange(loan.getInitialBaseQuantity())
                    .balanceAfter(balance.getQuantityOnHand())
                    .unitCost(loan.getUnitCostSnapshot())
                    .referenceId(loan.getId())
                    .referenceType("STOCK_LOAN_CANCEL")
                    .notes("Cancelled loan " + loan.getLoanReference() + " (stock restored): " + dto.getReason())
                    .build();
            movementRepository.save(movement);
        } else {
            // Cancel incoming borrowing: deduct un-returned stock from physical on-hand
            if (balance.getQuantityOnHand().compareTo(loan.getInitialBaseQuantity()) < 0) {
                throw new BadRequestException("Cannot cancel borrowing. Physical stock has already been consumed or sold.");
            }
            balance.setQuantityOnHand(balance.getQuantityOnHand().subtract(loan.getInitialBaseQuantity()).setScale(4, RoundingMode.HALF_UP));
            balanceRepository.save(balance);

            StockMovement movement = StockMovement.builder()
                    .tenantId(tenantId)
                    .productId(loan.getProductId())
                    .movementType(MovementType.BORROW_RETURN_DISPATCH)
                    .quantityChange(loan.getInitialBaseQuantity().negate())
                    .balanceAfter(balance.getQuantityOnHand())
                    .unitCost(loan.getUnitCostSnapshot())
                    .referenceId(loan.getId())
                    .referenceType("STOCK_LOAN_CANCEL")
                    .notes("Cancelled borrowing " + loan.getLoanReference() + " (stock removed): " + dto.getReason())
                    .build();
            movementRepository.save(movement);
        }

        loan.setStatus(LoanStatus.CANCELLED);
        loan.setCancellationReason(dto.getReason().trim());
        StockLoan cancelled = loanRepository.save(loan);

        logAudit("LOAN_CANCELLED", loan.getId(), "Cancelled loan " + loan.getLoanReference() + ": " + dto.getReason(), principal, request);
        return mapToResponseDto(cancelled, List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<StockLoanResponseDto> listLoans(LoanDirection direction, LoanStatus status, String search, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        String cleanSearch = StringUtils.hasText(search) ? search.trim() : null;

        Page<StockLoan> page = loanRepository.searchLoans(tenantId, direction, status, cleanSearch, pageable);
        List<StockLoanResponseDto> content = page.getContent().stream()
                .map(loan -> mapToResponseDto(loan, List.of()))
                .toList();

        return PageResponse.of(page, content);
    }

    @Override
    @Transactional(readOnly = true)
    public StockLoanResponseDto getLoan(UUID loanId) {
        UUID tenantId = TenantContext.requireTenantId();
        StockLoan loan = loanRepository.findByTenantIdAndId(tenantId, loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Stock loan record not found with ID: " + loanId));

        List<StockLoanReturn> returns = returnRepository.findAllByTenantIdAndLoanIdOrderByReturnDateDesc(tenantId, loan.getId());
        return mapToResponseDto(loan, returns);
    }

    @Override
    @Transactional(readOnly = true)
    public LendingObligationsSummaryDto getObligationsSummary() {
        UUID tenantId = TenantContext.requireTenantId();

        List<StockLoan> activeLoans = loanRepository.findAllByTenantIdAndStatusIn(
                tenantId, List.of(LoanStatus.ACTIVE, LoanStatus.PARTIALLY_RETURNED));

        long totalBorrowings = 0;
        long totalLoans = 0;
        BigDecimal totalBorrowedUnits = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        BigDecimal totalLoanedUnits = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

        Map<UUID, ProductObligationDto> productMap = new HashMap<>();

        for (StockLoan loan : activeLoans) {
            BigDecimal outstanding = loan.getOutstandingBaseQuantity();
            if (loan.getDirection() == LoanDirection.BORROW) {
                totalBorrowings++;
                totalBorrowedUnits = totalBorrowedUnits.add(outstanding).setScale(4, RoundingMode.HALF_UP);
            } else {
                totalLoans++;
                totalLoanedUnits = totalLoanedUnits.add(outstanding).setScale(4, RoundingMode.HALF_UP);
            }

            ProductObligationDto prodDto = productMap.computeIfAbsent(loan.getProductId(), pId -> ProductObligationDto.builder()
                    .productId(pId)
                    .productSku(loan.getProductSku())
                    .productName(loan.getProductName())
                    .baseUnitCode(loan.getUnitCode())
                    .totalBorrowedBaseQuantity(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                    .totalLoanedBaseQuantity(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                    .netObligationBaseQuantity(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                    .build());

            if (loan.getDirection() == LoanDirection.BORROW) {
                prodDto.setTotalBorrowedBaseQuantity(prodDto.getTotalBorrowedBaseQuantity().add(outstanding).setScale(4, RoundingMode.HALF_UP));
            } else {
                prodDto.setTotalLoanedBaseQuantity(prodDto.getTotalLoanedBaseQuantity().add(outstanding).setScale(4, RoundingMode.HALF_UP));
            }

            prodDto.setNetObligationBaseQuantity(prodDto.getTotalBorrowedBaseQuantity().subtract(prodDto.getTotalLoanedBaseQuantity()).setScale(4, RoundingMode.HALF_UP));
        }

        return LendingObligationsSummaryDto.builder()
                .totalActiveBorrowings(totalBorrowings)
                .totalActiveLoans(totalLoans)
                .totalOutstandingBorrowedBaseUnits(totalBorrowedUnits)
                .totalOutstandingLoanedBaseUnits(totalLoanedUnits)
                .productObligations(new ArrayList<>(productMap.values()))
                .build();
    }

    private BigDecimal resolveConversionFactor(UUID tenantId, Product product, UnitDefinition unit) {
        if (unit.getId().equals(product.getBaseUnitId())) {
            return BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP);
        }
        ProductUnitConversion conversion = conversionRepository
                .findByTenantIdAndProductIdAndUnitId(tenantId, product.getId(), unit.getId())
                .orElseThrow(() -> new BadRequestException(
                        "Unit '" + unit.getCode() + "' is not configured as a valid unit conversion for product '" + product.getSku() + "'"
                ));
        return conversion.getConversionFactor().setScale(4, RoundingMode.HALF_UP);
    }

    private StockLoanResponseDto mapToResponseDto(StockLoan loan, List<StockLoanReturn> returns) {
        List<StockLoanReturnResponseDto> returnDtos = returns.stream().map(r -> StockLoanReturnResponseDto.builder()
                .id(r.getId())
                .loanId(r.getLoanId())
                .returnReference(r.getReturnReference())
                .returnQuantity(r.getReturnQuantity())
                .returnBaseQuantity(r.getReturnBaseQuantity())
                .unitId(r.getUnitId())
                .unitCode(r.getUnitCode())
                .returnDate(r.getReturnDate())
                .notes(r.getNotes())
                .receivedByUserId(r.getReceivedByUserId())
                .createdAt(r.getCreatedAt())
                .build()).toList();

        return StockLoanResponseDto.builder()
                .id(loan.getId())
                .tenantId(loan.getTenantId())
                .loanReference(loan.getLoanReference())
                .direction(loan.getDirection())
                .status(loan.getStatus())
                .counterpartyName(loan.getCounterpartyName())
                .counterpartyContactPerson(loan.getCounterpartyContactPerson())
                .counterpartyPhone(loan.getCounterpartyPhone())
                .counterpartyAddress(loan.getCounterpartyAddress())
                .productId(loan.getProductId())
                .productSku(loan.getProductSku())
                .productName(loan.getProductName())
                .unitId(loan.getUnitId())
                .unitCode(loan.getUnitCode())
                .conversionFactor(loan.getConversionFactor())
                .initialQuantity(loan.getInitialQuantity())
                .initialBaseQuantity(loan.getInitialBaseQuantity())
                .returnedQuantity(loan.getReturnedQuantity())
                .returnedBaseQuantity(loan.getReturnedBaseQuantity())
                .outstandingBaseQuantity(loan.getOutstandingBaseQuantity())
                .unitCostSnapshot(loan.getUnitCostSnapshot())
                .transactionDate(loan.getTransactionDate())
                .expectedReturnDate(loan.getExpectedReturnDate())
                .notes(loan.getNotes())
                .cancellationReason(loan.getCancellationReason())
                .createdByUserId(loan.getCreatedByUserId())
                .createdAt(loan.getCreatedAt())
                .updatedAt(loan.getUpdatedAt())
                .returns(returnDtos)
                .build();
    }

    private void logAudit(String action, UUID entityId, String details, KuterUserPrincipal principal, HttpServletRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID actorId = principal != null ? principal.getUserId() : null;
        String actorEmail = principal != null ? principal.getEmail() : "system";
        String ip = request != null ? request.getRemoteAddr() : "127.0.0.1";
        auditService.logEvent(tenantId, actorId, actorEmail, action, "STOCK_LOAN", entityId.toString(), details, ip);
    }
}
