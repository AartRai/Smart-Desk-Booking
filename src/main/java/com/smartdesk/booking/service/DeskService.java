package com.smartdesk.booking.service;

import com.smartdesk.booking.dto.DeskRequest;
import com.smartdesk.booking.dto.DeskResponse;
import java.util.List;

public interface DeskService {
    DeskResponse createDesk(DeskRequest request);
    List<DeskResponse> getAllDesks();
    DeskResponse getDeskById(Long id);
}
