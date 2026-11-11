package com.kuter.catalog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Product persistence with strict tenant boundaries.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

    Optional<Product> findByTenantIdAndId(UUID tenantId, UUID id);

    Optional<Product> findByTenantIdAndSku(UUID tenantId, String sku);

    Page<Product> findAllByTenantId(UUID tenantId, Pageable pageable);

    List<Product> findAllByTenantId(UUID tenantId);

    List<Product> findAllByTenantIdAndCategoryId(UUID tenantId, UUID categoryId);

    long countByTenantIdAndCategoryId(UUID tenantId, UUID categoryId);

    long countByTenantIdAndStatus(UUID tenantId, ProductStatus status);

    long countByTenantId(UUID tenantId);

    boolean existsByTenantIdAndSku(UUID tenantId, String sku);

    boolean existsByTenantIdAndBaseUnitId(UUID tenantId, UUID baseUnitId);

    default Page<Product> searchProducts(UUID tenantId,
                                         UUID categoryId,
                                         ProductStatus status,
                                         String search,
                                         Pageable pageable) {
        Specification<Product> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("tenantId"), tenantId));
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("categoryId"), categoryId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate nameMatch = cb.like(cb.lower(root.get("name")), pattern);
                Predicate skuMatch = cb.like(cb.lower(root.get("sku")), pattern);
                Predicate brandMatch = cb.like(cb.lower(root.get("brand")), pattern);
                Predicate barcodeMatch = cb.like(cb.lower(root.get("barcode")), pattern);
                predicates.add(cb.or(nameMatch, skuMatch, brandMatch, barcodeMatch));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return findAll(spec, pageable);
    }
}

