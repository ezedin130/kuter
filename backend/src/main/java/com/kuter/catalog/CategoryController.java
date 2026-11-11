package com.kuter.catalog;

import com.kuter.catalog.dto.CategoryResponse;
import com.kuter.catalog.dto.CreateCategoryDto;
import com.kuter.catalog.dto.UpdateCategoryDto;
import com.kuter.common.ApiResponse;
import com.kuter.common.Constants;
import com.kuter.common.PageResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(Constants.API_BASE_PATH + "/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    @PreAuthorize("@tenantSecurity.canManageCatalog()")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(@Valid @RequestBody CreateCategoryDto dto,
                                                                         HttpServletRequest request) {
        CategoryResponse response = categoryService.createCategory(dto, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Category created successfully"));
    }

    @GetMapping
    @PreAuthorize("@tenantSecurity.canReadInventory()")
    public ResponseEntity<ApiResponse<PageResponse<CategoryResponse>>> listCategories(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        PageResponse<CategoryResponse> response = categoryService.listCategories(active, search, pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "Categories retrieved successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@tenantSecurity.canReadInventory()")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategory(@PathVariable UUID id) {
        CategoryResponse response = categoryService.getCategory(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Category retrieved successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@tenantSecurity.canManageCatalog()")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(@PathVariable UUID id,
                                                                         @Valid @RequestBody UpdateCategoryDto dto,
                                                                         HttpServletRequest request) {
        CategoryResponse response = categoryService.updateCategory(id, dto, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Category updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@tenantSecurity.canManageCatalog()")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable UUID id,
                                                            HttpServletRequest request) {
        categoryService.deleteCategory(id, request);
        return ResponseEntity.ok(ApiResponse.success(null, "Category deleted or deactivated successfully"));
    }
}
