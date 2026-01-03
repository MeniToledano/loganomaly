package com.loganomaly.detector.analysis_service.repository;

import com.loganomaly.detector.analysis_service.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface AlertRepository extends JpaRepository<Alert, UUID> {

    /**
     * Find all alerts ordered by detection time (newest first)
     */
    List<Alert> findAllByOrderByDetectedAtDesc();

    /**
     * Find unacknowledged alerts
     */
    List<Alert> findByAcknowledgedFalseOrderByDetectedAtDesc();

    /**
     * Find alerts by severity
     */
    List<Alert> findBySeverityOrderByDetectedAtDesc(String severity);

    /**
     * Find alerts by service
     */
    List<Alert> findByServiceOrderByDetectedAtDesc(String service);

    /**
     * Find alerts by type
     */
    List<Alert> findByTypeOrderByDetectedAtDesc(String type);

    /**
     * Find recent alerts (within time range)
     */
    List<Alert> findByDetectedAtAfterOrderByDetectedAtDesc(Instant since);

    /**
     * Count unacknowledged alerts
     */
    long countByAcknowledgedFalse();

    /**
     * Check if similar alert exists recently (to avoid duplicates)
     */
    boolean existsByTypeAndServiceAndDetectedAtAfter(String type, String service, Instant since);
}

