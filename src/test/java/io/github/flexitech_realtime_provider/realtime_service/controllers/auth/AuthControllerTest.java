package io.github.flexitech_realtime_provider.realtime_service.controllers.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.flexitech_realtime_provider.common.dtos.user.ClientDetailDTO;
import io.github.flexitech_realtime_provider.realtime_service.config.SecurityConfig;
import io.github.flexitech_realtime_provider.realtime_service.repositories.collection.CollectionRepository;
import io.github.flexitech_realtime_provider.realtime_service.security.JwtAuthFilter;
import io.github.flexitech_realtime_provider.realtime_service.services.external.AuthServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.List;

import static io.github.flexitech_realtime_provider.common.constant.AuthenticationScopes.REALTIME_SCOPE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = AuthController.class)
@Import({SecurityConfig.class, AuthControllerTest.TestConfig.class})
public class AuthControllerTest {

    private static final String SCOPE_PREFIX = "SCOPE_";

    @Autowired
    private WebTestClient webClient;

    @Autowired
    private AuthServiceClient authServiceClient;

    @Autowired
    @Qualifier("testAuthenticationManager")
    private ReactiveAuthenticationManager testAuthManager;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public CollectionRepository documentRepository() {
            return Mockito.mock(CollectionRepository.class);
        }

        @Bean
        public JwtAuthFilter jwtAuthFilterTest() { // Only one @Bean method with this name
            return new JwtAuthFilter(testAuthenticationManager());
        }


        /*@Bean
        public DocumentServiceImpl documentService(DocumentRepository documentRepo) {
            return new DocumentServiceImpl(documentRepo);
        }*/

        @Bean("testAuthenticationManager")
        public ReactiveAuthenticationManager testAuthenticationManager() {
            return Mockito.mock(ReactiveAuthenticationManager.class);
        }

        @Bean
        public ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        public AuthServiceClient authServiceClient() {
            return Mockito.mock(AuthServiceClient.class);
        }

        @Bean
        public JwtAuthFilter jwtAuthFilter(@Qualifier("testAuthenticationManager") ReactiveAuthenticationManager authManager) {
            return new JwtAuthFilter(authManager);
        }
    }

    private final ClientDetailDTO mockUser = new ClientDetailDTO(
            "testUser",
            "user123",
            List.of(REALTIME_SCOPE),
            ""
    );

    @BeforeEach
    void setup() {
        // Mock authentication manager behavior
        when(testAuthManager.authenticate(any()))
                .thenReturn(Mono.just(new UsernamePasswordAuthenticationToken(
                        mockUser,
                        null,
                        List.of(new SimpleGrantedAuthority(SCOPE_PREFIX + REALTIME_SCOPE))
                )));
    }

    @Test
    void getAuthorizedInfo_authenticated_shouldReturnUserInfo() {
        webClient.mutateWith(SecurityMockServerConfigurers.mockAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                mockUser,
                                null,
                                List.of(new SimpleGrantedAuthority(SCOPE_PREFIX + REALTIME_SCOPE)
                                )))
                )
                .get().uri("/api/v1/realtime/authorized-info")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.username").isEqualTo("testUser")
                .jsonPath("$.data.scopes[0]").isEqualTo(REALTIME_SCOPE);
    }

    @Test
    void getAuthorizedInfo_unauthenticated_shouldReturnUnauthorized() {
        webClient.get().uri("/api/v1/realtime/authorized-info")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Authentication required");
    }

    @Test
    void getAuthorizedInfo_insufficientScopes_shouldReturnForbidden() {
        // Create authentication without required scope
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                mockUser,
                null,
                List.of(new SimpleGrantedAuthority("SCOPE_OTHER_SCOPE"))
        );

        webClient.mutateWith(SecurityMockServerConfigurers.mockAuthentication(authentication))
                .get().uri("/api/v1/realtime/authorized-info")
                .exchange()
                .expectStatus().isForbidden();
    }

}
