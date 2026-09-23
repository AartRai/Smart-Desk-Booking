package com.smartdesk.booking.service.impl;

import com.smartdesk.booking.dto.ZoneRequest;
import com.smartdesk.booking.dto.ZoneResponse;
import com.smartdesk.booking.entity.Floor;
import com.smartdesk.booking.entity.Zone;
import com.smartdesk.booking.exception.ResourceNotFoundException;
import com.smartdesk.booking.repository.FloorRepository;
import com.smartdesk.booking.repository.ZoneRepository;
import com.smartdesk.booking.service.ZoneService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ZoneServiceImpl implements ZoneService {
    private final ZoneRepository zoneRepository;
    private final FloorRepository floorRepository;

    @Override
    @Transactional
    @CacheEvict(value = {"zones", "zone"}, allEntries = true)
    public ZoneResponse createZone(ZoneRequest request) {
        Floor floor = floorRepository.findById(request.getFloorId())
                .orElseThrow(() -> new ResourceNotFoundException("Floor not found with id: " + request.getFloorId()));

        Zone zone = new Zone();
        zone.setName(request.getName());
        zone.setFloor(floor);
        
        Zone savedZone = zoneRepository.save(zone);
        return mapToResponse(savedZone);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "zones")
    public List<ZoneResponse> getAllZones() {
        return zoneRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "zone", key = "#id")
    public ZoneResponse getZoneById(Long id) {
        Zone zone = zoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Zone not found with id: " + id));
        return mapToResponse(zone);
    }

    private ZoneResponse mapToResponse(Zone zone) {
        ZoneResponse response = new ZoneResponse();
        response.setId(zone.getId());
        response.setName(zone.getName());
        response.setFloorId(zone.getFloor().getId());
        return response;
    }
}
