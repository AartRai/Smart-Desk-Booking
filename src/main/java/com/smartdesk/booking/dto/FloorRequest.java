package com.smartdesk.booking.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FloorRequest {
    @NotBlank(message = "Floor name is required")
    private String name;

    @NotNull(message = "Max capacity is required")
    @Min(value = 1, message = "Max capacity must be greater than 0")
    private Integer maxCapacity;

    private String timezone = "UTC";
}
