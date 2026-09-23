package com.smartdesk.booking.repository;

import com.smartdesk.booking.entity.TeamQuota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TeamQuotaRepository extends JpaRepository<TeamQuota, Long> {
    Optional<TeamQuota> findByTeamIdAndFloorId(Long teamId, Long floorId);
}
