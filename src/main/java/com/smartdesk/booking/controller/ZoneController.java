package com.smartdesk.booking.controller;

import com.smartdesk.booking.dto.ZoneRequest;
import com.smartdesk.booking.dto.ZoneResponse;
import com.smartdesk.booking.service.ZoneService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/zones")
@RequiredArgsConstructor
public class ZoneController {
    private final ZoneService zoneService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ZoneResponse createZone(@Valid @RequestBody ZoneRequest request) {
        return zoneService.createZone(request);
    }

    @GetMapping
    public List<ZoneResponse> getAllZones() {
        return zoneService.getAllZones();
    }

    @GetMapping("/{id}")
    public ZoneResponse getZoneById(@PathVariable Long id) {
        return zoneService.getZoneById(id);
    }
}
