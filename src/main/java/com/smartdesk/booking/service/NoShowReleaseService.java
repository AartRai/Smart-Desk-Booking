package com.smartdesk.booking.service;

import com.smartdesk.booking.entity.Booking;
import com.smartdesk.booking.entity.BookingStatus;
import com.smartdesk.booking.repository.BookingRepository;
import com.smartdesk.booking.repository.FloorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoShowReleaseService {

    private final BookingRepository bookingRepository;
    private final FloorRepository floorRepository;

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
        log.info("Running No-Show Auto-Release Job");
        
        List<String> timezones = floorRepository.findDistinctTimezones();
        if (timezones.isEmpty()) {
            log.info("No floors found, skipping auto-release.");
            return;
        }

        int totalReleased = 0;

        for (String timezone : timezones) {
            ZoneId zoneId = ZoneId.of(timezone);
            ZonedDateTime nowInTimezone = ZonedDateTime.now(zoneId);
            
            if (nowInTimezone.toLocalTime().isBefore(cutoffTime)) {
                log.debug("Current local time in {} is before the cutoff time ({}), skipping.", timezone, cutoffTime);
                continue;
            }

            LocalDate localDate = nowInTimezone.toLocalDate();
            log.info("Checking for no-shows in timezone {} for date {}", timezone, localDate);

            List<Booking> noShows = bookingRepository.findNoShowHotDeskBookingsByTimezone(localDate, timezone);

            if (!noShows.isEmpty()) {
                for (Booking booking : noShows) {
                    log.info("Releasing booking {} for desk {} due to no-show.", booking.getId(), booking.getDesk().getId());
                    booking.setStatus(BookingStatus.CANCELLED);
                }
                bookingRepository.saveAll(noShows);
                totalReleased += noShows.size();
            }
        }
        
        log.info("Successfully released {} no-show bookings globally.", totalReleased);
    }
}
