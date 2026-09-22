package com.smartdesk.booking.controller;

import com.smartdesk.booking.dto.DeskRequest;
import com.smartdesk.booking.dto.DeskResponse;
import com.smartdesk.booking.service.DeskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/desks")
@RequiredArgsConstructor
public class DeskController {
    private final DeskService deskService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DeskResponse createDesk(@Valid @RequestBody DeskRequest request) {
        return deskService.createDesk(request);
    }

    @GetMapping
    public List<DeskResponse> getAllDesks() {
        return deskService.getAllDesks();
    }

    @GetMapping("/{id}")
    public DeskResponse getDeskById(@PathVariable Long id) {
        return deskService.getDeskById(id);
    }
}
