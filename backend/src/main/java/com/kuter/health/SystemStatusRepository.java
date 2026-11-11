package com.kuter.health;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for checking system status records.
 */
@Repository
public interface SystemStatusRepository extends JpaRepository<SystemStatus, UUID> {

    Optional<SystemStatus> findByStatusName(String statusName);
}
