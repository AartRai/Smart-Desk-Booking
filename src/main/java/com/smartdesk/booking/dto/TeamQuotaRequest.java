package com.smartdesk.booking.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TeamQuotaRequest {
    @NotNull(message = "Team ID is required")
    private Long teamId;

    @NotNull(message = "Floor ID is required")
    private Long floorId;

    @NotNull(message = "Max desks is required")
    @Min(value = 0, message = "Max desks cannot be negative")
    private Integer maxDesks;
}
