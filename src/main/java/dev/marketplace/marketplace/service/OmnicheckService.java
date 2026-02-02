package dev.marketplace.marketplace.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class OmnicheckService {
    @Value("${omnicheck.api-key:}")
    private String apiKey;

    // Allow these to be replaced in tests in future (keeps current behaviour by default)
    private RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Value("${omnicheck.base-url:https://www.omnicheck.co.za/webservice}")
    private String baseUrl;

    // --- Testing / billing helpers ---
    // If true, network calls will be skipped and a successful dummy response returned.
    @Value("${omnicheck.dry-run:true}")
//    @Value("${OMNICHECK_DRY_RUN:false}")
    private boolean dryRun;

    // In-memory token balances keyed by account id (for local testing only)
    private final ConcurrentHashMap<String, Integer> tokenBalances = new ConcurrentHashMap<>();

    // Simple idempotency tracker: stores recently seen request IDs per account
    private final ConcurrentHashMap<String, Set<String>> recentRequests = new ConcurrentHashMap<>();

    // Define approximate token costs per operation (adjust as needed)
    private static final int COST_SAID = 1;
    private static final int COST_CIPC_MATCH = 2;
    private static final int COST_CIPC_SEARCH = 2;

    // Public helpers for tests to manipulate balances
    public void addTestTokens(String accountId, int amount) {
        tokenBalances.merge(accountId, amount, Integer::sum);
        log.info("Added {} test tokens to account {}. New balance={}", amount, accountId, tokenBalances.get(accountId));
    }

    public int getTokenBalance(String accountId) {
        return tokenBalances.getOrDefault(accountId, 0);
    }

    public void clearTestTokens(String accountId) {
        tokenBalances.remove(accountId);
        recentRequests.remove(accountId);
    }

    // Internal check that enforces idempotency and token balance
    private synchronized boolean checkAndReserveTokens(String accountId, int cost, String requestId) {
        if (dryRun) {
            log.debug("Dry run enabled - skipping token consumption for account={} requestId={}", accountId, requestId);
            return true;
        }

        if (accountId == null || accountId.isEmpty()) {
            log.warn("No accountId provided - proceeding without token checks");
            return true;
        }

        // idempotency
        Set<String> seen = recentRequests.computeIfAbsent(accountId, k -> ConcurrentHashMap.newKeySet());
        if (requestId != null && !requestId.isEmpty()) {
            if (seen.contains(requestId)) {
                log.info("Duplicate request detected for account={} requestId={}", accountId, requestId);
                return false; // duplicate
            }
        }

        int balance = tokenBalances.getOrDefault(accountId, 0);
        if (balance < cost) {
            log.info("Insufficient tokens for account={} required={} available={}", accountId, cost, balance);
            return false;
        }

        // Reserve tokens and record request id
        tokenBalances.put(accountId, balance - cost);
        if (requestId != null && !requestId.isEmpty()) seen.add(requestId);
        log.info("Reserved {} tokens for account={} requestId={} remaining={}", cost, accountId, requestId, tokenBalances.get(accountId));
        return true;
    }

    /**
     * Verify South African ID for individual users
     * @param idNumber South African ID number
     * @param firstName First name
     * @param lastName Last name
     * @return VerificationResult containing success status and response data
     */
    public VerificationResult verifySouthAfricanId(String idNumber, String firstName, String lastName) {
        // Keep original behaviour for callers that don't pass an account id / request id
        return verifySouthAfricanIdForAccount(null, null, idNumber, firstName, lastName);
    }

    /**
     * Account-aware version which will attempt to consume tokens and block duplicates.
     * requestId is optional but recommended to enforce idempotency from callers.
     */
    public VerificationResult verifySouthAfricanIdForAccount(String accountId, String requestId, String idNumber, String firstName, String lastName) {
        // Enforce costs and idempotency
        if (!checkAndReserveTokens(accountId, COST_SAID, requestId)) {
            return new VerificationResult(false, null, "INSUFFICIENT_TOKENS_OR_DUPLICATE");
        }

        if (dryRun) {
            // Return a dummy successful response for testing without consuming real tokens
            JsonNode fake = objectMapper.createObjectNode();
            return new VerificationResult(true, "DRY_RUN", fake);
        }

        String url = baseUrl + "/said_verification";

        HttpHeaders headers = createHeaders();
        
        Map<String, String> body = new HashMap<>();
        body.put("id_number", idNumber);
        body.put("first_name", firstName);
        body.put("last_name", lastName);

        try {
            String jsonBody = objectMapper.writeValueAsString(body);
            HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);
            
            log.info("Calling Omnicheck SAID verification for ID: {} (account={})", idNumber, accountId);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode responseJson = objectMapper.readTree(response.getBody());
                boolean success = isSuccessResponse(responseJson);
                
                log.info("Omnicheck SAID verification response: success={}", success);
                return new VerificationResult(success, response.getBody(), responseJson);
            }
            
            return new VerificationResult(false, response.getBody(), (JsonNode) null);
        } catch (RestClientException e) {
            log.error("Error calling Omnicheck SAID verification API", e);
            return new VerificationResult(false, null, "API_ERROR: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during SAID verification", e);
            return new VerificationResult(false, null, "ERROR: " + e.getMessage());
        }
    }

    /**
     * Match company name in CIPC database (first step of company verification)
     * @param companyName Company name to search for
     * @return CipcMatchResult containing enquiry IDs if match found
     */
    public CipcMatchResult matchCipcCompany(String companyName) {
        return matchCipcCompanyForAccount(null, null, companyName);
    }

    public CipcMatchResult matchCipcCompanyForAccount(String accountId, String requestId, String companyName) {
        if (!checkAndReserveTokens(accountId, COST_CIPC_MATCH, requestId)) {
            return new CipcMatchResult(false, null, null, null, "INSUFFICIENT_TOKENS_OR_DUPLICATE");
        }

        if (dryRun) {
            JsonNode fake = objectMapper.createObjectNode();
            return new CipcMatchResult(true, "DRY_RUN_ENQUIRY", "DRY_RUN_RESULT", "DRY_RUN", fake);
        }

        String url = baseUrl + "/cipc_company_match";

        HttpHeaders headers = createHeaders();
        
        Map<String, String> body = new HashMap<>();
        body.put("company_name", companyName);

        try {
            String jsonBody = objectMapper.writeValueAsString(body);
            HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);
            
            log.info("Calling Omnicheck CIPC company match for: {} (account={})", companyName, accountId);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode responseJson = objectMapper.readTree(response.getBody());
                
                // Extract enquiry IDs from response
                String enquiryId = responseJson.has("enquiry_id") ? responseJson.get("enquiry_id").asText() : null;
                String enquiryResultId = responseJson.has("enquiry_result_id") ? responseJson.get("enquiry_result_id").asText() : null;
                boolean success = enquiryId != null && enquiryResultId != null;
                
                log.info("Omnicheck CIPC match response: success={}, enquiryId={}", success, enquiryId);
                return new CipcMatchResult(success, enquiryId, enquiryResultId, response.getBody(), responseJson);
            }
            
            return new CipcMatchResult(false, null, null, response.getBody(), (JsonNode) null);
        } catch (RestClientException e) {
            log.error("Error calling Omnicheck CIPC company match API", e);
            return new CipcMatchResult(false, null, null, null, "API_ERROR: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during CIPC company match", e);
            return new CipcMatchResult(false, null, null, null, "ERROR: " + e.getMessage());
        }
    }

    /**
     * Get full company details from CIPC using enquiry IDs from match step
     * @param enquiryId Enquiry ID from cipc_company_match
     * @param enquiryResultId Enquiry Result ID from cipc_company_match
     * @return VerificationResult containing company details
     */
    public VerificationResult searchCipcCompany(String enquiryId, String enquiryResultId) {
        return searchCipcCompanyForAccount(null, null, enquiryId, enquiryResultId);
    }

    public VerificationResult searchCipcCompanyForAccount(String accountId, String requestId, String enquiryId, String enquiryResultId) {
        if (!checkAndReserveTokens(accountId, COST_CIPC_SEARCH, requestId)) {
            return new VerificationResult(false, null, "INSUFFICIENT_TOKENS_OR_DUPLICATE");
        }

        if (dryRun) {
            JsonNode fake = objectMapper.createObjectNode();
            return new VerificationResult(true, "DRY_RUN", fake);
        }

        String url = baseUrl + "/cipc_company_search";

        HttpHeaders headers = createHeaders();
        
        Map<String, String> body = new HashMap<>();
        body.put("enquiry_id", enquiryId);
        body.put("enquiry_result_id", enquiryResultId);

        try {
            String jsonBody = objectMapper.writeValueAsString(body);
            HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);
            
            log.info("Calling Omnicheck CIPC company search with enquiryId: {} (account={})", enquiryId, accountId);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode responseJson = objectMapper.readTree(response.getBody());
                boolean success = isSuccessResponse(responseJson);
                
                log.info("Omnicheck CIPC company search response: success={}", success);
                return new VerificationResult(success, response.getBody(), responseJson);
            }
            
            return new VerificationResult(false, response.getBody(), (JsonNode) null);
        } catch (RestClientException e) {
            log.error("Error calling Omnicheck CIPC company search API", e);
            return new VerificationResult(false, null, "API_ERROR: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during CIPC company search", e);
            return new VerificationResult(false, null, "ERROR: " + e.getMessage());
        }
    }

    /**
     * Complete company verification flow: match then search
     * @param companyName Company name to verify
     * @return VerificationResult with full company details
     */
    public VerificationResult verifyCompany(String companyName) {
        // Keep original behaviour for callers that don't pass an account id / request id
        return verifyCompanyForAccount(null, null, companyName);
    }

    /**
     * Account-aware verifyCompany which consumes tokens and enforces idempotency
     */
    public VerificationResult verifyCompanyForAccount(String accountId, String requestId, String companyName) {
        // Step 1: Match company
        CipcMatchResult matchResult = matchCipcCompanyForAccount(accountId, requestId, companyName);

        if (!matchResult.isSuccess() || matchResult.getEnquiryId() == null || matchResult.getEnquiryResultId() == null) {
            return new VerificationResult(false, matchResult.getRawResponse(),
                "Company match failed: " + (matchResult.getError() != null ? matchResult.getError() : "No match found"));
        }

        // Step 2: Get full company details
        return searchCipcCompanyForAccount(accountId, requestId, matchResult.getEnquiryId(), matchResult.getEnquiryResultId());
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private boolean isSuccessResponse(JsonNode responseJson) {
        if (responseJson == null) return false;
        
        // Check for common success indicators in Omnicheck responses
        if (responseJson.has("status")) {
            String status = responseJson.get("status").asText().toLowerCase();
            return status.contains("success") || status.contains("verified") || status.equals("200");
        }
        
        if (responseJson.has("success")) {
            return responseJson.get("success").asBoolean();
        }
        
        if (responseJson.has("verified")) {
            return responseJson.get("verified").asBoolean();
        }
        
        // If no explicit status, check for error fields
        if (responseJson.has("error") && !responseJson.get("error").asText().isEmpty()) {
            return false;
        }
        
        // Default: if we got a response with data, consider it potentially successful
        return !responseJson.isEmpty();
    }

    @Data
    public static class VerificationResult {
        private final boolean success;
        private final String rawResponse;
        private final JsonNode responseJson;
        private final String error;

        public VerificationResult(boolean success, String rawResponse, JsonNode responseJson) {
            this.success = success;
            this.rawResponse = rawResponse;
            this.responseJson = responseJson;
            this.error = null;
        }

        public VerificationResult(boolean success, String rawResponse, String error) {
            this.success = success;
            this.rawResponse = rawResponse;
            this.responseJson = null;
            this.error = error;
        }
    }

    @Data
    public static class CipcMatchResult {
        private final boolean success;
        private final String enquiryId;
        private final String enquiryResultId;
        private final String rawResponse;
        private final JsonNode responseJson;
        private final String error;

        public CipcMatchResult(boolean success, String enquiryId, String enquiryResultId, String rawResponse, JsonNode responseJson) {
            this.success = success;
            this.enquiryId = enquiryId;
            this.enquiryResultId = enquiryResultId;
            this.rawResponse = rawResponse;
            this.responseJson = responseJson;
            this.error = null;
        }

        public CipcMatchResult(boolean success, String enquiryId, String enquiryResultId, String rawResponse, String error) {
            this.success = success;
            this.enquiryId = enquiryId;
            this.enquiryResultId = enquiryResultId;
            this.rawResponse = rawResponse;
            this.responseJson = null;
            this.error = error;
        }
    }
}
