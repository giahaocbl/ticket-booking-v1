package com.haro.event.repository;

import com.haro.event.entity.EventOccurrence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface EventOccurrenceRepository extends JpaRepository<EventOccurrence, UUID> {

    List<EventOccurrence> findByEventId(UUID eventId);

    List<EventOccurrence> findByStatus(String status);

    @Query("SELECT eo FROM EventOccurrence eo WHERE eo.startsAt >= :startDate AND eo.startsAt <= :endDate")
    List<EventOccurrence> findByDateRange(@Param("startDate") OffsetDateTime startDate,
                                          @Param("endDate") OffsetDateTime endDate);

    @Query("SELECT eo FROM EventOccurrence eo WHERE eo.event.id = :eventId AND eo.status = :status")
    List<EventOccurrence> findByEventIdAndStatus(@Param("eventId") UUID eventId, @Param("status") String status);
}
