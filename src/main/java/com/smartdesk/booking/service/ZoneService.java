package com.smartdesk.booking.service;

import com.smartdesk.booking.dto.ZoneRequest;
import com.smartdesk.booking.dto.ZoneResponse;
import java.util.List;

public interface ZoneService {
    ZoneResponse createZone(ZoneRequest request);
    List<ZoneResponse> getAllZones();
    ZoneResponse getZoneById(Long id);
}
