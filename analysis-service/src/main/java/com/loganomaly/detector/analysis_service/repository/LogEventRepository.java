package com.loganomaly.detector.analysis_service.repository;

import com.loganomaly.detector.analysis_service.entity.LogEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface LogEventRepository extends JpaRepository<LogEvent, UUID> {

    /**
     * Find log events by service name, ordered by timestamp descending
     */
    List<LogEvent> findByServiceOrderByTimestampDesc(String service);

    /**
     * Find log events by log level
     */
    List<LogEvent> findByLevelOrderByTimestampDesc(String level);

    /**
     * Find log events within a time range
     */
    List<LogEvent> findByTimestampBetweenOrderByTimestampDesc(Instant start, Instant end);

    /**
     * Find log events by service within a time range
     */
    List<LogEvent> findByServiceAndTimestampBetweenOrderByTimestampDesc(
            String service, Instant start, Instant end);

    /**
     * Count log events by level within a time range (useful for anomaly detection)
     */
    @Query("SELECT COUNT(e) FROM LogEvent e WHERE e.level = :level AND e.timestamp BETWEEN :start AND :end")
    long countByLevelAndTimestampBetween(
            @Param("level") String level,
            @Param("start") Instant start,
            @Param("end") Instant end);

    /**
     * Find recent log events, limited
     */
    List<LogEvent> findTop100ByOrderByTimestampDesc();
}


