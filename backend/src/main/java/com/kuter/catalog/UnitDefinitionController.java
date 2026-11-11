package com.kuter.catalog;

import com.kuter.catalog.dto.CreateUnitDto;
import com.kuter.catalog.dto.UnitResponse;
import com.kuter.common.ApiResponse;
import com.kuter.common.Constants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(Constants.API_BASE_PATH + "/units")
public class UnitDefinitionController {

    private final UnitDefinitionService unitService;

    public UnitDefinitionController(UnitDefinitionService unitService) {
        this.unitService = unitService;
    }

    @GetMapping
    @PreAuthorize("@tenantSecurity.canReadInventory()")
    public ResponseEntity<ApiResponse<List<UnitResponse>>> listUnits() {
        List<UnitResponse> units = unitService.listUnits();
        return ResponseEntity.ok(ApiResponse.success(units, "Unit definitions retrieved successfully"));
    }

    @PostMapping
    @PreAuthorize("@tenantSecurity.canManageCatalog()")
    public ResponseEntity<ApiResponse<UnitResponse>> createUnit(@Valid @RequestBody CreateUnitDto dto,
                                                                 HttpServletRequest request) {
        UnitResponse response = unitService.createUnit(dto, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Unit definition created successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@tenantSecurity.canReadInventory()")
    public ResponseEntity<ApiResponse<UnitResponse>> getUnit(@PathVariable UUID id) {
        UnitResponse response = unitService.getUnit(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Unit definition retrieved successfully"));
    }
}
