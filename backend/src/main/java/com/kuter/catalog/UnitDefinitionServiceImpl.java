package com.kuter.catalog;

import com.kuter.audit.AuditService;
import com.kuter.catalog.dto.CreateUnitDto;
import com.kuter.catalog.dto.UnitResponse;
import com.kuter.common.ConflictException;
import com.kuter.common.ResourceNotFoundException;
import com.kuter.identity.KuterUserPrincipal;
import com.kuter.tenancy.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class UnitDefinitionServiceImpl implements UnitDefinitionService {

    private static final Logger log = LoggerFactory.getLogger(UnitDefinitionServiceImpl.class);

    private static final Map<String, String> DEFAULT_STANDARD_UNITS = Map.of(
            "PCS", "Piece / Individual item",
            "BAG", "50kg Standard Bag",
            "TON", "Metric Tonne (1,000 kg)",
            "KG", "Kilogram",
            "MTR", "Linear Meter",
            "BOX", "Box / Packaging unit",
            "BNDL", "Bundle / Structural pack",
            "PALLET", "Pallet",
            "SHT", "Sheet / Board"
    );

    private final UnitDefinitionRepository unitRepository;
    private final AuditService auditService;

    public UnitDefinitionServiceImpl(UnitDefinitionRepository unitRepository,
                                     AuditService auditService) {
        this.unitRepository = unitRepository;
        this.auditService = auditService;
    }

    @Override
    @Transactional
    public List<UnitResponse> listUnits() {
        UUID tenantId = TenantContext.requireTenantId();
        List<UnitDefinition> units = unitRepository.findAllByTenantId(tenantId);

        // Auto-seed standard building materials units if tenant has none yet
        if (units.isEmpty()) {
            log.info("Auto-seeding default standard building materials units for tenant {}", tenantId);
            DEFAULT_STANDARD_UNITS.forEach((code, name) -> {
                UnitDefinition u = UnitDefinition.builder()
                        .tenantId(tenantId)
                        .code(code)
                        .name(name)
                        .build();
                unitRepository.save(u);
            });
            units = unitRepository.findAllByTenantId(tenantId);
        }

        return units.stream().map(this::mapToResponse).toList();
    }

    @Override
    @Transactional
    public UnitResponse createUnit(CreateUnitDto dto, HttpServletRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        String code = dto.getCode().trim().toUpperCase();

        if (unitRepository.existsByTenantIdAndCodeIgnoreCase(tenantId, code)) {
            throw new ConflictException("Unit definition with code '" + code + "' already exists in this shop");
        }

        UnitDefinition unit = UnitDefinition.builder()
                .tenantId(tenantId)
                .code(code)
                .name(dto.getName().trim())
                .build();

        UnitDefinition saved = unitRepository.save(unit);

        logAudit("UNIT_CREATED", saved.getId(), "Created unit: " + saved.getCode() + " (" + saved.getName() + ")", request);
        log.info("Created unit definition '{}' ({}) in tenant {}", saved.getCode(), saved.getId(), tenantId);

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public UnitResponse getUnit(UUID id) {
        return mapToResponse(requireUnitEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public UnitDefinition requireUnitEntity(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return unitRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Unit definition not found with ID: " + id));
    }

    private UnitResponse mapToResponse(UnitDefinition unit) {
        return UnitResponse.builder()
                .id(unit.getId())
                .code(unit.getCode())
                .name(unit.getName())
                .createdAt(unit.getCreatedAt())
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
        auditService.logEvent(tenantId, actorId, actorEmail, action, "UNIT_DEFINITION",
                entityId.toString(), details, ip);
    }
}
