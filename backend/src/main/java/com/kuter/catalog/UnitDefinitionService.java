package com.kuter.catalog;

import com.kuter.catalog.dto.CreateUnitDto;
import com.kuter.catalog.dto.UnitResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.UUID;

public interface UnitDefinitionService {

    List<UnitResponse> listUnits();

    UnitResponse createUnit(CreateUnitDto dto, HttpServletRequest request);

    UnitResponse getUnit(UUID id);

    UnitDefinition requireUnitEntity(UUID id);
}
