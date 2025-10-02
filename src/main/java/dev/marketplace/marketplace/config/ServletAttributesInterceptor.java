package dev.marketplace.marketplace.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class ServletAttributesInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute("jakarta.servlet.http.HttpServletRequest", request);
        request.setAttribute("jakarta.servlet.http.HttpServletResponse", response);
        return true;
    }
}

