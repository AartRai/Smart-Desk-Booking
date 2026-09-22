package com.smartdesk.booking.dto;

import com.smartdesk.booking.entity.DeskType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DeskRequest {
    @NotNull(message = "Desk type is required")
    private DeskType deskType;

    @NotNull(message = "X coordinate is required")
    private Double xCoordinate;

    @NotNull(message = "Y coordinate is required")
    private Double yCoordinate;

    @NotNull(message = "Zone ID is required")
    private Long zoneId;

    private Long assignedEmployeeId;
}
