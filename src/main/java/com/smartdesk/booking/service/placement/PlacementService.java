package com.smartdesk.booking.service.placement;

import com.smartdesk.booking.entity.Booking;
import com.smartdesk.booking.entity.BookingStatus;
import com.smartdesk.booking.entity.Desk;
import com.smartdesk.booking.entity.DeskType;
import com.smartdesk.booking.entity.Employee;
import com.smartdesk.booking.entity.TimeWindow;
import com.smartdesk.booking.exception.ResourceNotFoundException;
import com.smartdesk.booking.repository.BookingRepository;
import com.smartdesk.booking.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlacementService {

    private final SpatialIndex spatialIndex;
    private final EmployeeRepository employeeRepository;
    private final BookingRepository bookingRepository;

    public Desk suggestDesk(Long employeeId, Long floorId, LocalDate date, TimeWindow window) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        if (employee.getTeam() == null) {
            log.info("Employee has no team, returning random available desk");
            return findAnyAvailableDesk(floorId, date, window);
        }

        List<Booking> teamBookings = bookingRepository.findByEmployeeTeamIdAndBookingDateAndTimeWindowAndStatusNot(
                employee.getTeam().getId(), date, window, BookingStatus.CANCELLED);

        // Filter bookings to only those on the requested floor
        List<Booking> floorTeamBookings = teamBookings.stream()
                .filter(b -> b.getDesk().getZone().getFloor().getId().equals(floorId))
                .toList();

        if (floorTeamBookings.isEmpty()) {
            log.info("No teammates booked on floor {}, returning random available desk", floorId);
            return findAnyAvailableDesk(floorId, date, window);
        }

        // Find neighbors for the first teammate's desk as an anchor
        Desk anchorDesk = floorTeamBookings.get(0).getDesk();
        log.info("Found teammate booked at desk {}. Scanning immediate 9-cell neighborhood...", anchorDesk.getId());

        List<Desk> neighborhoodDesks = spatialIndex.getDesksInNeighborhood(floorId, anchorDesk.getXCoordinate(), anchorDesk.getYCoordinate());
        
        // Shuffle to distribute load among neighborhood
        Collections.shuffle(neighborhoodDesks);

        for (Desk desk : neighborhoodDesks) {
            if (isDeskAvailable(desk, date, window, employee)) {
                return desk;
            }
        }

        log.info("Neighborhood is full. Falling back to anywhere on the floor.");
        return findAnyAvailableDesk(floorId, date, window);
    }

    private Desk findAnyAvailableDesk(Long floorId, LocalDate date, TimeWindow window) {
        List<Desk> allDesks = spatialIndex.getAllDesksOnFloor(floorId);
        Collections.shuffle(allDesks);
        
        for (Desk desk : allDesks) {
            if (isDeskAvailable(desk, date, window, null)) {
                return desk;
            }
        }
        throw new IllegalArgumentException("No desks available on this floor for the requested time");
    }

    private boolean isDeskAvailable(Desk desk, LocalDate date, TimeWindow window, Employee bookingEmployee) {
        if (desk.getDeskType() == DeskType.FIXED) {
            if (bookingEmployee == null || desk.getAssignedEmployee() == null || !desk.getAssignedEmployee().getId().equals(bookingEmployee.getId())) {
                return false; // Can't book someone else's fixed desk
            }
        }

        boolean isBooked = bookingRepository.existsByDeskIdAndBookingDateAndTimeWindowAndStatusNot(
                desk.getId(), date, window, BookingStatus.CANCELLED);
        
        return !isBooked;
    }
}
