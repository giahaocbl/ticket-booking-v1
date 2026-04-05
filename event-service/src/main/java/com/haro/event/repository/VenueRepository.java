package com.haro.event.repository;

import com.haro.event.entity.Venue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VenueRepository extends JpaRepository<Venue, UUID> {
    Optional<Venue> findBySlug(String slug);

    List<Venue> findByStatus(String status);

    @Query("SELECT v FROM Venue v WHERE v.countryCode = :countryCode and v.city = :city")
    List<Venue> findByCountryAndCity(@Param("countryCode") String countryCode, @Param("city") String city);

    @Query("SELECT v FROM Venue v WHERE v.latitude IS NOT NULL AND v.longitude IS NOT NULL")
    List<Venue> findVenuesWithLocation();

}
