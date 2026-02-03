package dev.marketplace.marketplace.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.ActiveProfiles;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

import dev.marketplace.marketplace.model.User;
import dev.marketplace.marketplace.model.TrustRating;
import dev.marketplace.marketplace.repository.UserRepository;
import dev.marketplace.marketplace.repository.TrustRatingRepository;
import dev.marketplace.marketplace.service.OmnicheckService;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class OmnicheckWireMockIntegrationTest {

    private static final WireMockServer wireMockServer;

    static {
        wireMockServer = new WireMockServer(); // dynamic port by default
        wireMockServer.start();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        String base = wireMockServer.baseUrl();
        // Omnicheck expects base + /webservice endpoints in our app
        registry.add("OMNICHECK_BASE_URL", () -> base + "/webservice");
        registry.add("OMNICHECK_DRY_RUN", () -> "false");
        registry.add("OMNICHECK_API_KEY", () -> "test-api-key");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TrustRatingRepository trustRatingRepository;

    @Autowired
    private OmnicheckService omnicheckService;

    @BeforeEach
    void setup() {
        wireMockServer.resetAll();

        // Stub SAID verification endpoint
        wireMockServer.stubFor(post(urlEqualTo("/webservice/said_verification"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"success\",\"verified\":true}")));
    }

    @AfterAll
    static void teardown() {
        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.stop();
        }
    }

    @Test
    void idVerify_shouldMarkTrustRatingVerified() throws Exception {
        UUID userId = UUID.fromString("a7234b8d-44db-45e8-8a65-a715b604b39a");

        // Create a user in DB (minimal required fields)
        User u = new User();
        u.setId(userId);
        u.setEmail("local@dev");
        u.setPassword("password");
        userRepository.save(u);

        // Ensure trust rating exists or will be created by service; add tokens for account
        omnicheckService.addTestTokens(userId.toString(), 10);

        // Build request payload like frontend
        Map<String, String> payload = new HashMap<>();
        payload.put("firstName", "Admin");
        payload.put("idNumber", "9301215190085");
        payload.put("lastName", "User");
        payload.put("userId", userId.toString());
        payload.put("requestId", "req-integ-1");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(payload, headers);

        String url = "http://localhost:" + port + "/api/id-verify";
        ResponseEntity<Map> resp = restTemplate.postForEntity(url, request, Map.class);

        assertEquals(200, resp.getStatusCodeValue());
        Map body = resp.getBody();
        assertNotNull(body);
        assertTrue(Boolean.TRUE.equals(body.get("success")));
        assertTrue(Boolean.TRUE.equals(body.get("verifiedID")));
        assertTrue("DRY_RUN".equals(body.get("omnicheckResult")) || body.get("omnicheckResult") != null);

        // Confirm TrustRating verified flag is true
        TrustRating tr = trustRatingRepository.findByUserId(userId).orElse(null);
        assertNotNull(tr, "TrustRating should exist for user");
        // Access getter; model has getVerifiedId() / isVerifiedId()
        assertTrue(tr.getVerifiedId() || tr.isVerifiedId());
    }
}
