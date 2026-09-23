package com.smartdesk.booking.repository;

import com.smartdesk.booking.entity.Floor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FloorRepository extends JpaRepository<Floor, Long> {
    
    @Query("SELECT DISTINCT f.timezone FROM Floor f")
    List<String> findDistinctTimezones();
}
