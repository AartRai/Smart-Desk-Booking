package com.smartdesk.booking.dto;

import lombok.Data;

@Data
public class FloorResponse {
    private Long id;
    private String name;
    private Integer maxCapacity;
}
