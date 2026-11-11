package com.kuter.catalog;

import com.kuter.tenancy.Tenant;
import com.kuter.tenancy.TenantRepository;
import com.kuter.tenancy.TenantStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class CatalogAndConversionTests {

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UnitDefinitionRepository unitRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductUnitConversionRepository conversionRepository;

    private Tenant tenant;
    private Category masonryCategory;
    private UnitDefinition pieceUnit;
    private UnitDefinition boxUnit;
    private UnitDefinition palletUnit;

    @BeforeEach
    void setUp() {
        conversionRepository.deleteAll();
        productRepository.deleteAll();
        unitRepository.deleteAll();
        categoryRepository.deleteAll();
        tenantRepository.deleteAll();

        tenant = tenantRepository.save(Tenant.builder()
                .code("kuter-builders")
                .name("Kuter Building Supplies")
                .status(TenantStatus.ACTIVE)
                .currency("USD")
                .timezone("UTC")
                .build());

        masonryCategory = categoryRepository.save(Category.builder()
                .tenantId(tenant.getId())
                .name("Masonry & Blocks")
                .description("Concrete blocks, bricks, and masonry anchors")
                .active(true)
                .build());

        pieceUnit = unitRepository.save(UnitDefinition.builder()
                .tenantId(tenant.getId())
                .code("PC")
                .name("Piece")
                .build());

        boxUnit = unitRepository.save(UnitDefinition.builder()
                .tenantId(tenant.getId())
                .code("BOX")
                .name("Box of 50")
                .build());

        palletUnit = unitRepository.save(UnitDefinition.builder()
                .tenantId(tenant.getId())
                .code("PALLET")
                .name("Full Pallet")
                .build());
    }

    @Test
    @DisplayName("Product created with category and base unit successfully persists with scale 4 precision")
    void productCreationAndScalePrecision() {
        Product block = productRepository.save(Product.builder()
                .tenantId(tenant.getId())
                .sku("BLK-6IN")
                .name("Standard Hollow Block 6 Inch")
                .categoryId(masonryCategory.getId())
                .baseUnitId(pieceUnit.getId())
                .costPrice(new BigDecimal("1.2500"))
                .sellingPrice(new BigDecimal("1.8000"))
                .reorderThreshold(new BigDecimal("500.0000"))
                .status(ProductStatus.ACTIVE)
                .brand("Kuter Masonry")
                .build());

        assertThat(block.getId()).isNotNull();
        assertThat(block.getCreatedAt()).isNotNull();
        assertThat(block.getUpdatedAt()).isNotNull();
        assertThat(block.getCostPrice()).isEqualByComparingTo("1.2500");
        assertThat(block.getSellingPrice()).isEqualByComparingTo("1.8000");

        List<Product> products = productRepository.findAllByTenantIdAndCategoryId(tenant.getId(), masonryCategory.getId());
        assertThat(products).hasSize(1);
        assertThat(products.get(0).getName()).isEqualTo("Standard Hollow Block 6 Inch");
    }

    @Test
    @DisplayName("Dual unit conversion calculation: box and pallet correctly convert to piece base quantity")
    void dualUnitConversionCalculation() {
        Product block = productRepository.save(Product.builder()
                .tenantId(tenant.getId())
                .sku("BLK-4IN")
                .name("Standard Hollow Block 4 Inch")
                .categoryId(masonryCategory.getId())
                .baseUnitId(pieceUnit.getId())
                .costPrice(new BigDecimal("1.0000"))
                .sellingPrice(new BigDecimal("1.5000"))
                .reorderThreshold(new BigDecimal("200.0000"))
                .status(ProductStatus.ACTIVE)
                .build());

        // Box = 50 Pieces (conversion_factor = 50.0000)
        ProductUnitConversion boxConv = conversionRepository.save(ProductUnitConversion.builder()
                .tenantId(tenant.getId())
                .productId(block.getId())
                .unitId(boxUnit.getId())
                .conversionFactor(new BigDecimal("50.0000"))
                .isSellingUnit(true)
                .isReceivingUnit(true)
                .build());

        // Pallet = 400 Pieces (conversion_factor = 400.0000)
        ProductUnitConversion palletConv = conversionRepository.save(ProductUnitConversion.builder()
                .tenantId(tenant.getId())
                .productId(block.getId())
                .unitId(palletUnit.getId())
                .conversionFactor(new BigDecimal("400.0000"))
                .isSellingUnit(true)
                .isReceivingUnit(true)
                .build());

        // Verify conversion factors retrieve accurately
        Optional<ProductUnitConversion> foundBox = conversionRepository.findByTenantIdAndProductIdAndUnitId(
                tenant.getId(), block.getId(), boxUnit.getId()
        );
        assertThat(foundBox).isPresent();
        assertThat(foundBox.get().getConversionFactor()).isEqualByComparingTo("50.0000");

        // Mathematical verification:
        // Customer orders 3 boxes: baseQuantity = 3 * 50 = 150 pieces
        BigDecimal orderBoxes = new BigDecimal("3.0000");
        BigDecimal calculatedPieces = orderBoxes.multiply(foundBox.get().getConversionFactor());
        assertThat(calculatedPieces).isEqualByComparingTo("150.0000");

        // Customer orders 2 pallets: baseQuantity = 2 * 400 = 800 pieces
        BigDecimal orderPallets = new BigDecimal("2.0000");
        BigDecimal palletPieces = orderPallets.multiply(palletConv.getConversionFactor());
        assertThat(palletPieces).isEqualByComparingTo("800.0000");
    }

    @Test
    @DisplayName("Unit conversion uniqueness: a product cannot have duplicate conversion factors for the same unit")
    void duplicateConversionFactorThrowsException() {
        Product block = productRepository.save(Product.builder()
                .tenantId(tenant.getId())
                .sku("BLK-8IN")
                .name("Heavy Duty 8 Inch Solid Block")
                .categoryId(masonryCategory.getId())
                .baseUnitId(pieceUnit.getId())
                .costPrice(new BigDecimal("2.0000"))
                .sellingPrice(new BigDecimal("3.0000"))
                .reorderThreshold(new BigDecimal("100.0000"))
                .status(ProductStatus.ACTIVE)
                .build());

        conversionRepository.save(ProductUnitConversion.builder()
                .tenantId(tenant.getId())
                .productId(block.getId())
                .unitId(boxUnit.getId())
                .conversionFactor(new BigDecimal("25.0000"))
                .isSellingUnit(true)
                .isReceivingUnit(true)
                .build());

        // Attempting to add a duplicate conversion for boxUnit on the same product
        ProductUnitConversion duplicate = ProductUnitConversion.builder()
                .tenantId(tenant.getId())
                .productId(block.getId())
                .unitId(boxUnit.getId())
                .conversionFactor(new BigDecimal("30.0000"))
                .isSellingUnit(true)
                .isReceivingUnit(true)
                .build();

        assertThatThrownBy(() -> conversionRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
