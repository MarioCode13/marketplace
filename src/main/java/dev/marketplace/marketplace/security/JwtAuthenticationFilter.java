package dev.marketplace.marketplace.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
// new logging import
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// new imports for fallback authentication
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // capture Authorization header early so we can tell where the token came from
        String authHeader = request.getHeader("Authorization");

        // Attempt to extract token from Authorization header, cookies, or raw Cookie header
        String jwt = extractToken(request);

        if (jwt == null || jwt.isBlank()) {
            log.debug("No JWT token found in request headers or cookies. Authorization header: {}, Cookie header: {}",
                    authHeader, request.getHeader("Cookie"));
            // continue filter chain without setting authentication
            filterChain.doFilter(request, response);
            return;
        }

        boolean tokenFromAuthHeader = false;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String hdrToken = authHeader.substring(7).trim();
            tokenFromAuthHeader = hdrToken.equals(jwt);
        }

        try {
            String email = jwtUtil.extractEmail(jwt);
            log.debug("Extracted email from token: {}", email);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = null;
                try {
                    userDetails = userDetailsService.loadUserByUsername(email);
                } catch (Exception e) {
                    // don't throw — log and continue to fallback
                    log.warn("UserDetails lookup failed for email '{}': {}", email, e.getMessage());
                }

                // Validate token first, then build auth. If UserDetails is available use it; otherwise create a lightweight Authentication
                boolean valid = false;
                try {
                    valid = jwtUtil.validateToken(jwt);
                } catch (Exception e) {
                    log.warn("Error validating JWT token: {}", e.getMessage());
                }

                if (valid) {
                    UsernamePasswordAuthenticationToken authToken;
                    if (userDetails != null) {
                        authToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                        log.debug("Authenticated using UserDetails for {}", email);
                    } else {
                        // Fallback: create lightweight principal so security context is populated and resolvers won't see anonymousUser
                        authToken = new UsernamePasswordAuthenticationToken(email, null,
                                List.of(new SimpleGrantedAuthority("ROLE_USER")));
                        log.debug("UserDetails not found; created lightweight auth for {}", email);
                    }

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    // If token came from Authorization header and there's no auth cookie, emit one so browser persists it.
                    boolean hasAuthCookie = false;
                    if (request.getCookies() != null) {
                        for (var c : request.getCookies()) {
                            if (c == null) continue;
                            String name = c.getName();
                            if (name == null) continue;
                            if ("auth-token".equalsIgnoreCase(name) || "jwt".equalsIgnoreCase(name) || "auth-token-dev".equalsIgnoreCase(name)) {
                                hasAuthCookie = true;
                                break;
                            }
                        }
                    }

                    if (tokenFromAuthHeader && !hasAuthCookie) {
                        // decide cookie attributes same as other code: don't add Secure on localhost unless request is secure
                        String host = request.getServerName();
                        boolean isLocalhost = "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host);
                        boolean isSecure = request.isSecure() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
                        boolean addSecure = isSecure && !isLocalhost;
                        String sameSite = isSecure ? "None" : "Lax";

                        StringBuilder sb = new StringBuilder();
                        sb.append("auth-token=").append(jwt);
                        sb.append("; Path=/");
                        sb.append("; HttpOnly");
                        if (addSecure) {
                            sb.append("; Secure");
                        }
                        sb.append("; SameSite=").append(sameSite);
                        // optional: set Max-Age if you want persistent cookie (omitted -> session cookie)

                        String header = sb.toString();
                        response.addHeader("Set-Cookie", header);
                        log.debug("Emitted auth cookie from JwtAuthenticationFilter: {}", header);

                        // For local dev, also emit a non-HttpOnly dev cookie and dev header so SPA can persist/send token if browser blocked HttpOnly cookie
                        if (isLocalhost) {
                            String devHeaderCookie = "auth-token-dev=" + jwt + "; Path=/; SameSite=Lax";
                            response.addHeader("Set-Cookie", devHeaderCookie);
                            response.addHeader("X-DEV-AUTH-TOKEN", jwt);
                            log.debug("Emitted dev auth cookie and header for localhost: {}", devHeaderCookie);
                        }
                    }
                } else {
                    log.debug("JWT not valid for token/email: tokenPresent={}, email={}", jwt != null, email);
                }
            }
        } catch (Exception e) {
            log.warn("Error processing JWT token: {}", e.getMessage());
            // Do not throw — continue without authenticated principal.
        }

        filterChain.doFilter(request, response);
    }

    // New helper: robust token extraction
    private String extractToken(HttpServletRequest request) {
        // 1) Authorization header
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            if (!token.isEmpty()) {
                return token;
            }
        }

        // 2) Servlet cookies (preferred)
        if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) {
                if (cookie == null) continue;
                String name = cookie.getName();
                if (name == null) continue;
                if ("auth-token".equalsIgnoreCase(name) || "jwt".equalsIgnoreCase(name) || "auth-token-dev".equalsIgnoreCase(name)) {
                    String val = cookie.getValue();
                    if (val != null && !val.isBlank()) {
                        log.debug("Token extracted from servlet cookie '{}'", name);
                        return val;
                    }
                }
            }
        }

        // 3) Raw Cookie header fallback (some clients / proxies may leave parsing inconsistent)
        String raw = request.getHeader("Cookie");
        if (raw != null && !raw.isBlank()) {
            // Cookie header format: "a=1; b=2; c=3"
            String[] parts = raw.split(";");
            for (String part : parts) {
                int idx = part.indexOf('=');
                if (idx <= 0) continue;
                String n = part.substring(0, idx).trim();
                String v = part.substring(idx + 1).trim();
                if ("auth-token".equalsIgnoreCase(n) || "jwt".equalsIgnoreCase(n) || "auth-token-dev".equalsIgnoreCase(n)) {
                    if (v != null && !v.isBlank()) {
                        log.debug("Token extracted from raw Cookie header key '{}'", n);
                        return v;
                    }
                }
            }
        }

        // 4) Optional fallback: query param "token"
        String q = request.getParameter("token");
        if (q != null && !q.isBlank()) {
            log.debug("Token extracted from request parameter 'token'");
            return q;
        }

        return null;
    }
}
