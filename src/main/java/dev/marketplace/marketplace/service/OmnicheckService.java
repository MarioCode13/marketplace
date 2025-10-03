package dev.marketplace.marketplace.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class OmnicheckService {
    @Value("${OMNICHECK_API_KEY}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public String verifySouthAfricanId(String idNumber, String firstName, String lastName) {
        String url = "https://www.omnicheck.co.za/webservice/said_verification";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        String body = String.format(
            "{\"id_number\":\"%s\",\"first_name\":\"%s\",\"last_name\":\"%s\"}",
            idNumber, firstName, lastName
        );

        HttpEntity<String> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            return response.getBody();
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }
}

