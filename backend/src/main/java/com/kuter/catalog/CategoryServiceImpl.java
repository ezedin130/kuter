package com.kuter.catalog;

import com.kuter.audit.AuditService;
import com.kuter.catalog.dto.CategoryResponse;
import com.kuter.catalog.dto.CreateCategoryDto;
import com.kuter.catalog.dto.UpdateCategoryDto;
import com.kuter.common.ConflictException;
import com.kuter.common.PageResponse;
import com.kuter.common.ResourceNotFoundException;
import com.kuter.identity.KuterUserPrincipal;
import com.kuter.tenancy.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Service
public class CategoryServiceImpl implements CategoryService {

    private static final Logger log = LoggerFactory.getLogger(CategoryServiceImpl.class);

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final AuditService auditService;

    public CategoryServiceImpl(CategoryRepository categoryRepository,
                               ProductRepository productRepository,
                               AuditService auditService) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.auditService = auditService;
    }

    @Override
    @Transactional
    public CategoryResponse createCategory(CreateCategoryDto dto, HttpServletRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        String trimmedName = dto.getName().trim();

        if (categoryRepository.existsByTenantIdAndNameIgnoreCase(tenantId, trimmedName)) {
            throw new ConflictException("Category with name '" + trimmedName + "' already exists in this shop");
        }

        Category category = Category.builder()
                .tenantId(tenantId)
                .name(trimmedName)
                .description(StringUtils.hasText(dto.getDescription()) ? dto.getDescription().trim() : null)
                .active(true)
                .build();

        Category saved = categoryRepository.save(category);

        logAudit("CATEGORY_CREATED", saved.getId(), "Created category: " + saved.getName(), request);
        log.info("Created category '{}' ({}) in tenant {}", saved.getName(), saved.getId(), tenantId);

        return mapToResponse(saved, 0);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CategoryResponse> listCategories(Boolean active, String search, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        String cleanSearch = StringUtils.hasText(search) ? search.trim() : null;

        Page<Category> page = categoryRepository.searchCategories(tenantId, active, cleanSearch, pageable);

        return PageResponse.of(page, page.getContent().stream()
                .map(cat -> {
                    long productCount = productRepository.countByTenantIdAndCategoryId(tenantId, cat.getId());
                    return mapToResponse(cat, productCount);
                })
                .toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> listAllActiveCategories() {
        UUID tenantId = TenantContext.requireTenantId();
        return categoryRepository.findAllByTenantIdAndActiveTrue(tenantId).stream()
                .map(cat -> {
                    long productCount = productRepository.countByTenantIdAndCategoryId(tenantId, cat.getId());
                    return mapToResponse(cat, productCount);
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategory(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        Category category = categoryRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));

        long productCount = productRepository.countByTenantIdAndCategoryId(tenantId, category.getId());
        return mapToResponse(category, productCount);
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(UUID id, UpdateCategoryDto dto, HttpServletRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        Category category = categoryRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));

        String trimmedName = dto.getName().trim();
        if (!category.getName().equalsIgnoreCase(trimmedName) &&
                categoryRepository.existsByTenantIdAndNameIgnoreCase(tenantId, trimmedName)) {
            throw new ConflictException("Category with name '" + trimmedName + "' already exists in this shop");
        }

        category.setName(trimmedName);
        if (dto.getDescription() != null) {
            category.setDescription(StringUtils.hasText(dto.getDescription()) ? dto.getDescription().trim() : null);
        }
        if (dto.getActive() != null) {
            category.setActive(dto.getActive());
        }

        Category updated = categoryRepository.save(category);

        logAudit("CATEGORY_UPDATED", updated.getId(), "Updated category: " + updated.getName(), request);
        log.info("Updated category '{}' ({}) in tenant {}", updated.getName(), updated.getId(), tenantId);

        long productCount = productRepository.countByTenantIdAndCategoryId(tenantId, updated.getId());
        return mapToResponse(updated, productCount);
    }

    @Override
    @Transactional
    public void deleteCategory(UUID id, HttpServletRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        Category category = categoryRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));

        long productCount = productRepository.countByTenantIdAndCategoryId(tenantId, id);
        if (productCount > 0) {
            // Avoid destructive deletion if category is referenced by products; safely deactivate
            category.setActive(false);
            categoryRepository.save(category);
            logAudit("CATEGORY_DEACTIVATED", category.getId(),
                    "Deactivated category with " + productCount + " linked products: " + category.getName(), request);
            log.info("Deactivated category '{}' ({}) because it has {} linked products",
                    category.getName(), category.getId(), productCount);
        } else {
            categoryRepository.delete(category);
            logAudit("CATEGORY_DELETED", category.getId(), "Deleted unused category: " + category.getName(), request);
            log.info("Deleted category '{}' ({}) with 0 linked products", category.getName(), category.getId());
        }
    }

    private CategoryResponse mapToResponse(Category category, long productCount) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .active(category.isActive())
                .productCount(productCount)
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }

    private void logAudit(String action, UUID entityId, String details, HttpServletRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UUID actorId = null;
        String actorEmail = "system";

        if (auth != null && auth.getPrincipal() instanceof KuterUserPrincipal principal) {
            actorId = principal.getUserId();
            actorEmail = principal.getEmail();
        }

        String ip = request != null ? request.getRemoteAddr() : "127.0.0.1";
        auditService.logEvent(tenantId, actorId, actorEmail, action, "CATEGORY",
                entityId.toString(), details, ip);
    }
}
