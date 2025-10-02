package dev.marketplace.marketplace.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import reactor.core.publisher.Mono;

import java.util.Map;

@Configuration
public class GraphQLServletContextConfig implements WebGraphQlInterceptor {
    @Override
    public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
        HttpServletRequest servletRequest = (HttpServletRequest) request.getAttributes().get("jakarta.servlet.http.HttpServletRequest");
        HttpServletResponse servletResponse = (HttpServletResponse) request.getAttributes().get("jakarta.servlet.http.HttpServletResponse");
        if (servletRequest != null && servletResponse != null) {
            request.configureExecutionInput((executionInput, builder) ->
                builder.context(Map.of(
                    "jakarta.servlet.http.HttpServletRequest", servletRequest,
                    "jakarta.servlet.http.HttpServletResponse", servletResponse
                )).build()
            );
        }
        return chain.next(request);
    }
}
