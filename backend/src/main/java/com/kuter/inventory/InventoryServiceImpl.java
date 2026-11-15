package com.kuter.inventory;

import com.kuter.catalog.Category;
import com.kuter.catalog.CategoryRepository;
import com.kuter.catalog.Product;
import com.kuter.catalog.ProductRepository;
import com.kuter.catalog.ProductUnitConversion;
import com.kuter.catalog.ProductUnitConversionRepository;
import com.kuter.catalog.UnitDefinition;
import com.kuter.catalog.UnitDefinitionRepository;
import com.kuter.common.BadRequestException;
import com.kuter.common.PageResponse;
import com.kuter.common.ResourceNotFoundException;
import com.kuter.identity.KuterUserPrincipal;
import com.kuter.identity.User;
import com.kuter.identity.UserRepository;
import com.kuter.inventory.dto.CategoryValuationSummary;
import com.kuter.inventory.dto.CreateStockAdjustmentDto;
import com.kuter.inventory.dto.CreateStockAdjustmentItemDto;
import com.kuter.inventory.dto.CreateStockReceivingDto;
import com.kuter.inventory.dto.CreateStockReceivingItemDto;
import com.kuter.inventory.dto.CurrentInventoryItemDto;
import com.kuter.inventory.dto.InventoryValuationReport;
import com.kuter.inventory.dto.ProductUnitConversionInfo;
import jakarta.persistence.criteria.Predicate;
import java.util.Collections;
import org.springframework.data.jpa.domain.Specification;
import com.kuter.inventory.dto.StockAdjustmentItemResponse;
import com.kuter.inventory.dto.StockAdjustmentResponse;
import com.kuter.inventory.dto.StockBalanceResponse;
import com.kuter.inventory.dto.StockMovementResponse;
import com.kuter.inventory.dto.StockReceivingItemResponse;
import com.kuter.inventory.dto.StockReceivingResponse;
import com.kuter.tenancy.Tenant;
import com.kuter.tenancy.TenantContext;
import com.kuter.tenancy.TenantRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class InventoryServiceImpl implements InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryServiceImpl.class);

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UnitDefinitionRepository unitRepository;
    private final ProductUnitConversionRepository conversionRepository;
    private final StockBalanceRepository balanceRepository;
    private final StockMovementRepository movementRepository;
    private final StockReceivingRepository receivingRepository;
    private final StockReceivingItemRepository receivingItemRepository;
    private final StockAdjustmentRepository adjustmentRepository;
    private final StockAdjustmentItemRepository adjustmentItemRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;

    public InventoryServiceImpl(ProductRepository productRepository,
                                CategoryRepository categoryRepository,
                                UnitDefinitionRepository unitRepository,
                                ProductUnitConversionRepository conversionRepository,
                                StockBalanceRepository balanceRepository,
                                StockMovementRepository movementRepository,
                                StockReceivingRepository receivingRepository,
                                StockReceivingItemRepository receivingItemRepository,
                                StockAdjustmentRepository adjustmentRepository,
                                StockAdjustmentItemRepository adjustmentItemRepository,
                                TenantRepository tenantRepository,
                                UserRepository userRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.unitRepository = unitRepository;
        this.conversionRepository = conversionRepository;
        this.balanceRepository = balanceRepository;
        this.movementRepository = movementRepository;
        this.receivingRepository = receivingRepository;
        this.receivingItemRepository = receivingItemRepository;
        this.adjustmentRepository = adjustmentRepository;
        this.adjustmentItemRepository = adjustmentItemRepository;
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public StockReceivingResponse receiveStock(CreateStockReceivingDto dto,
                                               KuterUserPrincipal principal,
                                               HttpServletRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        // 1. Idempotency Check: prevent duplicate processing on retries
        if (StringUtils.hasText(dto.getIdempotencyKey())) {
            Optional<StockReceiving> existing = receivingRepository
                    .findByTenantIdAndIdempotencyKey(tenantId, dto.getIdempotencyKey().trim());
            if (existing.isPresent()) {
                log.info("Returning existing stock receipt for idempotency key '{}'", dto.getIdempotencyKey());
                return getReceiving(existing.get().getId());
            }
        }

        String receivingNumber = generateReceivingNumber(tenantId);
        Instant receivedAt = dto.getReceivedAt() != null ? dto.getReceivedAt() : Instant.now();
        UUID actorId = principal != null ? principal.getUserId() : null;

        StockReceiving receiving = StockReceiving.builder()
                .tenantId(tenantId)
                .receivingNumber(receivingNumber)
                .supplierName(dto.getSupplierName().trim())
                .deliveryNoteRef(StringUtils.hasText(dto.getDeliveryNoteRef()) ? dto.getDeliveryNoteRef().trim() : null)
                .idempotencyKey(StringUtils.hasText(dto.getIdempotencyKey()) ? dto.getIdempotencyKey().trim() : null)
                .notes(StringUtils.hasText(dto.getNotes()) ? dto.getNotes().trim() : null)
                .status("COMPLETED")
                .receivedBy(actorId)
                .receivedAt(receivedAt)
                .totalCost(BigDecimal.ZERO)
                .build();

        StockReceiving savedReceiving = receivingRepository.save(receiving);

        BigDecimal totalReceiptCost = BigDecimal.ZERO;
        List<StockReceivingItemResponse> itemResponses = new ArrayList<>();

        for (CreateStockReceivingItemDto itemDto : dto.getItems()) {
            Product product = productRepository.findByTenantIdAndId(tenantId, itemDto.getProductId())
                    .orElseThrow(() -> new BadRequestException("Product not found with ID: " + itemDto.getProductId()));

            UnitDefinition receivedUnit = unitRepository.findByTenantIdAndId(tenantId, itemDto.getReceivedUnitId())
                    .orElseThrow(() -> new BadRequestException("Received unit not found with ID: " + itemDto.getReceivedUnitId()));

            UnitDefinition baseUnit = unitRepository.findByTenantIdAndId(tenantId, product.getBaseUnitId())
                    .orElseThrow(() -> new IllegalStateException("Product base unit not found: " + product.getBaseUnitId()));

            // Determine conversion factor
            BigDecimal conversionFactor;
            if (receivedUnit.getId().equals(product.getBaseUnitId())) {
                conversionFactor = BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP);
            } else {
                ProductUnitConversion conv = conversionRepository
                        .findByTenantIdAndProductIdAndUnitId(tenantId, product.getId(), receivedUnit.getId())
                        .orElseThrow(() -> new BadRequestException("Unit '" + receivedUnit.getCode() +
                                "' is not configured as a receiving unit for product '" + product.getSku() + "'"));
                conversionFactor = conv.getConversionFactor();
            }

            // Normalization calculations
            BigDecimal receivedQty = itemDto.getReceivedQuantity().setScale(4, RoundingMode.HALF_UP);
            BigDecimal unitCostInReceivedUnit = itemDto.getUnitCost().setScale(4, RoundingMode.HALF_UP);
            BigDecimal baseQuantity = receivedQty.multiply(conversionFactor).setScale(4, RoundingMode.HALF_UP);
            BigDecimal lineTotalCost = receivedQty.multiply(unitCostInReceivedUnit).setScale(4, RoundingMode.HALF_UP);
            BigDecimal baseUnitCost = baseQuantity.compareTo(BigDecimal.ZERO) > 0
                    ? lineTotalCost.divide(baseQuantity, 4, RoundingMode.HALF_UP)
                    : unitCostInReceivedUnit;

            totalReceiptCost = totalReceiptCost.add(lineTotalCost);

            // Pessimistic Write Lock on StockBalance for concurrency protection
            StockBalance balance = balanceRepository
                    .findByTenantIdAndProductIdForUpdate(tenantId, product.getId())
                    .orElseGet(() -> {
                        StockBalance sb = StockBalance.builder()
                                .tenantId(tenantId)
                                .productId(product.getId())
                                .quantityOnHand(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                                .averageCost(product.getCostPrice() != null ? product.getCostPrice() : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                                .build();
                        return balanceRepository.save(sb);
                    });

            BigDecimal existingQty = balance.getQuantityOnHand() != null ? balance.getQuantityOnHand() : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
            BigDecimal existingAvgCost = balance.getAverageCost() != null ? balance.getAverageCost() :
                    (product.getCostPrice() != null ? product.getCostPrice() : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));

            // Calculate Moving Weighted-Average Cost
            // New Average Cost = (Existing Stock Value + New Purchase Value) / New Total Stock Quantity
            BigDecimal newQuantityOnHand = existingQty.add(baseQuantity).setScale(4, RoundingMode.HALF_UP);
            BigDecimal newAverageCost;
            if (existingQty.compareTo(BigDecimal.ZERO) <= 0) {
                newAverageCost = baseUnitCost;
            } else if (newQuantityOnHand.compareTo(BigDecimal.ZERO) <= 0) {
                newAverageCost = baseUnitCost;
            } else {
                BigDecimal existingValuation = existingQty.multiply(existingAvgCost);
                BigDecimal combinedValuation = existingValuation.add(lineTotalCost);
                newAverageCost = combinedValuation.divide(newQuantityOnHand, 4, RoundingMode.HALF_UP);
            }

            // Update balance
            balance.setQuantityOnHand(newQuantityOnHand);
            balance.setAverageCost(newAverageCost);
            balanceRepository.save(balance);

            // Update product's cost price to reflect current weighted average cost immediately
            product.setCostPrice(newAverageCost);
            productRepository.save(product);

            // Record receiving item line (snapshots unit, conversion factor, quantities, and cost)
            StockReceivingItem item = StockReceivingItem.builder()
                    .tenantId(tenantId)
                    .receivingId(savedReceiving.getId())
                    .productId(product.getId())
                    .receivedUnitId(receivedUnit.getId())
                    .receivedQuantity(receivedQty)
                    .conversionFactor(conversionFactor)
                    .baseQuantity(baseQuantity)
                    .unitCost(unitCostInReceivedUnit)
                    .totalCost(lineTotalCost)
                    .build();
            StockReceivingItem savedItem = receivingItemRepository.save(item);

            // Append immutable stock movement to ledger
            StockMovement movement = StockMovement.builder()
                    .tenantId(tenantId)
                    .productId(product.getId())
                    .movementType(MovementType.RECEIPT)
                    .referenceId(savedReceiving.getId())
                    .referenceType("STOCK_RECEIVING")
                    .quantityChange(baseQuantity)
                    .balanceAfter(newQuantityOnHand)
                    .unitCost(baseUnitCost)
                    .notes("Received " + receivedQty + " " + receivedUnit.getCode() + " (supplier: " + dto.getSupplierName() + ")")
                    .actorId(actorId)
                    .build();
            movementRepository.save(movement);

            itemResponses.add(StockReceivingItemResponse.builder()
                    .id(savedItem.getId())
                    .productId(product.getId())
                    .productSku(product.getSku())
                    .productName(product.getName())
                    .receivedUnitId(receivedUnit.getId())
                    .receivedUnitCode(receivedUnit.getCode())
                    .receivedQuantity(receivedQty)
                    .conversionFactor(conversionFactor)
                    .baseQuantity(baseQuantity)
                    .baseUnitCode(baseUnit.getCode())
                    .unitCost(unitCostInReceivedUnit)
                    .totalCost(lineTotalCost)
                    .build());
        }

        savedReceiving.setTotalCost(totalReceiptCost);
        receivingRepository.save(savedReceiving);

        logAudit("STOCK_RECEIVED", savedReceiving.getId(),
                "Received delivery " + receivingNumber + " from " + savedReceiving.getSupplierName() +
                        ", total: " + totalReceiptCost, principal, request);
        log.info("Recorded stock receiving {} for tenant {}, total cost: {}", receivingNumber, tenantId, totalReceiptCost);

        String actorName = principal != null ? principal.getFullName() : "System";

        return StockReceivingResponse.builder()
                .id(savedReceiving.getId())
                .receivingNumber(savedReceiving.getReceivingNumber())
                .supplierName(savedReceiving.getSupplierName())
                .deliveryNoteRef(savedReceiving.getDeliveryNoteRef())
                .status(savedReceiving.getStatus())
                .totalCost(savedReceiving.getTotalCost())
                .notes(savedReceiving.getNotes())
                .receivedBy(savedReceiving.getReceivedBy())
                .receivedByName(actorName)
                .receivedAt(savedReceiving.getReceivedAt())
                .createdAt(savedReceiving.getCreatedAt())
                .items(itemResponses)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<StockReceivingResponse> listReceivings(Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<StockReceiving> page = receivingRepository.findAllByTenantIdOrderByReceivedAtDesc(tenantId, pageable);

        Map<UUID, String> userNames = userRepository.findAll().stream()
                .collect(Collectors.toMap(User::getId, User::getFullName, (a, b) -> a));

        List<StockReceivingResponse> content = page.getContent().stream()
                .map(rcv -> mapReceivingToResponse(rcv, userNames.get(rcv.getReceivedBy())))
                .toList();

        return PageResponse.of(page, content);
    }

    @Override
    @Transactional(readOnly = true)
    public StockReceivingResponse getReceiving(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        StockReceiving rcv = receivingRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Stock receiving record not found with ID: " + id));

        String receiverName = rcv.getReceivedBy() != null ?
                userRepository.findById(rcv.getReceivedBy()).map(User::getFullName).orElse(null) : null;

        return mapReceivingToResponse(rcv, receiverName);
    }

    @Override
    @Transactional
    public StockAdjustmentResponse adjustStock(CreateStockAdjustmentDto dto,
                                               KuterUserPrincipal principal,
                                               HttpServletRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        // 1. Idempotency Check
        if (StringUtils.hasText(dto.getIdempotencyKey())) {
            Optional<StockAdjustment> existing = adjustmentRepository
                    .findByTenantIdAndIdempotencyKey(tenantId, dto.getIdempotencyKey().trim());
            if (existing.isPresent()) {
                log.info("Returning existing stock adjustment for idempotency key '{}'", dto.getIdempotencyKey());
                return getAdjustment(existing.get().getId());
            }
        }

        if (!StringUtils.hasText(dto.getReasonCode())) {
            throw new BadRequestException("Adjustment reason code must be explicitly selected.");
        }
        String cleanReason = dto.getReasonCode().trim().toUpperCase();
        if ("OTHER".equals(cleanReason) && !StringUtils.hasText(dto.getNotes())) {
            throw new BadRequestException("Please provide an explanation in notes when reason is 'OTHER'.");
        }

        String adjustmentNumber = generateAdjustmentNumber(tenantId);
        UUID actorId = principal != null ? principal.getUserId() : null;

        StockAdjustment adjustment = StockAdjustment.builder()
                .tenantId(tenantId)
                .adjustmentNumber(adjustmentNumber)
                .reasonCode(cleanReason)
                .notes(StringUtils.hasText(dto.getNotes()) ? dto.getNotes().trim() : null)
                .idempotencyKey(StringUtils.hasText(dto.getIdempotencyKey()) ? dto.getIdempotencyKey().trim() : null)
                .status("COMPLETED")
                .createdBy(actorId)
                .build();

        StockAdjustment savedAdjustment = adjustmentRepository.save(adjustment);
        List<StockAdjustmentItemResponse> itemResponses = new ArrayList<>();

        for (CreateStockAdjustmentItemDto itemDto : dto.getItems()) {
            Product product = productRepository.findByTenantIdAndId(tenantId, itemDto.getProductId())
                    .orElseThrow(() -> new BadRequestException("Product not found with ID: " + itemDto.getProductId()));

            UnitDefinition baseUnit = unitRepository.findByTenantIdAndId(tenantId, product.getBaseUnitId()).orElse(null);
            UUID selectedUnitId = itemDto.getUnitId() != null ? itemDto.getUnitId() : product.getBaseUnitId();
            UnitDefinition selectedUnit = selectedUnitId.equals(product.getBaseUnitId()) ? baseUnit :
                    unitRepository.findByTenantIdAndId(tenantId, selectedUnitId).orElse(baseUnit);

            BigDecimal conversionFactor = BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP);
            if (selectedUnit != null && !selectedUnit.getId().equals(product.getBaseUnitId())) {
                conversionFactor = conversionRepository.findByTenantIdAndProductIdAndUnitId(tenantId, product.getId(), selectedUnit.getId())
                        .map(ProductUnitConversion::getConversionFactor)
                        .orElse(BigDecimal.ONE).setScale(4, RoundingMode.HALF_UP);
            }

            // Pessimistic Write Lock on balance
            StockBalance balance = balanceRepository
                    .findByTenantIdAndProductIdForUpdate(tenantId, product.getId())
                    .orElseGet(() -> {
                        StockBalance sb = StockBalance.builder()
                                .tenantId(tenantId)
                                .productId(product.getId())
                                .quantityOnHand(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                                .averageCost(product.getCostPrice() != null ? product.getCostPrice() : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                                .build();
                        return balanceRepository.save(sb);
                    });

            BigDecimal currentOnHand = balance.getQuantityOnHand() != null ? balance.getQuantityOnHand() : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
            BigDecimal quantityChange;
            BigDecimal enteredQuantity = itemDto.getEnteredQuantity();

            if (itemDto.getTargetQuantity() != null) {
                // Physical audit count mode: quantityChange = target - current
                quantityChange = itemDto.getTargetQuantity().subtract(currentOnHand).setScale(4, RoundingMode.HALF_UP);
                if (enteredQuantity == null) {
                    enteredQuantity = quantityChange.abs();
                }
            } else if (itemDto.getQuantityChange() != null) {
                quantityChange = itemDto.getQuantityChange().setScale(4, RoundingMode.HALF_UP);
                if (enteredQuantity == null) {
                    enteredQuantity = quantityChange.abs();
                }
            } else if (enteredQuantity != null) {
                quantityChange = enteredQuantity.multiply(conversionFactor).setScale(4, RoundingMode.HALF_UP);
            } else {
                throw new BadRequestException("Either quantityChange, enteredQuantity, or targetQuantity must be provided for SKU " + product.getSku());
            }

            BigDecimal newOnHand = currentOnHand.add(quantityChange).setScale(4, RoundingMode.HALF_UP);

            // Enforce negative stock prevention: reject adjustments that reduce balance below zero
            if (newOnHand.compareTo(BigDecimal.ZERO) < 0) {
                throw new BadRequestException("Adjustment of " + quantityChange +
                        " would result in negative stock (" + newOnHand + ") for product SKU: " + product.getSku() +
                        ". Current on-hand is: " + currentOnHand);
            }

            String adjustmentType = quantityChange.compareTo(BigDecimal.ZERO) >= 0 ? "INCREASE" : "DECREASE";

            // Costing treatment:
            // Downward adjustments (loss, damage) preserve average cost; valuation decreases by |change| * avgCost.
            // Upward adjustments (found stock) use supplied cost or current average cost.
            BigDecimal unitCost = balance.getAverageCost() != null ? balance.getAverageCost() :
                    (product.getCostPrice() != null ? product.getCostPrice() : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));

            if (quantityChange.compareTo(BigDecimal.ZERO) > 0 && itemDto.getUnitCost() != null) {
                BigDecimal suppliedCost = itemDto.getUnitCost().setScale(4, RoundingMode.HALF_UP);
                if (currentOnHand.compareTo(BigDecimal.ZERO) <= 0) {
                    unitCost = suppliedCost;
                    balance.setAverageCost(unitCost);
                    product.setCostPrice(unitCost);
                    productRepository.save(product);
                } else if (newOnHand.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal existingValuation = currentOnHand.multiply(unitCost);
                    BigDecimal addedValuation = quantityChange.multiply(suppliedCost);
                    unitCost = existingValuation.add(addedValuation).divide(newOnHand, 4, RoundingMode.HALF_UP);
                    balance.setAverageCost(unitCost);
                    product.setCostPrice(unitCost);
                    productRepository.save(product);
                }
            }

            BigDecimal valuationImpact = quantityChange.multiply(unitCost).setScale(4, RoundingMode.HALF_UP);

            balance.setQuantityOnHand(newOnHand);
            if ("STOCK_COUNT".equalsIgnoreCase(dto.getReasonCode())) {
                balance.setLastCountedAt(Instant.now());
            }
            balanceRepository.save(balance);

            // Record adjustment line with complete permanent historical audit fields
            StockAdjustmentItem item = StockAdjustmentItem.builder()
                    .tenantId(tenantId)
                    .adjustmentId(savedAdjustment.getId())
                    .productId(product.getId())
                    .quantityChange(quantityChange)
                    .unitCost(unitCost)
                    .adjustmentType(adjustmentType)
                    .enteredQuantity(enteredQuantity != null ? enteredQuantity : quantityChange.abs())
                    .unitId(selectedUnit != null ? selectedUnit.getId() : (baseUnit != null ? baseUnit.getId() : null))
                    .unitCode(selectedUnit != null ? selectedUnit.getCode() : (baseUnit != null ? baseUnit.getCode() : "UNIT"))
                    .conversionFactor(conversionFactor)
                    .quantityBefore(currentOnHand)
                    .quantityAfter(newOnHand)
                    .valuationImpact(valuationImpact)
                    .build();
            adjustmentItemRepository.save(item);

            // Record append-only movement in ledger
            StockMovement movement = StockMovement.builder()
                    .tenantId(tenantId)
                    .productId(product.getId())
                    .movementType(MovementType.ADJUSTMENT)
                    .referenceId(savedAdjustment.getId())
                    .referenceType("STOCK_ADJUSTMENT")
                    .quantityChange(quantityChange)
                    .balanceAfter(newOnHand)
                    .unitCost(unitCost)
                    .notes("Stock Adjustment (" + dto.getReasonCode() + "): " + (dto.getNotes() != null ? dto.getNotes() : ""))
                    .actorId(actorId)
                    .build();
            movementRepository.save(movement);

            itemResponses.add(StockAdjustmentItemResponse.builder()
                    .id(item.getId())
                    .productId(product.getId())
                    .productSku(product.getSku())
                    .productName(product.getName())
                    .quantityChange(quantityChange)
                    .previousQuantity(currentOnHand)
                    .newQuantity(newOnHand)
                    .unitCost(unitCost)
                    .totalValuationImpact(valuationImpact)
                    .adjustmentType(adjustmentType)
                    .enteredQuantity(item.getEnteredQuantity())
                    .unitId(item.getUnitId())
                    .unitCode(item.getUnitCode())
                    .conversionFactor(conversionFactor)
                    .baseUnitCode(baseUnit != null ? baseUnit.getCode() : item.getUnitCode())
                    .build());
        }

        logAudit("STOCK_ADJUSTED", savedAdjustment.getId(),
                "Recorded adjustment " + adjustmentNumber + " (" + savedAdjustment.getReasonCode() + ") with " +
                        itemResponses.size() + " items", principal, request);
        log.info("Recorded stock adjustment {} for tenant {}", adjustmentNumber, tenantId);

        String creatorName = principal != null ? principal.getFullName() : "System";

        return StockAdjustmentResponse.builder()
                .id(savedAdjustment.getId())
                .adjustmentNumber(savedAdjustment.getAdjustmentNumber())
                .reasonCode(savedAdjustment.getReasonCode())
                .notes(savedAdjustment.getNotes())
                .status(savedAdjustment.getStatus())
                .createdBy(savedAdjustment.getCreatedBy())
                .createdByName(creatorName)
                .createdAt(savedAdjustment.getCreatedAt())
                .items(itemResponses)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<StockAdjustmentResponse> listAdjustments(Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();

        Pageable safePageable = pageable;
        if (pageable != null && pageable.getSort().isSorted()) {
            List<org.springframework.data.domain.Sort.Order> safeOrders = pageable.getSort().stream()
                    .map(order -> {
                        if ("adjustedAt".equalsIgnoreCase(order.getProperty())) {
                            return new org.springframework.data.domain.Sort.Order(order.getDirection(), "createdAt");
                        }
                        if ("adjustedByName".equalsIgnoreCase(order.getProperty())) {
                            return new org.springframework.data.domain.Sort.Order(order.getDirection(), "createdBy");
                        }
                        return order;
                    })
                    .toList();
            safePageable = org.springframework.data.domain.PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    org.springframework.data.domain.Sort.by(safeOrders)
            );
        }

        Page<StockAdjustment> page = adjustmentRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId, safePageable);

        Map<UUID, String> userNames = userRepository.findAll().stream()
                .collect(Collectors.toMap(User::getId, User::getFullName, (a, b) -> a));

        List<StockAdjustmentResponse> content = page.getContent().stream()
                .map(adj -> mapAdjustmentToResponse(adj, userNames.get(adj.getCreatedBy())))
                .toList();

        return PageResponse.of(page, content);
    }

    @Override
    @Transactional(readOnly = true)
    public StockAdjustmentResponse getAdjustment(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        StockAdjustment adj = adjustmentRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Stock adjustment record not found with ID: " + id));

        String creatorName = adj.getCreatedBy() != null ?
                userRepository.findById(adj.getCreatedBy()).map(User::getFullName).orElse(null) : null;

        return mapAdjustmentToResponse(adj, creatorName);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<StockBalanceResponse> listBalances(UUID categoryId,
                                                           Boolean lowStockOnly,
                                                           String search,
                                                           Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        String cleanSearch = StringUtils.hasText(search) ? search.trim() : null;

        Map<UUID, String> categoryNames = categoryRepository.findAllByTenantId(tenantId).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));
        Map<UUID, UnitDefinition> units = unitRepository.findAllByTenantId(tenantId).stream()
                .collect(Collectors.toMap(UnitDefinition::getId, u -> u, (a, b) -> a));
        Map<UUID, StockBalance> balances = balanceRepository.findAllByTenantId(tenantId).stream()
                .collect(Collectors.toMap(StockBalance::getProductId, b -> b, (a, b) -> a));

        if (Boolean.TRUE.equals(lowStockOnly)) {
            List<Product> allProducts = (categoryId != null || cleanSearch != null)
                    ? productRepository.searchProducts(tenantId, categoryId, null, cleanSearch, Pageable.unpaged()).getContent()
                    : productRepository.findAllByTenantId(tenantId);

            List<StockBalanceResponse> allLowStock = allProducts.stream()
                    .map(p -> {
                        StockBalance b = balances.get(p.getId());
                        UnitDefinition u = units.get(p.getBaseUnitId());
                        String catName = p.getCategoryId() != null ? categoryNames.get(p.getCategoryId()) : null;

                        BigDecimal qty = b != null && b.getQuantityOnHand() != null ? b.getQuantityOnHand() : BigDecimal.ZERO;
                        BigDecimal avgCost = b != null && b.getAverageCost() != null ? b.getAverageCost() : (p.getCostPrice() != null ? p.getCostPrice() : BigDecimal.ZERO);
                        BigDecimal totalVal = qty.multiply(avgCost).setScale(4, RoundingMode.HALF_UP);
                        BigDecimal threshold = p.getReorderThreshold() != null ? p.getReorderThreshold() : BigDecimal.ZERO;
                        boolean isLow = qty.compareTo(threshold) <= 0;

                        return StockBalanceResponse.builder()
                                .productId(p.getId())
                                .productSku(p.getSku())
                                .productName(p.getName())
                                .categoryId(p.getCategoryId())
                                .categoryName(catName)
                                .baseUnitId(p.getBaseUnitId())
                                .baseUnitCode(u != null ? u.getCode() : null)
                                .baseUnitName(u != null ? u.getName() : null)
                                .quantityOnHand(qty)
                                .averageCost(avgCost)
                                .totalValuation(totalVal)
                                .reorderThreshold(threshold)
                                .isLowStock(isLow)
                                .lastCountedAt(b != null ? b.getLastCountedAt() : null)
                                .updatedAt(b != null ? b.getUpdatedAt() : p.getUpdatedAt())
                                .build();
                    })
                    .filter(StockBalanceResponse::isLowStock)
                    .toList();

            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), allLowStock.size());
            List<StockBalanceResponse> pagedLowStock = start < allLowStock.size() ? allLowStock.subList(start, end) : Collections.emptyList();
            Page<StockBalanceResponse> pagedResult = new org.springframework.data.domain.PageImpl<>(pagedLowStock, pageable, allLowStock.size());
            return PageResponse.of(pagedResult);
        }

        Page<Product> productPage = productRepository.searchProducts(tenantId, categoryId, null, cleanSearch, pageable);

        List<StockBalanceResponse> list = productPage.getContent().stream()
                .map(p -> {
                    StockBalance b = balances.get(p.getId());
                    UnitDefinition u = units.get(p.getBaseUnitId());
                    String catName = p.getCategoryId() != null ? categoryNames.get(p.getCategoryId()) : null;

                    BigDecimal qty = b != null && b.getQuantityOnHand() != null ? b.getQuantityOnHand() : BigDecimal.ZERO;
                    BigDecimal avgCost = b != null && b.getAverageCost() != null ? b.getAverageCost() : (p.getCostPrice() != null ? p.getCostPrice() : BigDecimal.ZERO);
                    BigDecimal totalVal = qty.multiply(avgCost).setScale(4, RoundingMode.HALF_UP);
                    BigDecimal threshold = p.getReorderThreshold() != null ? p.getReorderThreshold() : BigDecimal.ZERO;
                    boolean isLow = qty.compareTo(threshold) <= 0;

                    return StockBalanceResponse.builder()
                            .productId(p.getId())
                            .productSku(p.getSku())
                            .productName(p.getName())
                            .categoryId(p.getCategoryId())
                            .categoryName(catName)
                            .baseUnitId(p.getBaseUnitId())
                            .baseUnitCode(u != null ? u.getCode() : null)
                            .baseUnitName(u != null ? u.getName() : null)
                            .quantityOnHand(qty)
                            .averageCost(avgCost)
                            .totalValuation(totalVal)
                            .reorderThreshold(threshold)
                            .isLowStock(isLow)
                            .lastCountedAt(b != null ? b.getLastCountedAt() : null)
                            .updatedAt(b != null ? b.getUpdatedAt() : p.getUpdatedAt())
                            .build();
                })
                .toList();

        return PageResponse.of(productPage, list);
    }

    @Override
    @Transactional(readOnly = true)
    public StockBalanceResponse getBalance(UUID productId) {
        UUID tenantId = TenantContext.requireTenantId();
        Product p = productRepository.findByTenantIdAndId(tenantId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));

        StockBalance b = balanceRepository.findByTenantIdAndProductId(tenantId, productId).orElse(null);
        UnitDefinition u = unitRepository.findByTenantIdAndId(tenantId, p.getBaseUnitId()).orElse(null);
        String catName = p.getCategoryId() != null ?
                categoryRepository.findByTenantIdAndId(tenantId, p.getCategoryId()).map(Category::getName).orElse(null) : null;

        BigDecimal qty = b != null ? b.getQuantityOnHand() : BigDecimal.ZERO;
        BigDecimal avgCost = b != null ? b.getAverageCost() : p.getCostPrice();
        BigDecimal totalVal = qty.multiply(avgCost).setScale(4, RoundingMode.HALF_UP);

        return StockBalanceResponse.builder()
                .productId(p.getId())
                .productSku(p.getSku())
                .productName(p.getName())
                .categoryId(p.getCategoryId())
                .categoryName(catName)
                .baseUnitId(p.getBaseUnitId())
                .baseUnitCode(u != null ? u.getCode() : null)
                .baseUnitName(u != null ? u.getName() : null)
                .quantityOnHand(qty)
                .averageCost(avgCost)
                .totalValuation(totalVal)
                .reorderThreshold(p.getReorderThreshold())
                .isLowStock(qty.compareTo(p.getReorderThreshold()) <= 0)
                .lastCountedAt(b != null ? b.getLastCountedAt() : null)
                .updatedAt(b != null ? b.getUpdatedAt() : p.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<StockMovementResponse> listMovements(UUID productId,
                                                             MovementType movementType,
                                                             Pageable pageable) {
        return listMovements(productId, movementType, null, null, null, null, null, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<StockMovementResponse> listMovements(UUID productId,
                                                             MovementType movementType,
                                                             String referenceType,
                                                             UUID actorId,
                                                             Instant startDate,
                                                             Instant endDate,
                                                             String search,
                                                             Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();

        Specification<StockMovement> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("tenantId"), tenantId));

            if (productId != null) {
                predicates.add(cb.equal(root.get("productId"), productId));
            }
            if (movementType != null) {
                predicates.add(cb.equal(root.get("movementType"), movementType));
            }
            if (StringUtils.hasText(referenceType)) {
                predicates.add(cb.equal(root.get("referenceType"), referenceType.trim()));
            }
            if (actorId != null) {
                predicates.add(cb.equal(root.get("actorId"), actorId));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate));
            }
            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("notes")), pattern));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<StockMovement> page = movementRepository.findAll(spec, pageable);

        Map<UUID, Product> products = productRepository.findAllByTenantId(tenantId).stream()
                .collect(Collectors.toMap(Product::getId, p -> p, (a, b) -> a));
        Map<UUID, UnitDefinition> units = unitRepository.findAllByTenantId(tenantId).stream()
                .collect(Collectors.toMap(UnitDefinition::getId, u -> u, (a, b) -> a));
        Map<UUID, String> userEmails = userRepository.findAll().stream()
                .collect(Collectors.toMap(User::getId, User::getEmail, (a, b) -> a));

        List<StockMovementResponse> content = page.getContent().stream()
                .map(m -> {
                    Product p = products.get(m.getProductId());
                    UnitDefinition u = p != null ? units.get(p.getBaseUnitId()) : null;
                    return StockMovementResponse.builder()
                            .id(m.getId())
                            .productId(m.getProductId())
                            .productSku(p != null ? p.getSku() : null)
                            .productName(p != null ? p.getName() : null)
                            .baseUnitCode(u != null ? u.getCode() : null)
                            .movementType(m.getMovementType())
                            .referenceId(m.getReferenceId())
                            .referenceType(m.getReferenceType())
                            .quantityChange(m.getQuantityChange())
                            .balanceAfter(m.getBalanceAfter())
                            .unitCost(m.getUnitCost())
                            .notes(m.getNotes())
                            .actorId(m.getActorId())
                            .actorEmail(m.getActorId() != null ? userEmails.get(m.getActorId()) : "System")
                            .createdAt(m.getCreatedAt())
                            .build();
                })
                .toList();

        return PageResponse.of(page, content);
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryValuationReport getValuationReport() {
        UUID tenantId = TenantContext.requireTenantId();
        Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
        String currency = tenant != null && StringUtils.hasText(tenant.getCurrency()) ? tenant.getCurrency() : "ETB";

        List<Product> products = productRepository.findAllByTenantId(tenantId);
        List<StockBalance> balances = balanceRepository.findAllByTenantId(tenantId);
        Map<UUID, StockBalance> balanceMap = balances.stream()
                .collect(Collectors.toMap(StockBalance::getProductId, b -> b, (a, b) -> a));

        List<Category> categories = categoryRepository.findAllByTenantId(tenantId);
        Map<UUID, String> categoryNames = categories.stream()
                .collect(Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));

        long totalSkus = products.size();
        long inStockSkus = 0;
        long lowStockSkus = 0;
        long outOfStockSkus = 0;
        BigDecimal totalValuation = BigDecimal.ZERO;

        Map<UUID, List<Product>> byCategory = products.stream()
                .collect(Collectors.groupingBy(p -> p.getCategoryId() != null ? p.getCategoryId() : UUID.fromString("00000000-0000-0000-0000-000000000000")));

        List<CategoryValuationSummary> categorySummaries = new ArrayList<>();

        for (Product p : products) {
            StockBalance b = balanceMap.get(p.getId());
            BigDecimal qty = b != null ? b.getQuantityOnHand() : BigDecimal.ZERO;
            BigDecimal avgCost = b != null ? b.getAverageCost() : p.getCostPrice();
            BigDecimal val = qty.multiply(avgCost);

            totalValuation = totalValuation.add(val);

            if (qty.compareTo(BigDecimal.ZERO) <= 0) {
                outOfStockSkus++;
            } else {
                inStockSkus++;
            }

            if (qty.compareTo(p.getReorderThreshold()) <= 0) {
                lowStockSkus++;
            }
        }

        for (Map.Entry<UUID, List<Product>> entry : byCategory.entrySet()) {
            UUID catId = entry.getKey();
            String catName = catId.equals(UUID.fromString("00000000-0000-0000-0000-000000000000")) ?
                    "Uncategorized" : categoryNames.getOrDefault(catId, "General");

            BigDecimal catQty = BigDecimal.ZERO;
            BigDecimal catVal = BigDecimal.ZERO;

            for (Product p : entry.getValue()) {
                StockBalance b = balanceMap.get(p.getId());
                BigDecimal qty = b != null ? b.getQuantityOnHand() : BigDecimal.ZERO;
                BigDecimal avgCost = b != null ? b.getAverageCost() : p.getCostPrice();
                catQty = catQty.add(qty);
                catVal = catVal.add(qty.multiply(avgCost));
            }

            categorySummaries.add(CategoryValuationSummary.builder()
                    .categoryId(catId.equals(UUID.fromString("00000000-0000-0000-0000-000000000000")) ? null : catId)
                    .categoryName(catName)
                    .productCount(entry.getValue().size())
                    .totalQuantity(catQty)
                    .totalValuation(catVal.setScale(4, RoundingMode.HALF_UP))
                    .build());
        }

        return InventoryValuationReport.builder()
                .totalSkus(totalSkus)
                .inStockSkus(inStockSkus)
                .lowStockSkus(lowStockSkus)
                .outOfStockSkus(outOfStockSkus)
                .totalValuation(totalValuation.setScale(4, RoundingMode.HALF_UP))
                .currency(currency)
                .categoryBreakdowns(categorySummaries)
                .generatedAt(Instant.now())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<StockBalanceResponse> getLowStockReport(Pageable pageable) {
        return listBalances(null, true, null, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CurrentInventoryItemDto> getCurrentInventoryReport(UUID categoryId,
                                                                          String search,
                                                                          String stockStatus,
                                                                          Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        String cleanSearch = StringUtils.hasText(search) ? search.trim() : null;

        List<Product> products = productRepository.findAllByTenantId(tenantId);
        if (categoryId != null) {
            products = products.stream()
                    .filter(p -> categoryId.equals(p.getCategoryId()))
                    .toList();
        }
        if (cleanSearch != null) {
            String term = cleanSearch.toLowerCase();
            products = products.stream()
                    .filter(p -> (p.getName() != null && p.getName().toLowerCase().contains(term))
                            || (p.getSku() != null && p.getSku().toLowerCase().contains(term))
                            || (p.getBrand() != null && p.getBrand().toLowerCase().contains(term)))
                    .toList();
        }

        Map<UUID, String> categoryNames = categoryRepository.findAllByTenantId(tenantId).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));
        Map<UUID, UnitDefinition> units = unitRepository.findAllByTenantId(tenantId).stream()
                .collect(Collectors.toMap(UnitDefinition::getId, u -> u, (a, b) -> a));
        Map<UUID, StockBalance> balances = balanceRepository.findAllByTenantId(tenantId).stream()
                .collect(Collectors.toMap(StockBalance::getProductId, b -> b, (a, b) -> a));
        Map<UUID, List<ProductUnitConversion>> conversionsByProduct = conversionRepository.findAllByTenantId(tenantId).stream()
                .collect(Collectors.groupingBy(ProductUnitConversion::getProductId));

        List<CurrentInventoryItemDto> allItems = products.stream()
                .map(p -> {
                    StockBalance b = balances.get(p.getId());
                    UnitDefinition u = units.get(p.getBaseUnitId());
                    String catName = p.getCategoryId() != null ? categoryNames.get(p.getCategoryId()) : null;

                    BigDecimal qty = b != null ? b.getQuantityOnHand() : BigDecimal.ZERO;
                    BigDecimal avgCost = b != null ? b.getAverageCost() : p.getCostPrice();
                    BigDecimal totalVal = qty.compareTo(BigDecimal.ZERO) > 0
                            ? qty.multiply(avgCost).setScale(4, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

                    String computedStatus;
                    if (qty.compareTo(BigDecimal.ZERO) <= 0) {
                        computedStatus = "OUT_OF_STOCK";
                    } else if (p.getReorderThreshold() != null && qty.compareTo(p.getReorderThreshold()) <= 0) {
                        computedStatus = "LOW_STOCK";
                    } else {
                        computedStatus = "IN_STOCK";
                    }

                    List<ProductUnitConversionInfo> convInfos = new ArrayList<>();
                    List<ProductUnitConversion> convs = conversionsByProduct.getOrDefault(p.getId(), List.of());
                    for (ProductUnitConversion c : convs) {
                        UnitDefinition secUnit = units.get(c.getUnitId());
                        BigDecimal factor = c.getConversionFactor();
                        BigDecimal eqQty = factor != null && factor.compareTo(BigDecimal.ZERO) > 0
                                ? qty.divide(factor, 4, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO;

                        convInfos.add(ProductUnitConversionInfo.builder()
                                .unitId(c.getUnitId())
                                .unitCode(secUnit != null ? secUnit.getCode() : null)
                                .unitName(secUnit != null ? secUnit.getName() : null)
                                .conversionFactor(factor)
                                .equivalentQuantity(eqQty)
                                .isSellingUnit(c.isSellingUnit())
                                .isReceivingUnit(c.isReceivingUnit())
                                .build());
                    }

                    return CurrentInventoryItemDto.builder()
                            .productId(p.getId())
                            .productSku(p.getSku())
                            .productName(p.getName())
                            .categoryId(p.getCategoryId())
                            .categoryName(catName)
                            .quantityOnHand(qty)
                            .baseUnitId(p.getBaseUnitId())
                            .baseUnitCode(u != null ? u.getCode() : null)
                            .baseUnitName(u != null ? u.getName() : null)
                            .averageCost(avgCost)
                            .totalValuation(totalVal)
                            .reorderThreshold(p.getReorderThreshold())
                            .stockStatus(computedStatus)
                            .conversions(convInfos)
                            .lastCountedAt(b != null ? b.getLastCountedAt() : null)
                            .updatedAt(b != null ? b.getUpdatedAt() : p.getUpdatedAt())
                            .build();
                })
                .filter(item -> {
                    if (!StringUtils.hasText(stockStatus) || "ALL".equalsIgnoreCase(stockStatus.trim())) {
                        return true;
                    }
                    return item.getStockStatus().equalsIgnoreCase(stockStatus.trim());
                })
                .sorted((a, b) -> {
                    if (pageable.getSort().isSorted()) {
                        for (org.springframework.data.domain.Sort.Order order : pageable.getSort()) {
                            int cmp = 0;
                            if ("name".equalsIgnoreCase(order.getProperty()) || "productName".equalsIgnoreCase(order.getProperty())) {
                                cmp = a.getProductName().compareToIgnoreCase(b.getProductName());
                            } else if ("sku".equalsIgnoreCase(order.getProperty()) || "productSku".equalsIgnoreCase(order.getProperty())) {
                                cmp = a.getProductSku().compareToIgnoreCase(b.getProductSku());
                            } else if ("quantityOnHand".equalsIgnoreCase(order.getProperty())) {
                                cmp = a.getQuantityOnHand().compareTo(b.getQuantityOnHand());
                            } else if ("totalValuation".equalsIgnoreCase(order.getProperty())) {
                                cmp = a.getTotalValuation().compareTo(b.getTotalValuation());
                            }
                            if (cmp != 0) {
                                return order.isAscending() ? cmp : -cmp;
                            }
                        }
                    }
                    return a.getProductName().compareToIgnoreCase(b.getProductName());
                })
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allItems.size());
        List<CurrentInventoryItemDto> sublist = (start <= allItems.size()) ? allItems.subList(start, end) : List.of();

        Page<CurrentInventoryItemDto> page = new PageImpl<>(sublist, pageable, allItems.size());
        return PageResponse.of(page, sublist);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CurrentInventoryItemDto> getDetailedValuationReport(UUID categoryId,
                                                                           String search,
                                                                           Pageable pageable) {
        return getCurrentInventoryReport(categoryId, search, null, pageable);
    }

    private StockReceivingResponse mapReceivingToResponse(StockReceiving rcv, String receiverName) {
        List<StockReceivingItem> items = receivingItemRepository.findAllByTenantIdAndReceivingId(rcv.getTenantId(), rcv.getId());

        Map<UUID, Product> products = productRepository.findAllByTenantId(rcv.getTenantId()).stream()
                .collect(Collectors.toMap(Product::getId, p -> p, (a, b) -> a));
        Map<UUID, UnitDefinition> units = unitRepository.findAllByTenantId(rcv.getTenantId()).stream()
                .collect(Collectors.toMap(UnitDefinition::getId, u -> u, (a, b) -> a));

        List<StockReceivingItemResponse> itemResponses = items.stream()
                .map(item -> {
                    Product p = products.get(item.getProductId());
                    UnitDefinition rcvUnit = units.get(item.getReceivedUnitId());
                    UnitDefinition baseUnit = p != null ? units.get(p.getBaseUnitId()) : null;
                    return StockReceivingItemResponse.builder()
                            .id(item.getId())
                            .productId(item.getProductId())
                            .productSku(p != null ? p.getSku() : null)
                            .productName(p != null ? p.getName() : null)
                            .receivedUnitId(item.getReceivedUnitId())
                            .receivedUnitCode(rcvUnit != null ? rcvUnit.getCode() : null)
                            .receivedQuantity(item.getReceivedQuantity())
                            .conversionFactor(item.getConversionFactor())
                            .baseQuantity(item.getBaseQuantity())
                            .baseUnitCode(baseUnit != null ? baseUnit.getCode() : null)
                            .unitCost(item.getUnitCost())
                            .totalCost(item.getTotalCost())
                            .build();
                })
                .toList();

        return StockReceivingResponse.builder()
                .id(rcv.getId())
                .receivingNumber(rcv.getReceivingNumber())
                .supplierName(rcv.getSupplierName())
                .deliveryNoteRef(rcv.getDeliveryNoteRef())
                .status(rcv.getStatus())
                .totalCost(rcv.getTotalCost())
                .notes(rcv.getNotes())
                .receivedBy(rcv.getReceivedBy())
                .receivedByName(receiverName)
                .receivedAt(rcv.getReceivedAt())
                .createdAt(rcv.getCreatedAt())
                .items(itemResponses)
                .build();
    }

    private StockAdjustmentResponse mapAdjustmentToResponse(StockAdjustment adj, String creatorName) {
        List<StockAdjustmentItem> items = adjustmentItemRepository.findAllByTenantIdAndAdjustmentId(adj.getTenantId(), adj.getId());

        Map<UUID, Product> products = productRepository.findAllByTenantId(adj.getTenantId()).stream()
                .collect(Collectors.toMap(Product::getId, p -> p, (a, b) -> a));

        Map<UUID, UnitDefinition> units = unitRepository.findAllByTenantId(adj.getTenantId()).stream()
                .collect(Collectors.toMap(UnitDefinition::getId, u -> u, (a, b) -> a));

        List<StockAdjustmentItemResponse> itemResponses = items.stream()
                .map(item -> {
            Product p = products.get(item.getProductId());
            UnitDefinition baseUom = p != null && p.getBaseUnitId() != null ? units.get(p.getBaseUnitId()) : null;
            String baseUnitCode = baseUom != null ? baseUom.getCode() : item.getUnitCode();

            BigDecimal valImpact = item.getValuationImpact() != null ? item.getValuationImpact() :
                    item.getQuantityChange().multiply(item.getUnitCost()).setScale(4, RoundingMode.HALF_UP);

            BigDecimal qtyBefore = item.getQuantityBefore() != null ? item.getQuantityBefore() : BigDecimal.ZERO;
            BigDecimal qtyAfter = item.getQuantityAfter() != null ? item.getQuantityAfter() :
                    qtyBefore.add(item.getQuantityChange()).setScale(4, RoundingMode.HALF_UP);

            String adjType = item.getAdjustmentType() != null ? item.getAdjustmentType() :
                    (item.getQuantityChange().compareTo(BigDecimal.ZERO) >= 0 ? "INCREASE" : "DECREASE");

            BigDecimal enteredQty = item.getEnteredQuantity() != null ? item.getEnteredQuantity() : item.getQuantityChange().abs();
            BigDecimal convFactor = item.getConversionFactor() != null ? item.getConversionFactor() : BigDecimal.ONE;

            return StockAdjustmentItemResponse.builder()
                    .id(item.getId())
                    .productId(item.getProductId())
                    .productSku(p != null ? p.getSku() : null)
                    .productName(p != null ? p.getName() : null)
                    .quantityChange(item.getQuantityChange())
                    .previousQuantity(qtyBefore)
                    .newQuantity(qtyAfter)
                    .unitCost(item.getUnitCost())
                    .totalValuationImpact(valImpact)
                    .adjustmentType(adjType)
                    .enteredQuantity(enteredQty)
                    .unitId(item.getUnitId())
                    .unitCode(item.getUnitCode() != null ? item.getUnitCode() : baseUnitCode)
                    .conversionFactor(convFactor)
                    .baseUnitCode(baseUnitCode)
                    .build();
        })
                .toList();

        return StockAdjustmentResponse.builder()
                .id(adj.getId())
                .adjustmentNumber(adj.getAdjustmentNumber())
                .reasonCode(adj.getReasonCode())
                .notes(adj.getNotes())
                .status(adj.getStatus())
                .createdBy(adj.getCreatedBy())
                .createdByName(creatorName)
                .createdAt(adj.getCreatedAt())
                .items(itemResponses)
                .build();
    }

    private String generateReceivingNumber(UUID tenantId) {
        int year = Year.now().getValue();
        int randomDigits = 10000 + new Random().nextInt(90000);
        return "RCV-" + year + "-" + randomDigits;
    }

    private String generateAdjustmentNumber(UUID tenantId) {
        int year = Year.now().getValue();
        int randomDigits = 10000 + new Random().nextInt(90000);
        return "ADJ-" + year + "-" + randomDigits;
    }

    private void logAudit(String action, UUID entityId, String details, KuterUserPrincipal principal, HttpServletRequest request) {
        log.info("[INVENTORY_OP] action={} entityId={} details={}", action, entityId, details);
    }
}
