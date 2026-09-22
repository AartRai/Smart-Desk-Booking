package com.smartdesk.booking.service;

import com.smartdesk.booking.dto.BookingRequest;
import com.smartdesk.booking.dto.BookingResponse;

import java.util.List;

public interface BookingService {
    BookingResponse createBooking(BookingRequest request, Long employeeId);
    void cancelBooking(Long bookingId, Long employeeId);
    List<BookingResponse> getMyBookings(Long employeeId);
}
