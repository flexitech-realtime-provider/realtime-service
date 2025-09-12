package io.github.flexitech_realtime_provider.realtime_service.config;

import io.github.flexitech_realtime_provider.common.constant.AuthenticationScopes;
import io.github.flexitech_realtime_provider.realtime_service.security.JwtAuthFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private static final String SCOPE_PREFIX = "SCOPE_";
    private final JwtAuthFilter jwtAuthFilter;

    private final Logger log = LogManager.getLogger(getClass());

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    @Order(1)
    public SecurityWebFilterChain webSocketFilterChain(ServerHttpSecurity http) {
        return http
                .securityMatcher(ServerWebExchangeMatchers.pathMatchers("/ws/realtime/**"))
                .authorizeExchange(ex -> ex.anyExchange().permitAll())
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .build();
    }

    @Bean
    @Order(2)
    public SecurityWebFilterChain publicApiFilterChain(ServerHttpSecurity http) {
        return http
                .securityMatcher(
                        // List all public endpoints explicitly
                        ServerWebExchangeMatchers.pathMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**",
                                "/actuator/health",
                                "/ws/**"
                        )
                )
                .authorizeExchange(ex -> ex.anyExchange().permitAll())
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .build();
    }

    // Secured APIs (require authentication)
    @Bean
    @Order(3)
    public SecurityWebFilterChain securedApiFilterChain(ServerHttpSecurity http, JwtAuthFilter jwtAuthFilter) {
        return http
                .authorizeExchange(ex -> ex
                        .pathMatchers("/api/v1/realtime/**")
                        .hasAuthority(SCOPE_PREFIX + AuthenticationScopes.REALTIME_SCOPE)
                        .anyExchange().authenticated()
                )
                .exceptionHandling(exception ->
                        exception.authenticationEntryPoint((exchange, ex) -> {
                            ServerHttpResponse response = exchange.getResponse();
                            if(!response.isCommitted()){
                                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                                response.getHeaders().add("WWW-Authenticate", "Bearer");
                                response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                                String errorBody = "{\"status\":\"error\",\"message\":\"Authentication required\"}";
                                DataBuffer buffer = response.bufferFactory().wrap(errorBody.getBytes());
                                return response.writeWith(Mono.just(buffer));
                            }
                            return Mono.empty();
                        })
                )
                .addFilterAt(jwtAuthFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Allow your Vue.js frontend origin
        configuration.setAllowedOrigins(
                List.of("http://localhost:5173"));

        // Allow all headers
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization", "Cache-Control", "Content-Type"
        ));

        // Allow all HTTP methods
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        // Allow credentials (cookies, authorization headers)
        configuration.setAllowCredentials(true);

        // Allow exposed headers
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization", "Content-Type"
        ));

        // Set max age
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}