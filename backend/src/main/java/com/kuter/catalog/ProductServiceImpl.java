package com.kuter.catalog;

import com.kuter.audit.AuditService;
import com.kuter.catalog.dto.CreateProductDto;
import com.kuter.catalog.dto.CreateProductUnitConversionDto;
import com.kuter.catalog.dto.ProductDetailResponse;
import com.kuter.catalog.dto.ProductSummaryResponse;
import com.kuter.catalog.dto.ProductUnitConversionResponse;
import com.kuter.catalog.dto.UpdateProductDto;
import com.kuter.catalog.dto.UpdateSellingPriceDto;
import com.kuter.common.BadRequestException;
import com.kuter.common.ConflictException;
import com.kuter.common.ForbiddenException;
import com.kuter.common.PageResponse;
import com.kuter.common.ResourceNotFoundException;
import com.kuter.identity.KuterUserPrincipal;
import com.kuter.identity.TenantRole;
import com.kuter.inventory.MovementType;
import com.kuter.inventory.StockBalance;
import com.kuter.inventory.StockBalanceRepository;
import com.kuter.inventory.StockMovement;
import com.kuter.inventory.StockMovementRepository;
import com.kuter.tenancy.TenantContext;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductServiceImpl.class);

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UnitDefinitionRepository unitRepository;
    private final ProductUnitConversionRepository conversionRepository;
    private final StockBalanceRepository balanceRepository;
    private final StockMovementRepository movementRepository;
    private final AuditService auditService;
    private final com.kuter.subscriptions.EntitlementService entitlementService;

    public ProductServiceImpl(ProductRepository productRepository,
                              CategoryRepository categoryRepository,
                              UnitDefinitionRepository unitRepository,
                              ProductUnitConversionRepository conversionRepository,
                              StockBalanceRepository balanceRepository,
                              StockMovementRepository movementRepository,
                              AuditService auditService,
                              com.kuter.subscriptions.EntitlementService entitlementService) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.unitRepository = unitRepository;
        this.conversionRepository = conversionRepository;
        this.balanceRepository = balanceRepository;
        this.movementRepository = movementRepository;
        this.auditService = auditService;
        this.entitlementService = entitlementService;
    }

    @Override
    @Transactional
    public ProductDetailResponse createProduct(CreateProductDto dto, KuterUserPrincipal principal, HttpServletRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        entitlementService.checkProductLimit(tenantId);

        String sku = dto.getSku().trim().toUpperCase();

        if (productRepository.existsByTenantIdAndSku(tenantId, sku)) {
            throw new ConflictException("Product with SKU '" + sku + "' already exists in this shop");
        }

        // Validate Base Unit belongs to this tenant
        UnitDefinition baseUnit = unitRepository.findByTenantIdAndId(tenantId, dto.getBaseUnitId())
                .orElseThrow(() -> new BadRequestException("Base unit definition not found in this shop with ID: " + dto.getBaseUnitId()));

        // Validate Category if specified
        Category category = null;
        if (dto.getCategoryId() != null) {
            category = categoryRepository.findByTenantIdAndId(tenantId, dto.getCategoryId())
                    .orElseThrow(() -> new BadRequestException("Category not found in this shop with ID: " + dto.getCategoryId()));
        }

        validateImageMetadata(dto.getImageUrl(), dto.getImageStorageKey());

        BigDecimal costPrice = dto.getCostPrice() != null ? dto.getCostPrice() : BigDecimal.ZERO;
        BigDecimal sellingPrice = BigDecimal.ZERO;
        if (dto.getSellingPrice() != null && dto.getSellingPrice().compareTo(BigDecimal.ZERO) > 0) {
            if (isCallerOwner(tenantId, principal)) {
                sellingPrice = dto.getSellingPrice();
            } else {
                throw new ForbiddenException("Access denied: Only the shop owner can set product selling prices.");
            }
        }
        BigDecimal reorderThreshold = dto.getReorderThreshold() != null ? dto.getReorderThreshold() : BigDecimal.ZERO;

        Product product = Product.builder()
                .tenantId(tenantId)
                .sku(sku)
                .name(dto.getName().trim())
                .description(StringUtils.hasText(dto.getDescription()) ? dto.getDescription().trim() : null)
                .categoryId(category != null ? category.getId() : null)
                .baseUnitId(baseUnit.getId())
                .costPrice(costPrice)
                .sellingPrice(sellingPrice)
                .reorderThreshold(reorderThreshold)
                .status(ProductStatus.ACTIVE)
                .imageUrl(StringUtils.hasText(dto.getImageUrl()) ? dto.getImageUrl().trim() : null)
                .imageStorageKey(StringUtils.hasText(dto.getImageStorageKey()) ? dto.getImageStorageKey().trim() : null)
                .barcode(StringUtils.hasText(dto.getBarcode()) ? dto.getBarcode().trim() : null)
                .brand(StringUtils.hasText(dto.getBrand()) ? dto.getBrand().trim() : null)
                .build();

        Product savedProduct = productRepository.save(product);

        // Initialize Stock Balance
        BigDecimal initialQty = (dto.getInitialQuantity() != null && dto.getInitialQuantity().compareTo(BigDecimal.ZERO) > 0)
                ? dto.getInitialQuantity() : BigDecimal.ZERO;

        StockBalance balance = StockBalance.builder()
                .tenantId(tenantId)
                .productId(savedProduct.getId())
                .quantityOnHand(initialQty)
                .averageCost(costPrice)
                .lastCountedAt(initialQty.compareTo(BigDecimal.ZERO) > 0 ? Instant.now() : null)
                .build();
        balanceRepository.save(balance);

        // Record opening stock movement if initial quantity was supplied
        if (initialQty.compareTo(BigDecimal.ZERO) > 0) {
            StockMovement movement = StockMovement.builder()
                    .tenantId(tenantId)
                    .productId(savedProduct.getId())
                    .movementType(MovementType.RECEIPT)
                    .quantityChange(initialQty)
                    .balanceAfter(initialQty)
                    .unitCost(costPrice)
                    .referenceType("OPENING_STOCK")
                    .notes("Initial opening inventory balance for new product")
                    .actorId(principal != null ? principal.getUserId() : null)
                    .build();
            movementRepository.save(movement);
        }

        // Add initial unit conversions if supplied
        if (dto.getConversions() != null && !dto.getConversions().isEmpty()) {
            for (CreateProductUnitConversionDto convDto : dto.getConversions()) {
                if (convDto.getUnitId().equals(baseUnit.getId())) {
                    throw new BadRequestException("Cannot define secondary conversion factor for the base unit itself (" + baseUnit.getCode() + ")");
                }
                UnitDefinition secUnit = unitRepository.findByTenantIdAndId(tenantId, convDto.getUnitId())
                        .orElseThrow(() -> new BadRequestException("Secondary unit definition not found in this shop with ID: " + convDto.getUnitId()));

                ProductUnitConversion conversion = ProductUnitConversion.builder()
                        .tenantId(tenantId)
                        .productId(savedProduct.getId())
                        .unitId(secUnit.getId())
                        .conversionFactor(convDto.getConversionFactor())
                        .isSellingUnit(convDto.isSellingUnit())
                        .isReceivingUnit(convDto.isReceivingUnit())
                        .build();
                conversionRepository.save(conversion);
            }
        }

        logAudit("PRODUCT_CREATED", savedProduct.getId(),
                "Created product SKU: " + savedProduct.getSku() + ", name: " + savedProduct.getName(),
                principal, request);
        log.info("Created product '{}' (SKU: {}) in tenant {}", savedProduct.getName(), savedProduct.getSku(), tenantId);

        return getProduct(savedProduct.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductSummaryResponse> listProducts(UUID categoryId,
                                                             ProductStatus status,
                                                             String search,
                                                             Boolean lowStockOnly,
                                                             Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        String cleanSearch = StringUtils.hasText(search) ? search.trim() : null;

        Page<Product> page = productRepository.searchProducts(tenantId, categoryId, status, cleanSearch, pageable);

        // Preload caches for fast mapping
        Map<UUID, String> categoryNames = categoryRepository.findAllByTenantId(tenantId).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));
        Map<UUID, UnitDefinition> units = unitRepository.findAllByTenantId(tenantId).stream()
                .collect(Collectors.toMap(UnitDefinition::getId, u -> u, (a, b) -> a));
        Map<UUID, StockBalance> balances = balanceRepository.findAllByTenantId(tenantId).stream()
                .collect(Collectors.toMap(StockBalance::getProductId, b -> b, (a, b) -> a));

        List<ProductSummaryResponse> content = page.getContent().stream()
                .map(product -> mapToSummary(product, categoryNames, units, balances))
                .filter(summary -> {
                    if (Boolean.TRUE.equals(lowStockOnly)) {
                        return summary.isLowStock();
                    }
                    return true;
                })
                .toList();

        return PageResponse.of(page, content);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDetailResponse getProduct(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        Product product = productRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));

        String categoryName = null;
        if (product.getCategoryId() != null) {
            categoryName = categoryRepository.findByTenantIdAndId(tenantId, product.getCategoryId())
                    .map(Category::getName).orElse(null);
        }

        UnitDefinition baseUnit = unitRepository.findByTenantIdAndId(tenantId, product.getBaseUnitId())
                .orElse(null);

        StockBalance balance = balanceRepository.findByTenantIdAndProductId(tenantId, product.getId())
                .orElse(null);

        List<ProductUnitConversionResponse> conversions = listProductUnits(product.getId());

        BigDecimal qty = balance != null ? balance.getQuantityOnHand() : BigDecimal.ZERO;
        BigDecimal avgCost = balance != null ? balance.getAverageCost() : product.getCostPrice();
        BigDecimal valuation = qty.multiply(avgCost);
        boolean isLowStock = qty.compareTo(product.getReorderThreshold()) <= 0;

        return ProductDetailResponse.builder()
                .id(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .description(product.getDescription())
                .categoryId(product.getCategoryId())
                .categoryName(categoryName)
                .baseUnitId(product.getBaseUnitId())
                .baseUnitCode(baseUnit != null ? baseUnit.getCode() : null)
                .baseUnitName(baseUnit != null ? baseUnit.getName() : null)
                .costPrice(product.getCostPrice())
                .sellingPrice(product.getSellingPrice())
                .reorderThreshold(product.getReorderThreshold())
                .status(product.getStatus())
                .imageUrl(product.getImageUrl())
                .imageStorageKey(product.getImageStorageKey())
                .barcode(product.getBarcode())
                .brand(product.getBrand())
                .quantityOnHand(qty)
                .averageCost(avgCost)
                .totalValuation(valuation)
                .isLowStock(isLowStock)
                .conversions(conversions)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    public ProductDetailResponse updateProduct(UUID id, UpdateProductDto dto, KuterUserPrincipal principal, HttpServletRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        Product product = productRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));

        // Validate Category if specified
        if (dto.getCategoryId() != null) {
            categoryRepository.findByTenantIdAndId(tenantId, dto.getCategoryId())
                    .orElseThrow(() -> new BadRequestException("Category not found in this shop with ID: " + dto.getCategoryId()));
            product.setCategoryId(dto.getCategoryId());
        } else if (dto.getCategoryId() == null && dto.getDescription() != null) {
            // Note: null category is allowed
        }

        validateImageMetadata(dto.getImageUrl(), dto.getImageStorageKey());

        product.setName(dto.getName().trim());
        if (dto.getDescription() != null) {
            product.setDescription(StringUtils.hasText(dto.getDescription()) ? dto.getDescription().trim() : null);
        }
        if (dto.getSellingPrice() != null && dto.getSellingPrice().compareTo(product.getSellingPrice()) != 0) {
            if (!isCallerOwner(tenantId, principal)) {
                throw new ForbiddenException("Access denied: Only the shop owner can modify product selling prices.");
            }
            BigDecimal oldPrice = product.getSellingPrice();
            product.setSellingPrice(dto.getSellingPrice());
            logAudit("PRODUCT_PRICE_CHANGED", product.getId(),
                    "Selling price changed for SKU " + product.getSku() + " from " + oldPrice + " to " + dto.getSellingPrice(),
                    principal, request);
        }
        if (dto.getReorderThreshold() != null) {
            product.setReorderThreshold(dto.getReorderThreshold());
        }
        if (dto.getImageUrl() != null) {
            product.setImageUrl(StringUtils.hasText(dto.getImageUrl()) ? dto.getImageUrl().trim() : null);
        }
        if (dto.getImageStorageKey() != null) {
            product.setImageStorageKey(StringUtils.hasText(dto.getImageStorageKey()) ? dto.getImageStorageKey().trim() : null);
        }
        if (dto.getBarcode() != null) {
            product.setBarcode(StringUtils.hasText(dto.getBarcode()) ? dto.getBarcode().trim() : null);
        }
        if (dto.getBrand() != null) {
            product.setBrand(StringUtils.hasText(dto.getBrand()) ? dto.getBrand().trim() : null);
        }
        if (dto.getStatus() != null) {
            product.setStatus(dto.getStatus());
        }

        Product updated = productRepository.save(product);

        logAudit("PRODUCT_UPDATED", updated.getId(), "Updated product: " + updated.getSku() + " - " + updated.getName(), principal, request);
        log.info("Updated product '{}' ({}) in tenant {}", updated.getName(), updated.getId(), tenantId);

        return getProduct(updated.getId());
    }

    @Override
    @Transactional
    public ProductDetailResponse updateProductStatus(UUID id, ProductStatus status, KuterUserPrincipal principal, HttpServletRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        Product product = productRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));

        product.setStatus(status);
        Product updated = productRepository.save(product);

        logAudit("PRODUCT_STATUS_CHANGED", updated.getId(),
                "Changed status of SKU " + updated.getSku() + " to " + status, principal, request);
        log.info("Changed status of product '{}' to {} in tenant {}", updated.getSku(), status, tenantId);

        return getProduct(updated.getId());
    }

    @Override
    @Transactional
    public void deleteProduct(UUID id, KuterUserPrincipal principal, HttpServletRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        Product product = productRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));

        // Check if historical stock movements exist
        List<StockMovement> movements = movementRepository.findByTenantIdAndProductIdOrderByCreatedAtDesc(tenantId, id);
        if (!movements.isEmpty()) {
            // Cannot destructively delete product with inventory history; soft-deactivate instead
            product.setStatus(ProductStatus.INACTIVE);
            productRepository.save(product);
            logAudit("PRODUCT_DEACTIVATED", product.getId(),
                    "Soft-deactivated product with " + movements.size() + " stock ledger movements: " + product.getSku(),
                    principal, request);
            log.info("Soft-deactivated product '{}' ({}) because it has {} movement ledger records",
                    product.getSku(), product.getId(), movements.size());
        } else {
            // Delete conversions and stock balance
            conversionRepository.findAllByTenantIdAndProductId(tenantId, id)
                    .forEach(conversionRepository::delete);
            balanceRepository.findByTenantIdAndProductId(tenantId, id)
                    .ifPresent(balanceRepository::delete);
            productRepository.delete(product);

            logAudit("PRODUCT_DELETED", product.getId(),
                    "Deleted unused product: " + product.getSku() + " (" + product.getName() + ")", principal, request);
            log.info("Deleted unused product '{}' ({}) in tenant {}", product.getSku(), product.getId(), tenantId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductUnitConversionResponse> listProductUnits(UUID productId) {
        UUID tenantId = TenantContext.requireTenantId();
        Product product = productRepository.findByTenantIdAndId(tenantId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));

        UnitDefinition baseUnit = unitRepository.findByTenantIdAndId(tenantId, product.getBaseUnitId()).orElse(null);

        List<ProductUnitConversionResponse> list = new ArrayList<>();

        // Add base unit as primary with conversion factor 1.0000
        if (baseUnit != null) {
            list.add(ProductUnitConversionResponse.builder()
                    .id(null)
                    .unitId(baseUnit.getId())
                    .unitCode(baseUnit.getCode())
                    .unitName(baseUnit.getName())
                    .conversionFactor(BigDecimal.ONE.setScale(4))
                    .isBaseUnit(true)
                    .isSellingUnit(true)
                    .isReceivingUnit(true)
                    .build());
        }

        // Add configured secondary conversion units
        List<ProductUnitConversion> conversions = conversionRepository.findAllByTenantIdAndProductId(tenantId, productId);
        Map<UUID, UnitDefinition> unitMap = unitRepository.findAllByTenantId(tenantId).stream()
                .collect(Collectors.toMap(UnitDefinition::getId, u -> u, (a, b) -> a));

        for (ProductUnitConversion conv : conversions) {
            UnitDefinition u = unitMap.get(conv.getUnitId());
            list.add(ProductUnitConversionResponse.builder()
                    .id(conv.getId())
                    .unitId(conv.getUnitId())
                    .unitCode(u != null ? u.getCode() : "UNKNOWN")
                    .unitName(u != null ? u.getName() : "Unknown Unit")
                    .conversionFactor(conv.getConversionFactor())
                    .isBaseUnit(false)
                    .isSellingUnit(conv.isSellingUnit())
                    .isReceivingUnit(conv.isReceivingUnit())
                    .build());
        }

        return list;
    }

    @Override
    @Transactional
    public ProductUnitConversionResponse addProductUnitConversion(UUID productId,
                                                                  CreateProductUnitConversionDto dto,
                                                                  HttpServletRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        Product product = productRepository.findByTenantIdAndId(tenantId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));

        if (dto.getUnitId().equals(product.getBaseUnitId())) {
            throw new BadRequestException("Cannot add conversion rule for the product's base unit. Base unit factor is always 1.0.");
        }

        UnitDefinition unit = unitRepository.findByTenantIdAndId(tenantId, dto.getUnitId())
                .orElseThrow(() -> new BadRequestException("Unit definition not found in this shop with ID: " + dto.getUnitId()));

        Optional<ProductUnitConversion> existing = conversionRepository
                .findByTenantIdAndProductIdAndUnitId(tenantId, productId, dto.getUnitId());

        ProductUnitConversion conversion;
        if (existing.isPresent()) {
            conversion = existing.get();
            conversion.setConversionFactor(dto.getConversionFactor());
            conversion.setSellingUnit(dto.isSellingUnit());
            conversion.setReceivingUnit(dto.isReceivingUnit());
        } else {
            conversion = ProductUnitConversion.builder()
                    .tenantId(tenantId)
                    .productId(productId)
                    .unitId(unit.getId())
                    .conversionFactor(dto.getConversionFactor())
                    .isSellingUnit(dto.isSellingUnit())
                    .isReceivingUnit(dto.isReceivingUnit())
                    .build();
        }

        ProductUnitConversion saved = conversionRepository.save(conversion);

        logAudit("CONVERSION_RULE_SAVED", saved.getId(),
                "Set conversion for product " + product.getSku() + " unit " + unit.getCode() + " = " + dto.getConversionFactor(),
                null, request);
        log.info("Saved unit conversion rule for SKU {}: 1 {} = {} base units in tenant {}",
                product.getSku(), unit.getCode(), dto.getConversionFactor(), tenantId);

        return ProductUnitConversionResponse.builder()
                .id(saved.getId())
                .unitId(unit.getId())
                .unitCode(unit.getCode())
                .unitName(unit.getName())
                .conversionFactor(saved.getConversionFactor())
                .isBaseUnit(false)
                .isSellingUnit(saved.isSellingUnit())
                .isReceivingUnit(saved.isReceivingUnit())
                .build();
    }

    @Override
    @Transactional
    public void removeProductUnitConversion(UUID productId, UUID unitId, HttpServletRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        Product product = productRepository.findByTenantIdAndId(tenantId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));

        ProductUnitConversion conversion = conversionRepository.findByTenantIdAndProductIdAndUnitId(tenantId, productId, unitId)
                .orElseThrow(() -> new ResourceNotFoundException("Unit conversion not found for product " + product.getSku() + " and unit ID: " + unitId));

        conversionRepository.delete(conversion);

        logAudit("CONVERSION_RULE_REMOVED", conversion.getId(),
                "Removed unit conversion rule for product " + product.getSku() + ", unit ID: " + unitId,
                null, request);
        log.info("Removed unit conversion rule for SKU {} and unit {} in tenant {}", product.getSku(), unitId, tenantId);
    }

    private ProductSummaryResponse mapToSummary(Product product,
                                                Map<UUID, String> categoryNames,
                                                Map<UUID, UnitDefinition> units,
                                                Map<UUID, StockBalance> balances) {
        UnitDefinition baseUnit = units.get(product.getBaseUnitId());
        String categoryName = product.getCategoryId() != null ? categoryNames.get(product.getCategoryId()) : null;
        StockBalance balance = balances.get(product.getId());

        BigDecimal qty = balance != null ? balance.getQuantityOnHand() : BigDecimal.ZERO;
        BigDecimal avgCost = balance != null ? balance.getAverageCost() : product.getCostPrice();
        boolean isLowStock = qty.compareTo(product.getReorderThreshold()) <= 0;

        return ProductSummaryResponse.builder()
                .id(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .description(product.getDescription())
                .categoryId(product.getCategoryId())
                .categoryName(categoryName)
                .baseUnitId(product.getBaseUnitId())
                .baseUnitCode(baseUnit != null ? baseUnit.getCode() : null)
                .baseUnitName(baseUnit != null ? baseUnit.getName() : null)
                .costPrice(product.getCostPrice())
                .sellingPrice(product.getSellingPrice())
                .reorderThreshold(product.getReorderThreshold())
                .status(product.getStatus())
                .imageUrl(product.getImageUrl())
                .imageStorageKey(product.getImageStorageKey())
                .barcode(product.getBarcode())
                .brand(product.getBrand())
                .quantityOnHand(qty)
                .averageCost(avgCost)
                .isLowStock(isLowStock)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    public ProductDetailResponse updateSellingPrice(UUID id, UpdateSellingPriceDto dto, KuterUserPrincipal principal, HttpServletRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        if (!isCallerOwner(tenantId, principal)) {
            throw new ForbiddenException("Access denied: Only the shop owner can modify product selling prices.");
        }

        Product product = productRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));

        BigDecimal oldPrice = product.getSellingPrice();
        BigDecimal newPrice = dto.getSellingPrice();

        product.setSellingPrice(newPrice);
        Product updated = productRepository.save(product);

        logAudit("PRODUCT_PRICE_CHANGED", updated.getId(),
                "Selling price changed for SKU " + updated.getSku() + " from " + oldPrice + " to " + newPrice,
                principal, request);
        log.info("Owner updated selling price for SKU {} ({}) from {} to {} in tenant {}",
                updated.getSku(), updated.getId(), oldPrice, newPrice, tenantId);

        return getProduct(updated.getId());
    }

    private boolean isCallerOwner(UUID tenantId, KuterUserPrincipal principal) {
        if (principal == null) return false;
        if (principal.isPlatformAdmin()) return true;
        return principal.getRoleIn(tenantId).map(r -> r == TenantRole.OWNER).orElse(false);
    }

    private void validateImageMetadata(String imageUrl, String imageStorageKey) {
        if (StringUtils.hasText(imageUrl)) {
            String url = imageUrl.trim().toLowerCase();
            // Prevent dangerous URL schemes like javascript:, data:, file:
            if (!url.startsWith("https://") && !url.startsWith("http://") && !url.startsWith("/")) {
                throw new BadRequestException("Image URL must use a secure protocol (https://, http://, or relative path /)");
            }
        }
        if (StringUtils.hasText(imageStorageKey)) {
            String key = imageStorageKey.trim();
            // Disallow path traversal sequences
            if (key.contains("..") || key.contains("\\")) {
                throw new BadRequestException("Invalid image storage key: path traversal sequences are disallowed");
            }
        }
    }

    private void logAudit(String action, UUID entityId, String details, KuterUserPrincipal principal, HttpServletRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID actorId = principal != null ? principal.getUserId() : null;
        String actorEmail = principal != null ? principal.getEmail() : "system";

        String ip = request != null ? request.getRemoteAddr() : "127.0.0.1";
        auditService.logEvent(tenantId, actorId, actorEmail, action, "PRODUCT",
                entityId.toString(), details, ip);
    }
}
