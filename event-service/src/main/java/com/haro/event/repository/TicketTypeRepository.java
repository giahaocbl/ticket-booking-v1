package com.haro.event.repository;

import com.haro.event.entity.TicketType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TicketTypeRepository extends JpaRepository<TicketType, UUID> {
    List<TicketType> findByEventId(UUID eventId);

    @Query("SELECT t FROM TicketType t WHERE t.event.id = :eventId AND t.isActive = true ORDER BY t.sortOrder ASC")
    List<TicketType> findActiveByEventIdOrderBySortOrder(@Param("eventId") UUID eventId);

    List<TicketType> findByEventIdAndIsActive(UUID eventId, Boolean isActive);
}
