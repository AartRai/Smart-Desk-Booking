package com.smartdesk.booking.service.impl;

import com.smartdesk.booking.dto.BookingRequest;
import com.smartdesk.booking.dto.BookingResponse;
import com.smartdesk.booking.entity.Booking;
import com.smartdesk.booking.entity.BookingStatus;
import com.smartdesk.booking.entity.Desk;
import com.smartdesk.booking.entity.DeskType;
import com.smartdesk.booking.entity.Employee;
import com.smartdesk.booking.exception.ResourceNotFoundException;
import com.smartdesk.booking.repository.BookingRepository;
import com.smartdesk.booking.repository.DeskRepository;
import com.smartdesk.booking.repository.EmployeeRepository;
import com.smartdesk.booking.service.BookingService;
import com.smartdesk.booking.service.QuotaService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final DeskRepository deskRepository;
    private final EmployeeRepository employeeRepository;
    private final QuotaService quotaService;

    @Override
    @Transactional
    public BookingResponse createBooking(BookingRequest request, Long employeeId) {
        Desk desk = deskRepository.findById(request.getDeskId())
                .orElseThrow(() -> new ResourceNotFoundException("Desk not found with id: " + request.getDeskId()));
        
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));

        // Rule: Fixed desks can only be booked by their assigned owner
        if (desk.getDeskType() == DeskType.FIXED) {
            if (desk.getAssignedEmployee() == null || !desk.getAssignedEmployee().getId().equals(employee.getId())) {
                throw new IllegalArgumentException("This fixed desk can only be booked by its assigned owner");
            }
        }

        // Quota check
        quotaService.checkQuotaForBooking(employeeId, desk.getZone().getFloor().getId(), request.getBookingDate(), request.getTimeWindow());

        // Fast availability check for desk double booking
        boolean deskAlreadyBooked = bookingRepository.existsByDeskIdAndBookingDateAndTimeWindowAndStatusNot(
                desk.getId(), request.getBookingDate(), request.getTimeWindow(), BookingStatus.CANCELLED);
        if (deskAlreadyBooked) {
            throw new IllegalArgumentException("Desk is already booked for this time window");
        }

        // Fast availability check for employee double booking
        boolean employeeAlreadyBooked = bookingRepository.existsByEmployeeIdAndBookingDateAndTimeWindowAndStatusNot(
                employee.getId(), request.getBookingDate(), request.getTimeWindow(), BookingStatus.CANCELLED);
        if (employeeAlreadyBooked) {
            throw new IllegalArgumentException("You already have a booking for this time window");
        }

        Booking booking = new Booking();
        booking.setDesk(desk);
        booking.setEmployee(employee);
        booking.setBookingDate(request.getBookingDate());
        booking.setTimeWindow(request.getTimeWindow());
        booking.setStatus(BookingStatus.BOOKED);

        try {
            Booking savedBooking = bookingRepository.save(booking);
            return mapToResponse(savedBooking);
        } catch (DataIntegrityViolationException ex) {
            // This acts as our safety net against race conditions (Module 4)
            throw new IllegalArgumentException("Booking failed due to a conflict. The desk might have just been booked.");
        }
    }

    @Override
    @Transactional
    public void cancelBooking(Long bookingId, Long employeeId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));
        
        if (!booking.getEmployee().getId().equals(employeeId)) {
            throw new IllegalArgumentException("You can only cancel your own bookings");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalArgumentException("Booking is already cancelled");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getMyBookings(Long employeeId) {
        return bookingRepository.findByEmployeeIdOrderByBookingDateDesc(employeeId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private BookingResponse mapToResponse(Booking booking) {
        BookingResponse response = new BookingResponse();
        response.setId(booking.getId());
        response.setDeskId(booking.getDesk().getId());
        response.setEmployeeId(booking.getEmployee().getId());
        response.setBookingDate(booking.getBookingDate());
        response.setTimeWindow(booking.getTimeWindow());
        response.setStatus(booking.getStatus());
        return response;
    }
}
