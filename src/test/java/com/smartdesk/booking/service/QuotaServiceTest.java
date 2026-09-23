package com.smartdesk.booking.service;

import com.smartdesk.booking.entity.*;
import com.smartdesk.booking.repository.BookingRepository;
import com.smartdesk.booking.repository.EmployeeRepository;
import com.smartdesk.booking.repository.FloorRepository;
import com.smartdesk.booking.repository.TeamQuotaRepository;
import com.smartdesk.booking.service.impl.QuotaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class QuotaServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private FloorRepository floorRepository;

    @Mock
    private TeamQuotaRepository teamQuotaRepository;

    @InjectMocks
    private QuotaServiceImpl quotaService;

    private Floor floor;
    private Team team;
    private Employee employee;

    @BeforeEach
    void setUp() {
        floor = new Floor();
        floor.setId(10L);
        floor.setMaxCapacity(60);

        team = new Team();
        team.setId(100L);

        employee = new Employee();
        employee.setId(1L);
        employee.setTeam(team);
    }

    @Test
    void checkQuota_UnderLimits_Success() {
        when(floorRepository.findById(10L)).thenReturn(Optional.of(floor));
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        
        when(bookingRepository.countByDeskZoneFloorIdAndBookingDateAndTimeWindowAndStatusNot(
                eq(10L), any(), any(), any())).thenReturn(50L);

        TeamQuota quota = new TeamQuota();
        quota.setMaxDesks(8);
        when(teamQuotaRepository.findByTeamIdAndFloorId(100L, 10L)).thenReturn(Optional.of(quota));

        when(bookingRepository.countByEmployeeTeamIdAndDeskZoneFloorIdAndBookingDateAndTimeWindowAndStatusNot(
                eq(100L), eq(10L), any(), any(), any())).thenReturn(5L);

        assertDoesNotThrow(() -> {
            quotaService.checkQuotaForBooking(1L, 10L, LocalDate.now(), TimeWindow.FULL_DAY);
        });
    }

    @Test
    void checkQuota_FloorCapacityReached_ThrowsException() {
        when(floorRepository.findById(10L)).thenReturn(Optional.of(floor));
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        
        when(bookingRepository.countByDeskZoneFloorIdAndBookingDateAndTimeWindowAndStatusNot(
                eq(10L), any(), any(), any())).thenReturn(60L); // Max capacity is 60

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            quotaService.checkQuotaForBooking(1L, 10L, LocalDate.now(), TimeWindow.FULL_DAY);
        });
        
        assertEquals("Floor capacity reached for this time window", exception.getMessage());
    }

    @Test
    void checkQuota_TeamQuotaReached_ThrowsException() {
        when(floorRepository.findById(10L)).thenReturn(Optional.of(floor));
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        
        when(bookingRepository.countByDeskZoneFloorIdAndBookingDateAndTimeWindowAndStatusNot(
                eq(10L), any(), any(), any())).thenReturn(30L); // Still room on floor

        TeamQuota quota = new TeamQuota();
        quota.setMaxDesks(8);
        when(teamQuotaRepository.findByTeamIdAndFloorId(100L, 10L)).thenReturn(Optional.of(quota));

        when(bookingRepository.countByEmployeeTeamIdAndDeskZoneFloorIdAndBookingDateAndTimeWindowAndStatusNot(
                eq(100L), eq(10L), any(), any(), any())).thenReturn(8L); // Team reached max

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            quotaService.checkQuotaForBooking(1L, 10L, LocalDate.now(), TimeWindow.FULL_DAY);
        });
        
        assertEquals("Team quota exceeded on this floor", exception.getMessage());
    }
}
