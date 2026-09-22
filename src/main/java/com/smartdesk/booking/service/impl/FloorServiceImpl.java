package com.smartdesk.booking.service.impl;

import com.smartdesk.booking.dto.FloorRequest;
import com.smartdesk.booking.dto.FloorResponse;
import com.smartdesk.booking.entity.Floor;
import com.smartdesk.booking.exception.ResourceNotFoundException;
import com.smartdesk.booking.repository.FloorRepository;
import com.smartdesk.booking.service.FloorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FloorServiceImpl implements FloorService {
    private final FloorRepository floorRepository;

    @Override
    @Transactional
    public FloorResponse createFloor(FloorRequest request) {
        Floor floor = new Floor();
        floor.setName(request.getName());
        floor.setMaxCapacity(request.getMaxCapacity());
        
        Floor savedFloor = floorRepository.save(floor);
        return mapToResponse(savedFloor);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FloorResponse> getAllFloors() {
        return floorRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public FloorResponse getFloorById(Long id) {
        Floor floor = floorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Floor not found with id: " + id));
        return mapToResponse(floor);
    }

    private FloorResponse mapToResponse(Floor floor) {
        FloorResponse response = new FloorResponse();
        response.setId(floor.getId());
        response.setName(floor.getName());
        response.setMaxCapacity(floor.getMaxCapacity());
        return response;
    }
}
