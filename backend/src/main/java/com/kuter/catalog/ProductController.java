package com.kuter.catalog;

import com.kuter.catalog.dto.CreateProductDto;
import com.kuter.catalog.dto.CreateProductUnitConversionDto;
import com.kuter.catalog.dto.ProductDetailResponse;
import com.kuter.catalog.dto.ProductSummaryResponse;
import com.kuter.catalog.dto.ProductUnitConversionResponse;
import com.kuter.catalog.dto.UpdateProductDto;
import com.kuter.catalog.dto.UpdateSellingPriceDto;
import com.kuter.common.ApiResponse;
import com.kuter.common.Constants;
import com.kuter.common.PageResponse;
import com.kuter.identity.KuterUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(Constants.API_BASE_PATH + "/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    @PreAuthorize("@tenantSecurity.canManageCatalog()")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> createProduct(
            @Valid @RequestBody CreateProductDto dto,
            @AuthenticationPrincipal KuterUserPrincipal principal,
            HttpServletRequest request) {
        ProductDetailResponse response = productService.createProduct(dto, principal, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Product created successfully"));
    }

    @GetMapping
    @PreAuthorize("@tenantSecurity.canReadInventory()")
    public ResponseEntity<ApiResponse<PageResponse<ProductSummaryResponse>>> listProducts(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean lowStockOnly,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        PageResponse<ProductSummaryResponse> response = productService.listProducts(categoryId, status, search, lowStockOnly, pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "Products retrieved successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@tenantSecurity.canReadInventory()")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getProduct(@PathVariable UUID id) {
        ProductDetailResponse response = productService.getProduct(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Product retrieved successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@tenantSecurity.canManageCatalog()")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> updateProduct(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProductDto dto,
            @AuthenticationPrincipal KuterUserPrincipal principal,
            HttpServletRequest request) {
        ProductDetailResponse response = productService.updateProduct(id, dto, principal, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Product updated successfully"));
    }

    @PatchMapping("/{id}/selling-price")
    @PreAuthorize("@tenantSecurity.hasPermission('PRICES_MANAGE')")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> updateSellingPrice(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSellingPriceDto dto,
            @AuthenticationPrincipal KuterUserPrincipal principal,
            HttpServletRequest request) {
        ProductDetailResponse response = productService.updateSellingPrice(id, dto, principal, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Product selling price updated successfully"));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@tenantSecurity.canManageCatalog()")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> updateProductStatus(
            @PathVariable UUID id,
            @RequestParam ProductStatus status,
            @AuthenticationPrincipal KuterUserPrincipal principal,
            HttpServletRequest request) {
        ProductDetailResponse response = productService.updateProductStatus(id, status, principal, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Product status updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@tenantSecurity.canManageCatalog()")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @PathVariable UUID id,
            @AuthenticationPrincipal KuterUserPrincipal principal,
            HttpServletRequest request) {
        productService.deleteProduct(id, principal, request);
        return ResponseEntity.ok(ApiResponse.success(null, "Product deleted or deactivated successfully"));
    }

    @GetMapping({"/{productId}/units", "/{productId}/conversions"})
    @PreAuthorize("@tenantSecurity.canReadInventory()")
    public ResponseEntity<ApiResponse<List<ProductUnitConversionResponse>>> listProductUnits(
            @PathVariable UUID productId) {
        List<ProductUnitConversionResponse> units = productService.listProductUnits(productId);
        return ResponseEntity.ok(ApiResponse.success(units, "Product unit conversions retrieved successfully"));
    }

    @PostMapping({"/{productId}/units", "/{productId}/conversions"})
    @PreAuthorize("@tenantSecurity.canManageCatalog()")
    public ResponseEntity<ApiResponse<ProductUnitConversionResponse>> addProductUnitConversion(
            @PathVariable UUID productId,
            @Valid @RequestBody CreateProductUnitConversionDto dto,
            HttpServletRequest request) {
        ProductUnitConversionResponse response = productService.addProductUnitConversion(productId, dto, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Product unit conversion saved successfully"));
    }

    @DeleteMapping({"/{productId}/units/{unitId}", "/{productId}/conversions/{unitId}"})
    @PreAuthorize("@tenantSecurity.canManageCatalog()")
    public ResponseEntity<ApiResponse<Void>> removeProductUnitConversion(
            @PathVariable UUID productId,
            @PathVariable UUID unitId,
            HttpServletRequest request) {
        productService.removeProductUnitConversion(productId, unitId, request);
        return ResponseEntity.ok(ApiResponse.success(null, "Product unit conversion removed successfully"));
    }
}
