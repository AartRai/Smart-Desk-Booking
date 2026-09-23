package com.smartdesk.booking.service;

import com.smartdesk.booking.dto.TeamQuotaRequest;
import com.smartdesk.booking.dto.TeamQuotaResponse;
import com.smartdesk.booking.entity.TimeWindow;

import java.time.LocalDate;

public interface QuotaService {
    void checkQuotaForBooking(Long employeeId, Long floorId, LocalDate date, TimeWindow window);
    TeamQuotaResponse setTeamQuota(TeamQuotaRequest request);
}
