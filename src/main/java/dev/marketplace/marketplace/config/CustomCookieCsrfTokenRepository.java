package dev.marketplace.marketplace.config;

import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomCookieCsrfTokenRepository implements CsrfTokenRepository {
    private static final Logger log = LoggerFactory.getLogger(CustomCookieCsrfTokenRepository.class);

    private final CookieCsrfTokenRepository delegate;

    public CustomCookieCsrfTokenRepository() {
        this.delegate = CookieCsrfTokenRepository.withHttpOnlyFalse();
    }

    @Override
    public CsrfToken generateToken(HttpServletRequest request) {
        return delegate.generateToken(request);
    }

    @Override
    public void saveToken(CsrfToken token, HttpServletRequest request, HttpServletResponse response) {
        // If token is null, clear the cookie to avoid NPEs and remove stale tokens
        String name = "XSRF-TOKEN";
        boolean isSecure = request.isSecure() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));

        // Treat localhost & 127.0.0.1 as development contexts
        String host = request.getServerName();
        boolean isLocalhost = "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host);

        // Only actually add Secure when the request is secure AND not localhost.
        boolean addSecure = isSecure && !isLocalhost;

        // Use SameSite=None for secure requests or localhost (previous behaviour).
        // Note: this may be rejected by some browsers if SameSite=None without Secure, but this mirrors prior behaviour.
        String sameSite = (isSecure || isLocalhost) ? "None" : "Lax";

        // Diagnostic logging
        log.debug("CSRF saveToken called - host: {}, remoteAddr: {}, isSecureHeader: {}, request.isSecure: {}, isLocalhost: {}, addSecure: {}, sameSite: {}",
                host,
                request.getRemoteAddr(),
                request.getHeader("X-Forwarded-Proto"),
                request.isSecure(),
                isLocalhost,
                addSecure,
                sameSite
        );

        if (token == null) {
            String header = name + "=; Path=/; Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT"
                    + (addSecure ? "; Secure" : "") + "; SameSite=" + sameSite;
            response.addHeader("Set-Cookie", header);
            log.debug("Cleared CSRF cookie (header): {}", header);
            return;
        }

        // Build Set-Cookie header manually so we can include SameSite and conditional Secure.
        String encoded = URLEncoder.encode(token.getToken(), StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("=").append(encoded);
        sb.append("; Path=/");
        // Do not set HttpOnly so client-side JS (e.g. SPA) can read XSRF-TOKEN
        if (addSecure) {
            sb.append("; Secure");
        }
        // Session cookie (no Max-Age) — adjust if you want persistent cookies
        sb.append("; SameSite=").append(sameSite);

        String cookieHeader = sb.toString();
        response.addHeader("Set-Cookie", cookieHeader);
        log.debug("Set CSRF cookie (header): {}", cookieHeader);
        log.debug("CSRF cookie header produced - {}", cookieHeader);
    }

    @Override
    public CsrfToken loadToken(HttpServletRequest request) {
        return delegate.loadToken(request);
    }
}
