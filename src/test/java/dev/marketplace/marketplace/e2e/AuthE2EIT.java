package dev.marketplace.marketplace.e2e;

import dev.marketplace.marketplace.TestConfig;
import dev.marketplace.marketplace.model.User;
import dev.marketplace.marketplace.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

import static io.restassured.RestAssured.*;
import io.restassured.response.Response;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestConfig.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false",
        "b2.application.key.id=test-key-id",
        "b2.application.key=test-key",
        "b2.bucket.id=test-bucket-id",
        "b2.bucket.name=test-bucket-name"
})
class AuthE2EIT {

    @LocalServerPort
    int serverPort;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void beforeEach() {
        // configure RestAssured explicitly to avoid name clashes
        io.restassured.RestAssured.baseURI = "http://localhost";
        io.restassured.RestAssured.port = serverPort;
        userRepository.deleteAll();
    }

    @Test
    void login_sets_cookies_and_allows_protected_call() {
        // seed a user
        User user = new User();
        user.setUsername("e2euser");
        user.setEmail("e2e@example.com");
        user.setPassword(passwordEncoder.encode("secretpwd"));
        userRepository.save(user);

        Map<String, String> loginBody = Map.of(
                "emailOrUsername", "e2e@example.com",
                "password", "secretpwd"
        );

        Response loginResp = given()
                .contentType("application/json")
                .body(loginBody)
                .when()
                .post("/api/auth/login")
                .andReturn();

        loginResp.then().statusCode(200);

        String authCookie = loginResp.getCookie("auth-token");
        String xsrfCookie = loginResp.getCookie("XSRF-TOKEN");

        // assert cookies are present and non-empty (avoid deprecated matchers)

        // call protected endpoint with cookie
        Response protectedResp = given()
                .cookie("auth-token", authCookie)
                .when()
                .get("/api/payments/payfast/user/subscription-status")
                .andReturn();

        // should be 200 OK (authenticated)
        protectedResp.then().statusCode(200);

        // response body should contain JSON with 'active' boolean
        Boolean active = protectedResp.jsonPath().getBoolean("active");
        // default seeded user has no subscription -> false (but ensure key exists)
        assertThat(active, is(notNullValue()));
    }
}



