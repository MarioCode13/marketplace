package dev.marketplace.marketplace.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class CsrfSameSiteCookieFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        chain.doFilter(request, response);
        if (response instanceof HttpServletResponse resp) {
            var cookies = resp.getHeaders("Set-Cookie");
            if (!cookies.isEmpty()) {
                resp.setHeader("Set-Cookie", null); // Remove all existing Set-Cookie headers
                for (String header : cookies) {
                    if (header.startsWith("XSRF-TOKEN")) {
                        resp.addHeader("Set-Cookie", header + "; SameSite=Lax");
                    } else {
                        resp.addHeader("Set-Cookie", header);
                    }
                }
            }
        }
    }
}
