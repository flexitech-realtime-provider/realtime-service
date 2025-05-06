package io.github.flexitech_realtime_provider.realtime_service.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.flexitech_realtime_provider.common.api.response.ApiResponse;
import io.github.flexitech_realtime_provider.common.api.response.token.TokenResponse;
import io.github.flexitech_realtime_provider.common.constant.AuthenticationScopes;
import io.github.flexitech_realtime_provider.common.dtos.user.ClientDetailDTO;
import io.github.flexitech_realtime_provider.common.exception.token.InvalidTokenException;
import io.github.flexitech_realtime_provider.common.exception.token.UnauthorizedServiceException;
import io.github.flexitech_realtime_provider.common.utils.jwt.JwtUtils;
import io.github.flexitech_realtime_provider.realtime_service.clients.external.AuthServiceClient;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {
    private static final String SCOPE_PREFIX = "SCOPE_";

    private final JwtUtils jwtUtils;

    @Autowired
    private AuthServiceClient authServiceClient;

    public JwtAuthFilter(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String token = extractToken(request);
            if (token != null && jwtUtils.validateToken(token)) {
                ResponseEntity<ApiResponse<Object>> authResponse = authServiceClient.getTokenInfo(token);

                if (authResponse.getStatusCode().is2xxSuccessful() && authResponse.getBody() != null && authResponse.getBody().isSuccess()) {
                    String json = new ObjectMapper().writeValueAsString(authResponse.getBody().getData());
                    TokenResponse tokenResponse = TokenResponse.fromJson(json);
                    if(tokenResponse.getScopes().contains(AuthenticationScopes.REALTIME_SCOPE)){
                        Authentication auth = createAuthentication(token);
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }else{
                        throw new UnauthorizedServiceException("Not enough permission to access this resource.");
                    }
                } else {
                    throw new RuntimeException("Token validation failed in auth service");
                }

            }/*else{
                throw new InvalidTokenException("Invalid token!");
            }*/
        } catch (Exception e) {
            log.error("Error on filter chain: {}", ExceptionUtils.getStackTrace(e));
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT: " + e.getMessage());
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    private Authentication createAuthentication(String token) {
        String username = jwtUtils.extractUsername(token);
        List<String> scopes = List.of(jwtUtils.extractScopes(token).split(","));

        log.info("Token scopes: {}", scopes);

        List<SimpleGrantedAuthority> authorities = scopes.stream()
                .map(scope -> new SimpleGrantedAuthority(SCOPE_PREFIX + scope))
                .collect(Collectors.toList());
        System.out.println("authority: " + authorities);
        return new UsernamePasswordAuthenticationToken(
                new ClientDetailDTO(username, scopes, token),
                null,
                authorities
        );
    }
}
