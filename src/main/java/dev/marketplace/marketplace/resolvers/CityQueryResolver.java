package dev.marketplace.marketplace.resolvers;

import dev.marketplace.marketplace.model.City;
import dev.marketplace.marketplace.repository.CityRepository;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.List;

@CrossOrigin(origins = "http://localhost:3000")
@Controller
public class CityQueryResolver {
    private final CityRepository cityRepository;

    public CityQueryResolver(CityRepository cityRepository) {
        this.cityRepository = cityRepository;
    }

    @QueryMapping
    public List<City> searchCities(@Argument String query) {
        return cityRepository.findByNameContainingIgnoreCase(query);
    }
} 