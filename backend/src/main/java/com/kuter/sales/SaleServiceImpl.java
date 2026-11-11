package com.kuter.sales;

import com.kuter.audit.AuditService;
import com.kuter.catalog.Product;
import com.kuter.catalog.ProductRepository;
import com.kuter.catalog.ProductStatus;
import com.kuter.catalog.ProductUnitConversion;
import com.kuter.catalog.ProductUnitConversionRepository;
import com.kuter.catalog.UnitDefinition;
import com.kuter.catalog.UnitDefinitionRepository;
import com.kuter.common.BadRequestException;
import com.kuter.common.ResourceNotFoundException;
import com.kuter.identity.KuterUserPrincipal;
import com.kuter.identity.TenantRole;
import com.kuter.identity.User;
import com.kuter.identity.UserRepository;
import com.kuter.inventory.MovementType;
import com.kuter.inventory.StockBalance;
import com.kuter.inventory.StockBalanceRepository;
import com.kuter.inventory.StockMovement;
import com.kuter.inventory.StockMovementRepository;
import com.kuter.sales.dto.CreateSaleItemRequestDto;
import com.kuter.sales.dto.CreateSaleRequestDto;
import com.kuter.sales.dto.DailySalesSummaryDto;
import com.kuter.sales.dto.ProductSalesMetricDto;
import com.kuter.sales.dto.ProductSalesReportDto;
import com.kuter.sales.dto.SaleItemResponseDto;
import com.kuter.sales.dto.SaleReceiptItemDto;
import com.kuter.sales.dto.SaleReceiptResponseDto;
import com.kuter.sales.dto.SaleResponseDto;
import com.kuter.sales.dto.SalesSummaryReportDto;
import com.kuter.sales.dto.VoidSaleRequestDto;
import com.kuter.tenancy.Tenant;
import com.kuter.tenancy.TenantContext;
import com.kuter.tenancy.TenantRepository;
import com.kuter.tenancy.TenantSecurityService;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SaleServiceImpl implements SaleService {

    private static final Logger log = LoggerFactory.getLogger(SaleServiceImpl.class);

    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final ProductRepository productRepository;
    private final ProductUnitConversionRepository conversionRepository;
    private final UnitDefinitionRepository unitDefinitionRepository;
    private final StockBalanceRepository stockBalanceRepository;
    private final StockMovementRepository stockMovementRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final TenantSecurityService tenantSecurityService;
    private final HttpServletRequest httpServletRequest;
    private final com.kuter.subscriptions.EntitlementService entitlementService;

    @Override
    @Transactional
    public SaleResponseDto createSale(CreateSaleRequestDto request) {
        UUID tenantId = TenantContext.requireTenantId();
        entitlementService.assertActiveSubscription(tenantId);

        // 1. Idempotency replay check
        if (StringUtils.hasText(request.getIdempotencyKey())) {
            Optional<Sale> existing = saleRepository.findByTenantIdAndIdempotencyKey(tenantId, request.getIdempotencyKey().trim());
            if (existing.isPresent()) {
                log.info("Idempotent sale request detected for key '{}'. Returning existing sale.", request.getIdempotencyKey());
                List<SaleItem> existingItems = saleItemRepository.findAllByTenantIdAndSaleId(tenantId, existing.get().getId());
                return mapToResponseDto(existing.get(), existingItems);
            }
        }

        // 2. Validate items non-empty
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BadRequestException("Sale must contain at least one line item");
        }

        // 3. Resolve actor details
        KuterUserPrincipal principal = getAuthenticatedPrincipal();
        UUID cashierId = principal != null ? principal.getUserId() : null;

        // 4. Validate products, units, calculate conversion factors and base quantities
        Map<UUID, Product> productMap = new HashMap<>();
        Map<UUID, UnitDefinition> unitMap = new HashMap<>();
        Map<CreateSaleItemRequestDto, BigDecimal> conversionFactors = new HashMap<>();
        Map<CreateSaleItemRequestDto, BigDecimal> baseQuantities = new HashMap<>();
        Map<UUID, BigDecimal> totalRequiredBaseQtyPerProduct = new HashMap<>();

        for (CreateSaleItemRequestDto itemDto : request.getItems()) {
            if (itemDto.getProductId() == null) {
                throw new BadRequestException("Product ID is required for each line item");
            }
            if (itemDto.getUnitDefinitionId() == null) {
                throw new BadRequestException("Selling unit ID is required for each line item");
            }
            if (itemDto.getQuantity() == null || itemDto.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Sold quantity must be strictly greater than zero");
            }

            // Verify product belongs to tenant and is ACTIVE
            Product product = productMap.computeIfAbsent(itemDto.getProductId(), id ->
                    productRepository.findByTenantIdAndId(tenantId, id)
                            .orElseThrow(() -> new BadRequestException("Product not found or unauthorized: " + id))
            );

            if (product.getStatus() != ProductStatus.ACTIVE) {
                throw new BadRequestException("Cannot sell inactive product: " + product.getSku() + " (status: " + product.getStatus() + ")");
            }

            // Verify unit definition belongs to tenant
            UnitDefinition unit = unitMap.computeIfAbsent(itemDto.getUnitDefinitionId(), id ->
                    unitDefinitionRepository.findByTenantIdAndId(tenantId, id)
                            .orElseThrow(() -> new BadRequestException("Unit definition not found: " + id))
            );

            // Determine conversion factor
            BigDecimal factor;
            if (unit.getId().equals(product.getBaseUnitId())) {
                factor = BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP);
            } else {
                ProductUnitConversion conversion = conversionRepository
                        .findByTenantIdAndProductIdAndUnitId(tenantId, product.getId(), unit.getId())
                        .orElseThrow(() -> new BadRequestException(
                                "Unit '" + unit.getCode() + "' is not configured as a selling unit for product '" + product.getSku() + "'"
                        ));
                if (!conversion.isSellingUnit()) {
                    throw new BadRequestException("Unit '" + unit.getCode() + "' is not enabled for sales on product '" + product.getSku() + "'");
                }
                factor = conversion.getConversionFactor().setScale(4, RoundingMode.HALF_UP);
            }

            BigDecimal baseQty = itemDto.getQuantity().multiply(factor).setScale(4, RoundingMode.HALF_UP);
            conversionFactors.put(itemDto, factor);
            baseQuantities.put(itemDto, baseQty);
            totalRequiredBaseQtyPerProduct.merge(product.getId(), baseQty, BigDecimal::add);
        }

        // 5. Deadlock prevention: Acquire pessimistic write locks in canonical (sorted) product ID order
        List<UUID> sortedProductIds = totalRequiredBaseQtyPerProduct.keySet().stream().sorted().toList();
        Map<UUID, StockBalance> lockedBalanceMap = new HashMap<>();

        for (UUID prodId : sortedProductIds) {
            Product product = productMap.get(prodId);
            StockBalance balance = stockBalanceRepository.findByTenantIdAndProductIdForUpdate(tenantId, prodId)
                    .orElseThrow(() -> new BadRequestException("Product '" + product.getSku() + "' has no stock balance record"));

            BigDecimal requiredBaseQty = totalRequiredBaseQtyPerProduct.get(prodId);
            if (balance.getQuantityOnHand().compareTo(requiredBaseQty) < 0) {
                throw new BadRequestException("Insufficient stock for product '" + product.getSku() + "'. Available: "
                        + balance.getQuantityOnHand() + ", requested: " + requiredBaseQty);
            }

            lockedBalanceMap.put(prodId, balance);
        }

        // 6. Deduct stock balances and prepare movement updates
        for (UUID prodId : sortedProductIds) {
            StockBalance balance = lockedBalanceMap.get(prodId);
            BigDecimal requiredBaseQty = totalRequiredBaseQtyPerProduct.get(prodId);
            BigDecimal newOnHand = balance.getQuantityOnHand().subtract(requiredBaseQty).setScale(4, RoundingMode.HALF_UP);
            balance.setQuantityOnHand(newOnHand);
            stockBalanceRepository.save(balance);
        }

        // 7. Calculate line item financials & cost snapshots
        List<SaleItem> saleItemsToSave = new ArrayList<>();
        BigDecimal totalSubtotal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        BigDecimal totalLineDiscounts = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        BigDecimal totalSaleCost = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

        // Pre-create temporary Sale entity to obtain ID or set during cascade
        String invoiceNumber = generateInvoiceNumber(tenantId);
        Sale sale = Sale.builder()
                .tenantId(tenantId)
                .invoiceNumber(invoiceNumber)
                .customerName(StringUtils.hasText(request.getCustomerName()) ? request.getCustomerName().trim() : null)
                .customerPhone(StringUtils.hasText(request.getCustomerPhone()) ? request.getCustomerPhone().trim() : null)
                .status(SaleStatus.COMPLETED)
                .subtotal(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                .discountAmount(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                .taxAmount(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                .totalAmount(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                .totalCost(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                .soldAt(Instant.now())
                .cashierId(cashierId)
                .notes(StringUtils.hasText(request.getNotes()) ? request.getNotes().trim() : null)
                .idempotencyKey(StringUtils.hasText(request.getIdempotencyKey()) ? request.getIdempotencyKey().trim() : null)
                .build();

        Sale savedSale = saleRepository.save(sale);

        for (CreateSaleItemRequestDto itemDto : request.getItems()) {
            Product product = productMap.get(itemDto.getProductId());
            UnitDefinition unit = unitMap.get(itemDto.getUnitDefinitionId());
            BigDecimal factor = conversionFactors.get(itemDto);
            BigDecimal baseQty = baseQuantities.get(itemDto);
            StockBalance balance = lockedBalanceMap.get(product.getId());

            // Determine authoritative unit price based on owner-configured selling price and conversion factor
            BigDecimal basePrice = product.getSellingPrice() != null ? product.getSellingPrice() : BigDecimal.ZERO;
            BigDecimal authorizedUnitPrice = basePrice.multiply(factor).setScale(4, RoundingMode.HALF_UP);

            boolean isOwner = principal != null && (principal.isPlatformAdmin() || principal.getRoleIn(tenantId).filter(r -> r == TenantRole.OWNER).isPresent());
            BigDecimal unitPrice;
            if (itemDto.getUnitPrice() != null) {
                BigDecimal clientPrice = itemDto.getUnitPrice().setScale(4, RoundingMode.HALF_UP);
                if (!isOwner && clientPrice.compareTo(authorizedUnitPrice) != 0) {
                    throw new BadRequestException("Selling price for product '" + product.getSku() +
                            "' does not match the authorized owner price. Expected: " + authorizedUnitPrice +
                            ", supplied: " + clientPrice + ". Please refresh POS catalog prices.");
                }
                unitPrice = clientPrice;
            } else {
                unitPrice = authorizedUnitPrice;
            }

            BigDecimal lineSubtotal = itemDto.getQuantity().multiply(unitPrice).setScale(4, RoundingMode.HALF_UP);
            BigDecimal lineDiscount = itemDto.getDiscountAmount() != null
                    ? itemDto.getDiscountAmount().setScale(4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

            if (lineDiscount.compareTo(BigDecimal.ZERO) < 0) {
                throw new BadRequestException("Discount cannot be negative on line item for " + product.getSku());
            }
            if (lineDiscount.compareTo(lineSubtotal) > 0) {
                throw new BadRequestException("Line discount cannot exceed line subtotal for " + product.getSku());
            }

            BigDecimal lineTotalPrice = lineSubtotal.subtract(lineDiscount).setScale(4, RoundingMode.HALF_UP);

            // Cost snapshot: unit cost from current stock balance moving average cost
            BigDecimal unitCost = balance.getAverageCost() != null ? balance.getAverageCost().setScale(4, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
            BigDecimal lineTotalCost = baseQty.multiply(unitCost).setScale(4, RoundingMode.HALF_UP);

            totalSubtotal = totalSubtotal.add(lineSubtotal);
            totalLineDiscounts = totalLineDiscounts.add(lineDiscount);
            totalSaleCost = totalSaleCost.add(lineTotalCost);

            SaleItem item = SaleItem.builder()
                    .tenantId(tenantId)
                    .saleId(savedSale.getId())
                    .productId(product.getId())
                    .productName(product.getName())
                    .productSku(product.getSku())
                    .soldUnitId(unit.getId())
                    .soldQuantity(itemDto.getQuantity().setScale(4, RoundingMode.HALF_UP))
                    .conversionFactor(factor)
                    .baseQuantity(baseQty)
                    .unitPrice(unitPrice)
                    .unitCost(unitCost)
                    .discountAmount(lineDiscount)
                    .totalPrice(lineTotalPrice)
                    .totalCost(lineTotalCost)
                    .build();

            saleItemsToSave.add(item);
        }

        List<SaleItem> savedItems = saleItemRepository.saveAll(saleItemsToSave);

        // 8. Record stock movements for each distinct product sold
        for (UUID prodId : sortedProductIds) {
            Product product = productMap.get(prodId);
            StockBalance balance = lockedBalanceMap.get(prodId);
            BigDecimal totalDeductedBaseQty = totalRequiredBaseQtyPerProduct.get(prodId);

            StockMovement movement = StockMovement.builder()
                    .tenantId(tenantId)
                    .productId(product.getId())
                    .movementType(MovementType.SALE)
                    .referenceType("SALE")
                    .referenceId(savedSale.getId())
                    .quantityChange(totalDeductedBaseQty.negate())
                    .balanceAfter(balance.getQuantityOnHand())
                    .unitCost(balance.getAverageCost())
                    .notes("Counter sale invoice #" + savedSale.getInvoiceNumber())
                    .actorId(cashierId)
                    .build();

            stockMovementRepository.save(movement);
        }

        // 9. Update and persist final Sale header totals
        BigDecimal headerDiscount = request.getDiscountAmount() != null
                ? request.getDiscountAmount().setScale(4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        BigDecimal totalDiscountAmount = totalLineDiscounts.add(headerDiscount).setScale(4, RoundingMode.HALF_UP);
        BigDecimal finalTotalAmount = totalSubtotal.subtract(totalDiscountAmount).setScale(4, RoundingMode.HALF_UP);

        if (finalTotalAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Total sale amount cannot be negative");
        }

        savedSale.setSubtotal(totalSubtotal);
        savedSale.setDiscountAmount(totalDiscountAmount);
        savedSale.setTotalAmount(finalTotalAmount);
        savedSale.setTotalCost(totalSaleCost);
        savedSale = saleRepository.save(savedSale);

        // 10. Audit logging
        logAudit("SALE_CREATED", savedSale.getId(),
                "Sale invoice #" + savedSale.getInvoiceNumber() + " created. Items: " + savedItems.size()
                        + ", Total: " + savedSale.getTotalAmount());

        return mapToResponseDto(savedSale, savedItems);
    }

    @Override
    @Transactional(readOnly = true)
    public SaleResponseDto getSaleById(UUID saleId) {
        UUID tenantId = TenantContext.requireTenantId();
        Sale sale = saleRepository.findByTenantIdAndId(tenantId, saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Sale not found: " + saleId));
        List<SaleItem> items = saleItemRepository.findAllByTenantIdAndSaleId(tenantId, sale.getId());
        return mapToResponseDto(sale, items);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SaleResponseDto> listSales(Instant startDate, Instant endDate, SaleStatus status,
                                           String search, UUID cashierId, UUID productId, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();

        Specification<Sale> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("tenantId"), tenantId));

            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("soldAt"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("soldAt"), endDate));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (cashierId != null) {
                predicates.add(cb.equal(root.get("cashierId"), cashierId));
            }
            if (productId != null) {
                jakarta.persistence.criteria.Subquery<UUID> subquery = query.subquery(UUID.class);
                jakarta.persistence.criteria.Root<SaleItem> itemRoot = subquery.from(SaleItem.class);
                subquery.select(itemRoot.get("saleId"))
                        .where(cb.equal(itemRoot.get("tenantId"), tenantId),
                               cb.equal(itemRoot.get("productId"), productId));
                predicates.add(root.get("id").in(subquery));
            }
            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate invoiceMatch = cb.like(cb.lower(root.get("invoiceNumber")), pattern);
                Predicate customerMatch = cb.like(cb.lower(root.get("customerName")), pattern);
                predicates.add(cb.or(invoiceMatch, customerMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Sale> salesPage = saleRepository.findAll(spec, pageable);

        // Batch load items for the page to prevent N+1 queries
        List<UUID> saleIds = salesPage.getContent().stream().map(Sale::getId).toList();
        Map<UUID, List<SaleItem>> itemsBySaleId = new HashMap<>();
        for (UUID sId : saleIds) {
            itemsBySaleId.put(sId, saleItemRepository.findAllByTenantIdAndSaleId(tenantId, sId));
        }

        return salesPage.map(s -> mapToResponseDto(s, itemsBySaleId.getOrDefault(s.getId(), List.of())));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SaleResponseDto> listSales(Instant startDate, Instant endDate, SaleStatus status,
                                           String search, UUID cashierId, Pageable pageable) {
        return listSales(startDate, endDate, status, search, cashierId, null, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DailySalesSummaryDto> getDailySalesReports(Instant startDate, Instant endDate) {
        SalesSummaryReportDto summary = getSalesSummaryReport(startDate, endDate);
        return summary.getDailyBreakdowns() != null ? summary.getDailyBreakdowns() : List.of();
    }

    @Override
    @Transactional
    public SaleResponseDto voidSale(UUID saleId, VoidSaleRequestDto request) {
        UUID tenantId = TenantContext.requireTenantId();

        Sale sale = saleRepository.findByTenantIdAndId(tenantId, saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Sale not found: " + saleId));

        if (sale.getStatus() == SaleStatus.VOIDED) {
            throw new BadRequestException("Sale #" + sale.getInvoiceNumber() + " is already voided");
        }
        if (sale.getStatus() != SaleStatus.COMPLETED) {
            throw new BadRequestException("Only COMPLETED sales can be voided. Current status: " + sale.getStatus());
        }

        if (request == null || !StringUtils.hasText(request.getReason())) {
            throw new BadRequestException("Void reason is required");
        }

        List<SaleItem> items = saleItemRepository.findAllByTenantIdAndSaleId(tenantId, sale.getId());
        KuterUserPrincipal principal = getAuthenticatedPrincipal();
        UUID actingUserId = principal != null ? principal.getUserId() : null;

        // Group returned quantities and returned costs per product
        Map<UUID, BigDecimal> returnedBaseQtyPerProduct = new HashMap<>();
        Map<UUID, BigDecimal> returnedCostPerProduct = new HashMap<>();
        Map<UUID, Product> productMap = new HashMap<>();

        for (SaleItem item : items) {
            returnedBaseQtyPerProduct.merge(item.getProductId(), item.getBaseQuantity(), BigDecimal::add);
            returnedCostPerProduct.merge(item.getProductId(), item.getTotalCost(), BigDecimal::add);
            if (!productMap.containsKey(item.getProductId())) {
                productRepository.findByTenantIdAndId(tenantId, item.getProductId()).ifPresent(p -> productMap.put(p.getId(), p));
            }
        }

        // Deadlock prevention: sort product IDs in ascending order before locking
        List<UUID> sortedProductIds = returnedBaseQtyPerProduct.keySet().stream().sorted().toList();

        for (UUID prodId : sortedProductIds) {
            Product product = productMap.get(prodId);
            StockBalance balance = stockBalanceRepository.findByTenantIdAndProductIdForUpdate(tenantId, prodId)
                    .orElseThrow(() -> new BadRequestException("Product has no inventory balance: " + prodId));

            BigDecimal returnedBaseQty = returnedBaseQtyPerProduct.get(prodId);
            BigDecimal returnedCost = returnedCostPerProduct.get(prodId);

            BigDecimal currentOnHand = balance.getQuantityOnHand();
            BigDecimal currentAvgCost = balance.getAverageCost() != null ? balance.getAverageCost() : BigDecimal.ZERO;
            BigDecimal currentValuation = currentOnHand.multiply(currentAvgCost);

            BigDecimal newOnHand = currentOnHand.add(returnedBaseQty).setScale(4, RoundingMode.HALF_UP);
            BigDecimal newValuation = currentValuation.add(returnedCost).setScale(4, RoundingMode.HALF_UP);

            BigDecimal newAvgCost = newOnHand.compareTo(BigDecimal.ZERO) > 0
                    ? newValuation.divide(newOnHand, 4, RoundingMode.HALF_UP)
                    : currentAvgCost;

            balance.setQuantityOnHand(newOnHand);
            balance.setAverageCost(newAvgCost);
            stockBalanceRepository.save(balance);

            // Record compensating movement
            StockMovement movement = StockMovement.builder()
                    .tenantId(tenantId)
                    .productId(product.getId())
                    .movementType(MovementType.RETURN)
                    .referenceType("SALE_VOID")
                    .referenceId(sale.getId())
                    .quantityChange(returnedBaseQty)
                    .balanceAfter(newOnHand)
                    .unitCost(newAvgCost)
                    .notes("Compensating movement for voided sale #" + sale.getInvoiceNumber() + ": " + request.getReason().trim())
                    .actorId(actingUserId)
                    .build();

            stockMovementRepository.save(movement);
        }

        // Transition sale to VOIDED
        sale.setStatus(SaleStatus.VOIDED);
        sale.setVoidReason(request.getReason().trim());
        sale.setVoidedAt(Instant.now());
        sale.setVoidedBy(actingUserId);
        sale = saleRepository.save(sale);

        // Audit log
        logAudit("SALE_VOIDED", sale.getId(),
                "Sale invoice #" + sale.getInvoiceNumber() + " voided. Reason: " + request.getReason().trim());

        return mapToResponseDto(sale, items);
    }

    @Override
    @Transactional(readOnly = true)
    public SaleReceiptResponseDto getSaleReceipt(UUID saleId) {
        UUID tenantId = TenantContext.requireTenantId();

        Sale sale = saleRepository.findByTenantIdAndId(tenantId, saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Sale not found: " + saleId));

        Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
        List<SaleItem> items = saleItemRepository.findAllByTenantIdAndSaleId(tenantId, sale.getId());

        String cashierName = "Counter Staff";
        if (sale.getCashierId() != null) {
            cashierName = userRepository.findById(sale.getCashierId())
                    .map(User::getFullName)
                    .orElse("Counter Staff");
        }

        // Pre-fetch unit definitions to display code on receipt
        Map<UUID, String> unitCodes = new HashMap<>();
        for (SaleItem item : items) {
            if (!unitCodes.containsKey(item.getSoldUnitId())) {
                unitDefinitionRepository.findById(item.getSoldUnitId())
                        .ifPresent(u -> unitCodes.put(u.getId(), u.getCode()));
            }
        }

        List<SaleReceiptItemDto> receiptItems = items.stream().map(i -> SaleReceiptItemDto.builder()
                .productName(i.getProductName())
                .productSku(i.getProductSku())
                .unitCode(unitCodes.getOrDefault(i.getSoldUnitId(), "UNIT"))
                .quantity(i.getSoldQuantity())
                .unitPrice(i.getUnitPrice())
                .discountAmount(i.getDiscountAmount())
                .totalPrice(i.getTotalPrice())
                .build()
        ).toList();

        return SaleReceiptResponseDto.builder()
                .shopName(tenant != null ? tenant.getName() : "Building Materials Shop")
                .shopPhone(null)
                .shopEmail(null)
                .shopAddress(null)
                .shopCity(null)
                .shopCountry(null)
                .invoiceNumber(sale.getInvoiceNumber())
                .soldAt(sale.getSoldAt())
                .cashierName(cashierName)
                .customerName(sale.getCustomerName())
                .customerPhone(sale.getCustomerPhone())
                .items(receiptItems)
                .subtotal(sale.getSubtotal())
                .discountAmount(sale.getDiscountAmount())
                .taxAmount(sale.getTaxAmount())
                .totalAmount(sale.getTotalAmount())
                .notes(sale.getNotes())
                .status(sale.getStatus())
                .voidReason(sale.getVoidReason())
                .voidedAt(sale.getVoidedAt())
                .disclaimer("Operational Sales Record - Not a Proof of Payment")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public SalesSummaryReportDto getSalesSummaryReport(Instant startDate, Instant endDate) {
        UUID tenantId = TenantContext.requireTenantId();

        Instant start = startDate != null ? startDate : Instant.now().minus(30, ChronoUnit.DAYS);
        Instant end = endDate != null ? endDate : Instant.now();

        List<Sale> allSales = saleRepository.findAllByTenantIdAndSoldAtBetween(tenantId, start, end);

        List<Sale> completedSales = allSales.stream()
                .filter(s -> s.getStatus() == SaleStatus.COMPLETED)
                .toList();

        List<Sale> voidedSales = allSales.stream()
                .filter(s -> s.getStatus() == SaleStatus.VOIDED)
                .toList();

        long totalSalesCount = completedSales.size();
        BigDecimal totalSalesAmount = completedSales.stream()
                .map(Sale::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(4, RoundingMode.HALF_UP);

        long voidedCount = voidedSales.size();
        BigDecimal voidedAmount = voidedSales.stream()
                .map(Sale::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(4, RoundingMode.HALF_UP);

        boolean canViewCost = tenantSecurityService.canViewSalesCostReports();

        BigDecimal totalCOGS = null;
        BigDecimal grossProfit = null;
        BigDecimal grossMarginPercentage = null;

        if (canViewCost) {
            totalCOGS = completedSales.stream()
                    .map(Sale::getTotalCost)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(4, RoundingMode.HALF_UP);

            grossProfit = totalSalesAmount.subtract(totalCOGS).setScale(4, RoundingMode.HALF_UP);

            grossMarginPercentage = totalSalesAmount.compareTo(BigDecimal.ZERO) > 0
                    ? grossProfit.multiply(BigDecimal.valueOf(100)).divide(totalSalesAmount, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        // Daily breakdown of completed sales
        Map<LocalDate, List<Sale>> salesByDate = completedSales.stream()
                .collect(Collectors.groupingBy(s -> s.getSoldAt().atZone(ZoneOffset.UTC).toLocalDate()));

        List<DailySalesSummaryDto> dailyBreakdowns = salesByDate.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    LocalDate date = entry.getKey();
                    List<Sale> daySales = entry.getValue();

                    BigDecimal dayAmount = daySales.stream()
                            .map(Sale::getTotalAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .setScale(4, RoundingMode.HALF_UP);

                    BigDecimal dayCOGS = null;
                    BigDecimal dayProfit = null;
                    if (canViewCost) {
                        dayCOGS = daySales.stream()
                                .map(Sale::getTotalCost)
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                                .setScale(4, RoundingMode.HALF_UP);
                        dayProfit = dayAmount.subtract(dayCOGS).setScale(4, RoundingMode.HALF_UP);
                    }

                    return DailySalesSummaryDto.builder()
                            .date(date)
                            .salesCount(daySales.size())
                            .salesAmount(dayAmount)
                            .costOfGoodsSold(dayCOGS)
                            .grossProfit(dayProfit)
                            .build();
                }).toList();

        return SalesSummaryReportDto.builder()
                .tenantId(tenantId)
                .startDate(start)
                .endDate(end)
                .generatedAt(Instant.now())
                .totalSalesCount(totalSalesCount)
                .totalSalesAmount(totalSalesAmount)
                .totalCostOfGoodsSold(totalCOGS)
                .grossProfit(grossProfit)
                .grossMarginPercentage(grossMarginPercentage)
                .voidedSalesCount(voidedCount)
                .voidedSalesAmount(voidedAmount)
                .dailyBreakdowns(dailyBreakdowns)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductSalesReportDto getProductSalesReport(Instant startDate, Instant endDate, int limit) {
        UUID tenantId = TenantContext.requireTenantId();

        Instant start = startDate != null ? startDate : Instant.now().minus(30, ChronoUnit.DAYS);
        Instant end = endDate != null ? endDate : Instant.now();
        int safeLimit = limit > 0 ? Math.min(limit, 100) : 20;

        List<Sale> completedSales = saleRepository.findAllByTenantIdAndStatusAndSoldAtBetween(
                tenantId, SaleStatus.COMPLETED, start, end);

        boolean canViewCost = tenantSecurityService.canViewSalesCostReports();

        // Aggregate across sale items
        Map<UUID, ProductSalesMetricAccumulator> metricsMap = new HashMap<>();

        for (Sale sale : completedSales) {
            List<SaleItem> items = saleItemRepository.findAllByTenantIdAndSaleId(tenantId, sale.getId());
            for (SaleItem item : items) {
                ProductSalesMetricAccumulator acc = metricsMap.computeIfAbsent(
                        item.getProductId(),
                        id -> new ProductSalesMetricAccumulator(id, item.getProductSku(), item.getProductName())
                );
                acc.addQuantity(item.getBaseQuantity());
                acc.addRevenue(item.getTotalPrice());
                acc.addCost(item.getTotalCost());
                acc.incrementTransactionCount();
            }
        }

        // Load base unit codes for the products
        Map<UUID, String> baseUnitCodes = new HashMap<>();
        for (UUID prodId : metricsMap.keySet()) {
            productRepository.findById(prodId).ifPresent(p -> {
                unitDefinitionRepository.findById(p.getBaseUnitId()).ifPresent(u -> baseUnitCodes.put(p.getId(), u.getCode()));
            });
        }

        List<ProductSalesMetricDto> rankedProducts = metricsMap.values().stream()
                .sorted(Comparator.comparing(ProductSalesMetricAccumulator::getTotalRevenue).reversed())
                .limit(safeLimit)
                .map(acc -> {
                    BigDecimal totalRev = acc.getTotalRevenue().setScale(4, RoundingMode.HALF_UP);
                    BigDecimal totalCost = null;
                    BigDecimal grossProfit = null;

                    if (canViewCost) {
                        totalCost = acc.getTotalCost().setScale(4, RoundingMode.HALF_UP);
                        grossProfit = totalRev.subtract(totalCost).setScale(4, RoundingMode.HALF_UP);
                    }

                    return ProductSalesMetricDto.builder()
                            .productId(acc.getProductId())
                            .productSku(acc.getProductSku())
                            .productName(acc.getProductName())
                            .baseUnitCode(baseUnitCodes.getOrDefault(acc.getProductId(), "UNIT"))
                            .totalQuantitySold(acc.getTotalQuantity().setScale(4, RoundingMode.HALF_UP))
                            .totalRevenue(totalRev)
                            .transactionCount(acc.getTransactionCount())
                            .totalCost(totalCost)
                            .grossProfit(grossProfit)
                            .build();
                }).toList();

        return ProductSalesReportDto.builder()
                .tenantId(tenantId)
                .startDate(start)
                .endDate(end)
                .generatedAt(Instant.now())
                .products(rankedProducts)
                .build();
    }

    private SaleResponseDto mapToResponseDto(Sale sale, List<SaleItem> items) {
        boolean canViewCost = tenantSecurityService.canViewSalesCostReports();

        String cashierName = null;
        if (sale.getCashierId() != null) {
            cashierName = userRepository.findById(sale.getCashierId())
                    .map(User::getFullName)
                    .orElse(null);
        }

        String voidedByName = null;
        if (sale.getVoidedBy() != null) {
            voidedByName = userRepository.findById(sale.getVoidedBy())
                    .map(User::getFullName)
                    .orElse(null);
        }

        // Fetch unit codes
        Map<UUID, String> unitCodes = new HashMap<>();
        for (SaleItem item : items) {
            if (!unitCodes.containsKey(item.getSoldUnitId())) {
                unitDefinitionRepository.findById(item.getSoldUnitId())
                        .ifPresent(u -> unitCodes.put(u.getId(), u.getCode()));
            }
        }

        List<SaleItemResponseDto> itemDtos = items.stream().map(i -> {
            BigDecimal unitCost = null;
            BigDecimal totalCost = null;
            BigDecimal grossProfit = null;

            if (canViewCost) {
                unitCost = i.getUnitCost();
                totalCost = i.getTotalCost();
                grossProfit = i.getTotalPrice().subtract(i.getTotalCost()).setScale(4, RoundingMode.HALF_UP);
            }

            return SaleItemResponseDto.builder()
                    .id(i.getId())
                    .productId(i.getProductId())
                    .productName(i.getProductName())
                    .productSku(i.getProductSku())
                    .soldUnitId(i.getSoldUnitId())
                    .soldUnitCode(unitCodes.getOrDefault(i.getSoldUnitId(), "UNIT"))
                    .soldQuantity(i.getSoldQuantity())
                    .conversionFactor(i.getConversionFactor())
                    .baseQuantity(i.getBaseQuantity())
                    .unitPrice(i.getUnitPrice())
                    .discountAmount(i.getDiscountAmount())
                    .totalPrice(i.getTotalPrice())
                    .unitCost(unitCost)
                    .totalCost(totalCost)
                    .grossProfit(grossProfit)
                    .build();
        }).toList();

        BigDecimal totalCost = null;
        BigDecimal grossProfit = null;
        if (canViewCost) {
            totalCost = sale.getTotalCost();
            grossProfit = sale.getTotalAmount().subtract(sale.getTotalCost()).setScale(4, RoundingMode.HALF_UP);
        }

        return SaleResponseDto.builder()
                .id(sale.getId())
                .tenantId(sale.getTenantId())
                .invoiceNumber(sale.getInvoiceNumber())
                .customerName(sale.getCustomerName())
                .customerPhone(sale.getCustomerPhone())
                .status(sale.getStatus())
                .subtotal(sale.getSubtotal())
                .discountAmount(sale.getDiscountAmount())
                .taxAmount(sale.getTaxAmount())
                .totalAmount(sale.getTotalAmount())
                .totalCost(totalCost)
                .grossProfit(grossProfit)
                .soldAt(sale.getSoldAt())
                .cashierId(sale.getCashierId())
                .cashierName(cashierName)
                .notes(sale.getNotes())
                .idempotencyKey(sale.getIdempotencyKey())
                .voidReason(sale.getVoidReason())
                .voidedAt(sale.getVoidedAt())
                .voidedBy(sale.getVoidedBy())
                .voidedByName(voidedByName)
                .items(itemDtos)
                .createdAt(sale.getCreatedAt())
                .build();
    }

    private String generateInvoiceNumber(UUID tenantId) {
        int year = Year.now().getValue();
        for (int attempt = 0; attempt < 10; attempt++) {
            int randomDigits = 10000 + new Random().nextInt(90000);
            String candidate = "INV-" + year + "-" + randomDigits;
            if (saleRepository.findByTenantIdAndInvoiceNumber(tenantId, candidate).isEmpty()) {
                return candidate;
            }
        }
        return "INV-" + year + "-" + System.currentTimeMillis();
    }

    private KuterUserPrincipal getAuthenticatedPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof KuterUserPrincipal principal) {
            return principal;
        }
        return null;
    }

    private void logAudit(String action, UUID entityId, String details) {
        UUID tenantId = TenantContext.requireTenantId();
        KuterUserPrincipal principal = getAuthenticatedPrincipal();
        UUID actorId = principal != null ? principal.getUserId() : null;
        String actorEmail = principal != null ? principal.getEmail() : "system";
        String ip = httpServletRequest != null ? httpServletRequest.getRemoteAddr() : "127.0.0.1";

        auditService.logEvent(tenantId, actorId, actorEmail, action, "SALE",
                entityId.toString(), details, ip);
    }

    private static class ProductSalesMetricAccumulator {
        private final UUID productId;
        private final String productSku;
        private final String productName;
        private BigDecimal totalQuantity = BigDecimal.ZERO;
        private BigDecimal totalRevenue = BigDecimal.ZERO;
        private BigDecimal totalCost = BigDecimal.ZERO;
        private long transactionCount = 0;

        public ProductSalesMetricAccumulator(UUID productId, String productSku, String productName) {
            this.productId = productId;
            this.productSku = productSku;
            this.productName = productName;
        }

        public void addQuantity(BigDecimal qty) {
            if (qty != null) totalQuantity = totalQuantity.add(qty);
        }

        public void addRevenue(BigDecimal rev) {
            if (rev != null) totalRevenue = totalRevenue.add(rev);
        }

        public void addCost(BigDecimal cost) {
            if (cost != null) totalCost = totalCost.add(cost);
        }

        public void incrementTransactionCount() {
            transactionCount++;
        }

        public UUID getProductId() { return productId; }
        public String getProductSku() { return productSku; }
        public String getProductName() { return productName; }
        public BigDecimal getTotalQuantity() { return totalQuantity; }
        public BigDecimal getTotalRevenue() { return totalRevenue; }
        public BigDecimal getTotalCost() { return totalCost; }
        public long getTransactionCount() { return transactionCount; }
    }
}
