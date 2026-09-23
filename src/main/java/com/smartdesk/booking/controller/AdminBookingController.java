package com.smartdesk.booking.controller;

import com.smartdesk.booking.service.NoShowReleaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/bookings")
@RequiredArgsConstructor
public class AdminBookingController {

    private final NoShowReleaseService noShowReleaseService;

    @PostMapping("/release-no-shows")
    public ResponseEntity<String> triggerNoShowRelease() {
        noShowReleaseService.releaseNoShows();
        return ResponseEntity.ok("No-show release job triggered successfully.");
    }
}
