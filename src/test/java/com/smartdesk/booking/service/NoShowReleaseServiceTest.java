package com.smartdesk.booking.service;

import com.smartdesk.booking.entity.Booking;
import com.smartdesk.booking.entity.BookingStatus;
import com.smartdesk.booking.entity.Desk;
import com.smartdesk.booking.repository.BookingRepository;
import com.smartdesk.booking.repository.FloorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.mockito.MockedStatic;

@ExtendWith(MockitoExtension.class)
public class NoShowReleaseServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private FloorRepository floorRepository;

    @InjectMocks
    private NoShowReleaseService noShowReleaseService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(noShowReleaseService, "cutoffTimeStr", "10:00");
    }

    @Test
    void releaseNoShows_NoFloors_DoesNothing() {
        when(floorRepository.findDistinctTimezones()).thenReturn(Collections.emptyList());
        noShowReleaseService.releaseNoShows();
        verify(bookingRepository, never()).findNoShowHotDeskBookingsByTimezone(any(), any());
    }

    @Test
    void releaseNoShows_BeforeCutoff_DoesNothing() {
        ReflectionTestUtils.setField(noShowReleaseService, "cutoffTimeStr", "23:59"); // way in the future
        when(floorRepository.findDistinctTimezones()).thenReturn(List.of("UTC"));
        
        noShowReleaseService.releaseNoShows();

        verify(bookingRepository, never()).findNoShowHotDeskBookingsByTimezone(any(), any());
        verify(bookingRepository, never()).saveAll(any());
    }

    @Test
    void releaseNoShows_AfterCutoff_ReleasesBookings() {
        ReflectionTestUtils.setField(noShowReleaseService, "cutoffTimeStr", "00:01"); // way in the past

        Desk desk = new Desk();
        desk.setId(10L);

        Booking noShowBooking = new Booking();
        noShowBooking.setId(100L);
        noShowBooking.setStatus(BookingStatus.BOOKED);
        noShowBooking.setDesk(desk);

        when(floorRepository.findDistinctTimezones()).thenReturn(List.of("UTC"));
        LocalDate localDate = ZonedDateTime.now(ZoneId.of("UTC")).toLocalDate();
        
        when(bookingRepository.findNoShowHotDeskBookingsByTimezone(localDate, "UTC"))
                .thenReturn(List.of(noShowBooking));

        noShowReleaseService.releaseNoShows();

        assertEquals(BookingStatus.CANCELLED, noShowBooking.getStatus());
        verify(bookingRepository, times(1)).saveAll(List.of(noShowBooking));
    }

    @Test
    void releaseNoShows_AfterCutoff_NoBookings_DoesNothing() {
        ReflectionTestUtils.setField(noShowReleaseService, "cutoffTimeStr", "00:01");
        
        when(floorRepository.findDistinctTimezones()).thenReturn(List.of("UTC"));
        LocalDate localDate = ZonedDateTime.now(ZoneId.of("UTC")).toLocalDate();

        when(bookingRepository.findNoShowHotDeskBookingsByTimezone(localDate, "UTC"))
                .thenReturn(Collections.emptyList());

        noShowReleaseService.releaseNoShows();

        verify(bookingRepository, never()).saveAll(any());
    }

    @Test
    void releaseNoShows_ExactCutoffBoundary_ReleasesBookings() {
        ReflectionTestUtils.setField(noShowReleaseService, "cutoffTimeStr", "10:00");
        
        Desk desk = new Desk();
        desk.setId(10L);

        Booking noShowBooking = new Booking();
        noShowBooking.setId(100L);
        noShowBooking.setStatus(BookingStatus.BOOKED);
        noShowBooking.setDesk(desk);

        when(floorRepository.findDistinctTimezones()).thenReturn(List.of("UTC"));
        
        ZonedDateTime exactCutoffTime = ZonedDateTime.parse("2026-10-15T10:00:00Z[UTC]");
        
        try (MockedStatic<ZonedDateTime> mockedZDT = mockStatic(ZonedDateTime.class, CALLS_REAL_METHODS)) {
            mockedZDT.when(() -> ZonedDateTime.now(ZoneId.of("UTC"))).thenReturn(exactCutoffTime);
            
            when(bookingRepository.findNoShowHotDeskBookingsByTimezone(exactCutoffTime.toLocalDate(), "UTC"))
                    .thenReturn(List.of(noShowBooking));

            noShowReleaseService.releaseNoShows();

            // At EXACTLY 10:00:00, isBefore(10:00) is false, so it falls through and releases the desk.
            assertEquals(BookingStatus.CANCELLED, noShowBooking.getStatus(), "Cut-off is exclusive: exactly at cut-off should release");
            verify(bookingRepository, times(1)).saveAll(List.of(noShowBooking));
        }
    }
}
