package io.github.flexitech_realtime_provider.realtime_service.services.external;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.flexitech_realtime_provider.common.api.response.ApiResponse;
import io.github.flexitech_realtime_provider.common.api.response.token.TokenResponse;
import io.github.flexitech_realtime_provider.common.constant.AuthenticationScopes;
import io.github.flexitech_realtime_provider.common.exception.service.ServiceException;
import io.github.flexitech_realtime_provider.common.exception.token.InvalidTokenException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@Slf4j
public class AuthServiceClient {
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public AuthServiceClient(WebClient.Builder webClientBuilder,
                             @Value("${external-services.auth-service.url}") String authServiceUrl,
                             ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.baseUrl(authServiceUrl).build();
        this.objectMapper = objectMapper;
    }

    public Mono<TokenResponse> validateToken(String token) {
        log.info("[NOTE: this must do before controller is called] Validating token ...");
        return webClient.get()
                .uri("/oauth2/v1/token?token={token}", token)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .onStatus(
                        HttpStatusCode::is4xxClientError,
                        response -> Mono.error(new InvalidTokenException("Invalid or expired token"))
                )
                .onStatus(
                        HttpStatusCode::is5xxServerError,
                        response -> Mono.error(new ServiceException("Auth service unavailable"))
                )
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<Object>>() {})
                .flatMap(apiResponse -> {
                    if (apiResponse.isSuccess()) {
                        return convertToTokenResponse(apiResponse.getData())
                                .flatMap(tokenResponse -> {
                                    log.info("Token is response success and checking scopes...");
                                    if (!tokenResponse.getScopes().contains(AuthenticationScopes.REALTIME_SCOPE)) {
                                        log.info("[If this happen any request will be denied]Token is not contain required scopes!");
                                        return Mono.error(new InvalidTokenException(
                                                "Missing required scope: " + AuthenticationScopes.REALTIME_SCOPE));
                                    }
                                    log.info("Token validation success!");
                                    return Mono.just(tokenResponse);
                                });
                    }
                    log.error("Error on token response: {}", apiResponse.getMessage());
                    return Mono.error(new Exception(apiResponse.getMessage()));
                });
    }

    private Mono<TokenResponse> convertToTokenResponse(Object data) {
        return Mono.fromCallable(() -> {
            return objectMapper.convertValue(data, TokenResponse.class);
        }).subscribeOn(Schedulers.boundedElastic());
    }
}