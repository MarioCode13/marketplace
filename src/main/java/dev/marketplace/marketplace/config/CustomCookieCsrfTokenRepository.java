package dev.marketplace.marketplace.config;

import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;

public class CustomCookieCsrfTokenRepository implements CsrfTokenRepository {
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
        if (token == null) {
            Cookie delete = new Cookie("XSRF-TOKEN", "");
            delete.setPath("/");
            delete.setHttpOnly(false);
            delete.setSecure(false);
            delete.setMaxAge(0); // delete immediately
            response.addCookie(delete);
            System.out.println("DEBUG: Clearing XSRF-TOKEN cookie (token was null)");
            return;
        }

        // Create the cookie manually instead of using the delegate
        Cookie cookie = new Cookie("XSRF-TOKEN", token.getToken());
        cookie.setPath("/");
        cookie.setHttpOnly(false); // Allow JavaScript to read it
        cookie.setSecure(false); // For local development
        // Don't set SameSite here - let the browser handle it
        
        response.addCookie(cookie);
        System.out.println("DEBUG: Setting XSRF-TOKEN cookie manually: " + token.getToken());
    }

    @Override
    public CsrfToken loadToken(HttpServletRequest request) {
        return delegate.loadToken(request);
    }
}
