package dev.marketplace.marketplace.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.marketplace.marketplace.model.User;
import dev.marketplace.marketplace.repository.TrustRatingRepository;
import dev.marketplace.marketplace.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "OMNICHECK_DRY_RUN=true"
})
public class VerificationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TrustRatingRepository trustRatingRepository;

    @Test
    @WithMockUser(username = "testuser")
    @Transactional
    public void whenIdVerified_thenTrustRatingVerifiedIdSet() throws Exception {
        // Create a test user
        User user = new User();
        user.setEmail("test-id-verify@example.com");
        user.setPassword("password");
        user.setUsername("test-id-verify");
        user = userRepository.save(user);

        String userId = user.getId().toString();

        Map<String, String> payload = Map.of(
                "idNumber", "8001015009087",
                "firstName", "Test",
                "lastName", "User",
                "userId", userId,
                "requestId", "req-integ-1"
        );

        mockMvc.perform(post("/api/id-verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());

        var opt = trustRatingRepository.findByUserId(user.getId());
        assertThat(opt).isPresent();
        assertThat(opt.get().isVerifiedId()).isTrue();
    }
}

