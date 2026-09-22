package com.smartdesk.booking.dto;

import com.smartdesk.booking.entity.TimeWindow;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class BookingRequest {
    @NotNull(message = "Desk ID is required")
    private Long deskId;

    @NotNull(message = "Booking date is required")
    @FutureOrPresent(message = "Booking date cannot be in the past")
    private LocalDate bookingDate;

    @NotNull(message = "Time window is required")
    private TimeWindow timeWindow;
}
