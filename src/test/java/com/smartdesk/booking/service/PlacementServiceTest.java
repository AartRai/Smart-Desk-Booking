package com.smartdesk.booking.service.placement;

import com.smartdesk.booking.entity.*;
import com.smartdesk.booking.repository.BookingRepository;
import com.smartdesk.booking.repository.EmployeeRepository;
import com.smartdesk.booking.service.QuotaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PlacementServiceTest {

    @Mock
    private SpatialIndex spatialIndex;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private QuotaService quotaService;

    @InjectMocks
    private PlacementService placementService;

    private Employee employee;
    private Team team;
    private Desk desk1;
    private Desk desk2;

    @BeforeEach
    void setUp() {
        team = new Team();
        team.setId(1L);

        employee = new Employee();
        employee.setId(10L);
        employee.setTeam(team);

        Floor floor = new Floor();
        floor.setId(100L);

        Zone zone = new Zone();
        zone.setId(200L);
        zone.setFloor(floor);

        desk1 = new Desk();
        desk1.setId(1L);
        desk1.setXCoordinate(5.0);
        desk1.setYCoordinate(5.0);
        desk1.setDeskType(DeskType.HOT);
        desk1.setZone(zone);

        desk2 = new Desk();
        desk2.setId(2L);
        desk2.setXCoordinate(12.0); // Different cell but adjacent
        desk2.setYCoordinate(5.0);
        desk2.setDeskType(DeskType.HOT);
        desk2.setZone(zone);
    }

    @Test
    void suggestDesk_NoTeammateBookings_ReturnsRandomDesk() {
        when(employeeRepository.findById(10L)).thenReturn(Optional.of(employee));
        when(bookingRepository.findByEmployeeTeamIdAndBookingDateAndTimeWindowAndStatusNot(
                eq(1L), any(), any(), any())).thenReturn(Collections.emptyList());
        when(spatialIndex.getAllDesksOnFloor(100L)).thenReturn(new java.util.ArrayList<>(List.of(desk1, desk2)));
        when(bookingRepository.existsByDeskIdAndBookingDateAndTimeWindowAndStatusNot(
                anyLong(), any(), any(), any())).thenReturn(false);
        doNothing().when(quotaService).checkQuotaForBooking(anyLong(), anyLong(), any(), any());

        Desk result = placementService.suggestDesk(10L, 100L, LocalDate.now(), TimeWindow.MORNING);

        assertNotNull(result);
        verify(spatialIndex, times(1)).getAllDesksOnFloor(100L);
        verify(spatialIndex, never()).getDesksInNeighborhood(anyLong(), anyDouble(), anyDouble());
    }

    @Test
    void suggestDesk_TeammateBooked_ReturnsNeighborhoodDesk() {
        Booking teammateBooking = new Booking();
        teammateBooking.setDesk(desk1);

        when(employeeRepository.findById(10L)).thenReturn(Optional.of(employee));
        when(bookingRepository.findByEmployeeTeamIdAndBookingDateAndTimeWindowAndStatusNot(
                eq(1L), any(), any(), any())).thenReturn(List.of(teammateBooking));
        
        // Return desk2 as the neighbor
        when(spatialIndex.getDesksInNeighborhood(100L, 5.0, 5.0)).thenReturn(new java.util.ArrayList<>(List.of(desk2)));
        
        // Desk 2 is available
        when(bookingRepository.existsByDeskIdAndBookingDateAndTimeWindowAndStatusNot(
                eq(2L), any(), any(), any())).thenReturn(false);
        doNothing().when(quotaService).checkQuotaForBooking(anyLong(), anyLong(), any(), any());

        Desk result = placementService.suggestDesk(10L, 100L, LocalDate.now(), TimeWindow.MORNING);

        assertNotNull(result);
        assertEquals(2L, result.getId());
        verify(spatialIndex, times(1)).getDesksInNeighborhood(100L, 5.0, 5.0);
        verify(spatialIndex, never()).getAllDesksOnFloor(anyLong());
    }
}
