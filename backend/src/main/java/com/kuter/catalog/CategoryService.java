package com.kuter.catalog;

import com.kuter.catalog.dto.CategoryResponse;
import com.kuter.catalog.dto.CreateCategoryDto;
import com.kuter.catalog.dto.UpdateCategoryDto;
import com.kuter.common.PageResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface CategoryService {

    CategoryResponse createCategory(CreateCategoryDto dto, HttpServletRequest request);

    PageResponse<CategoryResponse> listCategories(Boolean active, String search, Pageable pageable);

    List<CategoryResponse> listAllActiveCategories();

    CategoryResponse getCategory(UUID id);

    CategoryResponse updateCategory(UUID id, UpdateCategoryDto dto, HttpServletRequest request);

    void deleteCategory(UUID id, HttpServletRequest request);
}
