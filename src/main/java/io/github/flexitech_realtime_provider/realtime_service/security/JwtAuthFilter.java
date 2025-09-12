package io.github.flexitech_realtime_provider.realtime_service.security;

import io.github.flexitech_realtime_provider.common.exception.service.ServiceException;
import io.github.flexitech_realtime_provider.common.exception.token.InvalidTokenException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthFilter implements WebFilter {
    private static final String SCOPE_PREFIX = "SCOPE_";

    private final ReactiveAuthenticationManager authenticationManager;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (isPublicEndpoint(exchange.getRequest())) {
            return chain.filter(exchange);
        }

        String token = extractToken(exchange.getRequest());
        if (token == null || token.isEmpty()) {
            // Handle missing token immediately before processing continues
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
            byte[] bytes = "{\"error\": \"Missing token\"}".getBytes(StandardCharsets.UTF_8);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        }

        return Mono.just(token)
                .flatMap(t -> {
                    Authentication authRequest = new PreAuthenticatedAuthenticationToken(t, null);
                    return authenticationManager.authenticate(authRequest)
                            .flatMap(authentication ->
                                    chain.filter(exchange)
                                            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication))
                            );
                })
                .onErrorResume(AuthenticationException.class, e -> {
                    // Handle authentication errors
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
                    byte[] bytes = ("{\"error\": \"" + e.getMessage() + "\"}").getBytes(StandardCharsets.UTF_8);
                    DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
                    return exchange.getResponse().writeWith(Mono.just(buffer));
                });
    }

    private Mono<Void> authenticateAndProcess(String token, ServerWebExchange exchange, WebFilterChain chain) {
        // Create authentication request object
        Authentication authenticationRequest = new PreAuthenticatedAuthenticationToken(token, null);

        return authenticationManager.authenticate(authenticationRequest)
                .flatMap(authentication -> {
                    return chain.filter(exchange)
                            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
                })
                .onErrorResume(ex -> {
                    // Convert service exceptions to proper authentication exceptions
                    if (ex instanceof InvalidTokenException) {
                        return Mono.error(new BadCredentialsException("Invalid token", ex));
                    }
                    if (ex instanceof ServiceException) {
                        return Mono.error(new AuthenticationServiceException("Authentication service error", ex));
                    }
                    return Mono.error(new AuthenticationCredentialsNotFoundException("Authentication failed", ex));
                });
    }

    private boolean isPublicEndpoint(ServerHttpRequest request) {
        String path = request.getPath().toString();
        return path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/actuator/health");
    }

    private String extractToken(ServerHttpRequest request) {
        String header = request.getHeaders().getFirst("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
