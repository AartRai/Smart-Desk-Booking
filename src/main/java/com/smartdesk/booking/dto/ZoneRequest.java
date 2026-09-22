package com.smartdesk.booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ZoneRequest {
    @NotBlank(message = "Zone name is required")
    private String name;

    @NotNull(message = "Floor ID is required")
    private Long floorId;
}
