package com.smartdesk.booking.dto;

import com.smartdesk.booking.entity.BookingStatus;
import com.smartdesk.booking.entity.TimeWindow;
import lombok.Data;

import java.time.LocalDate;

@Data
public class BookingResponse {
    private Long id;
    private Long deskId;
    private Long employeeId;
    private LocalDate bookingDate;
    private TimeWindow timeWindow;
    private BookingStatus status;
}
