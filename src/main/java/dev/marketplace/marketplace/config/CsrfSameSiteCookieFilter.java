package dev.marketplace.marketplace.config;

import jakarta.servlet.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.util.Collection;
import java.net.URL;
import java.io.File; // added
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;
import org.springframework.core.Ordered;
import jakarta.annotation.PostConstruct;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CsrfSameSiteCookieFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(CsrfSameSiteCookieFilter.class);

    // add a one-time flag to emit diagnostic about profile/config files
    private volatile boolean startupChecksDone = false;

    @PostConstruct
    private void initDiagnostic() {
        // Emit diagnostic at bean initialization so it appears during startup logs
        try {
            String envProfile = System.getenv("SPRING_PROFILES_ACTIVE");
            String sysProfile = System.getProperty("spring.profiles.active");
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            URL devProps = cl.getResource("application-dev.properties");
            URL devYaml = cl.getResource("application-dev.yml");

            // Check filesystem locations (project root and src/main/resources)
            File fsRootYaml = new File("./application-dev.yml");
            File resourcesYaml = new File("src/main/resources/application-dev.yml");

            log.info("Config diagnostic (startup): SPRING_PROFILES_ACTIVE(env)='{}', spring.profiles.active(sys)='{}'", envProfile, sysProfile);
            log.info("Config diagnostic (startup): application-dev.properties on classpath: {}", devProps != null ? devProps.toString() : "NOT FOUND");
            log.info("Config diagnostic (startup): application-dev.yml on classpath: {}", devYaml != null ? devYaml.toString() : "NOT FOUND");
            log.info("Config diagnostic (startup): application-dev.yml on filesystem (project root): {}", fsRootYaml.exists() ? fsRootYaml.getAbsolutePath() : "NOT FOUND");
            log.info("Config diagnostic (startup): application-dev.yml on filesystem (src/main/resources): {}", resourcesYaml.exists() ? resourcesYaml.getAbsolutePath() : "NOT FOUND");
        } catch (Exception e) {
            log.warn("Config diagnostic startup check failed: {}", e.getMessage());
        } finally {
            startupChecksDone = true;
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        // One-time diagnostic in doFilter retained but will no-op if already done at init
        if (!startupChecksDone) {
            synchronized (this) {
                if (!startupChecksDone) {
                    try {
                        // duplicate diagnostic (kept) but will usually no-op because initDiagnostic sets the flag
                        String envProfile = System.getenv("SPRING_PROFILES_ACTIVE");
                        String sysProfile = System.getProperty("spring.profiles.active");
                        ClassLoader cl = Thread.currentThread().getContextClassLoader();
                        URL devProps = cl.getResource("application-dev.properties");
                        URL devYaml = cl.getResource("application-dev.yml");
                        File fsRootYaml = new File("./application-dev.yml");
                        File resourcesYaml = new File("src/main/resources/application-dev.yml");
                        log.info("Config diagnostic: SPRING_PROFILES_ACTIVE(env)='{}', spring.profiles.active(sys)='{}'", envProfile, sysProfile);
                        log.info("Config diagnostic: application-dev.properties on classpath: {}", devProps != null ? devProps.toString() : "NOT FOUND");
                        log.info("Config diagnostic: application-dev.yml on classpath: {}", devYaml != null ? devYaml.toString() : "NOT FOUND");
                        log.info("Config diagnostic: application-dev.yml on filesystem (project root): {}", fsRootYaml.exists() ? fsRootYaml.getAbsolutePath() : "NOT FOUND");
                        log.info("Config diagnostic: application-dev.yml on filesystem (src/main/resources): {}", resourcesYaml.exists() ? resourcesYaml.getAbsolutePath() : "NOT FOUND");
                    } catch (Exception e) {
                        log.warn("Config diagnostic check failed: {}", e.getMessage());
                    } finally {
                        startupChecksDone = true;
                    }
                }
            }
        }

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String host = req.getServerName();
        boolean isLocalhost = "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host);
        boolean isSecure = req.isSecure() || "https".equalsIgnoreCase(req.getHeader("X-Forwarded-Proto"));
        // Only actually add Secure when request is secure AND not localhost
        boolean addSecure = isSecure && !isLocalhost;
        // Use SameSite=None only when cookie will also be Secure (addSecure=true).
        // For localhost or non-secure requests use Lax so browsers accept the cookie in dev.
        String sameSite = addSecure ? "None" : "Lax";

        log.debug("CsrfSameSiteCookieFilter - host: {}, isLocalhost: {}, isSecure: {}, addSecure: {}, sameSite: {}",
                host, isLocalhost, isSecure, addSecure, sameSite);

        // Log incoming request cookies for diagnostics and detect missing auth cookie(s)
        boolean hasAuthToken = false;
        boolean hasJwt = false;
        if (req.getCookies() != null) {
            for (Cookie c : req.getCookies()) {
                log.debug("Incoming request cookie: name='{}' value='{}' httpOnly={} secure={} path={}", c.getName(), c.getValue(), c.isHttpOnly(), c.getSecure(), c.getPath());
                if ("auth-token".equalsIgnoreCase(c.getName())) hasAuthToken = true;
                if ("jwt".equalsIgnoreCase(c.getName())) hasJwt = true;
            }
        } else {
            log.debug("No incoming request cookies from servlet parser");
        }

        // Also log raw Cookie header (useful when servlet didn't populate getCookies())
        String rawCookieHeader = req.getHeader("Cookie");
        if (rawCookieHeader != null) {
            log.debug("Raw Cookie header: {}", rawCookieHeader);
        } else {
            log.debug("No raw Cookie header present");
        }

        if (!hasAuthToken && !hasJwt) {
            log.warn("No auth cookie present on request (neither 'auth-token' nor 'jwt'). Authentication may remain anonymous.");
        }

        HttpServletResponseWrapper wrapped = new HttpServletResponseWrapper(res) {
            @Override
            public void addHeader(String name, String value) {
                if ("Set-Cookie".equalsIgnoreCase(name) && value != null) {
                    String original = value;
                    String processed = value;

                    // If localhost, remove any Secure flag and Domain attribute that would prevent storing
                    if (isLocalhost) {
                        processed = processed.replaceAll("(?i);\\s*secure", "");
                        processed = processed.replaceAll("(?i);\\s*domain=[^;]*", "");
                    }

                    String lower = processed.toLowerCase();
                    if (!lower.contains("samesite")) {
                        StringBuilder sb = new StringBuilder(processed);
                        if (addSecure && !lower.contains("secure")) {
                            sb.append("; Secure");
                        }
                        sb.append("; SameSite=").append(sameSite);
                        processed = sb.toString();
                    }

                    if (!original.equals(processed)) {
                        log.debug("Modified Set-Cookie header for {}: '{}' -> '{}'", name, original, processed);
                    } else {
                        log.debug("Passing Set-Cookie header unchanged for {}: '{}'", name, original);
                    }
                    super.addHeader(name, processed);
                    return;
                }
                super.addHeader(name, value);
            }

            @Override
            public void setHeader(String name, String value) {
                if ("Set-Cookie".equalsIgnoreCase(name) && value != null) {
                    String original = value;
                    String processed = value;

                    if (isLocalhost) {
                        processed = processed.replaceAll("(?i);\\s*secure", "");
                        processed = processed.replaceAll("(?i);\\s*domain=[^;]*", "");
                    }

                    String lower = processed.toLowerCase();
                    if (!lower.contains("samesite")) {
                        StringBuilder sb = new StringBuilder(processed);
                        if (addSecure && !lower.contains("secure")) {
                            sb.append("; Secure");
                        }
                        sb.append("; SameSite=").append(sameSite);
                        processed = sb.toString();
                    }

                    if (!original.equals(processed)) {
                        log.debug("Modified Set-Cookie header for {} via setHeader: '{}' -> '{}'", name, original, processed);
                    } else {
                        log.debug("setHeader passing Set-Cookie unchanged for {}: '{}'", name, original);
                    }
                    super.setHeader(name, processed);
                    return;
                }
                super.setHeader(name, value);
            }

            @Override
            public void addCookie(Cookie cookie) {
                // Intercept cookies created via response.addCookie(...) and emit a Set-Cookie header we control.
                StringBuilder sb = new StringBuilder();
                sb.append(cookie.getName()).append("=").append(cookie.getValue() == null ? "" : cookie.getValue());

                // Path
                String path = cookie.getPath() == null ? "/" : cookie.getPath();
                sb.append("; Path=").append(path);

                // HttpOnly
                if (cookie.isHttpOnly()) {
                    sb.append("; HttpOnly");
                }

                // Secure decision: only add when request is secure and not localhost
                if (addSecure) {
                    sb.append("; Secure");
                }

                // Max-Age / Expires
                if (cookie.getMaxAge() > 0) {
                    sb.append("; Max-Age=").append(cookie.getMaxAge());
                } else if (cookie.getMaxAge() == 0) {
                    // explicit deletion
                    sb.append("; Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT");
                }

                // SameSite — ensure present
                sb.append("; SameSite=").append(sameSite);

                String header = sb.toString();
                log.debug("addCookie intercepted - name='{}' original cookie: httpOnly={}, secure={}, maxAge={} -> emitted header='{}'",
                        cookie.getName(), cookie.isHttpOnly(), cookie.getSecure(), cookie.getMaxAge(), header);

                super.addHeader("Set-Cookie", header);
            }
        };

        // Proceed with wrapped response so all subsequent Set-Cookie headers are normalized.
        chain.doFilter(req, wrapped);

        // After processing, log all Set-Cookie headers that will be sent (diagnostic, dev-only)
        Collection<String> setCookies = res.getHeaders("Set-Cookie");
        if (setCookies != null && !setCookies.isEmpty()) {
            for (String sc : setCookies) {
                log.debug("Final outgoing Set-Cookie: {}", sc);
            }
        } else {
            log.debug("No Set-Cookie headers present in response after filter");
        }
    }
}
