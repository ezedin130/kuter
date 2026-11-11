package com.kuter.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUnitDto {

    @NotBlank(message = "Unit code is required (e.g. PCS, BAG, BOX, TON)")
    @Size(min = 1, max = 20, message = "Unit code must be between 1 and 20 characters")
    private String code;

    @NotBlank(message = "Unit name is required (e.g. Piece, 50kg Bag, Metric Tonne)")
    @Size(min = 1, max = 100, message = "Unit name must be between 1 and 100 characters")
    private String name;
}
