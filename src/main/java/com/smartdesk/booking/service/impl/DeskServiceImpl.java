package com.smartdesk.booking.service.impl;

import com.smartdesk.booking.dto.DeskRequest;
import com.smartdesk.booking.dto.DeskResponse;
import com.smartdesk.booking.entity.Desk;
import com.smartdesk.booking.entity.DeskType;
import com.smartdesk.booking.entity.Employee;
import com.smartdesk.booking.entity.Zone;
import com.smartdesk.booking.exception.ResourceNotFoundException;
import com.smartdesk.booking.repository.DeskRepository;
import com.smartdesk.booking.repository.EmployeeRepository;
import com.smartdesk.booking.repository.ZoneRepository;
import com.smartdesk.booking.service.DeskService;
import com.smartdesk.booking.service.placement.SpatialIndex;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeskServiceImpl implements DeskService {
    private final DeskRepository deskRepository;
    private final ZoneRepository zoneRepository;
    private final EmployeeRepository employeeRepository;
    private final SpatialIndex spatialIndex;

    @Override
    @Transactional
    public DeskResponse createDesk(DeskRequest request) {
        Zone zone = zoneRepository.findById(request.getZoneId())
                .orElseThrow(() -> new ResourceNotFoundException("Zone not found with id: " + request.getZoneId()));

        Desk desk = new Desk();
        desk.setDeskType(request.getDeskType());
        desk.setXCoordinate(request.getXCoordinate());
        desk.setYCoordinate(request.getYCoordinate());
        desk.setZone(zone);

        if (request.getDeskType() == DeskType.FIXED) {
            if (request.getAssignedEmployeeId() == null) {
                throw new IllegalArgumentException("Fixed desk must have an assigned employee");
            }
            Employee employee = employeeRepository.findById(request.getAssignedEmployeeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + request.getAssignedEmployeeId()));
            desk.setAssignedEmployee(employee);
        } else if (request.getAssignedEmployeeId() != null) {
            throw new IllegalArgumentException("Hot desk cannot have an assigned employee");
        }
        
        Desk savedDesk = deskRepository.save(desk);
        
        // Rebuild the spatial index for this floor since layout changed
        spatialIndex.rebuildForFloor(zone.getFloor().getId());
        
        return mapToResponse(savedDesk);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeskResponse> getAllDesks() {
        return deskRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DeskResponse getDeskById(Long id) {
        Desk desk = deskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Desk not found with id: " + id));
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
