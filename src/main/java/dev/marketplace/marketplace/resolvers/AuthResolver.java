package dev.marketplace.marketplace.resolvers;

import dev.marketplace.marketplace.dto.AuthResponseDto;
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
    public AuthResponseDto register(@Argument String username, @Argument String email, @Argument String password) {
        User user = userService.registerUser(username, email, password);

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name(),  user.getId());

        return new AuthResponseDto(token, user.getEmail(), user.getRole().name(), user.getId());
    }

    @MutationMapping
    public AuthResponseDto login(@Argument String emailOrUsername, @Argument String password) {
        Optional<User> userOpt = userService.authenticateUser(emailOrUsername, password);

        if (userOpt.isEmpty()) {
            throw new BadCredentialsException("Invalid credentials");
        }

        User user = userOpt.get();
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name(),  user.getId());

        // Return userId along with other information
        return new AuthResponseDto(token, user.getEmail(), user.getRole().name(), user.getId());
    }


    public record AuthResponse(String token) {}
}
