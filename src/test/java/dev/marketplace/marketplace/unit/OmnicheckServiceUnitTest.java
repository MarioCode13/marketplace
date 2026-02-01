package dev.marketplace.marketplace.unit;

import dev.marketplace.marketplace.service.OmnicheckService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class OmnicheckServiceUnitTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;
    private OmnicheckService omnicheckService;

    @BeforeEach
    void setup() {
        restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);
        omnicheckService = new OmnicheckService(restTemplate);

        // Configure internal fields for test
        ReflectionTestUtils.setField(omnicheckService, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(omnicheckService, "dryRun", false);
        ReflectionTestUtils.setField(omnicheckService, "baseUrl", "http://localhost:9080/webservice");
    }

    @Test
    void verifySouthAfricanId_successfulResponse_parsesAsSuccess() {
        String stubUrl = "http://localhost:9080/webservice/said_verification";
        String responseBody = "{\"status\":\"success\",\"verified\":true}";

        mockServer.expect(requestTo(stubUrl))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        OmnicheckService.VerificationResult result = omnicheckService.verifySouthAfricanIdForAccount("acct1", "req1", "8001015009087", "Test", "User");

        mockServer.verify();
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertNotNull(result.getResponseJson());
        assertTrue(result.getResponseJson().has("verified"));
        assertTrue(result.getResponseJson().get("verified").asBoolean());
    }
}

