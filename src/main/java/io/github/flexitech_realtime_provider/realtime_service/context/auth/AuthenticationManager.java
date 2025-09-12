package io.github.flexitech_realtime_provider.realtime_service.context.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.flexitech_realtime_provider.common.api.response.ApiResponse;
import io.github.flexitech_realtime_provider.common.api.response.token.TokenResponse;
import io.github.flexitech_realtime_provider.common.constant.AuthenticationScopes;
import io.github.flexitech_realtime_provider.common.dtos.user.ClientDetailDTO;
import io.github.flexitech_realtime_provider.common.exception.service.ServiceException;
import io.github.flexitech_realtime_provider.common.exception.token.InvalidTokenException;
import io.github.flexitech_realtime_provider.common.utils.generators.TokenUtils;
import io.github.flexitech_realtime_provider.common.utils.jwt.JwtUtils;
import io.github.flexitech_realtime_provider.realtime_service.services.external.AuthServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AuthenticationManager implements ReactiveAuthenticationManager {
    private static final String SCOPE_PREFIX = "SCOPE_";
    private final AuthServiceClient authServiceClient;
    private final JwtUtils jwtUtils;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        return Mono.just(authentication)
                .filter(auth -> auth instanceof PreAuthenticatedAuthenticationToken)
                .cast(PreAuthenticatedAuthenticationToken.class)
                .map(PreAuthenticatedAuthenticationToken::getPrincipal)
                .cast(String.class)
                .flatMap(this::validateToken)
                .onErrorResume(ex -> Mono.error(new BadCredentialsException("Invalid token", ex)));
    }

    private Mono<Authentication> validateToken(String token) {
        return authServiceClient.validateToken(token)
                .flatMap(tokenResponse -> {
                    // First check if the token has the required scope
                    if (!tokenResponse.getScopes().contains(AuthenticationScopes.REALTIME_SCOPE)) {
                        return Mono.error(new InsufficientAuthenticationException("Missing required scope: " + AuthenticationScopes.REALTIME_SCOPE));
                    }

                    // Then create the authentication object
                    return createAuthentication(token, tokenResponse);
                })
                .onErrorResume(ex -> {
                    // Handle specific exceptions from the auth service client
                    if (ex instanceof InvalidTokenException) {
                        return Mono.error(new BadCredentialsException("Invalid token", ex));
                    }
                    if (ex instanceof ServiceException) {
                        return Mono.error(new AuthenticationServiceException("Authentication service error", ex));
                    }
                    return Mono.error(new AuthenticationServiceException("Authentication failed", ex));
                });
    }

    private Mono<TokenResponse> parseTokenResponse(ApiResponse<TokenResponse> apiResponse) {
        return Mono.fromCallable(() ->
                objectMapper.convertValue(apiResponse.getData(), TokenResponse.class)
        ).onErrorMap(ex -> new AuthenticationServiceException("Failed to parse token response", ex));
    }

    private Mono<Authentication> createAuthentication(String token, TokenResponse tokenResponse) {
        return Mono.fromCallable(() -> {
            String username = jwtUtils.extractUsername(token);
            String userId = jwtUtils.extractClaim(token, claims ->
                    claims.get(TokenUtils.USER_ID, String.class)
            );
            String moduleId = jwtUtils.extractClaim(token, claims-> claims.get(TokenUtils.MODULE_ID, String.class));
            List<String> scopes = List.of(jwtUtils.extractScopes(token).split(","));

            List<GrantedAuthority> authorities = scopes.stream()
                    .map(scope -> new SimpleGrantedAuthority(SCOPE_PREFIX + scope))
                    .collect(Collectors.toList());


            return new UsernamePasswordAuthenticationToken(
                    new ClientDetailDTO(username, userId, scopes, moduleId), // principal
                    null, // credentials (no password)
                    authorities // authorities list
            );
        });
    }
}
