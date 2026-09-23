package com.smartdesk.booking.dto;

import lombok.Data;

@Data
public class TeamQuotaResponse {
    private Long id;
    private Long teamId;
    private Long floorId;
    private Integer maxDesks;
}
