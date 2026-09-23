package com.smartdesk.booking.controller;

import com.smartdesk.booking.dto.DeskResponse;
import com.smartdesk.booking.entity.Desk;
import com.smartdesk.booking.entity.TimeWindow;
import com.smartdesk.booking.service.placement.PlacementService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/placements")
@RequiredArgsConstructor
public class PlacementController {

    private final PlacementService placementService;

    @GetMapping("/suggest")
    public DeskResponse suggestDesk(
            @org.springframework.security.core.annotation.AuthenticationPrincipal Long employeeId,
            @RequestParam Long floorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam TimeWindow timeWindow) {
        
        Desk desk = placementService.suggestDesk(employeeId, floorId, date, timeWindow);
        return mapToResponse(desk);
    }

    private DeskResponse mapToResponse(Desk desk) {
        DeskResponse response = new DeskResponse();
        response.setId(desk.getId());
        response.setDeskType(desk.getDeskType());
        response.setXCoordinate(desk.getXCoordinate());
        response.setYCoordinate(desk.getYCoordinate());
        response.setZoneId(desk.getZone().getId());
        if (desk.getAssignedEmployee() != null) {
            response.setAssignedEmployeeId(desk.getAssignedEmployee().getId());
        }
        return response;
    }
}
