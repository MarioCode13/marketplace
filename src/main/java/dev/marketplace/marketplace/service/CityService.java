package dev.marketplace.marketplace.service;

import dev.marketplace.marketplace.model.City;
import dev.marketplace.marketplace.repository.CityRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class CityService {

    private final CityRepository cityRepository;

    public CityService(CityRepository cityRepository) {
        this.cityRepository = cityRepository;
    }

    public City getCityById(UUID id) {
        return cityRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("City not found with id: " + id));
    }

    public City getCityBySlug(String slug) {
        return cityRepository.findBySlugIgnoreCase(slug)
                .orElseThrow(() -> new IllegalArgumentException("City not found with slug: " + slug));
    }

    public List<City> getCitiesByRegion(UUID regionId) {
        return cityRepository.findByRegionId(regionId);
    }

    public void validateCityOrCustomCity(UUID cityId, String customCity) {
        if ((cityId == null && (customCity == null || customCity.isBlank())) ||
                (cityId != null && customCity != null && !customCity.isBlank())) {
            throw new IllegalArgumentException("Provide either cityId or customCity, not both.");
        }

        if (customCity != null && cityRepository.existsByNameIgnoreCase(customCity.trim())) {
            throw new IllegalArgumentException("City already exists. Use the existing city instead.");
        }
    }
}
