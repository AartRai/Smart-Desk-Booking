package com.smartdesk.booking.dto;

import com.smartdesk.booking.entity.DeskType;
import lombok.Data;

@Data
public class DeskResponse {
    private Long id;
    private DeskType deskType;
    private Double xCoordinate;
    private Double yCoordinate;
    private Long zoneId;
    private Long assignedEmployeeId;
}
