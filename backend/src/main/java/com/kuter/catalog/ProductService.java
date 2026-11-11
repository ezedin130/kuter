package com.kuter.catalog;

import com.kuter.catalog.dto.CreateProductDto;
import com.kuter.catalog.dto.CreateProductUnitConversionDto;
import com.kuter.catalog.dto.ProductDetailResponse;
import com.kuter.catalog.dto.ProductSummaryResponse;
import com.kuter.catalog.dto.ProductUnitConversionResponse;
import com.kuter.catalog.dto.UpdateProductDto;
import com.kuter.catalog.dto.UpdateSellingPriceDto;
import com.kuter.common.PageResponse;
import com.kuter.identity.KuterUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ProductService {

    ProductDetailResponse createProduct(CreateProductDto dto, KuterUserPrincipal principal, HttpServletRequest request);

    PageResponse<ProductSummaryResponse> listProducts(UUID categoryId,
                                                      ProductStatus status,
                                                      String search,
                                                      Boolean lowStockOnly,
                                                      Pageable pageable);

    ProductDetailResponse getProduct(UUID id);

    ProductDetailResponse updateProduct(UUID id, UpdateProductDto dto, KuterUserPrincipal principal, HttpServletRequest request);

    ProductDetailResponse updateSellingPrice(UUID id, UpdateSellingPriceDto dto, KuterUserPrincipal principal, HttpServletRequest request);

    ProductDetailResponse updateProductStatus(UUID id, ProductStatus status, KuterUserPrincipal principal, HttpServletRequest request);

    void deleteProduct(UUID id, KuterUserPrincipal principal, HttpServletRequest request);

    List<ProductUnitConversionResponse> listProductUnits(UUID productId);

    ProductUnitConversionResponse addProductUnitConversion(UUID productId,
                                                           CreateProductUnitConversionDto dto,
                                                           HttpServletRequest request);

    void removeProductUnitConversion(UUID productId, UUID unitId, HttpServletRequest request);
}
