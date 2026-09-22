package com.smartdesk.booking.repository;

import com.smartdesk.booking.entity.Desk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeskRepository extends JpaRepository<Desk, Long> {
    List<Desk> findByZoneFloorId(Long floorId);
}
