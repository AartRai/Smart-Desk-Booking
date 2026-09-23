package com.smartdesk.booking.service;

import com.smartdesk.booking.entity.Booking;
import com.smartdesk.booking.entity.BookingStatus;
import com.smartdesk.booking.entity.Desk;
import com.smartdesk.booking.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NoShowReleaseServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private NoShowReleaseService noShowReleaseService;

    @BeforeEach
    void setUp() {
        // Since we are mocking the time using a property string, we can inject it
        ReflectionTestUtils.setField(noShowReleaseService, "cutoffTimeStr", "10:00");
    }

    @Test
    void releaseNoShows_BeforeCutoff_DoesNothing() {
        // In order to test the before cutoff scenario without static mocking of LocalTime,
        // we can dynamically set the cutoff time to be way in the future.
        ReflectionTestUtils.setField(noShowReleaseService, "cutoffTimeStr", "23:59");
        
        noShowReleaseService.releaseNoShows();

        verify(bookingRepository, never()).findNoShowHotDeskBookings(any());
        verify(bookingRepository, never()).saveAll(any());
    }

    @Test
    void releaseNoShows_AfterCutoff_ReleasesBookings() {
        // Set cutoff time to way in the past so it always executes
        ReflectionTestUtils.setField(noShowReleaseService, "cutoffTimeStr", "00:01");

        Desk desk = new Desk();
        desk.setId(10L);

        Booking noShowBooking = new Booking();
        noShowBooking.setId(100L);
        noShowBooking.setStatus(BookingStatus.BOOKED);
        noShowBooking.setDesk(desk);

        when(bookingRepository.findNoShowHotDeskBookings(LocalDate.now()))
                .thenReturn(List.of(noShowBooking));

        noShowReleaseService.releaseNoShows();

        assertEquals(BookingStatus.CANCELLED, noShowBooking.getStatus());
        verify(bookingRepository, times(1)).saveAll(List.of(noShowBooking));
    }

    @Test
    void releaseNoShows_AfterCutoff_NoBookings_DoesNothing() {
        ReflectionTestUtils.setField(noShowReleaseService, "cutoffTimeStr", "00:01");

        when(bookingRepository.findNoShowHotDeskBookings(LocalDate.now()))
                .thenReturn(Collections.emptyList());

        noShowReleaseService.releaseNoShows();

        verify(bookingRepository, never()).saveAll(any());
    }
}
