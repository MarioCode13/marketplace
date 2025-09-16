package dev.marketplace.marketplace.repository;

import dev.marketplace.marketplace.model.City;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CityRepository extends JpaRepository<City, UUID> {
    List<City> findByNameContainingIgnoreCase(String name);

    List<City> findByRegionId(UUID regionId);
    boolean existsByNameIgnoreCase(String name);
}
