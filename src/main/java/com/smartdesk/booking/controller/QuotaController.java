package com.smartdesk.booking.controller;

import com.smartdesk.booking.dto.TeamQuotaRequest;
import com.smartdesk.booking.dto.TeamQuotaResponse;
import com.smartdesk.booking.service.QuotaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/quotas")
@RequiredArgsConstructor
public class QuotaController {

    private final QuotaService quotaService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TeamQuotaResponse setTeamQuota(@Valid @RequestBody TeamQuotaRequest request) {
        return quotaService.setTeamQuota(request);
    }
}
