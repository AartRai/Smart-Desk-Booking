package com.smartdesk.booking.service;

import com.smartdesk.booking.entity.Booking;
import com.smartdesk.booking.entity.BookingStatus;
import com.smartdesk.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoShowReleaseService {

    private final BookingRepository bookingRepository;

    @Value("${booking.no-show.cutoff-time:10:00}")
    private String cutoffTimeStr;

    /**
     * Runs periodically to find and cancel hot desk bookings 
     * where the employee hasn't checked in by the cut-off time.
     * The cron runs every 15 minutes by default.
     */
    @Scheduled(cron = "${booking.no-show.cron:0 0/15 * * * *}")
    @Transactional
    public void releaseNoShows() {
        LocalTime cutoffTime = LocalTime.parse(cutoffTimeStr);
        
        if (LocalTime.now().isBefore(cutoffTime)) {
            log.debug("Current time is before the no-show cutoff time ({}), skipping auto-release.", cutoffTime);
            return;
        }

        log.info("Running No-Show Auto-Release Job for {}", LocalDate.now());
        
        List<Booking> noShows = bookingRepository.findNoShowHotDeskBookings(LocalDate.now());

        if (noShows.isEmpty()) {
            log.info("No un-checked-in bookings found to release.");
            return;
        }

        for (Booking booking : noShows) {
            log.info("Releasing booking {} for desk {} due to no-show.", booking.getId(), booking.getDesk().getId());
            booking.setStatus(BookingStatus.CANCELLED);
        }

        bookingRepository.saveAll(noShows);
        log.info("Successfully released {} no-show bookings.", noShows.size());
    }
}
