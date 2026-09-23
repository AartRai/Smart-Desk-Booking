package com.smartdesk.booking.service.placement;

import com.smartdesk.booking.entity.Desk;
import com.smartdesk.booking.repository.DeskRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class SpatialIndex {

    private final DeskRepository deskRepository;
    
    // Grid size: e.g., 10 units per cell
    private static final double CELL_SIZE = 10.0;

    // FloorId -> (CellKey -> List of Desks)
    private final Map<Long, Map<CellKey, List<Desk>>> index = new ConcurrentHashMap<>();

    @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    @org.springframework.transaction.annotation.Transactional
    public void init() {
        log.info("Initializing Spatial Index for Desks...");
        rebuildAll();
    }

    public void rebuildAll() {
        index.clear();
        List<Desk> allDesks = deskRepository.findAll();
        for (Desk desk : allDesks) {
            addDeskToIndex(desk);
        }
        log.info("Spatial Index rebuilt. Total floors indexed: {}", index.size());
    }

    public void rebuildForFloor(Long floorId) {
        log.info("Rebuilding Spatial Index for Floor ID: {}", floorId);
        index.remove(floorId);
        List<Desk> floorDesks = deskRepository.findByZoneFloorId(floorId);
        for (Desk desk : floorDesks) {
            addDeskToIndex(desk);
        }
    }

    public void addDeskToIndex(Desk desk) {
        Long floorId = desk.getZone().getFloor().getId();
        CellKey key = calculateCellKey(desk.getXCoordinate(), desk.getYCoordinate());
        
        index.computeIfAbsent(floorId, k -> new ConcurrentHashMap<>())
             .computeIfAbsent(key, k -> new ArrayList<>())
             .add(desk);
    }

    public List<Desk> getDesksInNeighborhood(Long floorId, double x, double y) {
        List<Desk> nearbyDesks = new ArrayList<>();
        Map<CellKey, List<Desk>> floorGrid = index.get(floorId);
        
        if (floorGrid == null) {
            return nearbyDesks;
        }

        CellKey centerKey = calculateCellKey(x, y);
        
        // Scan the center cell and 8 neighbors
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                CellKey neighborKey = new CellKey(centerKey.x() + i, centerKey.y() + j);
                List<Desk> cellDesks = floorGrid.get(neighborKey);
                if (cellDesks != null) {
                    nearbyDesks.addAll(cellDesks);
                }
            }
        }
        
        return nearbyDesks;
    }

    public List<Desk> getAllDesksOnFloor(Long floorId) {
        List<Desk> allDesks = new ArrayList<>();
        Map<CellKey, List<Desk>> floorGrid = index.get(floorId);
        if (floorGrid != null) {
            for (List<Desk> cellDesks : floorGrid.values()) {
                allDesks.addAll(cellDesks);
            }
        }
        return allDesks;
    }

    private CellKey calculateCellKey(double x, double y) {
        int cellX = (int) (x / CELL_SIZE);
        int cellY = (int) (y / CELL_SIZE);
        return new CellKey(cellX, cellY);
    }
}
