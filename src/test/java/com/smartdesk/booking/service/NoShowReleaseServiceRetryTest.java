package com.smartdesk.booking.service;

import com.smartdesk.booking.repository.BookingRepository;
import com.smartdesk.booking.repository.FloorRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Collections;

import static org.mockito.Mockito.*;

@SpringBootTest
public class NoShowReleaseServiceRetryTest {

    @Autowired
    private NoShowReleaseService noShowReleaseService;

    @MockitoBean
    private FloorRepository floorRepository;

    @MockitoBean
    private BookingRepository bookingRepository;

    @Test
    void testReleaseNoShows_TransientFailure_RetriesAndRecovers() {
        // Arrange
        // We simulate a database failure occurring when it tries to find the timezones
        // For the first two attempts, it throws a RuntimeException. On the third attempt, it succeeds.
        when(floorRepository.findDistinctTimezones())
                .thenThrow(new RuntimeException("Transient DB Failure 1"))
                .thenThrow(new RuntimeException("Transient DB Failure 2"))
                .thenReturn(Collections.emptyList());

        // Act
        noShowReleaseService.releaseNoShows();

        // Assert
        // The method should have been called 3 times total due to retries
        verify(floorRepository, times(3)).findDistinctTimezones();
    }
    
    @Test
    void testReleaseNoShows_PersistentFailure_CallsRecover() {
        // Arrange
        // Simulate a persistent failure that always throws
        when(floorRepository.findDistinctTimezones())
                .thenThrow(new RuntimeException("Persistent DB Failure"));

        // Act
        noShowReleaseService.releaseNoShows();

        // Assert
        // The method should have been called exactly 3 times (initial + 2 retries) before falling back to @Recover
        verify(floorRepository, times(3)).findDistinctTimezones();
    }
}
