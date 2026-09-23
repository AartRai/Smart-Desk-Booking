package com.smartdesk.booking.controller;

import com.smartdesk.booking.dto.BookingRequest;
import com.smartdesk.booking.dto.BookingResponse;
import com.smartdesk.booking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse createBooking(
            @Valid @RequestBody BookingRequest request,
            @org.springframework.security.core.annotation.AuthenticationPrincipal Long employeeId) {
        return bookingService.createBooking(request, employeeId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelBooking(
            @PathVariable Long id,
            @org.springframework.security.core.annotation.AuthenticationPrincipal Long employeeId) {
        bookingService.cancelBooking(id, employeeId);
    }

    @GetMapping("/me")
    public List<BookingResponse> getMyBookings(
            @org.springframework.security.core.annotation.AuthenticationPrincipal Long employeeId) {
        return bookingService.getMyBookings(employeeId);
    }

    @PostMapping("/{id}/checkin")
    public BookingResponse checkIn(
            @PathVariable Long id,
            @org.springframework.security.core.annotation.AuthenticationPrincipal Long employeeId) {
        return bookingService.checkIn(id, employeeId);
    }
}
