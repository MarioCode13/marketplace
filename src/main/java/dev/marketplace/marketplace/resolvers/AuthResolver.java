package dev.marketplace.marketplace.resolvers;

import dev.marketplace.marketplace.model.User;
import dev.marketplace.marketplace.security.JwtUtil;
import dev.marketplace.marketplace.service.UserService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Controller;

import java.util.Optional;

@Controller
public class AuthResolver {
    private final UserService userService;
    private final JwtUtil jwtUtil;

    public AuthResolver(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @MutationMapping
    public String register(@Argument String username, @Argument String email, @Argument String password) {
        userService.registerUser(username, email, password);
        return "User registered successfully";
    }

    @MutationMapping
    public String login(@Argument String emailOrUsername, @Argument String password) {
        Optional<User> userOpt = userService.authenticateUser(emailOrUsername, password);
        if (userOpt.isPresent()) {
            return jwtUtil.generateToken(userOpt.get().getEmail());
        }
        throw new BadCredentialsException("Invalid credentials");
    }


    public record AuthResponse(String token) {}
}
