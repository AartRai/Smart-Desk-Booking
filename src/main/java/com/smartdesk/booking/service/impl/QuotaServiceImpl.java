package com.smartdesk.booking.service.impl;

import com.smartdesk.booking.dto.TeamQuotaRequest;
import com.smartdesk.booking.dto.TeamQuotaResponse;
import com.smartdesk.booking.entity.*;
import com.smartdesk.booking.exception.ResourceNotFoundException;
import com.smartdesk.booking.repository.BookingRepository;
import com.smartdesk.booking.repository.EmployeeRepository;
import com.smartdesk.booking.repository.FloorRepository;
import com.smartdesk.booking.repository.TeamRepository;
import com.smartdesk.booking.repository.TeamQuotaRepository;
import com.smartdesk.booking.service.QuotaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class QuotaServiceImpl implements QuotaService {

    private final BookingRepository bookingRepository;
    private final EmployeeRepository employeeRepository;
    private final FloorRepository floorRepository;
    private final TeamRepository teamRepository;
    private final TeamQuotaRepository teamQuotaRepository;

    @Override
    @Transactional(readOnly = true)
    public void checkQuotaForBooking(Long employeeId, Long floorId, LocalDate date, TimeWindow window) {
        Floor floor = floorRepository.findById(floorId)
                .orElseThrow(() -> new ResourceNotFoundException("Floor not found"));

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        // 1. Check overall floor capacity
        long floorBookingsCount = bookingRepository.countByDeskZoneFloorIdAndBookingDateAndTimeWindowAndStatusNot(
                floorId, date, window, BookingStatus.CANCELLED);
        
        if (floorBookingsCount >= floor.getMaxCapacity()) {
            throw new IllegalArgumentException("Floor capacity reached for this time window");
        }

        // 2. Check team-specific quota on this floor
        if (employee.getTeam() != null) {
            Long teamId = employee.getTeam().getId();
            Optional<TeamQuota> teamQuotaOpt = teamQuotaRepository.findByTeamIdAndFloorId(teamId, floorId);
            
            if (teamQuotaOpt.isPresent()) {
                TeamQuota teamQuota = teamQuotaOpt.get();
                long teamBookingsCount = bookingRepository.countByEmployeeTeamIdAndDeskZoneFloorIdAndBookingDateAndTimeWindowAndStatusNot(
                        teamId, floorId, date, window, BookingStatus.CANCELLED);
                
                if (teamBookingsCount >= teamQuota.getMaxDesks()) {
                    throw new IllegalArgumentException("Team quota exceeded on this floor");
                }
            }
        }
    }

    @Override
    @Transactional
    public TeamQuotaResponse setTeamQuota(TeamQuotaRequest request) {
        Team team = teamRepository.findById(request.getTeamId())
                .orElseThrow(() -> new ResourceNotFoundException("Team not found"));
        Floor floor = floorRepository.findById(request.getFloorId())
                .orElseThrow(() -> new ResourceNotFoundException("Floor not found"));

        TeamQuota quota = teamQuotaRepository.findByTeamIdAndFloorId(team.getId(), floor.getId())
                .orElse(new TeamQuota());

        quota.setTeam(team);
        quota.setFloor(floor);
        quota.setMaxDesks(request.getMaxDesks());

        TeamQuota savedQuota = teamQuotaRepository.save(quota);
        
        TeamQuotaResponse response = new TeamQuotaResponse();
        response.setId(savedQuota.getId());
        response.setTeamId(savedQuota.getTeam().getId());
        response.setFloorId(savedQuota.getFloor().getId());
        response.setMaxDesks(savedQuota.getMaxDesks());
        return response;
    }
}
