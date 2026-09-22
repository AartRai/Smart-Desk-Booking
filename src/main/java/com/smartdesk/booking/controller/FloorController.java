package com.smartdesk.booking.controller;

import com.smartdesk.booking.dto.FloorRequest;
import com.smartdesk.booking.dto.FloorResponse;
import com.smartdesk.booking.service.FloorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/floors")
@RequiredArgsConstructor
public class FloorController {
    private final FloorService floorService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FloorResponse createFloor(@Valid @RequestBody FloorRequest request) {
        return floorService.createFloor(request);
    }

    @GetMapping
    public List<FloorResponse> getAllFloors() {
        return floorService.getAllFloors();
    }

    @GetMapping("/{id}")
    public FloorResponse getFloorById(@PathVariable Long id) {
        return floorService.getFloorById(id);
    }
}
