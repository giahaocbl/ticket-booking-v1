package com.haro.event.repository;

import com.haro.event.entity.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID> {

    Optional<Event> findByOrganizerIdAndSlug(UUID organizerId, String slug);

    List<Event> findByOrganizerId(UUID organizerId);

    List<Event> findByStatus(String status);

    List<Event> findByCategory(String category);

    @Query("SELECT e FROM Event e WHERE e.status = :status AND e.saleStartsAt <= :now AND (e.saleEndsAt IS NULL OR e.saleEndsAt >= :now)")
    List<Event> findActiveEventsOnSale(@Param("status") String status, @Param("now") OffsetDateTime now);

    @Query("SELECT e FROM Event e WHERE e.venue.id = :venueId")
    List<Event> findByVenueId(@Param("venueId") UUID venueId);

    @Query("""
            SELECT e
            FROM Event e
            WHERE (:organizerId IS NULL OR e.organizerId = :organizerId)
            AND (:status IS NULL OR e.status = :status)
            AND (:category IS NULL OR e.category = :category)
            """)
    Page<Event> findByFilters(@Param("organizerId")UUID organizerId, @Param("status")String status, @Param("category")String category, Pageable pageable);

}
