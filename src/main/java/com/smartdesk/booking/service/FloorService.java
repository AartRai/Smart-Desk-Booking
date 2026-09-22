package com.smartdesk.booking.service;

import com.smartdesk.booking.dto.FloorRequest;
import com.smartdesk.booking.dto.FloorResponse;
import java.util.List;

public interface FloorService {
    FloorResponse createFloor(FloorRequest request);
    List<FloorResponse> getAllFloors();
    FloorResponse getFloorById(Long id);
}
